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

package io.github.matyrobbrt.eventdispatcher.reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.matyrobbrt.eventdispatcher.LazySupplier;

/**
 * Used for automatically registering classes or static objects to an
 * {@link io.github.matyrobbrt.eventdispatcher.EventBus}, using
 * {@link io.github.matyrobbrt.eventdispatcher.EventBus#register(Class)} and
 * {@link io.github.matyrobbrt.eventdispatcher.EventBus#register(Object)}
 * respectively. <br>
 * Provide an {@link AnnotationFilter} for each annotation you want to look for,
 * and a filter for deciding if an object should be registered or not. <br>
 * 
 * <strong>This feature requires manually adding the
 * <a href="https://github.com/ronmamo/reflections">Reflections library</a> to
 * the project.</strong>
 * 
 * @author matyrobbrt
 *
 */
public class AnnotationProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationProvider.class);
	private static final LazySupplier<Reflections> DEFAULT_REFLECTIONS = LazySupplier.of(() -> new Reflections(
			new ConfigurationBuilder().forPackages("").setScanners(Scanners.FieldsAnnotated, Scanners.TypesAnnotated)));

	private final Supplier<Reflections> reflections;
	private final List<AnnotationFilter<?>> filters;

	/**
	 * Creates a new {@link AnnotationProvider}.
	 * 
	 * @param reflections a supplier containing the {@link Reflections} used for
	 *                    discovering the objects annotated with an annotation. <br>
	 *                    It is recommended that these reflections have at least the
	 *                    {@link Scanners#FieldsAnnotated} and
	 *                    {@link Scanners#TypesAnnotated} scanners.
	 * @param filters     the {@link AnnotationFilter}s used for discovering the
	 *                    objects that should be automatically registered.
	 */
	public AnnotationProvider(Supplier<Reflections> reflections, List<AnnotationFilter<?>> filters) {
		this.reflections = reflections;
		this.filters = filters;
	}

	/**
	 * Creates a new {@link AnnotationProvider}, using the
	 * {@link #DEFAULT_REFLECTIONS}.
	 * 
	 * @param filters the {@link AnnotationFilter}s used for discovering the objects
	 *                that should be automatically registered.
	 */
	public AnnotationProvider(List<AnnotationFilter<?>> filters) {
		this(DEFAULT_REFLECTIONS, filters);
	}

	/**
	 * Creates a new {@link AnnotationProvider}.
	 * 
	 * @param reflections a supplier containing the {@link Reflections} used for
	 *                    discovering the objects annotated with an annotation. <br>
	 *                    It is recommended that these reflections have at least the
	 *                    {@link Scanners#FieldsAnnotated} and
	 *                    {@link Scanners#TypesAnnotated} scanners.
	 * @param filters     the {@link AnnotationFilter}s used for discovering the
	 *                    objects that should be automatically registered.
	 */
	public AnnotationProvider(Supplier<Reflections> reflections, AnnotationFilter<?>... filters) {
		this.reflections = reflections;
		this.filters = Arrays.asList(filters);
	}

	/**
	 * Creates a new {@link AnnotationProvider}, using the
	 * {@link #DEFAULT_REFLECTIONS}.
	 * 
	 * @param filters     the {@link AnnotationFilter}s used for discovering the
	 *                    objects that should be automatically registered.
	 */
	public AnnotationProvider(AnnotationFilter<?>... filters) {
		this(DEFAULT_REFLECTIONS, filters);
	}

	//@formatter:off
	/**
	 * Searches for the objects annotated with the filters, and registers them to the bus.
	 * @param onField the consumer to accept on fields
	 * @param onClazz the consumer to accept on classes
	 */
	public void register(Consumer<? super Object> onField, Consumer<? super Class<?>> onClazz) {
		final Reflections ref = reflections.get();
		filters.forEach(filter -> {
			ref.getFieldsAnnotatedWith(filter.annotation())
				.stream()
				.filter(f -> Modifier.isStatic(f.getModifiers()) && filter.testObj(f.getAnnotation(filter.annotation())))
				.forEach(field -> {
					try {
						onField.accept(field.get(null));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						LOGGER.error("Exception while finding fields annotated with {} for registering to event bus!", filter.annotation(), e);
					}
				});
			
			ref.getTypesAnnotatedWith(filter.annotation())
				.stream()
				.filter(c -> Modifier.isStatic(c.getModifiers()) && filter.testObj(c.getAnnotation(filter.annotation())))
				.forEach(onClazz);
		});
	}
	//@formatter:on

	/**
	 * Used for the lookup of {@link AnnotationProvider}. <br>
	 * Contains an annotation type, and a filter for it, which will decide if that
	 * annotation instance should be processed.
	 * 
	 * @author     matyrobbrt
	 *
	 * @param  <A> the type of the annotation
	 */
    public static final class AnnotationFilter<A extends Annotation>
			implements Predicate<A> {

        private final Class<A> annotation;
        private final Predicate<A> predicate;

        public AnnotationFilter(Class<A> annotation, Predicate<A> predicate) {
            this.annotation = annotation;
            this.predicate = predicate;
        }

        public Class<A> annotation() {
            return annotation;
        }

        public Predicate<A> predicate() {
            return predicate;
        }

        @SuppressWarnings("unchecked")
		public boolean testObj(Object o) {
			return test((A) o);
		}

		@Override
		public boolean test(A t) {
			return predicate.test(t);
		}

	}
}
