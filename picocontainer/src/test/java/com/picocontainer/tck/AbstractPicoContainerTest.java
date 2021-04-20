/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer.tck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import com.picocontainer.testmodel.DependsOnTouchable;
import com.picocontainer.testmodel.SimpleTouchable;
import com.picocontainer.testmodel.Touchable;
import com.picocontainer.testmodel.Washable;
import com.picocontainer.testmodel.WashableTouchable;
import com.picocontainer.ChangedBehavior;
import com.picocontainer.Characteristics;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentFactory;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.Converting;
import com.picocontainer.DefaultPicoContainer;
import com.picocontainer.Disposable;
import com.picocontainer.MutablePicoContainer;
import com.picocontainer.NameBinding;
import com.picocontainer.Parameter;
import com.picocontainer.PicoCompositionException;
import com.picocontainer.PicoContainer;
import com.picocontainer.PicoException;
import com.picocontainer.PicoVerificationException;
import com.picocontainer.PicoVisitor;
import com.picocontainer.Startable;
import com.picocontainer.adapters.InstanceAdapter;
import com.picocontainer.behaviors.AbstractBehavior;
import com.picocontainer.behaviors.AdaptingBehavior;
import com.picocontainer.injectors.AbstractInjector;
import com.picocontainer.injectors.ConstructorInjection;
import com.picocontainer.injectors.AbstractInjector.UnsatisfiableDependenciesException;
import com.picocontainer.injectors.MultiArgMemberInjector.ParameterCannotBeNullException;
import com.picocontainer.lifecycle.NullLifecycleStrategy;
import com.picocontainer.monitors.ConsoleComponentMonitor;
import com.picocontainer.monitors.NullComponentMonitor;
import com.picocontainer.parameters.BasicComponentParameter;
import com.picocontainer.parameters.ComponentParameter;
import com.picocontainer.parameters.ConstantParameter;
import com.picocontainer.parameters.DefaultConstructorParameter;
import com.picocontainer.parameters.NullParameter;
import com.picocontainer.visitors.AbstractPicoVisitor;
import com.picocontainer.visitors.TraversalCheckingVisitor;
import com.picocontainer.visitors.VerifyingVisitor;

/** This test tests (at least it should) all the methods in MutablePicoContainer. */
@SuppressWarnings("serial")
public abstract class AbstractPicoContainerTest {

    protected abstract MutablePicoContainer createPicoContainer(PicoContainer parent);

    protected final MutablePicoContainer createPicoContainerWithDependsOnTouchableOnly() throws PicoCompositionException {
        MutablePicoContainer pico = createPicoContainer(null);
        pico.addComponent(DependsOnTouchable.class);
        return pico;
    }

    protected final MutablePicoContainer createPicoContainerWithTouchableAndDependsOnTouchable() throws PicoCompositionException {
        MutablePicoContainer pico = createPicoContainerWithDependsOnTouchableOnly();
        pico.as(Characteristics.CACHE).addComponent(Touchable.class, SimpleTouchable.class);
        return pico;
    }

    @Test public void testBasicInstantiationAndContainment() throws PicoException {
        PicoContainer pico = createPicoContainerWithTouchableAndDependsOnTouchable();
        assertTrue("Component should be instance of Touchable",
                   Touchable.class.isAssignableFrom(pico.getComponentAdapter(Touchable.class, (NameBinding) null).getComponentImplementation()));
    }

    @Test public void testRegisteredComponentsExistAndAreTheCorrectTypes() throws PicoException {
        PicoContainer pico = createPicoContainerWithTouchableAndDependsOnTouchable();
        assertNotNull("Container should have Touchable addComponent",
                      pico.getComponentAdapter(Touchable.class, (NameBinding) null));
        assertNotNull("Container should have DependsOnTouchable addComponent",
                      pico.getComponentAdapter(DependsOnTouchable.class, (NameBinding) null));
        assertTrue("Component should be instance of Touchable",
                   pico.getComponent(Touchable.class) != null);
        assertTrue("Component should be instance of DependsOnTouchable",
                   pico.getComponent(DependsOnTouchable.class) != null);
        assertNull("should not have non existent addComponent", pico.getComponentAdapter(Map.class, (NameBinding) null));
    }

    @Test public void testRegistersSingleInstance() throws PicoException {
        MutablePicoContainer pico = createPicoContainer(null);
        StringBuffer sb = new StringBuffer();
        pico.addComponent(sb);
        assertSame(sb, pico.getComponent(StringBuffer.class));
    }

    @Test public void testContainerIsSerializable() throws PicoException,
                                                     IOException, ClassNotFoundException
    {

        getTouchableFromSerializedContainer();

    }

