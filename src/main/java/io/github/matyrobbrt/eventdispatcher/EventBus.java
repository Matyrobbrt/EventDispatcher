package io.github.matyrobbrt.eventdispatcher;

import java.util.function.Consumer;

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
	void post(Event event);

	/**
	 * Adds an event listener to the bus.
	 * 
	 * @param <E>      the type of the event to listen for
	 * @param priority the priority of the listener. <strong>Higher priority ==
	 *                 first to run</strong>
	 * @param consumer the event handler
	 */
	<E extends Event> void addListener(int priority, Consumer<E> consumer);

	<E extends GenericEvent> void addGenericListener(int priority, Class<?> genericFilter, Consumer<E> consumer);

	void register(Object object);

	void register(Class<?> clazz);
}