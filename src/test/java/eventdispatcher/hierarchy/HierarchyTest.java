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

package eventdispatcher.hierarchy;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.matyrobbrt.eventdispatcher.EventBus;

@SuppressWarnings("static-method")
final class HierarchyTest {

	//@formatter:off
	public static final EventBus WITH_HIERARCHY = EventBus.builder("With Hierarchy")
			.walksEventHierarcy(true)
			.build();
	
	public static final EventBus WITHOUT_HIERARCHY = EventBus.builder("Without Hierarchy")
			.walksEventHierarcy(false)
			.build();
	//@formatter:on

	@Test
	void testWithHierarchy() {
        final HT$MainEvent event = new HT$MainEvent();
		WITH_HIERARCHY.post(event);
		assertThat(event.getNumberA()).isEqualTo(12);
		assertThat(event.getNumberB()).isEqualTo(120);
	}

	@Test
	void testWithoutHierarchy() {
        final HT$MainEvent event = new HT$MainEvent();
		WITHOUT_HIERARCHY.post(event);
		assertThat(event.getNumberA()).isEqualTo(5);
		assertThat(event.getNumberB()).isEqualTo(120);
	}

	@BeforeAll
	static void registerSubscriber() {
        final HierarchySubscriber sub = new HierarchySubscriber();
		WITH_HIERARCHY.register(sub);
		WITHOUT_HIERARCHY.register(sub);
	}
}
