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

package io.github.matyrobbrt.eventdispatcher.util;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.matyrobbrt.eventdispatcher.Cancellable;
import io.github.matyrobbrt.eventdispatcher.Event;
import io.github.matyrobbrt.eventdispatcher.EventBus;
import io.github.matyrobbrt.eventdispatcher.EventInterceptor;
import io.github.matyrobbrt.eventdispatcher.EventListener;
import io.github.matyrobbrt.eventdispatcher.GenericEvent;
import io.github.matyrobbrt.eventdispatcher.ShutdownEvent;

@SuppressWarnings("rawtypes")
public class EventWaiter implements EventListener, EventInterceptor {

	private static final Logger LOGGER = LoggerFactory.getLogger(EventWaiter.class);
	private final HashMap<Class<?>, Set<WaitingEvent>> waitingEvents;
	private final ScheduledExecutorService threadpool;
	private final boolean shutdownAutomatically;

	/**
	 * Constructs an empty EventWaiter, with a single thread executor, and which
	 * shutdowns automatically.
	 */
	public EventWaiter() {
		this(Executors.newSingleThreadScheduledExecutor(), true);
	}

	/**
	 * Constructs an EventWaiter using the provided
	 * {@link java.util.concurrent.ScheduledExecutorService Executor} as its
	 * threadpool.
	 *
	 * <p>
	 * {@code shutdownAutomatically} is required to be manually specified by
	 * developers as a way of verifying a contract that the developer will conform
	 * to the behaviour of the newly generated EventWaiter:
	 * <ul>
	 * <li>If {@code true}, shutdown is handled when a
	 * {@link io.github.matyrobbrt.eventdispatcher.ShutdownEvent ShutdownEvent} is
	 * fired, or when the waiter is added as an {@link EventInterceptor}, and
	 * {@link io.github.matyrobbrt.eventdispatcher.EventInterceptor#onShutdown(EventBus)}
	 * is called.. This means that any external functions of the provided Executor
	 * is now impossible and any externally queued tasks are lost if they have yet
	 * to be run.</li>
	 * <li>If {@code false}, shutdown is now placed as a responsibility of the
	 * developer, and no attempt will be made to shutdown the provided
	 * Executor.</li>
	 * </ul>
	 * It's worth noting that this EventWaiter can serve as a delegate to invoke the
	 * threadpool's shutdown via a call to {@link #shutdown()
	 * EventWaiter#shutdown()}. However, this operation is only supported for
	 * EventWaiters that are not supposed to shutdown automatically, otherwise
	 * invocation of {@code EventWaiter#shutdown()} will result in an
	 * {@link java.lang.UnsupportedOperationException}.
	 *
	 * @param  threadpool                         The ScheduledExecutorService to
	 *                                            use for this EventWaiter's
	 *                                            threadpool.
	 * @param  shutdownAutomatically              Whether or not the
	 *                                            {@code threadpool} will shutdown
	 *                                            automatically when a
	 *                                            {@link net.dv8tion.jda.api.events.ShutdownEvent
	 *                                            ShutdownEvent} is fired.
	 *
	 * @throws java.lang.IllegalArgumentException If the threadpool provided
	 *                                            {@link java.util.concurrent.ScheduledExecutorService#isShutdown()
	 *                                            is shutdown}
	 *
	 * @see                                       com.jagrosh.jdautilities.commons.waiter.EventWaiter#shutdown()
	 *                                            EventWaiter#shutdown()
	 */
	public EventWaiter(@NotNull ScheduledExecutorService threadpool, @NotNull boolean shutdownAutomatically) {
		if (threadpool == null) { throw new NullPointerException("threadpool"); }
		if (threadpool.isShutdown()) {
			throw new IllegalArgumentException("Cannot construct EventWaiter with a shutdown threadpool!");
		}

		this.waitingEvents = new HashMap<>();
		this.threadpool = threadpool;

		this.shutdownAutomatically = shutdownAutomatically;
	}

	/**
	 * Gets whether the EventWaiter's internal ScheduledExecutorService
	 * {@link java.util.concurrent.ScheduledExecutorService#isShutdown() is
	 * shutdown}.
	 *
	 * @return {@code true} if the ScheduledExecutorService is shutdown,
	 *         {@code false} otherwise.
	 */
	public boolean isShutdown() {
		return threadpool.isShutdown();
	}

	/**
	 * Waits an indefinite amount of time for an
	 * {@link io.github.matyrobbrt.eventdispatcher.Event Event} that returns
	 * {@code true} when tested with the provided
	 * {@link java.util.function.Predicate Predicate}.
	 * 
	 * <p>
	 * When this occurs, the provided {@link java.util.function.Consumer Consumer}
	 * will accept and execute using the same Event.
	 * 
	 * @param  <T>                      the type of Event to wait for.
	 * @param  classType                the {@link java.lang.Class} of the Event to
	 *                                  wait for. Never null.
	 * @param  condition                the Predicate to test when Events of the
	 *                                  provided type are thrown. Never null.
	 * @param  action                   the Consumer to perform an action when the
	 *                                  condition Predicate returns {@code true}.
	 *                                  Never null.
	 *
	 * @throws IllegalArgumentException if the internal threadpool is shutdown,
	 *                                  meaning that no more tasks can be submitted.
	 */
	public <T extends Event> void waitForEvent(Class<T> classType, Predicate<T> condition, Consumer<T> action) {
		waitForEvent(classType, condition, action, -1, null, null);
	}