    private Touchable getTouchableFromSerializedContainer() throws IOException, ClassNotFoundException {
        MutablePicoContainer pico = createPicoContainerWithTouchableAndDependsOnTouchable();
        // Add a list too, using a constant parameter
        pico.addComponent("list", ArrayList.class, new ConstantParameter(10));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(pico);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));

        pico = (MutablePicoContainer)ois.readObject();

        DependsOnTouchable dependsOnTouchable = pico.getComponent(DependsOnTouchable.class);
        assertNotNull(dependsOnTouchable);
        return pico.getComponent(Touchable.class);
    }

    @Test public void testSerializedContainerCanRetrieveImplementation() throws PicoException,
                                                                          IOException, ClassNotFoundException
    {

        Touchable touchable = getTouchableFromSerializedContainer();

        SimpleTouchable simpleTouchable = (SimpleTouchable)touchable;

        assertTrue(simpleTouchable.wasTouched);
    }


    @Test public void testGettingComponentWithMissingDependencyFails() throws PicoException {
    	MutablePicoContainer picoContainer = createPicoContainerWithDependsOnTouchableOnly();
        picoContainer.setName("parent");
        try {
            picoContainer.getComponent(DependsOnTouchable.class);
            fail("should need a Touchable");
        } catch (AbstractInjector.UnsatisfiableDependenciesException e) {
            String message = e.getMessage().replace("com.picocontainer.testmodel.", "");
            assertEquals("DependsOnTouchable has unsatisfied dependency 'interface Touchable' for constructor 'public DependsOnTouchable(Touchable)' from parent:1<|", message);
        }
    }

    @Test public void testDuplicateRegistration() {
        try {
            MutablePicoContainer pico = createPicoContainer(null);
            pico.addComponent(Object.class);
            pico.addComponent(Object.class);
            fail("Should have failed with duplicate registration");
        } catch (PicoCompositionException e) {
            assertTrue("Wrong key", e.getMessage().indexOf(Object.class.toString()) > -1);
        }
    }

    @SuppressWarnings("rawtypes")
	@Test public void testExternallyInstantiatedObjectsCanBeRegisteredAndLookedUp() throws PicoException {
        MutablePicoContainer pico = createPicoContainer(null);
        final HashMap map = new HashMap();
        pico.as(getProperties()).addComponent(Map.class, map);
        assertSame(map, pico.getComponent(Map.class));
    }

    @Test public void testAmbiguousResolution() throws PicoCompositionException {
        MutablePicoContainer pico = createPicoContainer(null);
        pico.addComponent("ping", String.class);
        pico.addComponent("pong", "pang");
        try {
            pico.getComponent(String.class);
        } catch (AbstractInjector.AmbiguousComponentResolutionException e) {
            assertTrue(e.getMessage().indexOf("java.lang.String") != -1);
            assertTrue(e.getMessage().indexOf("<not-specified>") != -1);
            assertTrue(e.getMessage().indexOf("<unknown>") != -1);
        }
    }

    @Test public void testLookupWithUnregisteredKeyReturnsNull() throws PicoCompositionException {
        MutablePicoContainer pico = createPicoContainer(null);
        assertNull(pico.getComponent(String.class));
    }

    @Test public void testLookupWithUnregisteredTypeReturnsNull() throws PicoCompositionException {
        MutablePicoContainer pico = createPicoContainer(null);
        assertNull(pico.getComponent(String.class));
    }

    public static class ListAdder {
        public ListAdder(final Collection<String> list) {
            list.add("something");
        }
    }

    @Test public void testUnsatisfiableDependenciesExceptionGivesVerboseEnoughErrorMessage() {
        MutablePicoContainer pico = createPicoContainer(null);
        pico.setName("parent");
        pico.addComponent(ComponentD.class);

        try {
            pico.getComponent(ComponentD.class);
        } catch (AbstractInjector.UnsatisfiableDependenciesException e) {
            String msg = e.getMessage().replace("com.picocontainer.tck.AbstractPicoContainerTest$Component", "");
            assertEquals("D has unsatisfied dependency 'class B' for constructor 'public D(E,B)' from parent:1<|", msg);
        }
    }

    @Test public void testUnsatisfiableDependenciesExceptionGivesUnsatisfiedDependencyTypes() {
        MutablePicoContainer pico = createPicoContainer(null);
        pico.setName("parent");
        // D depends on E and B
        pico.addComponent(ComponentD.class);

        // first - do not register any dependency
        // should yield first unsatisfied dependency
        try {
            pico.getComponent(ComponentD.class);
        } catch (AbstractInjector.UnsatisfiableDependenciesException e) {
            String message = e.getMessage().replace("com.picocontainer.tck.AbstractPicoContainerTest$Component", "");
            assertEquals("D has unsatisfied dependency 'class B' for constructor 'public D(E,B)' from parent:1<|", message);
        }

        // now register only first dependency
        // should yield second unsatisfied dependency
        pico.addComponent(ComponentE.class);
        try {
            pico.getComponent(ComponentD.class);
        } catch (AbstractInjector.UnsatisfiableDependenciesException e) {
            String message = e.getMessage().replace("com.picocontainer.tck.AbstractPicoContainerTest$Component", "");
            assertEquals("D has unsatisfied dependency 'class B' for constructor 'public D(E,B)' from parent:2<|", message);
        }
    }

    @Test public void testCyclicDependencyThrowsCyclicDependencyException() {
        assertCyclicDependencyThrowsCyclicDependencyException(createPicoContainer(null));
    }

    private static void assertCyclicDependencyThrowsCyclicDependencyException(final MutablePicoContainer pico) {
        pico.addComponent(ComponentB.class);
        pico.addComponent(ComponentD.class);
        pico.addComponent(ComponentE.class);

        try {
            pico.getComponent(ComponentD.class);
            fail("CyclicDependencyException expected");
        } catch (AbstractInjector.CyclicDependencyException e) {
            // CyclicDependencyException reports now the stack.
            //final List dependencies = Arrays.asList(ComponentD.class.getConstructors()[0].getParameterTypes());
            final List<Class> dependencies = Arrays.<Class>asList(ComponentD.class, ComponentE.class, ComponentD.class);
            final List<Class> reportedDependencies = Arrays.asList(e.getDependencies());
            assertEquals(dependencies, reportedDependencies);
        } catch (StackOverflowError e) {
            fail();
        }
    }

    @Test public void testCyclicDependencyThrowsCyclicDependencyExceptionWithParentContainer() {
        MutablePicoContainer pico = createPicoContainer(createPicoContainer(null));
        assertCyclicDependencyThrowsCyclicDependencyException(pico);
    }

    @Test public void testRemovalNonRegisteredComponentAdapterWorksAndReturnsNull() {
        final MutablePicoContainer picoContainer = createPicoContainer(null);
        assertNull(picoContainer.removeComponent("COMPONENT DOES NOT EXIST"));
    }

    /* Important! Nanning really, really depends on this! */
    @Test public void testComponentAdapterRegistrationOrderIsMaintained() throws NoSuchMethodException {

        ConstructorInjection.ConstructorInjector c1 = new ConstructorInjection.ConstructorInjector<Object>("1", Object.class);
        ConstructorInjection.ConstructorInjector c2 = new ConstructorInjection.ConstructorInjector<String>("2", String.class);

        MutablePicoContainer picoContainer = createPicoContainer(null);
        picoContainer.addAdapter(c1).addAdapter(c2);
        Collection<ComponentAdapter<?>> list2 = picoContainer.getComponentAdapters();
        //registration order should be maintained
        assertEquals(2, list2.size());
        assertEquals(c1.getComponentKey(), ((ComponentAdapter)list2.toArray()[0]).getComponentKey());
        assertEquals(c2.getComponentKey(), ((ComponentAdapter)list2.toArray()[1]).getComponentKey());

        picoContainer.getComponents(); // create all the instances at once
        assertFalse("instances should be created in same order as adapters are created",
                    picoContainer.getComponents().get(0) instanceof String);
        assertTrue("instances should be created in same order as adapters are created",
                   picoContainer.getComponents().get(1) instanceof String);

        MutablePicoContainer reversedPicoContainer = createPicoContainer(null);
        reversedPicoContainer.addAdapter(c2);
        reversedPicoContainer.addAdapter(c1);
        //registration order should be maintained
        list2 = reversedPicoContainer.getComponentAdapters();
        assertEquals(2, list2.size());
        assertEquals(c2.getComponentKey(), ((ComponentAdapter)list2.toArray()[0]).getComponentKey());
        assertEquals(c1.getComponentKey(), ((ComponentAdapter)list2.toArray()[1]).getComponentKey());

        reversedPicoContainer.getComponents(); // create all the instances at once
        assertTrue("instances should be created in same order as adapters are created",
                   reversedPicoContainer.getComponents().get(0) instanceof String);
        assertFalse("instances should be created in same order as adapters are created",
                    reversedPicoContainer.getComponents().get(1) instanceof String);
    }

    public static final class NeedsTouchable {
        public final Touchable touchable;

        public NeedsTouchable(final Touchable touchable) {
            this.touchable = touchable;
        }
    }

    public static final class NeedsWashable {
        public final Washable washable;

        public NeedsWashable(final Washable washable) {
            this.washable = washable;
        }
    }

    @Test public void testSameInstanceCanBeUsedAsDifferentTypeWhenCaching() {
        MutablePicoContainer pico = createPicoContainer(null);
        pico.as(Characteristics.CACHE).addComponent("wt", WashableTouchable.class);
        pico.addComponent("nw", NeedsWashable.class);
        pico.as(Characteristics.CACHE).addComponent("nt", NeedsTouchable.class);

        NeedsWashable nw = (NeedsWashable)pico.getComponent("nw");
        NeedsTouchable nt = (NeedsTouchable)pico.getComponent("nt");
        assertSame(nw.washable, nt.touchable);
    }

    @Test public void testRegisterComponentWithObjectBadType() throws PicoCompositionException {
        MutablePicoContainer pico = createPicoContainer(null);

        try {
            pico.addComponent(Serializable.class, new Object());
            fail("Shouldn't be able to register an Object.class as Serializable because it is not, " +
                 "it does not implement it, Object.class does not implement much.");
        } catch (ClassCastException e) {
            assertNotNull(e.getMessage());
        }

    }

    public static class JMSService {
        public final String serverid;
        public final String path;

        public JMSService(final String serverid, final String path) {
            this.serverid = serverid;
            this.path = path;
        }
    }

    // http://jira.codehaus.org/secure/ViewIssue.jspa?key=PICO-52
    @Test public void testPico52() {
        MutablePicoContainer pico = createPicoContainer(null);

        pico.addComponent("foo", JMSService.class, new ConstantParameter("0"), new ConstantParameter("something"));
        JMSService jms = (JMSService)pico.getComponent("foo");
        assertEquals("0", jms.serverid);
        assertEquals("something", jms.path);
    }

    public static class ComponentA {
        public final ComponentC c;

        public ComponentA(final ComponentB b, final ComponentC c) {
            this.c = c;
            assertNotNull(b);
            assertNotNull(c);
        }
    }

    public static class ComponentB {
        //Does nothing.
    }

    public static class ComponentC {
        //Does nothing.
    }

    public static class ComponentD {
        public ComponentD(final ComponentE e, final ComponentB b) {
            assertNotNull(e);
            assertNotNull(b);
        }
    }

    public static class ComponentE {
        public ComponentE(final ComponentD d) {
            assertNotNull(d);
        }
    }

    public static class ComponentF {
        public ComponentF(final ComponentA a) {
            assertNotNull(a);
        }
    }

    @Test public void testAggregatedVerificationException() {
        MutablePicoContainer pico = createPicoContainer(null);
        pico.addComponent(ComponentA.class);
        pico.addComponent(ComponentE.class);
        try {
            new VerifyingVisitor().traverse(pico);
            fail("we expect a PicoVerificationException");
        } catch (PicoVerificationException e) {
            List nested = e.getNestedExceptions();
            assertEquals(2, nested.size());
            assertTrue(-1 != e.getMessage().indexOf(ComponentA.class.getName()));
            assertTrue(-1 != e.getMessage().indexOf(ComponentE.class.getName()));
        }
    }

    // An adapter has no longer a hosting container.

