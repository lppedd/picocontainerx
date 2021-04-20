/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer.injectors;

import com.picocontainer.ComponentAdapter;
import com.picocontainer.lifecycle.NullLifecycleStrategy;
import com.picocontainer.monitors.ConsoleComponentMonitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TypedFieldInjectionTestCase {
    private static final String FIELD_TYPES = Integer.class.getName() + " " + PogoStick.class.getName() + " " + Float.class.getName();

    public static class Helicopter {
        private PogoStick pogo;
    }

    public static class PogoStick {
    }


    @Test public void testFactoryMakesNamedInjector() {

        TypedFieldInjection injectionFactory = new TypedFieldInjection();

        ConsoleComponentMonitor cm = new ConsoleComponentMonitor();
        Properties props = new Properties();
        props.setProperty("injectionFieldTypes", FIELD_TYPES);
        ComponentAdapter ca = injectionFactory.createComponentAdapter(cm, new NullLifecycleStrategy(),
                props, Map.class, HashMap.class, null, null, null);

        Assertions.assertTrue(ca instanceof TypedFieldInjection.TypedFieldInjector);

        TypedFieldInjection.TypedFieldInjector tfi = (TypedFieldInjection.TypedFieldInjector) ca;

        Assertions.assertEquals(3, tfi.getInjectionFieldTypes().size());
        Assertions.assertEquals(Integer.class.getName(), tfi.getInjectionFieldTypes().get(0));
        Assertions.assertEquals(PogoStick.class.getName(), tfi.getInjectionFieldTypes().get(1));
        Assertions.assertEquals(Float.class.getName(), tfi.getInjectionFieldTypes().get(2));
    }

    @Test
    public void testPropertiesAreRight() {
        Properties props = TypedFieldInjection.injectionFieldTypes(FIELD_TYPES);
        Assertions.assertEquals("java.lang.Integer com.picocontainer.injectors.TypedFieldInjectionTestCase$PogoStick java.lang.Float", props.getProperty("injectionFieldTypes"));
        Assertions.assertEquals(1, props.size());
    }


}