	/**
	 * Waits a predetermined amount of time for an
	 * {@link io.github.matyrobbrt.eventdispatcher.Event} that returns {@code true}
	 * when tested with the provided {@link java.util.function.Predicate Predicate}.
	 * 
	 * <p>
	 * Once started, there are two possible outcomes:
	 * <ul>
	 * <li>The correct Event occurs within the time allotted, and the provided
	 * {@link java.util.function.Consumer Consumer} will accept and execute using
	 * the same Event.</li>
	 * 
	 * <li>The time limit is elapsed and the provided {@link java.lang.Runnable} is
	 * executed.</li>
	 * </ul>
	 * 
	 * @param  <T>                      the type of Event to wait for.
	 * @param  classType                the {@link java.lang.Class} of the Event to
	 *                                  wait for. Never null.
	 * @param  condition                the Predicate to test when Events of the
	 *                                  provided type are thrown. Never null.
	 * @param  action                   the Consumer to perform an action when the
	 *                                  condition Predicate returns {@code true}.
	 *                                  Never null.
	 * @param  timeout                  the maximum amount of time to wait for, or
	 *                                  {@code -1} if there is no timeout.
	 * @param  unit                     the {@link java.util.concurrent.TimeUnit
	 *                                  TimeUnit} measurement of the timeout, or
	 *                                  {@code null} if there is no timeout.
	 * @param  timeoutAction            the Runnable to run if the time runs out
	 *                                  before a correct Event is thrown, or
	 *                                  {@code null} if there is no action on
	 *                                  timeout.
	 *
	 * @throws IllegalArgumentException if the internal threadpool is shutdown,
	 *                                  meaning that no more tasks can be submitted.
	 */
	public <T extends Event> void waitForEvent(Class<T> classType, Predicate<T> condition, Consumer<T> action,
			long timeout, TimeUnit unit, Runnable timeoutAction) {
		if (isShutdown()) {
			throw new IllegalArgumentException(
					"Attempted to register a WaitingEvent while the EventWaiter's threadpool was already shutdown!");
		}
		if (classType == null) { throw new NullPointerException("The provided class type"); }
		if (condition == null) { throw new NullPointerException("The provided condition predicate"); }
		if (action == null) { throw new NullPointerException("The provided action consumer"); }

		WaitingEvent we = new WaitingEvent<>(condition, action);
		Set<WaitingEvent> set = waitingEvents.computeIfAbsent(classType, c -> ConcurrentHashMap.newKeySet());
		set.add(we);

		if (timeout > 0 && unit != null) {
			threadpool.schedule(() -> {
				try {
					if (set.remove(we) && timeoutAction != null)
						timeoutAction.run();
				} catch (Exception ex) {
					LOGGER.error("Failed to run timeoutAction", ex);
				}
			}, timeout, unit);
		}
	}

	/**
	 * Waits an indefinite amount of time for an
	 * {@link io.github.matyrobbrt.eventdispatcher.Event Event} that returns
	 * {@code true} when tested with the provided
	 * {@link java.util.function.Predicate Predicate}.
	 * 
	 * <p>
	 * When this occurs, the provided {@link java.util.function.Consumer Consumer}
	 * will accept and execute using the same Event.
	 * 
	 * @param  <T>                      the type of Event to wait for
	 * @param  <F>                      the generic type to wait for
	 * @param  classType                the {@link java.lang.Class} of the Event to
	 *                                  wait for. Never null.
	 * @param  genericFilter            the generic filter of the Event to wait for.
	 *                                  Never null.
	 * @param  condition                the Predicate to test when Events of the
	 *                                  provided type are thrown. Never null.
	 * @param  action                   the Consumer to perform an action when the
	 *                                  condition Predicate returns {@code true}.
	 *                                  Never null.
	 *
	 * @throws IllegalArgumentException If the internal threadpool is shutdown,
	 *                                  meaning that no more tasks can be submitted.
	 */
	public <T extends GenericEvent<F>, F> void waitForGenericEvent(Class<T> classType, Class<F> genericFilter,
			Predicate<T> condition, Consumer<T> action) {
		waitForGenericEvent(classType, genericFilter, condition, action, -1, null, null);
	}

