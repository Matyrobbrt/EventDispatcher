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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import io.github.matyrobbrt.eventdispatcher.Event;
import io.github.matyrobbrt.eventdispatcher.EventListener;

public class EventDispatcher implements EventListener {

	private final List<EventListenerInstance> listeners = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Sorts the listeners in order to match their priorities.
	 */
	public void sortOrder() {
		listeners.sort((l1, l2) -> -Comparator.comparingInt(EventListenerInstance::priority).compare(l1, l2));
	}

	public void register(int priority, EventListener listener) {
		listeners.add(new EventListenerInstance(priority, listener));
		sortOrder();
	}

	public void registerWithoutSorting(int priority, EventListener listener) {
		listeners.add(new EventListenerInstance(priority, listener));
	}

	public void unregister(Predicate<? super EventListener> predicate) {
		listeners.removeIf(l -> predicate.test(l.listener()));
		sortOrder();
	}

	@Override
	public void handle(Event event) {
		listeners.forEach(l -> l.handle(event));
	}

	public void handleWithExceptionCatch(Event event, BiConsumer<EventListener, Throwable> onException) {
		listeners.forEach(listener -> {
			try {
				listener.handle(event);
			} catch (Throwable t) {
				onException.accept(listener.listener(), t);
			}
		});
	}
}