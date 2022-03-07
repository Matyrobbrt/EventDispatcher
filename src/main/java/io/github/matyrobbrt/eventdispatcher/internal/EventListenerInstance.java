package io.github.matyrobbrt.eventdispatcher.internal;

import io.github.matyrobbrt.eventdispatcher.Event;
import io.github.matyrobbrt.eventdispatcher.EventListener;

/**
 * Listener instances used for prioritising events. <br>
 * Higher priority == first to run.
 * 
 * @author matyrobbrt
 *
 */
public record EventListenerInstance(int priority, EventListener listener) implements EventListener {

	@Override
	public void handle(Event event) {
		listener.handle(event);
	}

}
