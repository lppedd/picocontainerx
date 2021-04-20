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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Named;

import org.junit.Test;
import com.picocontainer.containers.SomeQualifier;

import com.picocontainer.DefaultPicoContainer;
import com.picocontainer.MutablePicoContainer;
import com.picocontainer.PicoBuilder;
import com.picocontainer.ComponentAdapter.NOTHING;
import com.picocontainer.annotations.Inject;
import com.picocontainer.containers.JSR330PicoContainer;
import com.picocontainer.injectors.AbstractInjector;
import com.picocontainer.injectors.AnnotatedFieldInjection;
import com.picocontainer.injectors.SetterInjection;
import com.picocontainer.injectors.AnnotatedFieldInjection.AnnotatedFieldInjector;
import com.picocontainer.monitors.NullComponentMonitor;

public class AnnotatedFieldInjectorTestCase {

    public static class Helicopter {
        @Inject
        private PogoStick pogo;

        public Helicopter() {
        }
    }

    public static class Helicopter2 {
        private PogoStick pogo;

        public Helicopter2() {
        }
    }

    public static class PogoStick {
    }

    @Test
    public void testFieldInjection() {
        MutablePicoContainer pico = new DefaultPicoContainer();
        pico.addAdapter(new AnnotatedFieldInjection.AnnotatedFieldInjector(Helicopter.class, Helicopter.class, null,
                new NullComponentMonitor(), false, true, Inject.class));
        pico.addComponent(PogoStick.class, new PogoStick());
        Helicopter chopper = pico.getComponent(Helicopter.class);
        assertNotNull(chopper);
        assertNotNull(chopper.pogo);
    }

    @Test
    public void testFieldInjectionWithoutAnnotationDoesNotWork() {
        MutablePicoContainer pico = new DefaultPicoContainer();
        pico.addAdapter(new AnnotatedFieldInjection.AnnotatedFieldInjector(Helicopter2.class, Helicopter2.class, null,
                new NullComponentMonitor(), false, true, Inject.class));
        pico.addComponent(PogoStick.class, new PogoStick());
        Helicopter2 chopper = pico.getComponent(Helicopter2.class);
        assertNotNull(chopper);
        assertNull(chopper.pogo);
    }

