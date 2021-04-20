/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer.behaviors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import com.picocontainer.Characteristics;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.adapters.InstanceAdapter;
import com.picocontainer.annotations.Cache;
import com.picocontainer.behaviors.AdaptingBehavior;
import com.picocontainer.behaviors.Caching;
import com.picocontainer.behaviors.ImplementationHiding;
import com.picocontainer.behaviors.PropertyApplying;
import com.picocontainer.behaviors.Synchronizing;
import com.picocontainer.containers.EmptyPicoContainer;
import com.picocontainer.injectors.CompositeInjection;
import com.picocontainer.lifecycle.NullLifecycleStrategy;
import com.picocontainer.monitors.NullComponentMonitor;
import com.thoughtworks.xstream.XStream;

@SuppressWarnings("serial")
public class AdaptingBehaviorTestCase {

    @Test public void testCachingBehaviorCanBeAddedByCharacteristics() {
        AdaptingBehavior adaptingBehavior = new AdaptingBehavior();
        Properties cc = new Properties();
        mergeInto(Characteristics.CACHE,cc);
        ComponentAdapter ca = adaptingBehavior.createComponentAdapter(new NullComponentMonitor(), new NullLifecycleStrategy(), cc, Map.class, HashMap.class, null, null, null);
        assertTrue(ca instanceof Caching.Cached);
        Map map = (Map)ca.getComponentInstance(new EmptyPicoContainer(), ComponentAdapter.NOTHING.class);
        assertNotNull(map);
        Map map2 = (Map)ca.getComponentInstance(new EmptyPicoContainer(), ComponentAdapter.NOTHING.class);
        assertSame(map, map2);
        assertEquals(0, cc.size());
        assertEquals("Cached:CompositeInjector(ConstructorInjector)-interface java.util.Map",ca.toString());
    }

    @Test public void testCachingBehaviorCanBeAddedByAnnotation() {
        AdaptingBehavior adaptingBehavior = new AdaptingBehavior();
        Properties cc = new Properties();
        ComponentAdapter ca = adaptingBehavior.createComponentAdapter(new NullComponentMonitor(), new NullLifecycleStrategy(), cc, Map.class, MyHashMap.class, null, null, null);
        assertTrue(ca instanceof Caching.Cached);
        Map map = (Map)ca.getComponentInstance(new EmptyPicoContainer(), ComponentAdapter.NOTHING.class);
        assertNotNull(map);
        Map map2 = (Map)ca.getComponentInstance(new EmptyPicoContainer(), ComponentAdapter.NOTHING.class);
        assertSame(map, map2);
        assertEquals(0, cc.size());
        assertEquals("Cached:CompositeInjector(ConstructorInjector)-interface java.util.Map",ca.toString());
    }

    @Cache
    public static class MyHashMap extends HashMap {
        public MyHashMap() {
        }
    }

    public static class MyHashMap2 extends HashMap {
        private String foo;

        public MyHashMap2() {
        }
        public void setFoo(final String foo) {
            this.foo = foo;
        }
    }

    @Test public void testImplementationHidingBehaviorCanBeAddedByCharacteristics() {
        AdaptingBehavior adaptingBehavior = new AdaptingBehavior();
        Properties cc = new Properties();
        mergeInto(Characteristics.HIDE_IMPL,cc);
        ComponentAdapter ca = adaptingBehavior.createComponentAdapter(new NullComponentMonitor(), new NullLifecycleStrategy(), cc, Map.class, HashMap.class, null, null, null);
        assertTrue(ca instanceof ImplementationHiding.HiddenImplementation);
        Map map = (Map)ca.getComponentInstance(new EmptyPicoContainer(), ComponentAdapter.NOTHING.class);
        assertNotNull(map);
        assertTrue(!(map instanceof HashMap));

        assertEquals(0, cc.size());
        assertEquals("Hidden:CompositeInjector(ConstructorInjector)-interface java.util.Map",ca.toString());

    }

    @Test public void testPropertyApplyingBehaviorCanBeAddedByCharacteristics() {
        AdaptingBehavior adaptingBehavior = new AdaptingBehavior();
        Properties cc = new Properties();
        mergeInto(Characteristics.PROPERTY_APPLYING,cc);
        ComponentAdapter ca = adaptingBehavior.createComponentAdapter(new NullComponentMonitor(), new NullLifecycleStrategy(), cc, Map.class, MyHashMap2.class, null, null, null);
        assertTrue(ca instanceof PropertyApplying.PropertyApplicator);
        PropertyApplying.PropertyApplicator pa = (PropertyApplying.PropertyApplicator)ca;
        pa.setProperty("foo", "bar");
        Map map = (Map)ca.getComponentInstance(new EmptyPicoContainer(), ComponentAdapter.NOTHING.class);
        assertNotNull(map);
        assertTrue(map instanceof HashMap);
        assertTrue(map instanceof MyHashMap2);
        assertEquals("bar", ((MyHashMap2) map).foo);

        assertEquals(0, cc.size());
        assertEquals("PropertyApplied:CompositeInjector(ConstructorInjector)-interface java.util.Map",ca.toString());
    }

