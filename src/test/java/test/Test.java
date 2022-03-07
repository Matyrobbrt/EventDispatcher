package test;

import org.slf4j.LoggerFactory;

import io.github.matyrobbrt.eventdispatcher.Event;
import io.github.matyrobbrt.eventdispatcher.SubscribeEvent;
import io.github.matyrobbrt.eventdispatcher.internal.EventBusImpl;

public class Test {

	public static final class TestEvent implements Event {

	}

	@SubscribeEvent
	static void listen(TestEvent e) {
		System.out.println(12);
	}

	public static void main(String[] args) {
		final var bus = new EventBusImpl(Event.class, LoggerFactory.getLogger(Test.class));
		bus.register(Test.class);

		bus.post(new TestEvent());
	}
}
