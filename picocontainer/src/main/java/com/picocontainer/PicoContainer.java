/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer;

import com.googlecode.jtype.Generic;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/**
 * This is the core interface for PicoContainer. It is used to retrieve component instances from the container; it only
 * has accessor methods (in addition to the {@link #accept(PicoVisitor)} method). In order to register components in a
 * PicoContainer, use a {@link MutablePicoContainer}, such as {@link DefaultPicoContainer}.
 *
 * @author Paul Hammant
 * @author Aslak Helles&oslash;y
 * @author Jon Tirs&eacute;n
 * @see <a href="package-summary.html#package_description">See package description for basic overview how to use
 *      PicoContainer.</a>
 */
public interface PicoContainer {

    /**
     * Retrieve a component instance registered with a specific key or type. If a component cannot be found in this container,
     * the parent container (if one exists) will be searched.
     *
     * @param keyOrType the key or Type that the component was registered with.
     * @return an instantiated component, or <code>null</code> if no component has been registered for the specified
     *         key.
     */
    Object getComponent(Object keyOrType);

    Object getComponentInto(Object keyOrType, Type into);

    /**
     * Retrieve a component keyed by the component type.
     * @param componentType the type of the component
     * @return the typed resulting object instance or null if the object does not exist.
     */
    <T> T getComponent(Class<T> componentType);

    <T> T getComponent(Generic<T> componentType);

    /**
     * Retrieve a component keyed by the component type.
     * @param componentType the type of the component
     * @param into
     * @return the typed resulting object instance or null if the object does not exist.
     */
    <T> T getComponentInto(Class<T> componentType, Type into);

    <T> T getComponentInto(Generic<T> componentType, Type into);

    /**
     * Retrieve a component keyed by the component type and binding type.
     * @param componentType the type of the component
     * @param binding the binding type of the component
     * @param into
     * @return the typed resulting object instance or null if the object does not exist.
     */
    <T> T getComponent(Class<T> componentType, Class<? extends Annotation> binding, Type into);

    <T> T getComponent(Class<T> componentType, Class<? extends Annotation> binding);

    /**
     * Retrieve all the registered component instances in the container, (not including those in the parent container).
     * The components are returned in their order of instantiation, which depends on the dependency order between them.
     *
     * @return all the components.
     * @throws PicoException if the instantiation of the component fails
     */
    List<Object> getComponents();

    /**
     * Retrieve the parent container of this container.
     *
     * @return a {@link PicoContainer} instance, or <code>null</code> if this container does not have a parent.
     */
    PicoContainer getParent();

    /**
     * Find a component adapter associated with the specified key. If a component adapter cannot be found in this
     * container, the parent container (if one exists) will be searched.
     *
     * @param key the key that the component was registered with.
     * @return the component adapter associated with this key, or <code>null</code> if no component has been
     *         registered for the specified key.
     */
    ComponentAdapter<?> getComponentAdapter(Object key);

    /**
     * Find a component adapter associated with the specified type and binding name. If a component adapter cannot be found in this
     * container, the parent container (if one exists) will be searched.
     *
     * @param componentType the type of the component.
     * @param nameBinding the name binding to use. May be {@link com.picocontainer.NameBinding.NULL NameBinding.NULL}
     * 			if name binding is not going to be used to resolve the component adapter.
     * @return the component adapter associated with this class, or <code>null</code> if no component has been
     *         registered for the specified key.
     */
    <T> ComponentAdapter<T> getComponentAdapter(Class<T> componentType, NameBinding nameBinding);

    /**
     * Similar to {@link #getComponentAdapter(Class, NameBinding)}, except that it uses a JType generic
     * as its type.
     * @param <T> refers to the type of object the ComponentAdapter will construct.
     * @param componentType the type of the component that a ComponentAdapter retrieved by
     * this call will construct.
     * @param nameBinding the name binding to use. May be {@link com.picocontainer.NameBinding.NULL NameBinding.NULL}
     * 			if name binding is not going to be used to resolve the component adapter.
     * @return the component adapter associated with this class, or <code>null</code> if no component has been
     *         registered for the specified key.
     */
    <T> ComponentAdapter<T> getComponentAdapter(Generic<T> componentType, NameBinding nameBinding);

    /**
     * Find a component adapter associated with the specified type and binding type. If a component adapter cannot be found in this
     * container, the parent container (if one exists) will be searched.
     *
     * @return the component adapter associated with this class, or <code>null</code> if no component has been
     *         registered for the specified key.  @param componentType the type of the component.
     * @param binding the typed binding to use
     */
    <T> ComponentAdapter<T> getComponentAdapter(Class<T> componentType, Class<? extends Annotation> binding);
    <T> ComponentAdapter<T> getComponentAdapter(Generic<T> componentType, Class<? extends Annotation> binding);

    /**
     * Retrieve all the component adapters inside this container. The component adapters from the parent container are
     * not returned.
     *
     * @return a collection containing all the {@link ComponentAdapter}s inside this container. The collection will not
     *         be modifiable.
     * @see #getComponentAdapters(Generic a variant of this method which returns the component adapters inside this
     *      container that are associated with the specified type.
     */
    Collection<ComponentAdapter<?>> getComponentAdapters();

    /**
     * Retrieve all component adapters inside this container that are associated with the specified type. The addComponent
     * adapters from the parent container are not returned.
     *
     * @param componentType the type of the components.
     * @return a collection containing all the {@link ComponentAdapter}s inside this container that are associated with
     *         the specified type. Changes to this collection will not be reflected in the container itself.
     */
    <T> List<ComponentAdapter<T>> getComponentAdapters(Class<T> componentType);

    <T> List<ComponentAdapter<T>> getComponentAdapters(Generic<T> componentType);

    /**
     * Retrieve all component adapters inside this container that are associated with the specified type and binding type. The addComponent
     * adapters from the parent container are not returned.
     *
     * @param componentType the type of the components.
     * @param binding the typed binding to use
     * @return a collection containing all the {@link ComponentAdapter}s inside this container that are associated with
     *         the specified type. Changes to this collection will not be reflected in the container itself.
     */
    <T> List<ComponentAdapter<T>> getComponentAdapters(Class<T> componentType, Class<? extends Annotation> binding);
    <T> List<ComponentAdapter<T>> getComponentAdapters(Generic<T> componentType, Class<? extends Annotation> binding);

    /**
     * Returns a List of components of a certain componentType. The list is ordered by instantiation order, starting
     * with the components instantiated first at the beginning.
     *
     * @param componentType the searched type.
     * @return a List of components.
     * @throws PicoException if the instantiation of a component fails
     */
    <T> List<T> getComponents(Class<T> componentType);

    /**
     * Accepts a visitor that should visit the child containers, component adapters and component instances.
     *
     * @param visitor the visitor
     */
    void accept(PicoVisitor visitor);

}
