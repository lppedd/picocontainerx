/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
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
 * This behavior factory provides {@code synchronized} wrappers to control access to a particular component.
 * It is recommended that you use {@link Locking} instead since it results in better performance and does the same job.
 *
 * @author Aslak Helles&oslash;y
 */
@SuppressWarnings("serial")
public class Synchronizing extends AbstractBehavior {
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
    if (removePropertiesIfPresent(componentProps, Characteristics.NO_SYNCHRONIZE)) {
      return super.createComponentAdapter(
          monitor,
          lifecycle,
          componentProps,
          key,
          impl,
          constructorParams,
          fieldParams,
          methodParams
      );
    }

    removePropertiesIfPresent(componentProps, Characteristics.SYNCHRONIZE);
    return monitor.changedBehavior(
        new Synchronized<>(
            super.createComponentAdapter(
                monitor,
                lifecycle,
                componentProps,
                key,
                impl,
                constructorParams,
                fieldParams,
                methodParams
            )
        )
    );
  }

  @Override
  public <T> ComponentAdapter<T> addComponentAdapter(
      final ComponentMonitor monitor,
      final LifecycleStrategy lifecycle,
      final Properties componentProps,
      final ComponentAdapter<T> adapter) {
    if (removePropertiesIfPresent(componentProps, Characteristics.NO_SYNCHRONIZE)) {
      return super.addComponentAdapter(monitor, lifecycle, componentProps, adapter);
    }

    removePropertiesIfPresent(componentProps, Characteristics.SYNCHRONIZE);
    return monitor.changedBehavior(
        new Synchronized<>(super.addComponentAdapter(monitor, lifecycle, componentProps, adapter))
    );
  }

  /**
   * {@link ComponentAdapter} that uses {@code synchronized} around {@link ComponentAdapter#getComponentInstance}.
   *
   * @author Aslak Helles&oslash;y
   * @author Manish Shah
   */
  @SuppressWarnings("serial")
  public static class Synchronized<T> extends AbstractChangedBehavior<T> {
    public Synchronized(final ComponentAdapter<T> delegate) {
      super(delegate);
    }

    @Override
    public synchronized T getComponentInstance(final PicoContainer container, final Type into) {
      return super.getComponentInstance(container, into);
    }

    @Override
    public String getDescriptor() {
      return "Synchronized";
    }
  }
}
