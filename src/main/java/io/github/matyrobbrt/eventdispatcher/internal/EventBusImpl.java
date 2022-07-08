/*
 * This file is part of the Event Dispatcher library and is licensed under the
 * MIT license:
 *
 * MIT License
 *
 * Copyright (c) 2022 Matyrobbrt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.matyrobbrt.eventdispatcher.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import io.github.matyrobbrt.eventdispatcher.BusRegistrable;
import io.github.matyrobbrt.eventdispatcher.Event;
import io.github.matyrobbrt.eventdispatcher.EventBus;
import io.github.matyrobbrt.eventdispatcher.EventInterceptor;
import io.github.matyrobbrt.eventdispatcher.EventListener;
import io.github.matyrobbrt.eventdispatcher.GenericEvent;
import io.github.matyrobbrt.eventdispatcher.ShutdownEvent;
import io.github.matyrobbrt.eventdispatcher.StartEvent;
import io.github.matyrobbrt.eventdispatcher.SubscribeEvent;
import io.github.matyrobbrt.eventdispatcher.internal.asm.ASMEventListener;
import io.github.matyrobbrt.eventdispatcher.util.ClassWalker;
import net.jodah.typetools.TypeResolver;

/**
 * An implementation of {@link EventBus}.
 * 
 * @author matyrobbrt
 *
 */
@org.jetbrains.annotations.ApiStatus.Internal
@SuppressWarnings("unchecked")
final class EventBusImpl implements EventBus {

	private final Map<Class<? extends Event>, EventDispatcher> dispatchers = Collections
			.synchronizedMap(new HashMap<>());
	private final String name;
	private final Class<? extends Event> baseEventType;
	private final Logger logger;
	private final EventInterceptor interceptor;
	private volatile boolean shutdown = false;
	private final boolean walksEventHierarchy;

	private final boolean async;
	private final Executor executor;

