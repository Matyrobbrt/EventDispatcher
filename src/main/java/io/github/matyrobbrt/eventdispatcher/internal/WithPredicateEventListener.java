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

import java.util.function.Predicate;

import io.github.matyrobbrt.eventdispatcher.Event;
import io.github.matyrobbrt.eventdispatcher.EventListener;

public final class WithPredicateEventListener<E extends Event> implements EventListener {

	private final Class<? super E> eventType;
	private final Predicate<? super E> predicate;
	private final EventListener handler;

	public WithPredicateEventListener(Class<? super E> eventType, Predicate<? super E> predicate,
			EventListener handler) {
		this.eventType = eventType;
		this.predicate = predicate;
		this.handler = handler;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void handle(Event event) {
		if (eventType.isAssignableFrom(event.getClass()) && predicate.test((E) event)) {
			handler.handle(event);
		}
	}
	
	public Class<? super E> eventType() {
		return eventType;
	}
	
	public Predicate<? super E> predicate() {
		return predicate;
	}

	public EventListener handler() {
		return handler;
	}

}
