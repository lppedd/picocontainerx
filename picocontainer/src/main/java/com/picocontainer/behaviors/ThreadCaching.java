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
import com.picocontainer.behaviors.Storing.Stored;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;
import com.picocontainer.references.ThreadLocalReference;

import java.util.Properties;

/**
 * @author Paul Hammant
 */
@SuppressWarnings("serial")
public class ThreadCaching extends AbstractBehavior {
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
    if (removePropertiesIfPresent(componentProps, Characteristics.NO_CACHE)) {
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

    removePropertiesIfPresent(componentProps, Characteristics.CACHE);
    return monitor.changedBehavior(
        new ThreadCached<>(
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
    if (removePropertiesIfPresent(componentProps, Characteristics.NO_CACHE)) {
      return super.addComponentAdapter(monitor, lifecycle, componentProps, adapter);
    }

    removePropertiesIfPresent(componentProps, Characteristics.CACHE);
    return monitor.changedBehavior(
        new ThreadCached<>(super.addComponentAdapter(monitor, lifecycle, componentProps, adapter))
    );
  }

  /**
   * This behavior supports cached values per thread.
   *
   * @author Paul Hammant
   */
  public static final class ThreadCached<T> extends Stored<T> {
    public ThreadCached(final ComponentAdapter<T> delegate) {
      super(delegate, new ThreadLocalReference<>());
    }

    @Override
    public String getDescriptor() {
      return "ThreadCached" + getLifecycleDescriptor();
    }
  }
}
