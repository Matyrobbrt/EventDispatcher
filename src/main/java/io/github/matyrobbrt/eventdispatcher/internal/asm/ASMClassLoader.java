package io.github.matyrobbrt.eventdispatcher.internal.asm;

/**
 * Used for defining classes from in-memory bytes.
 * 
 * @author matyrobbrt
 *
 */
final class ASMClassLoader extends ClassLoader {

	static final ASMClassLoader INSTANCE = new ASMClassLoader();

	private ASMClassLoader() {
		super(null);
	}

	@Override
	protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
		return Class.forName(name, resolve, Thread.currentThread().getContextClassLoader());
	}

	/**
	 * Defines a class.
	 * 
	 * @param  name the name of the class
	 * @param  data the data of the class
	 * @return
	 */
	Class<?> define(String name, byte[] data) {
		return defineClass(name, data, 0, data.length);
	}

}
