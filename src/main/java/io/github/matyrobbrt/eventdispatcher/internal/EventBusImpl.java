/*
 * This file is part of the Event Dispatcher library and is licensed under
 * the MIT license:
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
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import io.github.matyrobbrt.eventdispatcher.Event;
import io.github.matyrobbrt.eventdispatcher.EventBus;
import io.github.matyrobbrt.eventdispatcher.EventInterceptor;
import io.github.matyrobbrt.eventdispatcher.EventListener;
import io.github.matyrobbrt.eventdispatcher.GenericEvent;
import io.github.matyrobbrt.eventdispatcher.SubscribeEvent;
import io.github.matyrobbrt.eventdispatcher.internal.asm.ASMEventListener;
import net.jodah.typetools.TypeResolver;

/**
 * An implementation of {@link EventBus}.
 * 
 * @author matyrobbrt
 *
 */
@org.jetbrains.annotations.ApiStatus.Internal
@SuppressWarnings("unchecked")
public final class EventBusImpl implements EventBus {

	private final Map<Class<? extends Event>, EventDispatcher> dispatchers = Collections
			.synchronizedMap(new HashMap<>());
	private final String name;
	private final Class<? extends Event> baseEventType;
	private final Logger logger;
	private final EventInterceptor interceptor;
	private volatile boolean shutdown = false;

	EventBusImpl(String name, Class<? extends Event> baseEventType, Logger logger, EventInterceptor interceptor) {
		this.name = name;
		this.baseEventType = baseEventType;
		this.logger = logger;
		this.interceptor = interceptor;
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
	public void shutdown() {
		this.shutdown = true;
		logger.warn("EventBus {} shutting down - future events will not be posted.", getName());
	}

	@Override
	public void start() {
		this.shutdown = false;
	}

	@Override
	public boolean isShutdown() {
		return shutdown;
	}

	@Override
	public void post(Event event) {
		final var eClass = event.getClass();
		if (shutdown || !baseEventType.isAssignableFrom(eClass)) { return; }
		final var newEvent = interceptor.onEvent(this, event);
		if (newEvent != null) {
			getDispatcher(eClass).handleWithExceptionCatch(newEvent,
					(listener, t) -> interceptor.onException(this, event, t, listener));
		}
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
	public <F, E extends GenericEvent<F>> void addGenericListener(int priority, @NotNull Class<F> genericFilter,
			@NotNull Consumer<E> consumer) {
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

	@Override
	public void unregister(Class<?> clazz) {
		unregisterClass(clazz);
	}

	@Override
	public void unregister(Object object) {
		if (object instanceof Class<?> clazz) {
			unregisterClass(clazz);
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
                .flatMap(m -> parents.stream() // This search is for registering methods from subtypes
                        .map(c -> getActualMethod(c, m))
                        .filter(subM -> subM.isPresent() && subM.get().isAnnotationPresent(SubscribeEvent.class))
                        .filter(subM -> methodCanBeListener(subM.get(), warnIfWrongModifier))
                        .findFirst()
                        .stream()
                        .filter(Optional::isPresent)
                        .map(Optional::get));
	}
	
	private void registerClass(final Class<?> clazz) {
        collectMethodsFromClass(clazz, true).forEach(mthd -> registerListener(mthd.getAnnotation(SubscribeEvent.class).priority(), clazz, mthd));
    }

	private void registerObject(final Object obj) {
        collectMethodsFromObject(obj, true).forEach(m -> registerListener(m.getAnnotation(SubscribeEvent.class).priority(), obj, m));
    }
	
	private void unregisterClass(final Class<?> clazz) {
		collectMethodsFromClass(clazz, false).forEach(mthd -> unregisterListener(clazz, mthd));
	}
	
	private void unregisterObject(final Object obj) {
		collectMethodsFromObject(obj, false).forEach(m -> unregisterListener(obj, m));
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

	private void unregisterListener(final Object target, final Method method) {
		Class<?>[] parameterTypes = method.getParameterTypes();
		if (parameterTypes.length != 1)
			return;
		Class<?> eventType = parameterTypes[0];
		if (!Event.class.isAssignableFrom(eventType) || !baseEventType.isAssignableFrom(eventType))
			return;
		getDispatcher((Class<? extends Event>) eventType).unregister(l -> {
			ASMEventListener asm = null;
			if (l instanceof ASMEventListener a) {
				asm = a;
			} else if (l instanceof WithPredicateEventListener<?> p) {
				if (p.handler() instanceof ASMEventListener a) {
					asm = a;
				}
			}
			if (asm == null) { return false; }
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

	private EventDispatcher getDispatcher(Class<? extends Event> eventClass) {
		return dispatchers.computeIfAbsent(eventClass, k -> new EventDispatcher());
	}

}
