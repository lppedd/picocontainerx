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
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;

import java.io.Serializable;
import java.util.Properties;

/**
 * TODO
 */
@SuppressWarnings("serial")
public class Automating extends AbstractBehavior implements Serializable {
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
    removePropertiesIfPresent(componentProps, Characteristics.AUTOMATIC);
    return monitor.changedBehavior(
        new Automated<>(
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
    removePropertiesIfPresent(componentProps, Characteristics.AUTOMATIC);
    return monitor.changedBehavior(
        new Automated<>(
            super.addComponentAdapter(
                monitor,
                lifecycle,
                componentProps,
                adapter
            )
        )
    );
  }

  @SuppressWarnings("serial")
  public static class Automated<T> extends AbstractChangedBehavior<T> implements Serializable {
    public Automated(final ComponentAdapter<T> delegate) {
      super(delegate);
    }

    @Override
    public boolean hasLifecycle(final Class<?> type) {
      return true;
    }

    @Override
    public String getDescriptor() {
      return "Automated";
    }
  }
}
