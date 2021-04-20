/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by Joerg Schaible                                           *
 *****************************************************************************/
package com.picocontainer.defaults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import org.junit.Test;

import com.picocontainer.ComponentAdapter;
import com.picocontainer.DefaultPicoContainer;
import com.picocontainer.PicoCompositionException;
import com.picocontainer.PicoException;
import com.picocontainer.injectors.ConstructorInjection;
import com.picocontainer.monitors.AbstractComponentMonitor;

/**
 * Unit tests for the several PicoException classes.
 */
@SuppressWarnings("serial")
public class PicoExceptionsTestCase {

    final static public String MESSAGE = "Message of the exception";
    final static public Throwable THROWABLE = new Throwable();

    @SuppressWarnings({ "unchecked" })
    final void executeTestOfStandardException(final Class clazz) {
        final ComponentAdapter<?> componentAdapter = new ConstructorInjection.ConstructorInjector(new AbstractComponentMonitor(), false, false, clazz, clazz, null);
        DefaultPicoContainer pico = new DefaultPicoContainer();
        pico.addComponent(MESSAGE);
        Exception exception = (Exception) componentAdapter.getComponentInstance(pico, ComponentAdapter.NOTHING.class);
            assertEquals(MESSAGE, exception.getMessage());
        pico = new DefaultPicoContainer();
        pico.addComponent(THROWABLE);
        exception = (PicoException) componentAdapter.getComponentInstance(pico, ComponentAdapter.NOTHING.class);
        assertSame(THROWABLE, exception.getCause());
        pico.addComponent(MESSAGE);
        exception = (PicoException) componentAdapter.getComponentInstance(pico, ComponentAdapter.NOTHING.class);
        assertEquals(MESSAGE, exception.getMessage());
        assertSame(THROWABLE, exception.getCause());
   }

    @Test public void testPicoInitializationException() {
        executeTestOfStandardException(PicoCompositionException.class);
    }

    @Test public void testPicoInitializationExceptionWithDefaultConstructor() {
        TestException e = new TestException(null);
        assertNull(e.getMessage());
        assertNull(e.getCause());
    }

    private static class TestException extends PicoCompositionException {
        public TestException(final String message) {
            super(message);
        }
    }

    @Test public void testPrintStackTrace() throws IOException {
        PicoException nestedException = new PicoException("Outer", new Exception("Inner")) {
        };
        PicoException simpleException = new PicoException("Outer") {
        };
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);
        nestedException.printStackTrace(printStream);
        simpleException.printStackTrace(printStream);
        out.close();
        assertTrue(out.toString().indexOf("Caused by:") > 0);
        out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out);
        nestedException.printStackTrace(writer);
        simpleException.printStackTrace(writer);
        writer.flush();
        out.close();
        assertTrue(out.toString().indexOf("Caused by:") > 0);
        //simpleException.printStackTrace();
    }
}
