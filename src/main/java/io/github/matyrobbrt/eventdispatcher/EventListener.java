package io.github.matyrobbrt.eventdispatcher;

/**
 * A listener used for handling dispatched events.
 * 
 * @author matyrobbrt
 *
 */
@FunctionalInterface
public interface EventListener {

	/**
	 * Handles an event
	 * 
	 * @param event the event
	 */
	void handle(Event event);

}
