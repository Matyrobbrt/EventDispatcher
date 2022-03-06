package io.github.matyrobbrt.eventdispatcher;

import java.lang.reflect.Type;

/**
 * The base interface for generic events
 * 
 * @author matyrobbrt
 *
 */
public interface GenericEvent {

	Type getGenericType();
}
