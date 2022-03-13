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

package io.github.matyrobbrt.eventdispatcher.internal.asm;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import io.github.matyrobbrt.asmutils.wrapper.ConsumerWrapper;
import io.github.matyrobbrt.eventdispatcher.Event;
import io.github.matyrobbrt.eventdispatcher.EventListener;
import io.github.matyrobbrt.eventdispatcher.GenericEvent;

/**
 * Class used for generating dynamic event handlers using ASM, loading them into
 * memory, and then using them in order to handle events.
 * 
 * @author matyrobbrt
 *
 */
public class ASMEventListener implements EventListener {

	private final EventListener listener;
	private final ConsumerWrapper<? super Event> wrapper;
	private final Method method;
	private final Object ownerInstance;
	private Type filter = null;

	public ASMEventListener(Object ownerInstance, Method method, boolean isGeneric)
			throws IllegalArgumentException, SecurityException {
		this.ownerInstance = ownerInstance;
		this.method = method;
		this.wrapper = ConsumerWrapper.create(method);
		if (wrapper.isStatic()) {
			listener = wrapper::accept;
		} else {
			final var consumer = wrapper.onTarget(ownerInstance);
			listener = consumer::accept;
		}
		if (isGeneric) {
			final var genericType = method.getGenericParameterTypes()[0];
			if (genericType instanceof ParameterizedType pType) {
				filter = pType.getActualTypeArguments()[0];
				if (filter instanceof ParameterizedType pType2) { // Nested generics.. discard them
					filter = pType2.getRawType();
				} else if (filter instanceof WildcardType wType) {
					if (wType.getUpperBounds().length == 1 && wType.getUpperBounds()[0] == Object.class
							&& wType.getLowerBounds().length == 0) {
						filter = null;
					}
				}
			}
		}
	}

	@Override
	public void handle(Event event) {
		if (listener != null) {
			if (filter == null || (event instanceof GenericEvent<?> ge && filter == ge.getGenericType())) {
				listener.handle(event);
			}
		}
	}

	public boolean isSame(ASMEventListener other) {
		return other.wrapper == this.wrapper || isSame(other.method) || isSame(other.ownerInstance);
	}

	public boolean isSame(Method method) {
		return this.method.equals(method);
	}

	public boolean isSame(Object ownerInstance) {
		return this.ownerInstance.equals(ownerInstance);
	}
}
