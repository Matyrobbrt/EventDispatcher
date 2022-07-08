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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Implement this interface in objects that provide special behaviour (usually
 * dynamic listeners) when registered to an {@link EventBus}, using
 * {@link EventBus#register(Object)}.
 * 
 * @author matyrobbrt
 *
 * @since  1.6.0
 */
public interface BusRegistrable {

    /**
     * This method is called when the object is registered to a bus using
     * {@link EventBus#register(Object)}.
     * 
     * @param bus the bus the object was registered to
     */
    default void whenRegistered(EventBus bus) {

    }

    /**
     * This method is called when the object is unregistered from a bus using
     * {@link EventBus#unregister(Object)}.
     * 
     * @param bus the bus the object was unregistered from
     */
    default void whenUnregistered(EventBus bus) {

    }

    /**
     * Annotate a class with this annotation in order to provide special behaviour
     * when the class is registered to an {@link EventBus} using
     * {@link EventBus#register(Class)}. <br>
     * <br>
     * The methods that should be called when the class is registered respectively
     * unregistered from a bus need to be static, and have only one parameter that
     * is of the type {@link EventBus}.
     * 
     * @author matyrobbrt
     *
     */
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface ForClass {

        /**
         * The method to call when the class is registered to an {@link EventBus} using
         * {@link EventBus#register(Class)}. <br>
         * Can be left blank to remove this functionality.
         * 
         * @return the method to call when the class is registered
         */
        String registered() default "";

        /**
         * The method to call when the class is unregistered from an {@link EventBus}
         * using {@link EventBus#unregister(Class)}. <br>
         * Can be left blank to remove this functionality.
         * 
         * @return the method to call when the class is unregistered
         */
        String unregistered() default "";
    }
}
