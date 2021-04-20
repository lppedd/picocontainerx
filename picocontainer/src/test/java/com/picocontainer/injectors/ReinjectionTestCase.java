/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/

package com.picocontainer.injectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static com.picocontainer.tck.MockFactory.mockeryWithCountingNamingScheme;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.picocontainer.tck.AbstractComponentFactoryTest;

import com.picocontainer.Characteristics;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentFactory;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.DefaultPicoContainer;
import com.picocontainer.PicoCompositionException;
import com.picocontainer.PicoContainer;
import com.picocontainer.behaviors.Caching;
import com.picocontainer.containers.EmptyPicoContainer;
import com.picocontainer.containers.TransientPicoContainer;
import com.picocontainer.injectors.AbstractInjectionType;
import com.picocontainer.injectors.AnnotatedMethodInjection;
import com.picocontainer.injectors.ConstructorInjection;
import com.picocontainer.injectors.MethodInjection;
import com.picocontainer.injectors.Reinjection;
import com.picocontainer.injectors.Reinjector;
import com.picocontainer.monitors.NullComponentMonitor;

@RunWith(JMock.class)
public class ReinjectionTestCase extends AbstractComponentFactoryTest {

    private final Mockery mockery = mockeryWithCountingNamingScheme();

    @Retention(RetentionPolicy.RUNTIME)
    @Target(value={ElementType.METHOD, ElementType.FIELD})
    public @interface Hurrah {
    }

    public static interface INeedsShoe {
        int doIt(String s);

        Object getBar();

        Object getString();
    }

    public static class NeedsShoe implements INeedsShoe {
        private final Shoe bar;
        private String string;

        public NeedsShoe(final Shoe bar) {
            this.bar = bar;
        }

        @Hurrah
        public int doIt(final String s) {
            this.string = s;
            return Integer.parseInt(s) / 2;
        }

        public int doInt(final int s) {
            this.string = "i="+ s;
            return s/2;
        }

        public Object getBar() {
            return bar;
        }

        public Object getString() {
            return string;
        }
        public static enum M {
            doIt("doIt", String.class);
            private Method method;

            M(final String s, final Class... paramTypes) {
                try {
                    method = NeedsShoe.class.getMethod(s, paramTypes);
                } catch (NoSuchMethodException e) {
                    throw new UnsupportedOperationException(e);
                }
            }
            public Method toMethod() {
                return method;
            }
        }
    }

    public static class Shoe {
    }

    private static Method DOIT_METHOD;

