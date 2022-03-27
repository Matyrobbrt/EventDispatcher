/*
 * This file is part of the Event Dispatcher library and is licensed under the
 * MIT license:
 *
 * MIT License
 *
 * Copyright (c) 2022 Matyrobbrt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.matyrobbrt.eventdispatcher.util;

import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * A class used for walking parents of a class.
 * 
 * @since 1.3.0
 */
public final class ClassWalker implements Iterable<Class<?>> {

	private final Class<?> clazz;
	private final Class<?> end;

	private ClassWalker(Class<?> clazz) {
		this(clazz, Object.class);
	}

	private ClassWalker(Class<?> clazz, Class<?> end) {
		this.clazz = clazz;
		this.end = end;
	}

	/**
	 * Creates a {@link ClassWalker} which walks the subclasses from the
	 * {@code start} class to the {@code end} one.
	 * 
	 * @param  start the class to start from
	 * @param  end   the last class to walk to
	 * @return       the walker
	 */
	public static ClassWalker range(Class<?> start, Class<?> end) {
		return new ClassWalker(start, end);
	}

	/**
	 * Creates a {@link ClassWalker} which walks the subclasses from the
	 * {@code start} all the way to {@link Object}.
	 * 
	 * @param  start the class to start from
	 * @return       the walker
	 */
	public static ClassWalker walk(Class<?> start) {
		return new ClassWalker(start);
	}

	@Nonnull
	@Override
	public Iterator<Class<?>> iterator() {
		return new Iterator<>() {

			private final Set<Class<?>> doneParents = new HashSet<>();
			private final Deque<Class<?>> inProgressParents = new LinkedList<>();

			{
				inProgressParents.addLast(clazz);
				doneParents.add(end);
			}

			@Override
			public boolean hasNext() {
				return !inProgressParents.isEmpty();
			}

			@Override
			public Class<?> next() {
				if (inProgressParents.isEmpty())
					throw new NoSuchElementException();
				Class<?> current = inProgressParents.removeFirst();
				doneParents.add(current);
				for (Class<?> parent : current.getInterfaces()) {
					if (!doneParents.contains(parent)) {
						inProgressParents.addLast(parent);
					}
				}

				Class<?> parent = current.getSuperclass();
				if (parent != null && !doneParents.contains(parent)) {
					inProgressParents.addLast(parent);
				}
				return current;
			}
		};
	}

}
