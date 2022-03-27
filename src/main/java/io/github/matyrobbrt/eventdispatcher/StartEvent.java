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

package io.github.matyrobbrt.eventdispatcher;

/**
 * This event is fired on a bus, when it is
 * {@link io.github.matyrobbrt.eventdispatcher.EventBus#start() started}. <br>
 * The event will not be fired on the initial start of the bus (when it is
 * instantiated). <br>
 * This event will always be fired, and even if the bus
 * {@link io.github.matyrobbrt.eventdispatcher.EventBus#walksEventHierarchy()
 * walks event hierarchy} its hierarchy will <strong>not</strong> be walked.
 */
public final class StartEvent implements Event {

	private final EventBus bus;

	public StartEvent(EventBus bus) {
		this.bus = bus;
	}

	/**
	 * @return the bus that was started
	 */
	public EventBus getBus() {
		return bus;
	}

}
