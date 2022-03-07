package io.github.matyrobbrt.eventdispatcher.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

	@Override
	public void handle(Event event) {
		listeners.forEach(l -> l.handle(event));
	}
}
