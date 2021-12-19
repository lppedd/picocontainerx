/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *****************************************************************************/
package com.picocontainer;

import com.picocontainer.behaviors.Caching.Cached;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

/**
 * A component adapter is responsible for providing a specific component instance of type {@code T}.
 * An instance of an implementation of this interface is used inside a {@link PicoContainer}
 * for every registered component or instance.
 * <p>
 * Each {@code ComponentAdapter} instance has to have a key which is unique within that container.
 * The key itself is either a class type (normally an interface) or an identifier.
 * <p>
 * In an overly simplistic sense, the {@code ComponentAdapter} can be thought of as a type of object factory.
 * If you need to modify how your object is constructed, use an appropriate
 * {@code ComponentAdapter} or roll your own since the API is purposely kept rather simple.
 * <p>
 * See <a href="http://www.picocontainer.org/adapters.html">http://www.picocontainer.org/adapters.html</a>
 * for more information.
 *
 * @author Jon Tirs&eacute;n
 * @author Paul Hammant
 * @author Aslak Helles&oslash;y
 */
public interface ComponentAdapter<T> {
  class NOTHING {
    private NOTHING() {}
  }

  /**
   * Retrieve the key associated with the component.
   *
   * @return the component's key.
   *     Should either be a class type (normally an interface) or an identifier that is
   *     unique (within the scope of the current PicoContainer).
   */
  @NotNull
  Object getComponentKey();

  /**
   * Retrieve the class of the component.
   *
   * @return the component's implementation class.
   *     Should normally be a concrete class (ie, a class that can be instantiated).
   */
  @NotNull
  Class<? extends T> getComponentImplementation();

  /**
   * Retrieve the component instance.
   * <p>
   * This method will usually create a new instance each time it is called,
   * but that is not required. For example, {@link Cached} will always return the same instance.
   *
   * @param container the {@link PicoContainer} that is used to resolve
   *     any possible dependencies of the instance
   * @param into the class that is about to be injected into.
   *     Use {@link NOTHING#getClass()} if this is not important to you
   *
   * @return the component instance
   *
   * @throws PicoCompositionException if the component has dependencies which could not be resolved,
   *     or instantiation of the component lead to an ambiguous situation within the container
   */
  @Nullable
  T getComponentInstance(@NotNull final PicoContainer container, @NotNull final Type into);

  /**
   * Verify that all dependencies for this adapter can be satisfied.
   * Normally, the adapter should verify this by checking that the associated PicoContainer
   * contains all the needed dependencies.
   *
   * @param container the {@link PicoContainer} that is used to resolve
   *     any possible dependencies of the instance.
   *
   * @throws PicoCompositionException if one or more dependencies cannot be resolved.
   */
  void verify(@NotNull final PicoContainer container);

  /**
   * Accepts a visitor for this {@code ComponentAdapter}.
   * The method is normally called by visiting a {@link PicoContainer},
   * that cascades the visitor also down to all its {@code ComponentAdapter} instances.
   *
   * @param visitor the visitor
   */
  void accept(@NotNull final PicoVisitor visitor);

  /**
   * Component adapters may be nested in a chain, and this method is used to grab
   * the next {@code ComponentAdapter} in the chain.
   *
   * @return the next {@code ComponentAdapter} in line
   *     or {@code null} if there is no delegate {@code ComponentAdapter}.
   */
  @Nullable
  ComponentAdapter<T> getDelegate();

  /**
   * Locates a component adapter of type {@code adapterType} in the {@code ComponentAdapter} chain.
   * Will return {@code null} if there is no adapter of the given type.
   *
   * @param <U> the type of {@code ComponentAdapter} being located
   * @param adapterType the class of the adapter type being located
   *
   * @return the appropriate component adapter of type {@code U}.
   *     May return {@code null} if the component adapter type is not returned
   */
  @Nullable <U extends ComponentAdapter<?>> U findAdapterOfType(@NotNull final Class<U> adapterType);

  /**
   * Get a string key descriptor of the component adapter.
   *
   * @return the descriptor
   */
  @NotNull
  String getDescriptor();
}
