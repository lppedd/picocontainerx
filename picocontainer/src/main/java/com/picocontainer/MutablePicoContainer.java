/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Idea by Rachel Davies, Original code by various                           *
 *****************************************************************************/
package com.picocontainer;

import com.picocontainer.injectors.Provider;
import com.picocontainer.lifecycle.LifecycleState;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.Properties;

/**
 * This is the core interface used for registration of components with a container.
 * It is possible to register implementations and instances.
 *
 * @author Paul Hammant
 * @author Aslak Helles&oslash;y
 * @author Jon Tirs&eacute;n
 * @see <a href="package-summary.html#package_description">See package description for basic overview how to use
 * PicoContainer</a>
 */
public interface MutablePicoContainer extends PicoContainer, Startable, Disposable {
  interface BindTo<T> {
    MutablePicoContainer to(final Class<? extends T> impl);
    MutablePicoContainer to(final T instance);
    MutablePicoContainer toProvider(final javax.inject.Provider<? extends T> provider);
    MutablePicoContainer toProvider(final Provider provider);
  }

  interface BindWithOrTo<T> extends BindTo<T> {
    <T> BindTo<T> withAnnotation(final Class<? extends Annotation> annotation);
    <T> BindTo<T> named(final String name);
  }

  <T> BindWithOrTo<T> bind(final Class<T> type);

  /**
   * Register a component and creates specific instructions on which constructor to use, along with
   * which components and/or constants to provide as constructor arguments.
   * These "directives" are provided through an array of {@code Parameter} objects.
   * {@code Parameter[0]} corresponds to the first constructor argument,
   * {@code Parameter[N]} corresponds to the N+1th constructor argument.
   *
   * <h4>Tips for Parameter usage</h4>
   * <ul>
   * <li><strong>Partial Autowiring:</strong> if you have two constructor args to match
   * and you only wish to specify one of the constructors and let PicoContainer wire the other one,
   * you can use as parameters: {@code new ComponentParameter(), new ComponentParameter("someService")}.
   * The default constructor for the component parameter indicates auto-wiring should take place for that parameter.
   * </li>
   * <li><strong>Force No-Arg constructor usage:</strong> if you wish to force a component to be constructed with
   * the no-arg constructor, use a zero length {@code Parameter} array.
   * <ul>
   *
   * @param key a key that identifies the component. Must be unique within the container.
   * The type of the key object has no semantic significance unless explicitly specified in the
   * documentation of the implementing container.
   * @param implOrInstance the component's implementation class. This must be a concrete class (i.e., a
   * class that can be instantiated), or an instance of the component.
   * @param constructorParameters the parameters that gives the container hints about what arguments to pass
   * to the constructor when it is instantiated. Container implementations may ignore
   * one or more of these hints.
   * @return the same instance of {@code MutablePicoContainer}
   *
   * @see Parameter
   * @see com.picocontainer.parameters.ConstantParameter
   * @see com.picocontainer.parameters.ComponentParameter
   */
  MutablePicoContainer addComponent(
      final Object key,
      final Object implOrInstance,
      final Parameter... constructorParameters
  );

  /**
   * Longhand method for adding components when multiple injection is used.
   *
   * @param key the object key. Most often either a {@code String} or a {@code Class}.
   * @param implOrInstance the component's implementation class.
   * @param constructorParams parameters for the constructor of the object may be
   * zero length by using {@linkplain ConstructorParameters#NO_ARG_CONSTRUCTOR}
   * @param fieldParams an array of field parameters to override PicoContainer's autowiring capabilities.
   * @param methodParams an array of method parameters to override PicoContainer's autowiring capabilities.
   * @return the same instance of {@code MutablePicoContainer}
   */
  MutablePicoContainer addComponent(
      final Object key,
      final Object implOrInstance,
      final ConstructorParameters constructorParams,
      final FieldParameters[] fieldParams,
      final MethodParameters[] methodParams
  );

  /**
   * Register an arbitrary object. The class of the object will be used as a key.
   * Calling this method is equivalent to calling {@code addComponent(impl, impl)}.
   *
   * @param implOrInstance component implementation or instance
   * @return the same instance of {@code MutablePicoContainer}
   */
  MutablePicoContainer addComponent(final Object implOrInstance);

  /**
   * Register a config item.
   *
   * @param name the name of the config item
   * @param val the value of the config item
   * @return the same instance of {@code MutablePicoContainer}
   *
   * @throws PicoCompositionException if registration fails.
   */
  MutablePicoContainer addConfig(final String name, final Object val);

  /**
   * Register a component via a {@link ComponentAdapter}.
   * <p>
   * Use this if you need fine grained control over what {@code ComponentAdapter} to use for a specific component.
   * The adapter will be wrapped in whatever behaviors the container has been set up with.
   * If you want to bypass that behavior for the adapter you are adding you should use
   * {@link Characteristics#NONE} (e.g. {@code container.as(Characteristics.NONE).addAdapter(...)}).
   *
   * @param componentAdapter the adapter
   * @return the same instance of {@code MutablePicoContainer}
   */
  MutablePicoContainer addAdapter(final ComponentAdapter<?> componentAdapter);

