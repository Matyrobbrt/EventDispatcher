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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.matyrobbrt.eventdispatcher.Event;
import io.github.matyrobbrt.eventdispatcher.EventBus;
import io.github.matyrobbrt.eventdispatcher.EventInterceptor;
import io.github.matyrobbrt.eventdispatcher.reflections.AnnotationProvider;

/**
 * A Builder for creating {@link EventBus} instances.
 * 
 * @author matyrobbrt
 *
 */
public final class BusBuilder {

	private final String name;
	private Class<? extends Event> baseEventType = Event.class;
	private final List<EventInterceptor> interceptors = new ArrayList<>();
	private Logger logger;
	private final List<AnnotationProvider> annotationProviders = new ArrayList<>();
	private boolean walksEventHierarchy;
	private Executor executor;

	private BusBuilder(String name) {
		this.name = name;
	}

	/**
	 * Sets the base type of the events fired on the bus.
	 * 
	 * @param  baseEventType the base type of the events
	 * @return               the builder instance
	 */
	public BusBuilder baseEventType(Class<? extends Event> baseEventType) {
		this.baseEventType = baseEventType;
		return this;
	}

	/**
	 * Adds an {@link EventInterceptor} to the interceptors of the bus.
	 * 
	 * @param  interceptor the interceptor to add
	 * @return             the builder instance
	 */
	public BusBuilder addInterceptor(EventInterceptor interceptor) {
		this.interceptors.add(interceptor);
		return this;
	}

	/**
	 * Adds {@link EventInterceptor}s to the interceptors of the bus.
	 * 
	 * @param  interceptors the interceptors to add
	 * @return              the builder instance
	 */
	public BusBuilder addInterceptors(EventInterceptor... interceptors) {
		this.interceptors.addAll(Arrays.asList(interceptors));
		return this;
	}

	/**
	 * Sets the logger used for logging messages produced by the bus.
	 * 
	 * @param  logger the logger
	 * @return        the builder instance
	 */
	public BusBuilder logger(Logger logger) {
		this.logger = logger;
		return this;
	}

	/**
	 * Adds an
	 * {@link io.github.matyrobbrt.eventdispatcher.reflections.AnnotationProvider}
	 * to the annotation providers of the bus.
	 * 
	 * @param  provider the provider to add
	 * @return          the builder instance
	 */
	public BusBuilder addAnnotationProvider(AnnotationProvider provider) {
		this.annotationProviders.add(provider);
		return this;
	}

	/**
	 * Adds
	 * {@link io.github.matyrobbrt.eventdispatcher.reflections.AnnotationProvider}s
	 * to the annotation providers of the bus.
	 * 
	 * @param  providers the providers to add
	 * @return           the builder instance
	 */
	public BusBuilder addAnnotationProviders(AnnotationProvider... providers) {
		this.annotationProviders.addAll(Arrays.asList(providers));
		return this;
	}

	/**
	 * Sets if the bus will walk event type hierarchy when dispatching events. <br>
	 * If {@code true}, the bus will fire the event to listeners listening to its
	 * subclasses.
	 * 
	 * @param  walksEventHierarchy if the bus will walk event hierarchy when
	 *                             dispatching events
	 * @return                     the builder instance
	 * @since                      1.3.0
	 */
	public BusBuilder walksEventHierarcy(boolean walksEventHierarchy) {
		this.walksEventHierarchy = walksEventHierarchy;
		return this;
	}

	/**
	 * Sets an executor for dispatching and handling events, effectively making the
	 * bus {@link EventBus#isAsync() asynchronous}. <br>
	 * 
	 * @apiNote          Note that due to the fact that calling
	 *                   {@link EventBus#post(Event)} will no longer block the
	 *                   caller thread, a modification of the event cannot be easily
	 *                   detected. If you fire events for them to be modified in
	 *                   their handling, it is recommended that you <strong>DO
	 *                   NOT</strong> set an executor.
	 * @apiNote          The handling of an event of the same type is <i>still</i>
	 *                   done on the same thread, but not on the caller one.
	 * @param   executor sets the executor which dispatches and handles events
	 * @return           the builder instance
	 * @since            1.4.0
	 */
	public BusBuilder executor(Executor executor) {
		this.executor = executor;
		return this;
	}

	/**
	 * Builds the {@link EventBus}.
	 * 
	 * @return the built {@link EventBus}
	 */
	public EventBus build() {
		final var bus = new EventBusImpl(name, baseEventType == null ? Event.class : baseEventType,
				logger == null ? LoggerFactory.getLogger("EventBus %s".formatted(name)) : logger,
				new MultiEventInterceptor(interceptors), walksEventHierarchy, executor);
		annotationProviders.forEach(provider -> provider.register(bus::register, bus::register));
		return bus;
	}

	/**
	 * Creates a builder instance.
	 * 
	 * @param  name the name of the bus
	 * @return      the builder instance
	 */
	public static BusBuilder builder(String name) {
		return new BusBuilder(name);
	}

}
