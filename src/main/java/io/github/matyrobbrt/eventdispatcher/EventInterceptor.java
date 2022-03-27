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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An interface used for intercepting events fired on an {@link EventBus}, or
 * for tracking the status of a bus.
 * 
 * @author matyrobbrt
 *
 */
public interface EventInterceptor {

	/**
	 * Intercepts an event, before it is dispatched to listeners. <br>
	 * Usually used for modifying events before they are dispatched, or for logging
	 * their firing.
	 * 
	 * @apiNote       Changing the event to a totally different one is not
	 *                supported. In those cases, it is preferred that {@code null}
	 *                is returned, and the new event is posted to the {@code bus}.
	 * 
	 * @param   <E>   the type of the intercepted event
	 * @param   bus   the bus on which the event was fired
	 * @param   event the intercepted event
	 * @return        a (maybe) modified event, which will be dispatched to
	 *                listeners. Usually, this is the {@code event} received as a
	 *                parameter. Returning {@code null} will stop the event from
	 *                being dispatched.
	 */
	@Nullable
	default <E extends Event> E onEvent(@NotNull EventBus bus, @NotNull E event) {
		return event;
	}

	/**
	 * Called when an exception is encountered while handling an
	 * {@link EventListener}.
	 * 
	 * @param bus       the bus on which the event was fired
	 * @param event     the event
	 * @param throwable the caught exception
	 * @param listener  the listener who threw this exception
	 */
	default void onException(@NotNull EventBus bus, @NotNull Event event, @NotNull Throwable throwable,
			@NotNull EventListener listener) {
	}

	/**
	 * Called when an {@link EventBus} is shut down.
	 * 
	 * @param bus the bus that was shut down
	 */
	default void onShutdown(@NotNull EventBus bus) {

	}

	/**
	 * Called when an {@link EventBus} was {@link EventBus#start() started} after
	 * being previously shut down.
	 * 
	 * @param bus the bus that was started
	 */
	default void onStart(@NotNull EventBus bus) {

	}
}
