package io.github.matyrobbrt.eventdispatcher.internal.asm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import io.github.matyrobbrt.eventdispatcher.Event;
import io.github.matyrobbrt.eventdispatcher.EventListener;
import io.github.matyrobbrt.eventdispatcher.GenericEvent;

/**
 * Class used for generating dynamic event handlers using ASM, loading them into
 * memory, and then using them in order to handle events.
 * 
 * @author matyrobbrt
 *
 */
public class ASMEventListener implements EventListener {

	private final EventListener listener;
	private Type filter = null;

	public ASMEventListener(Object ownerInstance, Method method, boolean isGeneric)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		final var wrapper = ASMListenerGenerator.createMethodWrapper(method);
		if (Modifier.isStatic(method.getModifiers())) {
			listener = (EventListener) wrapper.getDeclaredConstructor().newInstance();
		} else {
			listener = (EventListener) wrapper.getDeclaredConstructor(Object.class).newInstance(ownerInstance);
		}
		if (isGeneric) {
			final var genericType = method.getGenericParameterTypes()[0];
			if (genericType instanceof ParameterizedType pType) {
				filter = pType.getActualTypeArguments()[0];
				if (filter instanceof ParameterizedType pType2) { // Nested generics.. discard them
					filter = pType2.getRawType();
				} else if (filter instanceof WildcardType wType) {
					if (wType.getUpperBounds().length == 1 && wType.getUpperBounds()[0] == Object.class
							&& wType.getLowerBounds().length == 0) {
						filter = null;
					}
				}
			}
		}
	}

	@Override
	public void handle(Event event) {
		if (listener != null) {
			if (filter == null || (event instanceof GenericEvent ge && filter == ge.getGenericType())) {
				listener.handle(event);
			}
		}
	}

}