//    @Test public void testRegistrationOfAdapterSetsHostingContainerAsSelf() {
//        final InstanceAdapter componentAdapter = new InstanceAdapter("", new Object());
//        final MutablePicoContainer picoContainer = createPicoContainer(null);
//        picoContainer.addAdapter(componentAdapter);
//        assertSame(picoContainer, componentAdapter.getContainer());
//    }

    public static class ContainerDependency {
        public ContainerDependency(final PicoContainer container) {
            assertNotNull(container);
        }
    }

    // ImplicitPicoContainer injection is bad. It is an open door for hackers. Developers with
    // special PicoContainer needs should specifically register() a comtainer they want components to
    // be able to pick up on.

//    @Test public void testImplicitPicoContainerInjection() {
//        MutablePicoContainer pico = createPicoContainer(null);
//        pico.addAdapter(ContainerDependency.class);
//        ContainerDependency dep = (ContainerDependency) pico.getComponent(ContainerDependency.class);
//        assertSame(pico, dep.pico);
//    }

    @Test public void testShouldReturnNullWhenUnregistereingUnmanagedComponent() {
        final MutablePicoContainer pico = createPicoContainer(null);
        assertNull(pico.removeComponentByInstance("yo"));
    }

    @Test public void testShouldReturnNullForComponentAdapterOfUnregisteredType() {
        final MutablePicoContainer pico = createPicoContainer(null);
        assertNull(pico.getComponent(List.class));
    }

    @Test public void testShouldReturnNonMutableParent() {
        DefaultPicoContainer parent = new DefaultPicoContainer();
        final MutablePicoContainer picoContainer = createPicoContainer(parent);
        assertNotSame(parent, picoContainer.getParent());
        assertFalse(picoContainer.getParent() instanceof MutablePicoContainer);
    }

    class Foo implements Startable, Disposable {
        public boolean started;
        public boolean stopped;
        public boolean disposed;

        public void start() {
            started = true;
        }

        public void stop() {
            stopped = true;
        }

        public void dispose() {
            disposed = true;
        }

    }

    @Test public void testContainerCascadesDefaultLifecycle() {
        final MutablePicoContainer picoContainer = createPicoContainer(null);
        Foo foo = new Foo();
        picoContainer.addComponent(foo);
        picoContainer.start();
        assertEquals(true, foo.started);
        picoContainer.stop();
        assertEquals(true, foo.stopped);
        picoContainer.dispose();
        assertEquals(true, foo.disposed);
    }

    @Test public void testComponentInstancesFromParentsAreNotDirectlyAccessible2() {
        final MutablePicoContainer a = createPicoContainer(null);
        final MutablePicoContainer b = createPicoContainer(a);
        final MutablePicoContainer c = createPicoContainer(b);

        Object ao = new Object();
        Object bo = new Object();
        Object co = new Object();

        a.addComponent("a", ao);
        b.addComponent("b", bo);
        c.addComponent("c", co);

        assertEquals(1, a.getComponents().size());
        assertEquals(1, b.getComponents().size());
        assertEquals(1, c.getComponents().size());
    }

    @Test public void testStartStopAndDisposeCascadedtoChildren() {
        final MutablePicoContainer parent = createPicoContainer(null);
        parent.addComponent(new StringBuffer());
        final MutablePicoContainer child = createPicoContainer(parent);
        parent.addChildContainer(child);
        child.addComponent(LifeCycleMonitoring.class);
        parent.start();
        try {
            child.start();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            assertTrue("child already started", e.getMessage().contains("STARTED"));
        }
        parent.stop();
        try {
            child.stop();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            assertTrue("child not started", e.getMessage().contains("STOPPED"));
        }
        parent.dispose();
        try {
            child.dispose();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            assertTrue("child already disposed",  e.getMessage().contains("DISPOSED"));
        }

    }

    @Test public void testMakingOfChildContainer() {
        final MutablePicoContainer parent = createPicoContainer(null);
        MutablePicoContainer child = parent.makeChildContainer();
        assertNotNull(child);
    }

    @Test public void testMakingOfChildContainerPercolatesLifecycleManager() {
        final MutablePicoContainer parent = createPicoContainer(null);
        parent.addComponent("one", TestLifecycleComponent.class);
        MutablePicoContainer child = parent.makeChildContainer();
        assertNotNull(child);
        child.addComponent("two", TestLifecycleComponent.class);
        parent.start();
        try {
            child.start();
        } catch (IllegalStateException e) {
            assertTrue("child already started", e.getMessage().contains("STARTED"));
        }
        //TODO - The Behavior reference in child containers is not used. Thus is is almost pointless
        // The reason is because DefaultPicoContainer's accept() method visits child containers' on its own.
        // This may be file for visiting components in a tree for general cases, but for lifecycle, we
        // should hand to each Behavior's start(..) at each appropriate node. See mail-list discussion.
    }

    @SuppressWarnings("unused")
    public static final class TestChangedBehavior extends AbstractBehavior.AbstractChangedBehavior implements ChangedBehavior {

        public final ArrayList<PicoContainer> started = new ArrayList<PicoContainer>();

        public TestChangedBehavior(final ComponentAdapter delegate) {
            super(delegate);
        }

        @Override
        public void start(final PicoContainer node) {
            started.add(node);
        }

        @Override
        public void stop(final PicoContainer node) {
            //Does nothing.
        }

        @Override
        public void dispose(final PicoContainer node) {
            //Does nothing.
        }

        @Override
        public boolean componentHasLifecycle() {
            return true;
        }

        public String getDescriptor() {
            return null;
        }
    }

    public static class TestLifecycleComponent implements Startable {
        public boolean started;

        public void start() {
            started = true;
        }

        public void stop() {
            //Does nothing.
        }
    }

    @Test public void testStartStopAndDisposeNotCascadedtoRemovedChildren() {
        final MutablePicoContainer parent = createPicoContainer(null);
        parent.addComponent(new StringBuffer());
        StringBuffer sb = parent.getComponents(StringBuffer.class).get(0);

        final MutablePicoContainer child = createPicoContainer(parent);
        assertEquals(parent, parent.addChildContainer(child));
        child.addComponent(LifeCycleMonitoring.class);
        assertTrue(parent.removeChildContainer(child));
        parent.start();
        assertTrue(sb.toString().indexOf("-started") == -1);
        parent.stop();
        assertTrue(sb.toString().indexOf("-stopped") == -1);
        parent.dispose();
        assertTrue(sb.toString().indexOf("-disposed") == -1);
    }

    @Test public void testShouldCascadeStartStopAndDisposeToChild() {

        StringBuffer sb = new StringBuffer();
        final MutablePicoContainer parent = createPicoContainer(null);
        parent.addComponent(sb);
        parent.addComponent(Map.class, HashMap.class);

        final MutablePicoContainer child = parent.makeChildContainer();
        child.addComponent(LifeCycleMonitoring.class);

        Map map = parent.getComponent(Map.class);
        assertNotNull(map);
        parent.start();
        try {
            child.start();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            assertTrue("child already started", e.getMessage().contains("STARTED"));
        }
        parent.stop();
        try {
            child.stop();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            assertTrue("child not started", e.getMessage().contains("STOPPED"));
        }
        parent.dispose();
        try {
            child.dispose();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            assertTrue("child already disposed",  e.getMessage().contains("DISPOSED"));
        }
    }

    public static final class LifeCycleMonitoring implements Startable, Disposable {
        final StringBuffer sb;

        public LifeCycleMonitoring(final StringBuffer sb) {
            this.sb = sb;
            sb.append("-instantiated");
        }

        public void start() {
            sb.append("-started");
        }

        public void stop() {
            sb.append("-stopped");
        }

        public void dispose() {
            sb.append("-disposed");
        }
    }

    public static class RecordingStrategyVisitor extends AbstractPicoVisitor {

        private final List<Object> list;

        public RecordingStrategyVisitor(final List<Object> list) {
            this.list = list;
        }

        public boolean visitContainer(final PicoContainer pico) {
            list.add(pico.getClass());
            return CONTINUE_TRAVERSAL;
        }

        public void visitComponentAdapter(final ComponentAdapter componentAdapter) {
            list.add(componentAdapter.getClass());
        }

        public void visitComponentFactory(final ComponentFactory componentFactory) {
            list.add(componentFactory.getClass());
        }

        public void visitParameter(final Parameter parameter) {
            list.add(parameter.getClass());
        }

    }

    protected abstract Properties[] getProperties();


    @Test public void testAcceptImplementsBreadthFirstStrategy() {
        final MutablePicoContainer parent = createPicoContainer(null);
        final MutablePicoContainer child = parent.makeChildContainer();
        ComponentAdapter hashMapAdapter =
            parent.as(getProperties()).addAdapter(new ConstructorInjection.ConstructorInjector<HashMap>(HashMap.class, HashMap.class))
                .getComponentAdapter(HashMap.class, (NameBinding) null);
        ComponentAdapter hashSetAdapter =
            parent.as(getProperties()).addAdapter(new ConstructorInjection.ConstructorInjector<HashSet>(HashSet.class, HashSet.class))
                .getComponentAdapter(HashSet.class, (NameBinding) null);
        InstanceAdapter instanceAdapter = new InstanceAdapter<String>(String.class, "foo",
                                                              new NullLifecycleStrategy(),
                                                              new NullComponentMonitor());
        ComponentAdapter stringAdapter = parent.as(getProperties()).addAdapter(instanceAdapter).getComponentAdapter(instanceAdapter.getComponentKey());
        ComponentAdapter arrayListAdapter =
            child.as(getProperties()).addAdapter(new ConstructorInjection.ConstructorInjector<ArrayList>(ArrayList.class, ArrayList.class))
                .getComponentAdapter(ArrayList.class, (NameBinding) null);
        Parameter componentParameter = BasicComponentParameter.BASIC_DEFAULT;
        Parameter throwableParameter = new ConstantParameter(new Throwable("bar"));
        ConstructorInjection.ConstructorInjector<Exception> ci = new ConstructorInjection.ConstructorInjector<Exception>(Exception.class, Exception.class, componentParameter,
                throwableParameter);
        ComponentAdapter exceptionAdapter = child.as(getProperties()).addAdapter(ci).getComponentAdapter(Exception.class, (NameBinding) null);

        List<Class> expectedList = new ArrayList<Class>();

        addContainers(expectedList);
        addDefaultComponentFactories(expectedList);
        expectedList.add(hashMapAdapter.getClass());
        expectedList.add(hashSetAdapter.getClass());
        expectedList.add(stringAdapter.getClass());
        addContainers(expectedList);
        addDefaultComponentFactories(expectedList);
        expectedList.add(arrayListAdapter.getClass());
        expectedList.add(exceptionAdapter.getClass());
        expectedList.add(componentParameter.getClass());
        expectedList.add(throwableParameter.getClass());
        List<Object> visitedList = new LinkedList<Object>();
        PicoVisitor visitor = new RecordingStrategyVisitor(visitedList);
        visitor.traverse(parent);
//        assertEquals("Expected: " + Arrays.deepToString(expectedList.toArray())
//        		+ "\nGot:  " + Arrays.deepToString(visitedList.toArray()), expectedList.size(), visitedList.size());

        assertEquals(Arrays.deepToString(expectedList.toArray()), Arrays.deepToString(visitedList.toArray()));
//        for (Class<?> c : expectedList) {
//            assertTrue(visitedList.remove(c));
//        }
//        assertEquals(0, visitedList.size());
    }

    /**
     * Verifies that you can halt a container traversal.
     */
    @Test
    public void testAcceptIsAbortable() {
        final MutablePicoContainer parent = createPicoContainer(null);
        final MutablePicoContainer child = parent.makeChildContainer();
        child.addComponent("This is a test");

        TraversalCheckingVisitor parentComponentCountingVisitor = new TraversalCheckingVisitor() {
        	private int containerCount = 0;

			@Override
            @SuppressWarnings("unused")
			public void visitComponentAdapter(final ComponentAdapter<?> componentAdapter) {
				if (containerCount == 0) {
					fail("Should have visited a container first");
				}
				fail("Should never have visited an adapter.");
			}

			@Override
	        @SuppressWarnings("unused")
			public boolean visitContainer(final PicoContainer pico) {
				containerCount++;
				if (containerCount > 1) {
					return ABORT_TRAVERSAL;
				}

				return CONTINUE_TRAVERSAL;
			}

        };

        parentComponentCountingVisitor.traverse(parent);
    }

    protected void addContainers(final List expectedList) {
        expectedList.add(DefaultPicoContainer.class);
    }

    protected void addDefaultComponentFactories(final List expectedList) {
        expectedList.add(AdaptingBehavior.class);
    }

    @Test public void testAmbiguousDependencies() throws PicoCompositionException {

        MutablePicoContainer pico = this.createPicoContainer(null);

        // Register two Touchables that Fred will be confused about
        pico.addComponent(SimpleTouchable.class);
        pico.addComponent(DerivedTouchable.class);

        // Register a confused DependsOnTouchable
        pico.addComponent(DependsOnTouchable.class);

        try {
            pico.getComponent(DependsOnTouchable.class);
            fail("DependsOnTouchable should have been confused about the two Touchables");
        } catch (AbstractInjector.AmbiguousComponentResolutionException e) {
            List impls = Arrays.asList(e.getAmbiguousComponentKeys());
            assertTrue(impls.contains(DerivedTouchable.class));
            assertTrue(impls.contains(SimpleTouchable.class));

            assertTrue(e.getMessage().indexOf(DerivedTouchable.class.getName()) != -1);
            assertTrue(e.getMessage().indexOf("public com.picocontainer.testmodel.DependsOnTouchable(com.picocontainer.testmodel.Touchable)") != -1);
        }
    }


    public static class DerivedTouchable extends SimpleTouchable {
        public DerivedTouchable() {
            //Does nothing.
        }
    }


    public static final class NonGreedyClass {

        public final int value = 0;

        public NonGreedyClass() {
            //Do nothing.
        }

        public NonGreedyClass(final ComponentA component) {
            fail("Greedy Constructor should never have been called.  Instead got: " + component);
        }


    }

    @Test public void testNoArgConstructorToBeSelected() {
        MutablePicoContainer pico = this.createPicoContainer(null);
        pico.addComponent(ComponentA.class);
        pico.addComponent(NonGreedyClass.class, NonGreedyClass.class, Parameter.ZERO);


        NonGreedyClass instance = pico.getComponent(NonGreedyClass.class);
        assertNotNull(instance);
    }

    public static class ConstantParameterTestService {
    	private final String arg;

    	public ConstantParameterTestService(final String arg) {
			this.arg = arg;
    	}

		public String getArg() {
			return arg;
		}
    }


    /**
     * Currently failing
     */
    @Test
    public void testNullConstantParameter() {
    	MutablePicoContainer pico = createPicoContainer(null);
    	pico.addComponent(ConstantParameterTestService.class, ConstantParameterTestService.class, NullParameter.INSTANCE);
    	ConstantParameterTestService service = pico.getComponent(ConstantParameterTestService.class);
    	assertNotNull(service);
    	assertNull(service.getArg());
    }


    public static class PrimitiveConstructor {
        @SuppressWarnings("unused")
        public PrimitiveConstructor(final int number) {
            //does nothing.
        }
    }

    @Test(expected=ParameterCannotBeNullException.class)
    public void testNullConstantParametersDoNotInjectOnPrimitives() {
        MutablePicoContainer pico = createPicoContainer(null);
        pico.addComponent(PrimitiveConstructor.class, PrimitiveConstructor.class, NullParameter.INSTANCE);

        //Should throw exception here.
        pico.getComponent(PrimitiveConstructor.class);
     }


    @Test
    public void testNullValuesDoNotInject() {
        MutablePicoContainer pico = createPicoContainer(null);
        pico.addComponent(ConstantParameterTestService.class, ConstantParameterTestService.class, new ConstantParameter(null));
        try {
            ConstantParameterTestService service = pico.getComponent(ConstantParameterTestService.class);
            fail("Should have thrown unsatisfiable dependencies exception.  Instead got " + service + " as a return value");
        } catch (UnsatisfiableDependenciesException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testNullComponentsDoNotInject() {
        MutablePicoContainer pico = createPicoContainer(null)
            .addComponent(ComponentA.class)
            .addComponent(ComponentB.class);


        try {
            pico.addComponent(ComponentC.class, null);
            fail("Pico should not have been able to register null component instance");
        } catch (NullPointerException e) {
            assertNotNull(e.getMessage());
        }
    }


    public static class ConverterSample {
        public final int value;

        public ConverterSample(final Integer value) {
            this.value = value;
        }
    }

    @Test
    public void testIntegrationWithConverters() {
        MutablePicoContainer mpc = createPicoContainer(null);
        if (!(mpc instanceof Converting)) {
            System.out.println("Skipping 'testIntegrationWithConverters' " +
            		"because pico implementation is not Converting");
            return;
        }

        mpc.addComponent("converterParameter", "42")
            .addComponent(ConverterSample.class, ConverterSample.class,
                    new ComponentParameter("converterParameter"));
        ConverterSample result = mpc.getComponent(ConverterSample.class);
        assertEquals(42, result.value);
    }


    /**
     * Not all containers might allow caching/nocache, but
     * we need to verify that it is getting properly processed.
     */
    @Test
    public void testAsPropagatesForExactlyOneInvocation() {
        MutablePicoContainer mpc = createPicoContainer(null);
        mpc.change(Characteristics.CACHE);

        mpc
        	.as(Characteristics.CACHE).addComponent("bufferTwo", StringBuffer.class, DefaultConstructorParameter.INSTANCE)
        	.as(Characteristics.NO_CACHE).addComponent("bufferOne", StringBuffer.class, DefaultConstructorParameter.INSTANCE)
        	.addComponent("bufferThree", StringBuffer.class, DefaultConstructorParameter.INSTANCE);

        assertNotSame(mpc.getComponent("bufferOne"), mpc.getComponent("bufferOne"));
        assertSame(mpc.getComponent("bufferTwo"), mpc.getComponent("bufferTwo"));

        //Default behavior is caching.
        assertSame(mpc.getComponent("bufferThree"), mpc.getComponent("bufferThree"));

    }


	@Test
	public void testStaticInjectionMethodAsDefaultCharacteristicDoesntThrowExceptionOnNormalRegistration() {

		MutablePicoContainer mpc = createPicoContainer(null);
		mpc.change(Characteristics.STATIC_INJECTION);


		ComponentAdapter<?> ca = new InstanceAdapter<Object>(Object.class, new Object());
		mpc.addAdapter(ca);

		assertNotNull(mpc.getComponent(Object.class));
	}


	@Test
	public void testAsReturnsToNormalPicoContainerAfterOneAddComponentInvocation() {
		MutablePicoContainer mpc = createPicoContainer(null);

		MutablePicoContainer afterAs = mpc.as(Characteristics.STATIC_INJECTION);

		MutablePicoContainer afterAddComponent = afterAs.addComponent("bufferTwo", StringBuffer.class, DefaultConstructorParameter.INSTANCE);

		assertSame(mpc, afterAddComponent);
	}


	@Test(expected=PicoCompositionException.class)
	public void testAsCannotBeFollowedByAStartLifecycleCall() {
		MutablePicoContainer mpc = createPicoContainer(null);
			mpc.as(Characteristics.NO_CACHE).start();
	}


	@Test(expected=PicoCompositionException.class)
	public void testAsCannotBeFollowedByAStopLifecycleCall() {
		MutablePicoContainer mpc = createPicoContainer(null);
		mpc.as(Characteristics.NO_CACHE).stop();
	}

	@Test(expected=PicoCompositionException.class)
	public void testAsCannotBeFollowedByADisposeLifecycleCall() {
		MutablePicoContainer mpc = createPicoContainer(null);
		mpc.as(Characteristics.NO_CACHE).dispose();
	}

	@Test
	public void testSwappingMonitorBasedOnOldValue() {
		ComponentMonitor consoleMonitor = new ConsoleComponentMonitor();
		MutablePicoContainer mpc = createPicoContainer(null);
		ComponentMonitor oldMonitor = mpc.changeMonitor(consoleMonitor);
		assertNotNull(oldMonitor);
		
		ComponentMonitor oldMonitor2 = mpc.changeMonitor(oldMonitor);
		assertSame(oldMonitor2, consoleMonitor);
	}

	
	public static class ErrorProne implements Startable, Disposable {
		

		public void start() {
			System.out.println("Got Here");
		}

		public void stop() {
			throw new IllegalArgumentException("Because I say it is");
		}			

		public void dispose() {
			throw new IllegalArgumentException("'Cause this class is a brat");
			
		}
		
	}
	

}
