/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/
package com.picocontainer.injectors;

import com.picocontainer.Characteristics;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.InjectionType;
import com.picocontainer.LifecycleStrategy;
import com.picocontainer.PicoContainer;
import com.picocontainer.behaviors.AbstractBehavior;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Properties;

/**
 * TODO
 */
@SuppressWarnings("serial")
public class Reinjection extends CompositeInjection {
  public Reinjection(final InjectionType reInjectionType, final PicoContainer parent) {
    super(new ReInjectionInjectionType(parent), reInjectionType);
  }

  private static class ReInjectionInjector<T> extends AbstractInjector<T> {
    @NotNull
    private final PicoContainer parent;

    ReInjectionInjector(
        @NotNull final Object key,
        @NotNull final Class<T> impl,
        @NotNull final ComponentMonitor monitor,
        @NotNull final PicoContainer parent,
        final boolean useNames,
        final ConstructorParameters constructorParams,
        final FieldParameters[] fieldParams,
        final MethodParameters[] methodParams) {
      super(key, impl, monitor, useNames, methodParams);
      this.parent = parent;
    }

    @Override
    public T getComponentInstance(final PicoContainer container, final Type into) {
      return (T) parent.getComponentInto(getComponentKey(), into);
    }
  }

  private static class ReInjectionInjectionType extends AbstractInjectionType {
    private final PicoContainer parent;

    ReInjectionInjectionType(final PicoContainer parent) {
      this.parent = parent;
    }

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
      final boolean useNames = AbstractBehavior.arePropertiesPresent(componentProps, Characteristics.USE_NAMES, true);
      return new ReInjectionInjector<>(key, impl, monitor, parent, useNames, constructorParams, fieldParams, methodParams);
    }
  }
}
