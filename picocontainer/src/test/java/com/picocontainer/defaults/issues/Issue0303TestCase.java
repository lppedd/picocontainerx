/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *****************************************************************************/
package com.picocontainer.defaults.issues;

import java.lang.reflect.Method;

import org.junit.Test;

import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.DefaultPicoContainer;
import com.picocontainer.MutablePicoContainer;
import com.picocontainer.Startable;
import com.picocontainer.lifecycle.ReflectionLifecycleStrategy;
import com.picocontainer.lifecycle.StartableLifecycleStrategy;
import com.picocontainer.monitors.NullComponentMonitor;

@SuppressWarnings("serial")
public class Issue0303TestCase {

	public static class SwallowingComponentMonitor extends NullComponentMonitor {
		@Override
		public void lifecycleInvocationFailed(final MutablePicoContainer container,
				final ComponentAdapter<?> componentAdapter, final Method method, final Object instance,
				final RuntimeException cause) {
			// swallow it
		}
	}

	public static class Starter implements Startable {

		public void start() {
			throw new RuntimeException("deliberate exception");
		}

		/**
		 * {@inheritDoc}
		 */
		public void stop() {
			// empty
		}
	}

	@Test
	public void testCanSwallowExceptionFromReflectionLifecycleStrategy() {
		ComponentMonitor monitor = new SwallowingComponentMonitor();
		DefaultPicoContainer container =
				new DefaultPicoContainer(null, new StartableLifecycleStrategy(monitor), monitor);
		container.addComponent(new Starter());
		container.start();
	}

	@Test
	public void testCanSwallowExceptionFromStarableLifecycleStrategy() {
		ComponentMonitor monitor = new SwallowingComponentMonitor();
		DefaultPicoContainer container =
				new DefaultPicoContainer(null, new ReflectionLifecycleStrategy(monitor), monitor);
		container.addComponent(new Starter());
		container.start();
	}
}
