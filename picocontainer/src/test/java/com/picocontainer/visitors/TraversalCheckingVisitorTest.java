/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *****************************************************************************/
package com.picocontainer.visitors;

import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentFactory;
import com.picocontainer.DefaultPicoContainer;
import com.picocontainer.MutablePicoContainer;
import com.picocontainer.NameBinding;
import com.picocontainer.Parameter;
import com.picocontainer.PicoContainer;
import com.picocontainer.PicoVisitor;
import com.picocontainer.behaviors.Caching;
import com.picocontainer.behaviors.ImplementationHiding;
import com.picocontainer.injectors.ConstructorInjection;
import com.picocontainer.injectors.SetterInjection;
import com.picocontainer.monitors.NullComponentMonitor;
import com.picocontainer.parameters.ConstantParameter;
import com.picocontainer.parameters.ConstructorParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Michael Rimov
 */
public class TraversalCheckingVisitorTest {

    private MutablePicoContainer pico;

    private MutablePicoContainer child;

    private ComponentAdapter parentAdapter;

    private ComponentAdapter childAdapter;

    @BeforeEach
    public void setUp() throws Exception {

        pico = new DefaultPicoContainer();
        SetterInjection.SetterInjector componentAdapter = new SetterInjection.SetterInjector(StringBuffer.class, StringBuffer.class,
                new NullComponentMonitor(), "set", false, "", false, null
       );
        parentAdapter = pico.addAdapter(componentAdapter).getComponentAdapter(StringBuffer.class, (NameBinding) null);
        child = pico.makeChildContainer();
        ConstructorInjection.ConstructorInjector adapter = new ConstructorInjection.ConstructorInjector(ArrayList.class, ArrayList.class, new ConstructorParameters(new ConstantParameter(3)));
        childAdapter = child.addAdapter(adapter).getComponentAdapter(ArrayList.class, (NameBinding) null);
    }

    @AfterEach
    public void tearDown() throws Exception {
        child = null;
        pico = null;
        parentAdapter = null;
        childAdapter = null;
    }

    @Test
    public void testVisitComponentAdapter() {
        final int numExpectedComponentAdapters = 2;
        final List<ComponentAdapter> allAdapters = new ArrayList<ComponentAdapter>();

        Set<ComponentAdapter> knownAdapters = new HashSet<ComponentAdapter>();
        knownAdapters.add(parentAdapter);
        knownAdapters.add(childAdapter);

        PicoVisitor containerCollector = new TraversalCheckingVisitor() {
            @Override
			public void visitComponentAdapter(final ComponentAdapter adapter) {
                super.visitComponentAdapter(adapter); //Calls checkTraversal for us.
                allAdapters.add(adapter);
            }
        };
        containerCollector.traverse(pico);

        Assertions.assertEquals(numExpectedComponentAdapters, allAdapters.size());

        for (ComponentAdapter allAdapter : allAdapters) {
            boolean knownAdapter = knownAdapters.remove(allAdapter);
            Assertions.assertTrue(knownAdapter, "Encountered unknown adapter in collection: " + allAdapters.toString());
        }

        Assertions.assertTrue(knownAdapters.size() == 0, "All adapters should match known adapters.");
    }

    @Test public void testVisitComponentFactory() {
        final List<ComponentFactory> allFactories = new ArrayList<ComponentFactory>();

        DefaultPicoContainer dpc = new DefaultPicoContainer(new Caching().wrap(new ImplementationHiding().wrap(new ConstructorInjection())));

        PicoVisitor containerCollector = new TraversalCheckingVisitor() {
            @Override
			public void visitComponentFactory(final ComponentFactory factory) {
                super.visitComponentFactory(factory); //Calls checkTraversal for us.
                allFactories.add(factory);
            }
        };
        containerCollector.traverse(dpc);

        Assertions.assertEquals(3, allFactories.size());
        Assertions.assertTrue(allFactories.get(0) instanceof Caching);
        Assertions.assertTrue(allFactories.get(1) instanceof ImplementationHiding);
        Assertions.assertTrue(allFactories.get(2) instanceof ConstructorInjection);

    }

    @Test
    public void testVisitContainer() {
        final List<PicoContainer> allContainers = new ArrayList<PicoContainer>();
        final int expectedNumberOfContainers = 2;

        PicoVisitor containerCollector = new TraversalCheckingVisitor() {
            @Override
			public boolean visitContainer(final PicoContainer pico) {
                super.visitContainer(pico); //Calls checkTraversal for us.
                allContainers.add(pico);
                return CONTINUE_TRAVERSAL;
            }
        };

        containerCollector.traverse(pico);

        Assertions.assertTrue(allContainers.size() == expectedNumberOfContainers);

        Set<MutablePicoContainer> knownContainers = new HashSet<MutablePicoContainer>();
        knownContainers.add(pico);
        knownContainers.add(child);
        for (PicoContainer oneContainer : allContainers) {
            boolean knownContainer = knownContainers.remove(oneContainer);
            Assertions.assertTrue(knownContainer, "Found a picocontainer that wasn't previously expected.");
        }

        Assertions.assertTrue(
            knownContainers.size() == 0, "All containers must match what is returned by traversal.");

    }


    @Test public void testVisitParameter() {
        final List allParameters = new ArrayList();

        PicoVisitor containerCollector = new TraversalCheckingVisitor() {
            @Override
			public void visitParameter(final Parameter param) {
                super.visitParameter(param); //Calls checkTraversal for us.
                allParameters.add(param);
            }
        };

        containerCollector.traverse(pico);

        Assertions.assertTrue(allParameters.size() == 1);
        Assertions.assertTrue(allParameters.get(0) instanceof ConstantParameter);
        ConstantParameter constantParameter = (ConstantParameter) allParameters.get(0);
        Parameter.Resolver resolver = constantParameter.resolve(null, null, null, null, null, false, null);
        Object o = resolver.resolveInstance(ComponentAdapter.NOTHING.class);
        Assertions.assertTrue(o instanceof Integer);
        Assertions.assertEquals(3, ((Integer) ((ConstantParameter) allParameters.get(0)).resolve(null, null,
                null, null, null, false, null).resolveInstance(ComponentAdapter.NOTHING.class)).intValue());
    }

}