	EventBusImpl(String name, Class<? extends Event> baseEventType, Logger logger, EventInterceptor interceptor,
			boolean walksEventHierarchy, @Nullable Executor executor) {
		this.name = name;
		this.baseEventType = baseEventType;
		this.logger = logger;
		this.interceptor = interceptor;
		this.walksEventHierarchy = walksEventHierarchy;

		this.async = executor != null;
		this.executor = executor == null ? /*
											 * If no executor was specified, then run the command directly on the caller
											 * thread.
											 */
				Runnable::run : executor;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class<? extends Event> getBaseEventType() {
		return baseEventType;
	}

	@Override
	public boolean walksEventHierarchy() {
		return walksEventHierarchy;
	}

	@Override
	public boolean isAsync() {
		return async;
	}

	@Override
	public void shutdown() {
		this.shutdown = true;
		interceptor.onShutdown(this);
		getDispatcher(ShutdownEvent.class).handle(new ShutdownEvent(this));
		logger.warn("EventBus {} shutting down - future events will not be posted.", getName());
	}

	@Override
	public void start() {
		this.shutdown = false;
		interceptor.onStart(this);
		getDispatcher(StartEvent.class).handle(new StartEvent(this));
		logger.warn("EventBus {} is starting - future events will be posted.", getName());
	}

	@Override
	public boolean isShutdown() {
		return shutdown;
	}

	@Override
	public void post(Event event) {
		final Class<? extends Event> eClass = event.getClass();
		if (shutdown) { return; }
		if (walksEventHierarchy) {
			executor.execute(() -> {
				ClassWalker.range(eClass, baseEventType).forEach(eParent -> {
					if (baseEventType.isAssignableFrom(eParent)) {
						// The event handling is not split here, because async requests in this case
						// might modify the event without others knowing
						tryPostEvent((Class<? extends Event>) eParent, event);
					}
				});
			});
		} else {
			executor.execute(() -> tryPostEvent(eClass, event));
		}
	}

	private void tryPostEvent(final Class<? extends Event> eventClass, final Event event) {
		if (!eventClassIsAccepted(eventClass)) {
			logger.warn("Tried posting event '{}' of type '{}' which is not assignable to the base event type '{}'!",
					event, eventClass, baseEventType);
		} else if (!Modifier.isPublic(eventClass.getModifiers())) {
			logger.warn(
					"Tried posting event '{}' of the not public type '{}'. Please ensure that event classes are public!",
					event, eventClass);
		} else {
			final Event newEvent = interceptor.onEvent(this, event);
			if (newEvent != null) {
				getDispatcher(eventClass).handleWithExceptionCatch(newEvent,
						(listener, t) -> interceptor.onException(this, event, t, listener));
			}
		}
	}

	private boolean eventClassIsAccepted(final Class<?> eventClass) {
		return baseEventType == Event.class || baseEventType.isAssignableFrom(eventClass)
				|| eventClass == ShutdownEvent.class || eventClass == StartEvent.class;
	}

	@Override
	public void addUniversalListener(int priority, EventListener listener) {
		getDispatcher(baseEventType).register(priority, listener);
	}

	@Override
	public <T extends Event> void addListener(int priority, Consumer<T> consumer) {
		final Class<T> eClass = getEventClass(consumer);
		if (!eventClassIsAccepted(eClass)) {
			throw new IllegalArgumentException(
                    String.format("Event class %s is not a subtype of %s!", eClass, baseEventType));
		}
		checkNotGeneric(eClass);
		addListener(eClass, e -> true, priority, e -> consumer.accept((T) e));
	}

	@Override
	public <F, E extends GenericEvent<F>> void addGenericListener(int priority, @NotNull Class<F> genericFilter,
			@NotNull Consumer<E> consumer) {
		final Class<E> eClass = getEventClass(consumer);
		if (!eventClassIsAccepted(eClass)) {
			throw new IllegalArgumentException(
                    String.format("Event class %s is not a subtype of %s!", eClass, baseEventType));
		}
		addListener(eClass, t -> t.getGenericType() == genericFilter, priority, e -> consumer.accept((E) e));
	}

	@Override
	public void register(Object object) {
		if (object instanceof Class<?>) {
			registerClass((Class<?>) object);
		} else {
			registerObject(object);
		}
	}

	@Override
	public void register(Class<?> clazz) {
		registerClass(clazz);
	}

	@Override
	public void unregister(Class<?> clazz) {
		unregisterClass(clazz);
	}

	@Override
	public void unregister(Object object) {
        if (object instanceof Class<?>) {
            unregisterClass((Class<?>) object);
		} else {
			unregisterObject(object);
		}
	}

	//@formatter:off
	private Stream<Method> collectMethodsFromClass(Class<?> clazz, boolean warnIfWrongModifier) {
		return Arrays.stream(clazz.getDeclaredMethods())
                .filter(mthd -> Modifier.isStatic(mthd.getModifiers()))
                .filter(mthd -> mthd.isAnnotationPresent(SubscribeEvent.class))
                .filter(mthd -> methodCanBeListener(mthd, warnIfWrongModifier));
	}
	private Stream<Method> collectMethodsFromObject(Object obj, boolean warnIfWrongModifier) {
		final HashSet<Class<?>> parents = new HashSet<>();
        collectParents(obj.getClass(), parents);
        return Arrays.stream(obj.getClass().getDeclaredMethods())
                .filter(m -> !Modifier.isStatic(m.getModifiers()))
                .flatMap(m -> optionalToStream(parents.stream() // This search is for registering methods from subtypes
                        .map(c -> getActualMethod(c, m))
                        .filter(subM -> subM.isPresent() && subM.get().isAnnotationPresent(SubscribeEvent.class))
                        .filter(subM -> methodCanBeListener(subM.get(), warnIfWrongModifier))
                        .findFirst())
                        .filter(Optional::isPresent)
                        .map(Optional::get));
	}
	
	private static <T> Stream<T> optionalToStream(Optional<T> opt) {
	    return opt.isPresent() ? Stream.of(opt.get()) : Stream.empty();
	}
	
	private void registerClass(final Class<?> clazz) {
		if (clazz.isAnnotationPresent(BusRegistrable.ForClass.class)) {
			final BusRegistrable.ForClass ann = clazz.getAnnotation(BusRegistrable.ForClass.class);
			if (!ann.registered().isEmpty()) {
				try {
					final MethodHandle mthd = tryFindMethod(clazz, ann.registered());
					try {
						mthd.invoke(this);
					} catch (Throwable e) {
						logger.error("Exception trying to fire static registered listener for clazz '{}'", clazz);
					}
				} catch (NoSuchMethodException | IllegalAccessException e) {
					logger.error("Could not find registered listener static method in class '{}', with name '{}'", clazz, ann.registered());
				}
			}
		}
        collectMethodsFromClass(clazz, true).forEach(mthd -> registerListener(mthd.getAnnotation(SubscribeEvent.class).priority(), clazz, mthd));
    }

	private void registerObject(final Object obj) {
		if (obj instanceof BusRegistrable) {
			((BusRegistrable) obj).whenRegistered(this);
		}
        collectMethodsFromObject(obj, true).forEach(m -> registerListener(m.getAnnotation(SubscribeEvent.class).priority(), obj, m));
    }
	
	private void unregisterClass(final Class<?> clazz) {
		if (clazz.isAnnotationPresent(BusRegistrable.ForClass.class)) {
			final BusRegistrable.ForClass ann = clazz.getAnnotation(BusRegistrable.ForClass.class);
			if (!ann.unregistered().isEmpty()) {
				try {
					final MethodHandle mthd = tryFindMethod(clazz, ann.unregistered());
					try {
						mthd.invoke(this);
					} catch (Throwable e) {
						logger.error("Exception trying to fire static unregistered listener for class '{}'", clazz);
					}
				} catch (NoSuchMethodException | IllegalAccessException e) {
					logger.error("Could not find unregistered listener static method in class '{}', with name '{}'", clazz, ann.unregistered());
				}
			}
		}
		collectMethodsFromClass(clazz, false).forEach(mthd -> unregisterListener(clazz, mthd));
	}
	
	private void unregisterObject(final Object obj) {
		if (obj instanceof BusRegistrable) {
			((BusRegistrable) obj).whenUnregistered(this);
		}
		collectMethodsFromObject(obj, false).forEach(m -> unregisterListener(obj, m));
	}
	
	private static MethodHandle tryFindMethod(Class<?> clazz, String name) throws NoSuchMethodException, IllegalAccessException {
		final MethodHandles.Lookup lookup = MethodHandles.lookup();
		final MethodType descriptor = MethodType.methodType(void.class, EventBus.class);
		return lookup.findStatic(clazz, name, descriptor);
	}
	//@formatter:on

	private void registerListener(final int priority, final Object target, final Method method) {
		final Class<?>[] parameterTypes = method.getParameterTypes();
		if (parameterTypes.length != 1) {
			throw new IllegalArgumentException(
                    String.format(
                            "Method %s has @SubscribeEvent annotation. It has %s arguments, but event handler methods require a single argument.",
                            method, parameterTypes.length));
		}

		final Class<?> eventType = parameterTypes[0];

		if (!eventClassIsAccepted(eventType)) {
			throw new IllegalArgumentException(
                    String.format(
                            "Method %s has @SubscribeEvent annotation, but takes an argument that is not a subtype of the base type %s: %s",
                            method, baseEventType, eventType));
		}

		register(eventType, priority, target, method);
	}

	private void unregisterListener(final Object target, final Method method) {
		final Class<?>[] parameterTypes = method.getParameterTypes();
		if (parameterTypes.length != 1)
			return;
		final Class<?> eventType = parameterTypes[0];
		if (!eventClassIsAccepted(eventType))
			return;
		getDispatcher((Class<? extends Event>) eventType).unregister(l -> {
			ASMEventListener asm = null;
			if (l instanceof ASMEventListener) {
				asm = (ASMEventListener) l;
			} else if (l instanceof WithPredicateEventListener<?>) {
				final WithPredicateEventListener<?> p = (WithPredicateEventListener<?>) l;
				if (p.handler() instanceof ASMEventListener) {
					asm = (ASMEventListener) p.handler();
				}
			}
			if (asm == null)
				return false;
			return asm.isSame(method) && asm.isSame(target);
		});
	}

	private void register(Class<?> eventType, int priority, Object target, Method method) {
		final ASMEventListener asm = new ASMEventListener(target, method,
				GenericEvent.class.isAssignableFrom(eventType));

		addListener((Class<? extends Event>) eventType, e -> true, priority, asm);
	}

	private boolean methodCanBeListener(Method method, boolean warn) {
		if (!Modifier.isPublic(method.getModifiers())) {
			if (warn) {
				logger.warn(
						"Method {} is not a valid candidate for event listeners! Only public methods are permitted. Skipping...",
						method);
			}
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
		} catch (NoSuchMethodException e) {
			return Optional.empty();
		}

	}

	private <E extends Event> void addListener(Class<E> eventClass, Predicate<? super E> predicate, int priority,
			EventListener listener) {
		getDispatcher(eventClass).register(priority, new WithPredicateEventListener<>(eventClass, predicate, listener));
	}

	private EventDispatcher getDispatcher(Class<? extends Event> eventClass) {
		return dispatchers.computeIfAbsent(eventClass, k -> new EventDispatcher());
	}

	private static <T extends Event> Class<T> getEventClass(Consumer<T> consumer) {
		final Class<T> eventClass = (Class<T>) TypeResolver.resolveRawArgument(Consumer.class, consumer.getClass());
		if ((Class<?>) eventClass == TypeResolver.Unknown.class) {
			throw new IllegalStateException("Failed to resolve consumer event type: " + consumer.toString());
		}
		return eventClass;
	}

	private static void checkNotGeneric(final Class<? extends Event> eventType) {
		if (GenericEvent.class.isAssignableFrom(eventType)) {
			throw new IllegalArgumentException(
					"Cannot register a generic event listener with addListener, use addGenericListener");
		}
	}

}