    @Test public void testSetterInjectionCanBeTriggereedMeaningAdaptiveInjectorIsUsed() {
        AdaptingBehavior adaptingBehavior = new AdaptingBehavior();
        Properties cc = new Properties();
        mergeInto(Characteristics.SDI,cc);
        ComponentAdapter ca = adaptingBehavior.createComponentAdapter(new NullComponentMonitor(), new NullLifecycleStrategy(), cc, Map.class, HashMap.class, null, null, null);
        assertTrue(ca instanceof CompositeInjection.CompositeInjector);
        Map map = (Map)ca.getComponentInstance(new EmptyPicoContainer(), ComponentAdapter.NOTHING.class);
        assertNotNull(map);
        assertEquals(0, cc.size());
        assertEquals("CompositeInjector(ConstructorInjector+SetterInjector)-interface java.util.Map", ca.toString());
    }

    @Test public void testCachingAndImplHidingAndThreadSafetySetupCorrectly() {
        AdaptingBehavior adaptingBehavior = new AdaptingBehavior();
        Properties cc = new Properties();
        mergeInto(Characteristics.CACHE,cc);
        mergeInto(Characteristics.HIDE_IMPL,cc);
        mergeInto(Characteristics.SYNCHRONIZE,cc);
        ComponentAdapter ca = adaptingBehavior.createComponentAdapter(new NullComponentMonitor(), new NullLifecycleStrategy(), cc, Map.class, HashMap.class, null, null, null);
        assertTrue(ca instanceof Caching.Cached);
        Map map = (Map)ca.getComponentInstance(new EmptyPicoContainer(), null);
        assertNotNull(map);
        assertTrue(!(map instanceof HashMap));

        XStream xs = new XStream();
        String foo = xs.toXML(ca);

        int ih = foo.indexOf(ImplementationHiding.HiddenImplementation.class.getName());
        int sb = foo.indexOf(Synchronizing.Synchronized.class.getName());

        // check right nesting order
        assertTrue(ih>0);
        assertTrue(sb>0);
        assertTrue(sb>ih);

        assertEquals(0, cc.size());
        assertEquals("Cached:Hidden:Synchronized:CompositeInjector(ConstructorInjector)-interface java.util.Map",ca.toString());
    }

    @Test public void testCachingAndImplHidingAndThreadSafetySetupCorrectlyForExtraCaching() {
        Caching cbf = new Caching();
        AdaptingBehavior adaptingBehavior = new AdaptingBehavior();
        cbf.wrap(adaptingBehavior);
        Properties cc = new Properties();
        mergeInto(Characteristics.CACHE,cc);
        mergeInto(Characteristics.HIDE_IMPL,cc);
        mergeInto(Characteristics.SYNCHRONIZE,cc);
        ComponentAdapter ca = cbf.createComponentAdapter(new NullComponentMonitor(), new NullLifecycleStrategy(), cc, Map.class, HashMap.class, null, null, null);
        assertTrue(ca instanceof Caching.Cached);
        Map map = (Map)ca.getComponentInstance(new EmptyPicoContainer(), ComponentAdapter.NOTHING.class);
        assertNotNull(map);
        assertTrue(!(map instanceof HashMap));

        XStream xs = new XStream();
        String foo = xs.toXML(ca).replace("_-", "$"); // xstream inner classes are represented like Outter_-Inner

        String str = "<" + Caching.Cached.class.getName() + ">";
        assertTrue(foo.indexOf(str, 0)  > -1);  // xml does start with CB
        assertFalse(foo.indexOf("<" + Caching.Cached.class.getName() + ">", 1)  > -1); // but only contains it once.
        assertEquals("Cached:Hidden:Synchronized:CompositeInjector(ConstructorInjector)-interface java.util.Map",ca.toString());
    }

    @Test public void testCachingAndImplHidingAndThreadSafetySetupCorrectlyForExtraCachingForAdapter() {
        Caching caching = new Caching();
        AdaptingBehavior abf = new AdaptingBehavior();
        caching.wrap(abf);
        Properties cc = new Properties();
        mergeInto(Characteristics.CACHE,cc);
        mergeInto(Characteristics.HIDE_IMPL,cc);
        mergeInto(Characteristics.SYNCHRONIZE,cc);
        ComponentAdapter ca = caching.addComponentAdapter(new NullComponentMonitor(), new NullLifecycleStrategy(), cc, new InstanceAdapter(Map.class, new HashMap(), new NullLifecycleStrategy(), new NullComponentMonitor()));
        assertTrue(ca instanceof Caching.Cached);
        Map map = (Map)ca.getComponentInstance(new EmptyPicoContainer(), ComponentAdapter.NOTHING.class);
        assertNotNull(map);
        assertTrue(!(map instanceof HashMap));

        XStream xs = new XStream();
        String foo = xs.toXML(ca).replace("_-", "$"); // xstream inner classes are represented like Outter_-Inner

        assertTrue(foo.indexOf("<" + Caching.Cached.class.getName() + ">", 0)  > -1);  // xml does start with CB
        assertFalse(foo.indexOf("<" + Caching.Cached.class.getName() + ">", 1)  > -1); // but only contains it once.
        assertEquals("Cached:Hidden:Synchronized:Instance-interface java.util.Map",ca.toString());
    }

    public void mergeInto(final Properties p, final Properties into) {
        Enumeration e = p.propertyNames();
        while (e.hasMoreElements()) {
            String s = (String)e.nextElement();
            into.setProperty(s, p.getProperty(s));
        }
    }

}