  /**
   * Adds a {@link javax.inject.Provider} to the container.
   *
   * @param provider
   * @return the same instance of {@code MutablePicoContainer}
   */
  MutablePicoContainer addProvider(final javax.inject.Provider<?> provider);

  /**
   * Adds a {@link javax.inject.Provider} with a particular key to the container.
   *
   * @param key if you use this, its usually a string value.
   * @param provider
   * @return the same instance of {@code MutablePicoContainer}
   */
  MutablePicoContainer addProvider(final Object key, final javax.inject.Provider<?> provider);

  /**
   * Unregister a component by key.
   *
   * @param key key of the component to unregister.
   * @return the {@link ComponentAdapter} that was associated with this component.
   */
  @Nullable <T> ComponentAdapter<T> removeComponent(final Object key);

  /**
   * Unregister a component by instance.
   *
   * @param componentInstance the component instance to unregister.
   * @return the same instance of {@code MutablePicoContainer}
   */
  @Nullable <T> ComponentAdapter<T> removeComponentByInstance(final T componentInstance);

  /**
   * Make a child container, using both the same implementation of {@code MutablePicoContainer}
   * as the parent and identical behaviors as well.
   * It will have a reference to this as parent and will list the resulting {@code MutablePicoContainer} as a child.
   * Lifecycle events will be cascaded from parent to child as a consequence.
   * <p>
   * Note that for long-lived parent containers, you need to unregister child containers
   * made with this call before disposing or you will leak memory.
   * </p>
   * <p>
   * Incorrect Example:
   * </p>
   * <pre>
   *   MutablePicoContainer parent = new PicoBuilder().withCaching().withLifecycle().build();
   *   MutablePicoContainer child = parent.makeChildContainer();
   *   child = null; //Child still retains in memory because parent still holds reference.
   * </pre>
   * <p>Correct Example:</p>
   * <pre>
   *   MutablePicoContainer parent = new PicoBuilder().withCaching().withLifecycle().build();
   *   MutablePicoContainer child = parent.makeChildContainer();
   *   parent.removeChildContainer(child); //Remove the bi-directional references.
   *   child = null;
   * </pre>
   *
   * @return the new child container.
   */
  @NotNull
  MutablePicoContainer makeChildContainer();

  /**
   * Add a child container.
   * This action will list the "child" as exactly that in the parents scope.
   * It will not change the child's view of a parent, which is determined by
   * the constructor arguments of the child itself.
   * Lifecycle events will be cascaded from parent to child as a consequence of calling this method.
   *
   * @param child the child container
   * @return the same instance of {@code MutablePicoContainer}
   */
  @NotNull
  MutablePicoContainer addChildContainer(final PicoContainer child);

  /**
   * Remove a child container from this container.
   * It will not change the child's view of a parent.
   * Lifecycle events will no longer be cascaded from the parent to the child.
   *
   * @param child the child container
   * @return {@code true} if the child container has been removed
   */
  boolean removeChildContainer(final PicoContainer child);

  /**
   * You can change the characteristic of registration of all subsequent components in this container.
   *
   * @param properties
   * @return the same Pico instance with changed properties
   */
  MutablePicoContainer change(final Properties... properties);

  /**
   * You can set for the following operation only the characteristic of registration of a component on the fly.
   *
   * @param properties
   * @return the same Pico instance with temporary properties
   */
  MutablePicoContainer as(final Properties... properties);

  /**
   * Name the container instance, to assist debugging.
   *
   * @param name the name to call it.
   * @since 2.8
   */
  void setName(@Nullable final String name);

  /**
   * To assist ThreadLocal usage, LifecycleState can be set.
   * No need to use this for normal usages.
   *
   * @param lifecycleState the lifecycle state to use.
   * @since 2.8
   */
  void setLifecycleState(final LifecycleState lifecycleState);

  /**
   * Retrieve the name set (if any).
   *
   * @return the arbitrary name of the container set by calling {@link #setName(String)}.
   *
   * @since 2.10.2
   */
  @Nullable
  String getName();

  /**
   * Allow querying of the current lifecycle state of a {@code MutablePicoContainer}.
   *
   * @return the current lifecycle state.
   *
   * @since 2.10.2
   */
  LifecycleState getLifecycleState();

  /**
   * Changes monitor in the {@link ComponentFactory}, the component adapters
   * and the child containers, if these support a {@link ComponentMonitorStrategy}.
   *
   * @return the old {@code ComponentMonitor}
   *
   * @since 3.0
   */
  ComponentMonitor changeMonitor(final ComponentMonitor monitor);
}
