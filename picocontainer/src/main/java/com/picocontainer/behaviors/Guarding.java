/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Idea by Rachel Davies, Original code by Aslak Hellesoy and Paul Hammant   *
 *****************************************************************************/

package com.picocontainer.behaviors;

import com.picocontainer.Characteristics;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.LifecycleStrategy;
import com.picocontainer.PicoContainer;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;

import java.lang.reflect.Type;
import java.util.Properties;

/**
 * Factory class creating guard behaviour.
 *
 * @author Paul Hammant
 */
@SuppressWarnings("serial")
public class Guarding extends AbstractBehavior {
  @Override
  public <T> ComponentAdapter<T> createComponentAdapter(
      final ComponentMonitor monitor,
      final LifecycleStrategy lifecycle,
      final Properties componentProps,
      final Object key,
      final Class<T> impl,
      final ConstructorParameters constructorParams,
      final FieldParameters[] fieldParams,
      final MethodParameters[] methodParams) {
    final String guard = getAndRemovePropertiesIfPresentByKey(componentProps, Characteristics.GUARD);
    final ComponentAdapter<T> delegate =
        super.createComponentAdapter(
            monitor,
            lifecycle,
            componentProps,
            key,
            impl,
            constructorParams,
            fieldParams,
            methodParams
        );

    // noinspection SimplifiableIfStatement
    if (guard == null) {
      return delegate;
    }

    return monitor.changedBehavior(new Guarded<>(delegate, guard));
  }

  @Override
  public <T> ComponentAdapter<T> addComponentAdapter(
      final ComponentMonitor monitor,
      final LifecycleStrategy lifecycle,
      final Properties componentProps,
      final ComponentAdapter<T> adapter) {
    final String guard = getAndRemovePropertiesIfPresentByKey(componentProps, Characteristics.GUARD);
    final ComponentAdapter<T> delegate = super.addComponentAdapter(monitor, lifecycle, componentProps, adapter);

    // noinspection SimplifiableIfStatement
    if (guard == null) {
      return delegate;
    }

    return monitor.changedBehavior(monitor.changedBehavior(new Guarded<>(delegate, guard)));
  }

  /**
   * Behaviour that allows components to be guarded by another component.
   *
   * @param <T>
   * @author Paul Hammant
   */
  @SuppressWarnings("serial")
  public static class Guarded<T> extends AbstractChangedBehavior<T> {
    private final String guard;

    public Guarded(final ComponentAdapter<T> delegate, final String guard) {
      super(delegate);
      this.guard = guard;
    }

    @Override
    public T getComponentInstance(final PicoContainer container, final Type into) {
      container.getComponentInto(guard, into);
      return super.getComponentInstance(container, into);
    }

    @Override
    public String getDescriptor() {
      return "Guarded(with " + guard + ")";
    }
  }
}
