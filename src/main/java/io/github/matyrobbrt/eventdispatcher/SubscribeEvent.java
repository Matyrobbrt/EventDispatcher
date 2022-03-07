package io.github.matyrobbrt.eventdispatcher;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.objectweb.asm.Type;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface SubscribeEvent {

	public static final String DESCRIPTOR = Type.getDescriptor(SubscribeEvent.class);

	int priority() default 0;

}