    static {
        try {
            DOIT_METHOD = NeedsShoe.class.getMethod("doIt", String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Test public void testCachedComponentCanBeReflectionMethodReinjectedByATransientChildContainer() {
        cachedComponentCanBeReinjectedByATransientChildContainer(new MethodInjection(DOIT_METHOD));
    }

    @Test public void testCachedComponentCanBeMethodNameReinjectedByATransientChildContainer() {
        cachedComponentCanBeReinjectedByATransientChildContainer(new MethodInjection("doIt"));
    }

    @Test public void testCachedComponentCanBeAnnotatedMethodReinjectedByATransientChildContainer() {
        cachedComponentCanBeReinjectedByATransientChildContainer(new AnnotatedMethodInjection(Hurrah.class, false));
    }

    private void cachedComponentCanBeReinjectedByATransientChildContainer(final AbstractInjectionType methodInjection) {
        DefaultPicoContainer parent = new DefaultPicoContainer(new Caching().wrap(new ConstructorInjection()));
        parent.addComponent(INeedsShoe.class, NeedsShoe.class);
        parent.addComponent(Shoe.class);
        parent.addComponent("12");

        INeedsShoe needsShoe = parent.getComponent(INeedsShoe.class);
        assertNotNull(needsShoe.getBar());
        assertTrue(needsShoe.getString() == null);

        TransientPicoContainer tpc = new TransientPicoContainer(new Reinjection(methodInjection, parent), parent);
        tpc.addComponent(INeedsShoe.class, NeedsShoe.class);

        INeedsShoe needsShoe2 = tpc.getComponent(INeedsShoe.class);
        assertSame(needsShoe, needsShoe2);
        assertNotNull(needsShoe2.getBar());
        assertNotNull(needsShoe2.getString());

        INeedsShoe needsShoe3 = parent.getComponent(INeedsShoe.class);
        assertSame(needsShoe, needsShoe3);
        assertNotNull(needsShoe3.getBar());
        assertNotNull(needsShoe3.getString());
    }

    @Test
    public void confirmThatReinjectionCanLeverageParameterNamesForDisambiguation() {
        MethodInjection methodInjection = new MethodInjection(DOIT_METHOD);
        DefaultPicoContainer parent = new DefaultPicoContainer(new Caching().wrap(new ConstructorInjection()));

        // parameter name leverage can't work on interfaces if using bytecode retrieval technique

        parent.addComponent(NeedsShoe.class);
        parent.addComponent(Shoe.class);
        parent.addComponent("a", "1333");
        parent.addComponent("s", "12");
        parent.addComponent("tjklhjkjhkjh", "44");

        NeedsShoe needsShoe = parent.getComponent(NeedsShoe.class);
        assertNotNull(needsShoe.bar);
        assertTrue(needsShoe.string == null);

        Reinjection reinjection = new Reinjection(methodInjection, parent);
        TransientPicoContainer tpc = new TransientPicoContainer(reinjection, parent);
        tpc.as(Characteristics.USE_NAMES).addComponent(NeedsShoe.class);

        NeedsShoe needsShoe2 = tpc.getComponent(NeedsShoe.class);
        assertSame(needsShoe, needsShoe2);
        assertNotNull(needsShoe2.bar);
        assertNotNull(needsShoe2.string);
        assertEquals("12", needsShoe2.string);

    }

    @Test
    public void confirmThatReinjectionCanLeverageParameterNamesForDisambiguationWithTypeConversion() throws NoSuchMethodException {
        MethodInjection methodInjection = new MethodInjection(NeedsShoe.class.getMethod("doInt", int.class));
        DefaultPicoContainer parent = new DefaultPicoContainer(new Caching().wrap(new ConstructorInjection()));

        // parameter name leverage can't work on interfaces if using bytecode retrieval technique

        parent.addComponent(NeedsShoe.class);
        parent.addComponent(Shoe.class);
        parent.addComponent("a", "1333");
        parent.addComponent("s", "12");
        parent.addComponent("tjklhjkjhkjh", "44");

        NeedsShoe needsShoe = parent.getComponent(NeedsShoe.class);
        assertNotNull(needsShoe.bar);
        assertTrue(needsShoe.string == null);

        Reinjection reinjection = new Reinjection(methodInjection, parent);
        TransientPicoContainer tpc = new TransientPicoContainer(reinjection, parent);
        tpc.as(Characteristics.USE_NAMES).addComponent(NeedsShoe.class);

        NeedsShoe needsShoe2 = tpc.getComponent(NeedsShoe.class);
        assertSame(needsShoe, needsShoe2);
        assertNotNull(needsShoe2.bar);
        assertNotNull(needsShoe2.string);
        assertEquals("i=12", needsShoe2.string);

    }

    @Test public void testCachedComponentCanBeReinjectedByATransientReflectionMethodReinjector() {
        cachedComponentCanBeReinjectedByATransientReinjector(new MethodInjection(DOIT_METHOD));
    }

    @Test public void testCachedComponentCanBeReinjectedByATransientMethodNameReinjector() {
        cachedComponentCanBeReinjectedByATransientReinjector(new MethodInjection("doIt"));
    }

    @Test public void testCachedComponentCanBeReinjectedByATransientAnnotatedMethodReinjector() {
        cachedComponentCanBeReinjectedByATransientReinjector(new AnnotatedMethodInjection(Hurrah.class, false));
    }

    public static class ReturnParameterAction implements Action {
        private final int parameter;

        public ReturnParameterAction(final int parameter) {
            this.parameter = parameter;
        }

        public void describeTo(final Description description) {
            // describe it
        }

        public Object invoke(final Invocation invocation) {
            return invocation.getParameter(parameter);
        }
    }

    private void cachedComponentCanBeReinjectedByATransientReinjector(final AbstractInjectionType methodInjection) {
        final DefaultPicoContainer parent = new DefaultPicoContainer(new Caching().wrap(new ConstructorInjection()));
        parent.setName("parent");
        parent.addComponent(INeedsShoe.class, NeedsShoe.class);
        parent.addComponent(Shoe.class);
        parent.addComponent("12");

        final INeedsShoe foo = parent.getComponent(INeedsShoe.class);
        assertNotNull(foo.getBar());
        assertTrue(foo.getString() == null);

        final ComponentMonitor cm = mockery.mock(ComponentMonitor.class);
        Reinjector reinjector = new Reinjector(parent, cm);
        mockery.checking(new Expectations() {{
            atLeast(1).of(cm).newInjector(with(any(com.picocontainer.Injector.class)));
            will(new ReturnParameterAction(0));
            one(cm).invoking(with(any(PicoContainer.class)), with(any(ComponentAdapter.class)),
                    with(any(Method.class)), with(any(Object.class)), with(any(Object[].class)));
            will(returnValue(ComponentMonitor.KEEP));
            one(cm).invoked(with(any(PicoContainer.class)), with(any(ComponentAdapter.class)),
                    with(any(Method.class)), with(any(Object.class)), with(any(Long.class)), with(any(Integer.class)), with(any(Object[].class)));
        }});

        Object o = reinjector.reinject(NeedsShoe.class, methodInjection);
        int result = (Integer) o;
        assertEquals(6, result);

        INeedsShoe foo3 = parent.getComponent(INeedsShoe.class);
        assertSame(foo, foo3);
        assertNotNull(foo3.getBar());
        assertNotNull(foo3.getString());
        assertEquals("12", foo3.getString());
    }

    @Test public void testOverloadedReinjectMethodsAreIdentical() {
        final DefaultPicoContainer parent = new DefaultPicoContainer(new Caching().wrap(new ConstructorInjection()));
        parent.addComponent(INeedsShoe.class, NeedsShoe.class);
        parent.addComponent(Shoe.class);
        parent.addComponent("12");

        final ComponentMonitor cm = new NullComponentMonitor();
        Reinjector reinjector = new Reinjector(parent, cm);

        //int result = (Integer) reinjector.reinject(NeedsShoe.class, DOIT_METHOD);
        assertEquals(6, (int) (Integer) reinjector.reinject(NeedsShoe.class, DOIT_METHOD));
        assertEquals(6, (int) (Integer) reinjector.reinject(NeedsShoe.class, new MethodInjection(DOIT_METHOD)));

    }

    @Test public void testOverloadedReinjectMethodsAreIdentical2() {
        final DefaultPicoContainer parent = new DefaultPicoContainer(new Caching().wrap(new ConstructorInjection()));
        parent.addComponent(INeedsShoe.class, NeedsShoe.class);
        parent.addComponent(Shoe.class);
        parent.addComponent("12");

        final ComponentMonitor cm = new NullComponentMonitor();
        Reinjector reinjector = new Reinjector(parent, cm);

        assertEquals(6, (int) (Integer) reinjector.reinject(NeedsShoe.class, NeedsShoe.M.doIt));
        assertEquals(6, (int) (Integer) reinjector.reinject(NeedsShoe.class, new MethodInjection(DOIT_METHOD)));

    }

    @Test public void testReinjectorCanBeOverridenByComponentMonitor() {
        final DefaultPicoContainer parent = new DefaultPicoContainer(new Caching().wrap(new ConstructorInjection()));
        parent.addComponent(INeedsShoe.class, NeedsShoe.class);
        parent.addComponent(Shoe.class);
        parent.addComponent("12");

        final ComponentMonitor cm = new NullComponentMonitor() {
            @Override
			public Object invoking(final PicoContainer container, final ComponentAdapter<?> componentAdapter, final Member member, final Object instance, final Object[] args) {
                return 4444;
            }
        };
        Reinjector reinjector = new Reinjector(parent, cm);

        assertEquals(4444, (int) (Integer) reinjector.reinject(NeedsShoe.class, DOIT_METHOD));

    }

    @Test public void testReinjectorCanBeHonoredByComponentMonitor() {
        final DefaultPicoContainer parent = new DefaultPicoContainer(new Caching().wrap(new ConstructorInjection()));
        parent.addComponent(INeedsShoe.class, NeedsShoe.class);
        parent.addComponent(Shoe.class);
        parent.addComponent("12");

        final ComponentMonitor cm = new NullComponentMonitor() {
            @Override
			public Object invoking(final PicoContainer container, final ComponentAdapter<?> componentAdapter, final Member member, final Object instance, final Object... args) {
                return ComponentMonitor.KEEP;
            }
        };
        Reinjector reinjector = new Reinjector(parent, cm);

        assertEquals(6, (int) (Integer) reinjector.reinject(NeedsShoe.class, DOIT_METHOD));

    }

    @Test public void testReinjectorCanBeNullifiedByComponentMonitor() {
        final DefaultPicoContainer parent = new DefaultPicoContainer(new Caching().wrap(new ConstructorInjection()));
        parent.addComponent(INeedsShoe.class, NeedsShoe.class);
        parent.addComponent(Shoe.class);
        parent.addComponent("12");

        final ComponentMonitor cm = new NullComponentMonitor() {
            @Override
			public Object invoking(final PicoContainer container, final ComponentAdapter<?> componentAdapter, final Member member, final Object instance, final Object[] args) {
                return null;
            }
        };
        Reinjector reinjector = new Reinjector(parent, cm);

        Object retval = reinjector.reinject(NeedsShoe.class, DOIT_METHOD);
        assertTrue(retval == null);

    }

    @Override
	protected ComponentFactory createComponentFactory() {
        return new Reinjection(new MethodInjection(DOIT_METHOD), new EmptyPicoContainer());
    }

    @Override
	@Test
    public void testRegisterComponent() throws PicoCompositionException {
        try {
            super.testRegisterComponent();
            fail();
        } catch (PicoCompositionException e) {
            assertTrue(e.getMessage().contains("] not on impl "));
        }
    }

    @Override
	@Test
    public void testUnregisterComponent() throws PicoCompositionException {
        try {
            super.testUnregisterComponent();
            fail();
        } catch (PicoCompositionException e) {
            assertTrue(e.getMessage().contains("] not on impl "));
        }
    }

    @Override
	@Test
    public void testEquals() throws PicoCompositionException {
        try {
            super.testEquals();
            fail();
        } catch (PicoCompositionException e) {
            assertTrue(e.getMessage().contains("] not on impl "));
        }
    }
}