package io.github.matyrobbrt.eventdispatcher.internal.asm;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

import io.github.matyrobbrt.eventdispatcher.Event;
import io.github.matyrobbrt.eventdispatcher.EventListener;
import io.github.matyrobbrt.eventdispatcher.internal.asm.annotation.ASMGeneratedEventHandlerInvoker;
import io.github.matyrobbrt.eventdispatcher.internal.asm.annotation.ASMGeneratedEventListener;

class ASMListenerGenerator {

	private static final AtomicInteger GENERATED_CLASSES = new AtomicInteger();
	private static final HashMap<Method, Class<?>> METHOD_CACHE = new HashMap<>();

	private static final String LISTENER_DESC = Type.getInternalName(EventListener.class);
	private static final String OBJECT_DESC = Type.getInternalName(Object.class);
	private static final String LISTENER_FUNC_DESC = Type.getMethodDescriptor(Type.VOID_TYPE,
			Type.getType(Event.class));
	private static final String HANDLE_EVENT_METHOD_NAME = EventListener.class.getDeclaredMethods()[0].getName();

	/**
	 * Generates a class for wrapping the event handler method.
	 * 
	 * @param  method          the method to wrap
	 * @param  byteDataHandler a consumer which will be accepted with byte data of
	 *                         the generated class. This is nullable
	 * @return                 the generated class
	 */
	static Class<?> createMethodWrapper(Method method, @Nullable Consumer<byte[]> byteDataHandler) {
		return METHOD_CACHE.computeIfAbsent(method, $ -> {
			ClassWriter classWriter = new ClassWriter(0);
			MethodVisitor methodVisitor;

			boolean isStatic = Modifier.isStatic(method.getModifiers());
			String name = getGeneratedName(method);
			String desc = name.replace('.', '/');
			String instType = Type.getInternalName(method.getDeclaringClass());
			String eventType = Type.getInternalName(method.getParameterTypes()[0]);

			// Implement the listener interface on the class
			classWriter.visit(V1_6, ACC_PUBLIC | ACC_SUPER, desc, null, OBJECT_DESC, new String[] {
					LISTENER_DESC
			});
			// Add the visual generated annotation
			classWriter.visitAnnotation(ASMGeneratedEventListener.DESCRIPTOR, false).visitEnd();

			classWriter.visitSource(".dynamic", null);
			if (!isStatic) {
				// Method is not static, so it needs to handle the events on an instance
				classWriter.visitField(ACC_PRIVATE + ACC_FINAL, "ownerInstance", OBJECT_DESC, null, null).visitEnd();
			}
			{
				// Generate the constructor of the class
				methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", isStatic ? "()V" : "(Ljava/lang/Object;)V", null,
						null);
				methodVisitor.visitCode();
				methodVisitor.visitVarInsn(ALOAD, 0);
				methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
				if (!isStatic) {
					// Method is not static, so assign the instance field
					methodVisitor.visitVarInsn(ALOAD, 0);
					methodVisitor.visitVarInsn(ALOAD, 1);
					methodVisitor.visitFieldInsn(PUTFIELD, desc, "ownerInstance", OBJECT_DESC);
				}
				methodVisitor.visitInsn(RETURN);
				methodVisitor.visitMaxs(2, 2);
				methodVisitor.visitEnd();
			}
			{
				// Add the handle method
				methodVisitor = classWriter.visitMethod(ACC_PUBLIC, HANDLE_EVENT_METHOD_NAME, LISTENER_FUNC_DESC, null, null);
				// Add the visual generated annotation
				methodVisitor.visitAnnotation(ASMGeneratedEventHandlerInvoker.DESCRIPTOR, false).visitEnd();
				methodVisitor.visitCode();
				methodVisitor.visitVarInsn(ALOAD, 0);
				if (!isStatic) {
					// Not static, so invoke the handle method on the instance
					methodVisitor.visitFieldInsn(GETFIELD, desc, "ownerInstance", "Ljava/lang/Object;");
					methodVisitor.visitTypeInsn(CHECKCAST, instType);
				}
				methodVisitor.visitVarInsn(ALOAD, 1);
				methodVisitor.visitTypeInsn(CHECKCAST, eventType);
				methodVisitor.visitMethodInsn(isStatic ? INVOKESTATIC : INVOKEVIRTUAL, instType, method.getName(),
						Type.getMethodDescriptor(method), false);
				methodVisitor.visitInsn(RETURN);
				methodVisitor.visitMaxs(2, 2);
				methodVisitor.visitEnd();
			}
			classWriter.visitEnd();
			final var ba = classWriter.toByteArray();
			if (byteDataHandler != null) {
				byteDataHandler.accept(ba);
			}
			return ASMClassLoader.INSTANCE.define(name, ba);
		});
	}

	static String getGeneratedName(Method method) {
		final var isStatic = Modifier.isStatic(method.getModifiers()) ? 1 : 0;
		return "ASMGeneratedEventListener$%s$%s$%s$%s$%s".formatted(GENERATED_CLASSES.getAndIncrement(),
				method.getDeclaringClass().getSimpleName(), method.getName(), isStatic,
				method.getParameterTypes()[0].getSimpleName());
		// Example: ASMGeneratedEventListener$0$TestClass$listenerMethod$0$TestEvent
	}
}