	/**
	 * Waits a predetermined amount of time for a
	 * {@link io.github.matyrobbrt.eventdispatcher.GenericEvent} that returns
	 * {@code true} when tested with the provided
	 * {@link java.util.function.Predicate Predicate}.
	 * 
	 * <p>
	 * Once started, there are two possible outcomes:
	 * <ul>
	 * <li>The correct Event occurs within the time allotted, and the provided
	 * {@link java.util.function.Consumer Consumer} will accept and execute using
	 * the same Event.</li>
	 * 
	 * <li>The time limit is elapsed and the provided {@link java.lang.Runnable} is
	 * executed.</li>
	 * </ul>
	 * 
	 * @param  <T>                      the type of Event to wait for
	 * @param  <F>                      the generic type to wait for
	 * @param  classType                the {@link java.lang.Class} of the Event to
	 *                                  wait for. Never null.
	 * @param  genericFilter            the generic filter of the Event to wait for.
	 *                                  Never null.
	 * @param  condition                the Predicate to test when Events of the
	 *                                  provided type are thrown. Never null.
	 * @param  action                   the Consumer to perform an action when the
	 *                                  condition Predicate returns {@code true}.
	 *                                  Never null.
	 * @param  timeout                  the maximum amount of time to wait for, or
	 *                                  {@code -1} if there is no timeout.
	 * @param  unit                     the {@link java.util.concurrent.TimeUnit
	 *                                  TimeUnit} measurement of the timeout, or
	 *                                  {@code null} if there is no timeout.
	 * @param  timeoutAction            the Runnable to run if the time runs out
	 *                                  before a correct Event is thrown, or
	 *                                  {@code null} if there is no action on
	 *                                  timeout.
	 *
	 * @throws IllegalArgumentException if the internal threadpool is shutdown,
	 *                                  meaning that no more tasks can be submitted.
	 */
	public <T extends GenericEvent<F>, F> void waitForGenericEvent(@NotNull Class<T> classType,
			@NotNull Class<F> genericFilter, @NotNull Predicate<T> condition, @NotNull Consumer<T> action, long timeout,
			TimeUnit unit, Runnable timeoutAction) {
		if (isShutdown()) {
			throw new IllegalArgumentException(
					"Attempted to register a WaitingEvent while the EventWaiter's threadpool was already shutdown!");
		}
		if (classType == null)
			throw new NullPointerException("The provided class type");
		if (genericFilter == null)
			throw new NullPointerException("The provided generic filter");
		if (condition == null)
			throw new NullPointerException("The provided condition predicate");
		if (action == null)
			throw new NullPointerException("The provided action consumer");

		final WaitingEvent we = new WaitingGenericEvent<>(condition, action, genericFilter);
		final Set<WaitingEvent> set = waitingEvents.computeIfAbsent(classType, c -> ConcurrentHashMap.newKeySet());
		set.add(we);

		if (timeout > 0 && unit != null) {
			threadpool.schedule(() -> {
				try {
					if (set.remove(we) && timeoutAction != null)
						timeoutAction.run();
				} catch (Exception ex) {
					LOGGER.error("Failed to run timeoutAction", ex);
				}
			}, timeout, unit);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public final void handle(final Event event) {
		if (event instanceof ShutdownEvent && shutdownAutomatically) {
			shutdown();
		}

		Class c = event.getClass();

		// Runs at least once for the fired Event, at most
		// once for each superclass (excluding Object) because
		// Class#getSuperclass() returns null when the superclass
		// is primitive, void, or (in this case) Object.
		while (c != null) {
			final Set<WaitingEvent> set = waitingEvents.get(c);
			if (set != null) {
				// Remove the ones that have successfully ran (those that return true)
				set.removeIf(wEvent -> wEvent.attempt(event));
			}
			c = c.getSuperclass();
		}
	}

	/**
	 * Closes this EventWaiter if it doesn't normally shutdown automatically.
	 *
	 * Calling this method on an EventWaiter that does shutdown automatically will
	 * result in an {@link java.lang.UnsupportedOperationException
	 * UnsupportedOperationException} being thrown.
	 *
	 * @throws UnsupportedOperationException If the EventWaiter is supposed to close
	 *                                       automatically.
	 */
	public void shutdown() {
		if (shutdownAutomatically) {
			throw new UnsupportedOperationException(
					"Shutting down EventWaiters that are set to automatically close is unsupported!");

		}
		threadpool.shutdown();
	}

	@Override
	public void onShutdown(@NotNull EventBus bus) {
		if (!isShutdown() && shutdownAutomatically) {
			threadpool.shutdown();
		}
	}

	private class WaitingEvent<T extends Event> {

		final Predicate<T> condition;
		final Consumer<T> action;

		WaitingEvent(Predicate<T> condition, Consumer<T> action) {
			this.condition = condition;
			this.action = action;
		}

		boolean attempt(T event) {
			if (condition.test(event) && checkEventNotCancelled(event)) {
				action.accept(event);
				return true;
			}
			return false;
		}

	}

	private final class WaitingGenericEvent<E extends GenericEvent<?>> extends WaitingEvent<E> {

		private final Type filter;

		WaitingGenericEvent(Predicate<E> condition, Consumer<E> action, Type filter) {
			super(condition, action);
			this.filter = filter;
		}

		@Override
		boolean attempt(E event) {
			if (checkEventNotCancelled(event)) {
                final GenericEvent<?> ge = event;
				if (filter == ge.getGenericType() && condition.test(event)) {
					action.accept(event);
					return true;
				}
			}
			return false;
		}

	}

	private static boolean checkEventNotCancelled(final Event event) {
		if (event instanceof Cancellable) { return !((Cancellable) event).isCancelled(); }
		return true;
	}
}
