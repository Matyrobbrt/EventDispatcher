package io.github.matyrobbrt.eventdispatcher.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.slf4j.Logger;

import io.github.matyrobbrt.eventdispatcher.Event;
import io.github.matyrobbrt.eventdispatcher.EventBus;
import io.github.matyrobbrt.eventdispatcher.EventListener;
import io.github.matyrobbrt.eventdispatcher.GenericEvent;
import io.github.matyrobbrt.eventdispatcher.SubscribeEvent;
import io.github.matyrobbrt.eventdispatcher.internal.asm.ASMEventListener;
import net.jodah.typetools.TypeResolver;

@SuppressWarnings("unchecked")
public final class EventBusImpl implements EventBus {

	private final Map<Class<? extends Event>, EventDispatcher> dispatchers = Collections
			.synchronizedMap(new HashMap<>());
	private final Class<? extends Event> baseEventType;
	private final Logger logger;

	public EventBusImpl(Class<? extends Event> baseEventType, Logger logger) {
		this.baseEventType = baseEventType;
		this.logger = logger;
	}

	@Override
	public void post(Event event) {
		getDispatcher(event.getClass()).handle(event);
	}

	@Override
	public <T extends Event> void addListener(int priority, Consumer<T> consumer) {
		final var eClass = getEventClass(consumer);
		if (baseEventType.isAssignableFrom(eClass)) {
			throw new IllegalArgumentException(
					"Event class %s is not a subtype of %s!".formatted(eClass, baseEventType));
		}
		checkNotGeneric(eClass);
		addListener(eClass, e -> true, priority, e -> consumer.accept((T) e));
	}

	@Override
	public <E extends GenericEvent> void addGenericListener(int priority, Class<?> genericFilter,
			Consumer<E> consumer) {
		final var eClass = getEventClass(consumer);
		if (baseEventType.isAssignableFrom(eClass)) {
			throw new IllegalArgumentException(
					"Event class %s is not a subtype of %s!".formatted(eClass, baseEventType));
		}
		addListener(eClass, t -> t.getGenericType() == genericFilter, priority, e -> consumer.accept((E) e));
	}

	@Override
	public void register(Object object) {
		if (object instanceof Class<?> clazz) {
			registerClass(clazz);
		} else {
			registerObject(object);
		}
	}

	@Override
	public void register(Class<?> clazz) {
		registerClass(clazz);
	}

	//@formatter:off
	private void registerClass(final Class<?> clazz) {
        Arrays.stream(clazz.getDeclaredMethods())
                .filter(mthd -> Modifier.isStatic(mthd.getModifiers()) && methodCanBeListener(mthd))
                .filter(mthd -> mthd.isAnnotationPresent(SubscribeEvent.class))
                .forEach(mthd -> registerListener(mthd.getAnnotation(SubscribeEvent.class).priority(), clazz, mthd));
    }

	private void registerObject(final Object obj) {
        final HashSet<Class<?>> parents = new HashSet<>();
        collectParents(obj.getClass(), parents);
        Arrays.stream(obj.getClass().getDeclaredMethods())
                .filter(m -> !Modifier.isStatic(m.getModifiers()) && methodCanBeListener(m))
                .forEach(m -> parents.stream() // This search is for registering methods from subtypes
                        .map(c -> getActualMethod(c, m))
                        .filter(subM -> subM.isPresent() && subM.get().isAnnotationPresent(io.github.matyrobbrt.eventdispatcher.SubscribeEvent.class))
                        .findFirst()
                        .ifPresent(subM -> registerListener(m.getAnnotation(SubscribeEvent.class).priority(), obj, subM.get())));
    }
	//@formatter:on

	private void registerListener(final int priority, final Object target, final Method method) {
		Class<?>[] parameterTypes = method.getParameterTypes();
		if (parameterTypes.length != 1) {
			throw new IllegalArgumentException(
					"Method %s has @SubscribeEvent annotation. It has %s arguments, but event handler methods require a single argument."
							.formatted(method, parameterTypes.length));
		}

		Class<?> eventType = parameterTypes[0];

		if (!Event.class.isAssignableFrom(eventType)) {
			throw new IllegalArgumentException(
					"Method %s has @SubscribeEvent annotation, but takes an argument that is not a subtype of Event: %s"
							.formatted(method, eventType));
		}
		if (baseEventType != Event.class && !baseEventType.isAssignableFrom(eventType)) {
			throw new IllegalArgumentException(
					"Method %s has @SubscribeEvent annotation, but takes an argument that is not a subtype of the base type %s: %s"
							.formatted(method, baseEventType, eventType));
		}

		register(eventType, priority, target, method);
	}

	private void register(Class<?> eventType, int priority, Object target, Method method) {
		try {
			final ASMEventListener asm = new ASMEventListener(target, method,
					GenericEvent.class.isAssignableFrom(eventType));

			addListener((Class<? extends Event>) eventType, e -> true, priority, asm);
		} catch (IllegalAccessException | InstantiationException | NoSuchMethodException
				| InvocationTargetException e) {
			logger.error("Exception registering event handler {}:{}", eventType, method, e);
		}
	}

	private boolean methodCanBeListener(Method method) {
		if (Modifier.isPrivate(method.getModifiers())) {
			logger.warn(
					"Method {} is not a valid candidate for event listeners! It is private, while the permitted modifiers are: public, protected and package-private. Skipping...",
					method);
			return false;
		}
		return true;
	}

	private static void collectParents(final Class<?> clz, final Set<Class<?>> visited) {
		if (clz.getSuperclass() == null)
			return;
		collectParents(clz.getSuperclass(), visited);
		Arrays.stream(clz.getInterfaces()).forEach(i -> collectParents(i, visited));
		visited.add(clz);
	}

	private static Optional<Method> getActualMethod(final Class<?> clz, final Method in) {
		try {
			return Optional.of(clz.getDeclaredMethod(in.getName(), in.getParameterTypes()));
		} catch (NoSuchMethodException nse) {
			return Optional.empty();
		}

	}

	private <E extends Event> void addListener(Class<E> eventClass, Predicate<? super E> predicate, int priority,
			EventListener listener) {
		getDispatcher(eventClass).register(priority, new WithPredicateEventListener<>(eventClass, predicate, listener));
	}

	private static <T extends Event> Class<T> getEventClass(Consumer<T> consumer) {
		final Class<T> eventClass = (Class<T>) TypeResolver.resolveRawArgument(Consumer.class, consumer.getClass());
		if ((Class<?>) eventClass == TypeResolver.Unknown.class) {
			throw new IllegalStateException("Failed to resolve consumer event type: " + consumer.toString());
		}
		return eventClass;
	}

	private static void checkNotGeneric(final Class<? extends Event> eventType) {
		if (io.github.matyrobbrt.eventdispatcher.GenericEvent.class.isAssignableFrom(eventType)) {
			throw new IllegalArgumentException(
					"Cannot register a generic event listener with addListener, use addGenericListener");
		}
	}

	private EventDispatcher getDispatcher(Class<? extends Event> eventClass) {
		return dispatchers.computeIfAbsent(eventClass, k -> new EventDispatcher());
	}

}
