/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by Joerg Schaibe                                            *
 *****************************************************************************/

package com.picocontainer.behaviors;

import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.Decorator;
import com.picocontainer.LifecycleStrategy;
import com.picocontainer.PicoContainer;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;

import java.lang.reflect.Type;
import java.util.Properties;

/**
 * Behavior for Decorating. This factory will create {@link Decorated} that will
 * allow you to decorate what you like on the component instance that has been created.
 *
 * @author Paul Hammant
 */
@SuppressWarnings("serial")
public abstract class Decorating extends AbstractBehavior implements Decorator {
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
    return monitor.changedBehavior(
        new Decorated<>(
            super.createComponentAdapter(
                monitor,
                lifecycle,
                componentProps,
                key,
                impl,
                constructorParams,
                fieldParams,
                methodParams
            ),
            this
        )
    );
  }

  @SuppressWarnings("serial")
  public static class Decorated<T> extends AbstractChangedBehavior<T> {
    private final Decorator decorator;

    public Decorated(final ComponentAdapter<T> delegate, final Decorator decorator) {
      super(delegate);
      this.decorator = decorator;
    }

    @Override
    public T getComponentInstance(final PicoContainer container, final Type into) {
      final T instance = super.getComponentInstance(container, into);
      decorator.decorate(instance);
      return instance;
    }

    @Override
    public String getDescriptor() {
      return "Decorated";
    }
  }
}
