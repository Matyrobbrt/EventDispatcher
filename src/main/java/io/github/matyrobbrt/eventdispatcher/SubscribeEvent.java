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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to subscribe a method to an {@link Event}.
 *
 * This annotation can only be applied to single parameter,
 * <strong>public</strong> methods, where the single parameter is a subclass of
 * {@link Event}.
 *
 * Use {@link EventBus#register(Object)} to submit an Object instance or a
 * {@link EventBus#register(Class)} to submit a class to the event bus for
 * scanning to generate callback {@link EventListener} wrappers.
 *
 * The {@link EventBus} system generates an ASM wrapper, using
 * {@link io.github.matyrobbrt.eventdispatcher.internal.asm.ASMEventListener},
 * that dispatches to the marked method. <br>
 * <br>
 * Currently, this annotation only works on public methods. Hopefully, this will
 * work for other access modifiers as well, in the future.
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface SubscribeEvent {

	/**
	 * The priority of this event listener. <br>
	 * <strong>Higher priority == first to run.</strong> <br>
	 * Defaults to 0 (neutral priority).
	 * 
	 * @return the priority of the listener
	 */
	int priority() default 0;

}
