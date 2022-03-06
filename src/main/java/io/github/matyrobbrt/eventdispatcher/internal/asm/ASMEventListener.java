package io.github.matyrobbrt.eventdispatcher.internal.asm;

import java.lang.reflect.Method;

import io.github.matyrobbrt.eventdispatcher.Event;
import io.github.matyrobbrt.eventdispatcher.EventListener;

/**
 * Class used for generating dynamic event handlers using ASM, loading them into
 * memory, and then using them in order to handle events.
 * 
 * @author matyrobbrt
 *
 */
public class ASMEventListener implements EventListener {

	public ASMEventListener(Method method) {
	}

	@Override
	public void handle(Event event) {
	}

}