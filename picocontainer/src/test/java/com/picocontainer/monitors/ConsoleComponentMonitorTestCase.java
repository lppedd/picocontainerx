/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer.monitors;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import com.picocontainer.ComponentMonitor;
import com.picocontainer.monitors.ConsoleComponentMonitor;

/**
 * @author Aslak Helles&oslash;y
 * @author Mauro Talevi
 */
public class ConsoleComponentMonitorTestCase {
    private ComponentMonitor monitor;
    private Constructor constructor;
    private Method method;

    @Before
    public void setUp() throws Exception {
        PrintStream out = System.out;
        constructor = getClass().getConstructor((Class[])null);
        method = getClass().getDeclaredMethod("setUp", (Class[])null);
        monitor = new ConsoleComponentMonitor(out);
    }

    @Test public void testShouldTraceInstantiating() {
        monitor.instantiating(null, null, constructor);
    }

    @Test public void testShouldTraceInstantiatedWithInjected() {
        monitor.instantiated(null, null, constructor, new Object(), new Object[0], 543);
    }

    @Test public void testShouldTraceInstantiationFailed() {
        monitor.instantiationFailed(null, null, constructor, new RuntimeException("doh"));
    }

    @Test public void testShouldTraceInvoking() {
        monitor.invoking(null, null, method, this, new Object[0]);
    }

    @Test public void testShouldTraceInvoked() {
        monitor.invoked(null, null, method, this, 543, null, new Object[0]);
    }

    @Test public void testShouldTraceInvocatiationFailed() {
        monitor.invocationFailed(method, this, new RuntimeException("doh"));
    }

}