    @Test
    public void testFieldDeosNotHappenWithoutRightInjectorDoesNotWork() {
        MutablePicoContainer pico = new DefaultPicoContainer();
        pico.addAdapter(new SetterInjection.SetterInjector(Helicopter.class, Helicopter.class, new NullComponentMonitor(), "set", false, "", false, null
        ));
        pico.addComponent(PogoStick.class, new PogoStick());
        Helicopter chopper = pico.getComponent(Helicopter.class);
        assertNotNull(chopper);
        assertNull(chopper.pogo);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(value = {ElementType.METHOD, ElementType.FIELD})
    public @interface AlternativeInject {
    }

    public static class Helicopter3 {
        @AlternativeInject
        private PogoStick pogo;

        public Helicopter3() {
        }
    }

    @Test
    public void testFieldInjectionWithAlternativeInjectionAnnotation() {
        MutablePicoContainer pico = new DefaultPicoContainer();
        pico.addAdapter(new AnnotatedFieldInjection.AnnotatedFieldInjector(Helicopter3.class, Helicopter3.class, null,
                new NullComponentMonitor(), false, true, AlternativeInject.class));
        pico.addComponent(PogoStick.class, new PogoStick());
        Helicopter3 chopper = pico.getComponent(Helicopter3.class);
        assertNotNull(chopper);
        assertNotNull(chopper.pogo);
    }

    public static abstract class A {
        @Inject
        protected C c;
    }

    public static class B extends A {
    }

    public static class C {
    }

    @Test
    public void testThatSuperClassCanHaveAnnotatedFields() {
        MutablePicoContainer container = new PicoBuilder().withAutomatic().build();
        container.addComponent(C.class);
        container.addComponent(B.class);

        B b = container.getComponent(B.class);
        assertNotNull(b);
        assertNotNull(b.c);
    }

    public static abstract class A2 {
        @Inject
        protected D2 d2;
    }

    public static abstract class B2 extends A2 {
    }

    public static class C2 extends B2 {
    }

    public static class D2 {
    }

    @Test
    public void testThatEvenMoreSuperClassCanHaveAnnotatedFields() {
        MutablePicoContainer container = new PicoBuilder().withAnnotatedFieldInjection().build();
        container.addComponent(D2.class);
        container.addComponent(C2.class);

        C2 c2 = container.getComponent(C2.class);
        assertNotNull(c2);
        assertNotNull(c2.d2);
    }

    @Test
    public void testThatEvenMoreSuperClassCanHaveAnnotatedFieldsViaAdaptingInjection() {
        MutablePicoContainer container = new PicoBuilder().build();
        container.addComponent(D2.class);
        container.addComponent(C2.class);

        C2 c2 = container.getComponent(C2.class);
        assertNotNull(c2);
        assertNotNull(c2.d2);
    }

    @Test public void testFieldInjectionByTypeWhereNoMatch() {
        MutablePicoContainer container = new PicoBuilder().withAnnotatedFieldInjection().build();
        container.setName("parent");
        container.addComponent(C2.class);
        try {
            container.getComponent(C2.class);
            fail("should have barfed");
        } catch (AbstractInjector.UnsatisfiableDependenciesException e) {
            String expected = "C2 has unsatisfied dependency for fields [ A2.d2 (field's type is D2) ] from parent:1<|";
            String actual = e.getMessage();
            actual = actual.replace(AnnotatedFieldInjectorTestCase.class.getName() + "$", "");
            assertEquals(expected, actual);
        }
    }


    public static interface A3 {

    }

    public static class B3 implements A3 {

    }

    public static class C3 implements A3 {

    }

    @SomeQualifier
    public static class D3 implements A3 {

    }



    public static class Z3 {

    	@Inject
    	@Named("b3")
    	private A3 b3;

    	public A3 getB3() {
    		return b3;
    	}


    	@Inject
    	@Named("c3")
    	private A3 c3;

    	public A3 getC3() {
    		return c3;
    	}

    	@Inject
    	@SomeQualifier
    	private A3 d3;
    	public A3 getD3() {
    		return d3;
    	}
    }

    @Test
    public void testFieldInjectionWithNamedQualifier() {
        JSR330PicoContainer container = new JSR330PicoContainer(new PicoBuilder().withAnnotatedFieldInjection().build());

        container.addComponent("b3", B3.class)
        		.addComponent("c3", C3.class)
        		.addComponent(D3.class)
        		.addComponent(Z3.class);


        Z3 z3 =  container.getComponent(Z3.class);
        assertNotNull(z3);
        assertTrue(z3.getB3() instanceof B3);
        assertTrue(z3.getC3() instanceof C3);
    	assertTrue(z3.getD3() instanceof D3);
    }


    public static class OrderBase {
    	@Inject
    	public static String something;

    	@Inject
    	public String somethingElse;

    	public static void reset() {
    		something = null;
    	}
    }

    public static class OrderChild extends OrderBase {

    	@Inject
    	public static String somethingChild;

    	@Inject
    	public String somethingElseChild;


    	public static void reset() {
    		somethingChild = null;
    		OrderBase.reset();
    	}
    }


    @Test
    public void testPartialDecorationOnBaseClassDoesntPropagateToChildren() {
    	JSR330PicoContainer pico = new JSR330PicoContainer().addComponent(String.class, "Testing");

    	OrderChild child = new OrderChild();
    	OrderChild.reset();

    	@SuppressWarnings("unchecked")
    	AnnotatedFieldInjector<OrderChild> adapter = new AnnotatedFieldInjector<OrderChild>(OrderChild.class, OrderChild.class, null, new NullComponentMonitor(), false, false, Inject.class);
    	assertNotNull(adapter);

    	adapter.partiallyDecorateComponentInstance(pico, NOTHING.class, child, OrderBase.class);

    	assertNull(OrderChild.somethingChild);
    	assertNull(child.somethingElseChild);
    	//Won't get injected here since its a static
    	//assertEquals("Testing", child.something);
    	assertEquals("Testing", child.somethingElse);
    }

    @Test
    public void testPartialDecorationOnChildClassDoesntPropagateToParent() {
    	JSR330PicoContainer pico = new JSR330PicoContainer().addComponent(String.class, "Testing");

    	OrderChild child = new OrderChild();
    	OrderChild.reset();

    	assertNull(OrderChild.somethingChild);
    	assertNull(OrderBase.something);

    	@SuppressWarnings("unchecked")
    	AnnotatedFieldInjector<OrderChild> adapter = new AnnotatedFieldInjector<OrderChild>(OrderChild.class, OrderChild.class, null, new NullComponentMonitor(), false, false, Inject.class);
    	assertNotNull(adapter);

    	adapter.partiallyDecorateComponentInstance(pico, NOTHING.class, child, OrderChild.class);

    	assertNull(OrderBase.something);
    	assertNull(child.somethingElse);

    	//Won't inject here since its a static
    	//assertEquals("Testing", OrderChild.somethingChild);
    	assertEquals("Testing", child.somethingElseChild);
    	OrderChild.reset();

    }


    public static class Multi {

    }

    public static class Single {

    	@Inject
    	public static Multi oneValue;

    	@Inject
    	public Multi manyValues;

    }

    @Test
    public void staticMembersAreOnlyInjectedOneTimeUponFirstInitialization() {
    	JSR330PicoContainer pico = new JSR330PicoContainer()
    			.addComponent(Multi.class)
    			.addComponent(Single.class);

    	Single singleOne = pico.getComponent(Single.class);
    	Multi staticOne = Single.oneValue;
    	Multi manyOne = singleOne.manyValues;


    	Single singleTwo = pico.getComponent(Single.class);
    	Multi staticTwo = Single.oneValue;
    	Multi manyTwo = singleTwo.manyValues;

    	assertNotSame(singleOne, singleTwo);
    	assertNotSame(manyOne, manyTwo);

    	//The important part :)
    	assertSame(staticOne, staticTwo);
    }

}