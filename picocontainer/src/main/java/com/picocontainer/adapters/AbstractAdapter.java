/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer.adapters;

import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.ComponentMonitorStrategy;
import com.picocontainer.PicoVisitor;
import com.picocontainer.injectors.Provider;
import com.picocontainer.injectors.ProviderAdapter;
import com.picocontainer.monitors.NullComponentMonitor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

/**
 * <p>
 * Base class for a {@link ComponentAdapter} with general functionality.
 * This implementation provides basic checks for a healthy implementation of a {@code ComponentAdapter}.
 * </p>
 * <p>
 * It does not allow to use {@code null} for the component key or the implementation,
 * ensures that the implementation is a concrete class, and that the key is assignable from the
 * implementation if the key represents a type.
 * </p>
 *
 * @author Paul Hammant
 * @author Aslak Helles&oslash;y
 * @author Jon Tirs&eacute;n
 */
@SuppressWarnings("serial")
public abstract class AbstractAdapter<T> implements ComponentAdapter<T>, ComponentMonitorStrategy, Serializable {
  @NotNull
  private final Object key;

  @NotNull
  private final Class<? extends T> impl;

  @NotNull
  private ComponentMonitor monitor;

  /**
   * Constructs a new {@link ComponentAdapter} for the given key and implementation.
   *
   * @param key the search key for this implementation
   * @param impl the concrete implementation
   */
  public AbstractAdapter(
      @NotNull final Object key,
      @NotNull final Class<? extends T> impl) {
    this(key, impl, new NullComponentMonitor());
  }

  /**
   * Constructs a new {@link ComponentAdapter} for the given key and implementation.
   *
   * @param key the search key for this implementation
   * @param impl the concrete implementation
   * @param monitor the component monitor used by this {@code ComponentAdapter}
   */
  public AbstractAdapter(
      @NotNull final Object key,
      @NotNull final Class<? extends T> impl,
      @NotNull final ComponentMonitor monitor) {
    this.key = requireNonNull(key, "The key cannot be null");
    this.impl = requireNonNull(impl, "The implementation's class cannot be null");
    this.monitor = requireNonNull(monitor, "The monitor cannot be null");
    checkTypeCompatibility();
  }

  @NotNull
  @Override
  public Object getComponentKey() {
    return key;
  }

  @NotNull
  @Override
  public Class<? extends T> getComponentImplementation() {
    return impl;
  }

  protected void checkTypeCompatibility() {
    if (key instanceof Class) {
      final Class<?> componentType = (Class<?>) key;

      if (Provider.class.isAssignableFrom(impl)) {
        if (!componentType.isAssignableFrom(ProviderAdapter.getProvideMethod(impl).getReturnType())) {
          throw new ClassCastException(impl.getName() + " is not a " + componentType.getName());
        }
      } else if (!componentType.isAssignableFrom(impl)) {
        throw new ClassCastException(impl.getName() + " is not a " + componentType.getName());
      }
    }
  }

  @Override
  public String toString() {
    return getDescriptor() + getComponentKey();
  }

  @Override
  public void accept(final PicoVisitor visitor) {
    visitor.visitComponentAdapter(this);
  }

  @NotNull
  @Override
  public ComponentMonitor changeMonitor(@NotNull final ComponentMonitor monitor) {
    final ComponentMonitor returnValue = this.monitor;
    this.monitor = requireNonNull(monitor, "The monitor cannot be null");
    return returnValue;
  }

  @NotNull
  @Override
  public ComponentMonitor currentMonitor() {
    return monitor;
  }

  @Contract("-> null")
  @Override
  public final ComponentAdapter<T> getDelegate() {
    return null;
  }

  @Nullable
  @Override
  public final <U extends ComponentAdapter<?>> U findAdapterOfType(final Class<U> adapterType) {
    return adapterType.isAssignableFrom(getClass()) ? (U) this : null;
  }
}
