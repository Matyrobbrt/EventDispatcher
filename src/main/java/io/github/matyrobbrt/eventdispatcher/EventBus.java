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

package io.github.matyrobbrt.eventdispatcher;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import io.github.matyrobbrt.eventdispatcher.internal.BusBuilder;

/**
 * Base interface for event buses.
 * 
 * @author matyrobbrt
 *
 */
public interface EventBus {

	/**
	 * Posts an event to the bus. The event will be dispatched to listeners based on
	 * the priority.
	 * 
	 * @param event
	 */
	void post(@NotNull Event event);

	/**
	 * Adds an universal listener to the bus. <br>
	 * If the bus {@link #walksEventHierarchy() walks event hierarchy}, then the
	 * listener will be handled on all events. Otherwise, the listener will be fired
	 * only when an event of the <i>exact</i> {@link #getBaseEventType() base type}
	 * is posted.
	 * 
	 * @param priority the priority of the listener. <strong>Higher priority ==
	 *                 first to run</strong>
	 * @param listener the listener to add
	 */
	void addUniversalListener(int priority, @NotNull EventListener listener);

	/**
	 * Adds an universal listener to the bus, with priority 0 (neutral). <br>
	 * If the bus {@link #walksEventHierarchy() walks event hierarchy}, then the
	 * listener will be handled on all events. Otherwise, the listener will be fired
	 * only when an event of the <i>exact</i> {@link #getBaseEventType() base type}
	 * is posted.
	 * 
	 * @param listener the listener to add
	 */
	default void addUniversalListener(@NotNull EventListener listener) {
		addUniversalListener(0, listener);
	}

	/**
	 * Adds an event listener to the bus.
	 * 
	 * @param <E>      the type of the event to listen for
	 * @param priority the priority of the listener. <strong>Higher priority ==
	 *                 first to run</strong>
	 * @param consumer the event handler
	 */
	<E extends Event> void addListener(int priority, @NotNull Consumer<E> consumer);

	/**
	 * Adds an event listener to the bus, with the priority 0 (neutral).
	 * 
	 * @param <E>      the type of the event to listen for
	 * @param consumer the event handler
	 * @see            EventBus#addListener(int, Consumer)
	 */
	default <E extends Event> void addListener(@NotNull Consumer<E> consumer) {
		addListener(0, consumer);
	}

	/**
	 * Adds a generic event listener to the bus.
	 * 
	 * @param <F>           the type of the generic filter
	 * @param <E>           the type of the {@link GenericEvent} to listen for
	 * @param priority      the priority of the listener. <strong>Higher priority ==
	 *                      first to run</strong>
	 * @param genericFilter a {@link Class} which the {@link GenericEvent} should be
	 *                      filtered for
	 * @param consumer      the event handler to invoke when a matching event is
	 *                      fired
	 */
	<F, E extends GenericEvent<F>> void addGenericListener(int priority, @NotNull Class<F> genericFilter,
			@NotNull Consumer<E> consumer);

	/**
	 * Adds a generic event listener to the bus, with the priority 0 (neutral).
	 * 
	 * @param <F>           the type of the generic filter
	 * @param <E>           the type of the {@link GenericEvent} to listen for
	 * @param genericFilter a {@link Class} which the {@link GenericEvent} should be
	 *                      filtered for
	 * @param consumer      the event handler to invoke when a matching event is
	 *                      fired
	 */
	default <F, E extends GenericEvent<F>> void addGenericListener(@NotNull Class<F> genericFilter,
			@NotNull Consumer<E> consumer) {
		addGenericListener(0, genericFilter, consumer);
	}

	/**
	 * Registers an object to the event bus, adding listeners for all <strong>public
	 * non-static</strong> (instance) methods annotated with {@link SubscribeEvent}
	 * from the object's class. <br>
	 * If the {@code object} is a {@link Class}, {@link #register(Class)} will be
	 * called.
	 * 
	 * @param object the object to register
	 */
	void register(@NotNull Object object);

	/**
	 * Registers a class to the event bus, adding listeners for all <strong>public
	 * static</strong> methods annotated with {@link SubscribeEvent} from the class.
	 * 
	 * @param clazz the class to register
	 */
	void register(@NotNull Class<?> clazz);

	/**
	 * Unregisters an object from the bus, removing listeners for the <strong>public
	 * non-static</strong> (instance) methods annotated with {@link SubscribeEvent}
	 * in the object's class. <br>
	 * If the {@code object} is a {@link Class}, {@link #unregister(Class)} will be
	 * called.
	 * 
	 * @param object the object to unregister
	 */
	void unregister(@NotNull Object object);

	/**
	 * Unregisters a class from the bus, removing listeners for the <strong>public
	 * static</strong> methods annotated with {@link SubscribeEvent} in the class.
	 * 
	 * @param clazz the class to unregister
	 */
	void unregister(@NotNull Class<?> clazz);

	/**
	 * Gets the base type of all the events fired on this bus.
	 * 
	 * @return the base type of all the events fired on this bus.
	 */
	Class<? extends Event> getBaseEventType();

	/**
	 * Gets the name of the bus.
	 * 
	 * @return the name of the bus.
	 */
	String getName();

	/**
	 * Checks if this bus walks event type hierarchy when dispatching events.
	 * 
	 * @return if this bus walks event type hierarchy when dispatching events
	 * @since  1.3.0
	 */
	boolean walksEventHierarchy();

	/**
	 * Checks if this bus is async. If it is, any {@link #post(Event)} calls will
	 * dispatch and handle the event on the {@link #getExecutor() dispatch
	 * executor}. Otherwise, calls to {@link #post(Event)} will have the event
	 * dispatched and handled on the same thread as the caller, making the operation
	 * <strong>blocking</strong>.
	 * 
	 * @return if the bus is asynchronous
	 * @since  1.4.0
	 */
	boolean isAsync();

	/**
	 * Starts this bus, if it is {@link #isShutdown() shutdown}.
	 */
	void start();

	/**
	 * Shuts down this bus. Any further events fired on this bus will not be
	 * dispatched to listeners anymore. <br>
	 * This action can be reverted by {@link #start()}.
	 */
	void shutdown();

	/**
	 * Checks if this bus is shutdown.
	 * 
	 * @return if this bus is shutdown.
	 */
	boolean isShutdown();

	/**
	 * Creates a new {@link BusBuilder}.
	 * 
	 * @param  busName the name of the bus
	 * @return         the builder
	 */
	static BusBuilder builder(String busName) {
		return BusBuilder.builder(busName);
	}
}
