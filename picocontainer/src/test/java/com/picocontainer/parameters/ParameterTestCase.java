/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer.parameters;

import com.picocontainer.ComponentAdapter;
import com.picocontainer.DefaultPicoContainer;
import com.picocontainer.MutablePicoContainer;
import com.picocontainer.NameBinding;
import com.picocontainer.Parameter;
import com.picocontainer.PicoCompositionException;
import com.picocontainer.adapters.NullCA;
import com.picocontainer.testmodel.DependsOnTouchable;
import com.picocontainer.testmodel.SimpleTouchable;
import com.picocontainer.testmodel.Touchable;
import com.picocontainer.visitors.VerifyingVisitor;
import junit.framework.TestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;


/**
 * @author Jon Tirsen (tirsen@codehaus.org)
 */
public final class ParameterTestCase {

    public static class FooNameBinding implements NameBinding {
        public String getName() {
            return "";
        }
    }

    final NameBinding pn = new FooNameBinding();

    @Test public void testComponentParameterFetches() throws PicoCompositionException {
        DefaultPicoContainer pico = new DefaultPicoContainer();
        ComponentAdapter adapter = pico.addComponent(Touchable.class, SimpleTouchable.class).getComponentAdapter(Touchable.class,
        		NameBinding.NULL);
        assertNotNull(adapter);
        assertNotNull(pico.getComponent(Touchable.class));
        Touchable touchable = (Touchable) ComponentParameter.DEFAULT.resolve(pico, new NullCA(String.class), null, Touchable.class, pn,
                                                                                     false, null).resolveInstance(ComponentAdapter.NOTHING.class);
        assertNotNull(touchable);
    }

    @Test public void testComponentParameterExcludesSelf() throws PicoCompositionException {
        DefaultPicoContainer pico = new DefaultPicoContainer();
        ComponentAdapter adapter = pico.addComponent(Touchable.class, SimpleTouchable.class).getComponentAdapter(Touchable.class,
        		NameBinding.NULL);

        assertNotNull(pico.getComponent(Touchable.class));
        Touchable touchable = (Touchable) ComponentParameter.DEFAULT.resolve(pico, adapter, null, Touchable.class, pn,
                                                                                     false, null).resolveInstance(ComponentAdapter.NOTHING.class);
        assertNull(touchable);
    }

    @Test public void testConstantParameter() throws PicoCompositionException {
        Object value = new Object();
        ConstantParameter parameter = new ConstantParameter(value);
        MutablePicoContainer picoContainer = new DefaultPicoContainer();
        assertSame(value, parameter.resolve(picoContainer, null, null, Object.class, pn, false, null).resolveInstance(ComponentAdapter.NOTHING.class));
    }

    @Test public void testDependsOnTouchableWithTouchableSpecifiedAsConstant() throws PicoCompositionException {
        DefaultPicoContainer pico = new DefaultPicoContainer();
        SimpleTouchable touchable = new SimpleTouchable();
        pico.addComponent(DependsOnTouchable.class, DependsOnTouchable.class, new ConstantParameter(touchable));
        pico.getComponents();
        assertTrue(touchable.wasTouched);
    }

    @Test public void testComponentParameterRespectsExpectedType() {
        MutablePicoContainer picoContainer = new DefaultPicoContainer();
        ComponentAdapter adapter = picoContainer.addComponent(Touchable.class, SimpleTouchable.class).getComponentAdapter(Touchable.class,
                                                                                                                          NameBinding.NULL);
        assertNull(ComponentParameter.DEFAULT.resolve(picoContainer, adapter, null, TestCase.class, pn, false, null).resolveInstance(ComponentAdapter.NOTHING.class));
    }

	@Test public void testComponentParameterResolvesPrimitiveType() {
        MutablePicoContainer picoContainer = new DefaultPicoContainer();
        ComponentAdapter adapter = picoContainer.addComponent("glarch", 239).getComponentAdapter("glarch");
        assertNotNull(adapter);
		Parameter parameter = new ComponentParameter("glarch");
        Parameter.Resolver resolve = parameter.resolve(picoContainer, adapter, null, Integer.TYPE, pn, false, null);
        Object object = resolve.resolveInstance(ComponentAdapter.NOTHING.class);
        assertNotNull(object);
		assertEquals(239, ((Integer) object).intValue());
	}

    @Test public void testConstantParameterRespectsExpectedType() {
        MutablePicoContainer picoContainer = new DefaultPicoContainer();
        Parameter parameter = new ConstantParameter(new SimpleTouchable());
        ComponentAdapter adapter = picoContainer.addComponent(Touchable.class, SimpleTouchable.class).getComponentAdapter(Touchable.class,
        		NameBinding.NULL);
        assertFalse(parameter.resolve(picoContainer, adapter, null, TestCase.class, pn, false, null).isResolved());
    }

    @Test public void testParameterRespectsExpectedType() throws PicoCompositionException {
        Parameter parameter = new ConstantParameter(Touchable.class);
        MutablePicoContainer picoContainer = new DefaultPicoContainer();
        assertFalse(parameter.resolve(picoContainer, null, null, TestCase.class, pn, false, null).isResolved());

        ComponentAdapter adapter = picoContainer.addComponent(Touchable.class, SimpleTouchable.class).getComponentAdapter(Touchable.class,
        		NameBinding.NULL);

        assertNull(ComponentParameter.DEFAULT.resolve(picoContainer, adapter, null, TestCase.class, pn, false, null).resolveInstance(ComponentAdapter.NOTHING.class));
    }

