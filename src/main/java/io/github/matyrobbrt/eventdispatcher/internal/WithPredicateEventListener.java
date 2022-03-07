package io.github.matyrobbrt.eventdispatcher.internal;

import java.util.function.Predicate;

import io.github.matyrobbrt.eventdispatcher.Event;
import io.github.matyrobbrt.eventdispatcher.EventListener;

public record WithPredicateEventListener<E extends Event> (Class<? super E> eventType, Predicate<? super E> predicate,
		EventListener handler)
		implements EventListener {

	@SuppressWarnings("unchecked")
	@Override
	public void handle(Event event) {
		if (eventType.isAssignableFrom(event.getClass()) && predicate.test((E) event)) {
			handler.handle(event);
		}
	}

}
