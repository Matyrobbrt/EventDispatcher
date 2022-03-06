package io.github.matyrobbrt.eventdispatcher.internal.asm.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.objectweb.asm.Type;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Indicates that a method has been generated by ASM for invoking event
 * handlers.
 */
@Retention(SOURCE)
@Target(METHOD)
public @interface ASMGeneratedEventHandlerInvoker {

	public static final String DESCRIPTOR = Type.getInternalName(ASMGeneratedEventHandlerInvoker.class);

}