    @Test public void testConstantParameterWithPrimitives() throws PicoCompositionException {
        MutablePicoContainer picoContainer = new DefaultPicoContainer();
        Byte byteValue = (byte)5;
        ConstantParameter parameter = new ConstantParameter(byteValue);
        assertSame(byteValue, parameter.resolve(picoContainer, null, null, Byte.TYPE, pn, false, null).resolveInstance(ComponentAdapter.NOTHING.class));
        assertSame(byteValue, parameter.resolve(picoContainer, null, null, Byte.class, pn, false, null).resolveInstance(ComponentAdapter.NOTHING.class));
        Short shortValue = (short)5;
        parameter = new ConstantParameter(shortValue);
        assertSame(shortValue, parameter.resolve(picoContainer, null, null, Short.TYPE, pn, false, null).resolveInstance(ComponentAdapter.NOTHING.class));
        assertSame(shortValue, parameter.resolve(picoContainer, null, null, Short.class, pn, false, null).resolveInstance(ComponentAdapter.NOTHING.class));
        Integer intValue = 5;
        parameter = new ConstantParameter(intValue);
        assertSame(intValue, parameter.resolve(picoContainer, null, null, Integer.TYPE, pn, false, null).resolveInstance(ComponentAdapter.NOTHING.class));
        assertSame(intValue, parameter.resolve(picoContainer, null, null, Integer.class, pn, false, null).resolveInstance(ComponentAdapter.NOTHING.class));
        Long longValue = (long)5;
        parameter = new ConstantParameter(longValue);
        assertSame(longValue, parameter.resolve(picoContainer, null, null, Long.TYPE, pn, false, null).resolveInstance(ComponentAdapter.NOTHING.class));
        assertSame(longValue, parameter.resolve(picoContainer, null, null, Long.class, pn, false, null).resolveInstance(ComponentAdapter.NOTHING.class));
        Float floatValue = new Float(5.5);
        parameter = new ConstantParameter(floatValue);
        assertSame(floatValue, parameter.resolve(picoContainer, null, null, Float.TYPE, pn, false, null).resolveInstance(ComponentAdapter.NOTHING.class));
        assertSame(floatValue, parameter.resolve(picoContainer, null, null, Float.class, pn, false, null).resolveInstance(ComponentAdapter.NOTHING.class));
        Double doubleValue = 5.5;
        parameter = new ConstantParameter(doubleValue);
        assertSame(doubleValue, parameter.resolve(picoContainer, null, null, Double.TYPE, pn, false, null).resolveInstance(ComponentAdapter.NOTHING.class));
        assertSame(doubleValue, parameter.resolve(picoContainer, null, null, Double.class, pn, false, null).resolveInstance(ComponentAdapter.NOTHING.class));
        Boolean booleanValue = true;
        parameter = new ConstantParameter(booleanValue);
        assertSame(booleanValue, parameter.resolve(picoContainer, null, null, Boolean.TYPE, pn, false, null).resolveInstance(ComponentAdapter.NOTHING.class));
        assertSame(booleanValue, parameter.resolve(picoContainer, null, null, Boolean.class, pn, false, null).resolveInstance(ComponentAdapter.NOTHING.class));
        Character charValue = 'x';
        parameter = new ConstantParameter(charValue);
        assertSame(charValue, parameter.resolve(picoContainer, null, null, Character.TYPE, pn, false, null).resolveInstance(ComponentAdapter.NOTHING.class));
        assertSame(charValue, parameter.resolve(picoContainer, null, null, Character.class, pn, false, null).resolveInstance(ComponentAdapter.NOTHING.class));
    }

    @Test public void testConstantParameterWithPrimitivesRejectsUnexpectedType() throws PicoCompositionException {
        MutablePicoContainer picoContainer = new DefaultPicoContainer();
        Byte byteValue = (byte)5;
        ConstantParameter parameter = new ConstantParameter(byteValue);
        assertFalse(parameter.resolve(picoContainer, null, null, Integer.TYPE, pn, false, null).isResolved());
        Short shortValue = (short)5;
        parameter = new ConstantParameter(shortValue);
        assertFalse(parameter.resolve(picoContainer, null, null, Byte.TYPE, pn, false, null).isResolved());
        Integer intValue = 5;
        parameter = new ConstantParameter(intValue);
        assertFalse(parameter.resolve(picoContainer, null, null, Byte.TYPE, pn, false, null).isResolved());
        Long longValue = (long)5;
        parameter = new ConstantParameter(longValue);
        assertFalse(parameter.resolve(picoContainer, null, null, Byte.TYPE, pn, false, null).isResolved());
        Float floatValue = new Float(5.5);
        parameter = new ConstantParameter(floatValue);
        assertFalse(parameter.resolve(picoContainer, null, null, Byte.TYPE, pn, false, null).isResolved());
        Double doubleValue = 5.5;
        parameter = new ConstantParameter(doubleValue);
        assertFalse(parameter.resolve(picoContainer, null, null, Byte.TYPE, pn, false, null).isResolved());
        Boolean booleanValue = true;
        parameter = new ConstantParameter(booleanValue);
        assertFalse(parameter.resolve(picoContainer, null, null, Byte.TYPE, pn, false, null).isResolved());
        Character charValue = 'x';
        parameter = new ConstantParameter(charValue);
        assertFalse(parameter.resolve(picoContainer, null, null, Byte.TYPE, pn, false, null).isResolved());
    }

    @Test public void testKeyClashBug118() throws PicoCompositionException {
        DefaultPicoContainer pico = new DefaultPicoContainer();
        pico.addComponent("A", String.class, new ConstantParameter("A"));
        pico.addComponent("B", String.class, new ConstantParameter("A"));
        new VerifyingVisitor().traverse(pico);
    }

}
