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
import com.picocontainer.ComponentMonitorStrategy;
import com.picocontainer.InjectionType;
import com.picocontainer.Injector;
import com.picocontainer.LifecycleStrategy;
import com.picocontainer.PicoContainer;
import com.picocontainer.PicoVisitor;
import com.picocontainer.behaviors.AbstractBehavior;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * A Composite of other types on InjectionFactories - pass them into the varargs constructor.
 *
 * @author Paul Hammant
 */
@SuppressWarnings("serial")
public class CompositeInjection extends AbstractInjectionType {
  private final InjectionType[] injectionTypes;

  public CompositeInjection(@NotNull final InjectionType... injectionTypes) {
    this.injectionTypes = injectionTypes;
  }

  @Override
  public <T> ComponentAdapter<T> createComponentAdapter(
      final ComponentMonitor monitor,
      final LifecycleStrategy lifecycle,
      final Properties componentProps,
      final Object key,
      final Class<T> impl,
      @Nullable final ConstructorParameters constructorParams,
      @Nullable final FieldParameters @Nullable [] fieldParams,
      @Nullable final MethodParameters @Nullable [] methodParams) {
    @SuppressWarnings("unchecked")
    final com.picocontainer.Injector<T>[] injectors = new com.picocontainer.Injector[injectionTypes.length];

    for (int i = 0; i < injectionTypes.length; i++) {
      final InjectionType injectionType = injectionTypes[i];
      injectors[i] = (com.picocontainer.Injector<T>) injectionType.createComponentAdapter(
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

    final boolean useNames = AbstractBehavior.arePropertiesPresent(componentProps, Characteristics.USE_NAMES, true);
    final Injector<T> injector = new CompositeInjector<>(key, impl, monitor, useNames, injectors);
    return wrapLifeCycle(monitor.newInjector(injector), lifecycle);
  }

  public static class CompositeInjector<T> extends AbstractInjector<T> {
    @NotNull
    private final com.picocontainer.Injector<T>[] injectors;

    @SafeVarargs
    public CompositeInjector(
        @NotNull final Object key,
        @NotNull final Class<T> impl,
        @NotNull final ComponentMonitor monitor,
        final boolean useNames,
        @NotNull final com.picocontainer.Injector<T>... injectors) {
      super(key, impl, monitor, useNames);
      this.injectors = injectors;
    }

    @Override
    public T getComponentInstance(final PicoContainer container, final Type into) {
      T instance = null;

      for (final Class<?> eachSuperClass : getListOfSupertypesToDecorate(getComponentImplementation())) {
        for (final com.picocontainer.Injector<T> injector : injectors) {
          if (instance == null) {
            instance = injector.getComponentInstance(container, NOTHING.class);
          } else {
            injector.partiallyDecorateComponentInstance(container, into, instance, eachSuperClass);
          }
        }
      }

      return instance;
    }

    protected Class<?>[] getListOfSupertypesToDecorate(final Class<?> startClass) {
      if (startClass == null) {
        throw new NullPointerException("startClass");
      }

      final List<Class<?>> result = new ArrayList<>();
      Class<?> current = startClass;

      while (!Object.class.getName().equals(current.getName())) {
        result.add(current);
        current = current.getSuperclass();
      }

      // Needed for: com.picocontainer.injectors.AdaptingInjectionTestCase.testSingleUsecanBeInstantiatedByDefaultComponentAdapter()
      if (result.isEmpty()) {
        result.add(Object.class);
      }

      // Start with base class, not derived class.
      Collections.reverse(result);
      return result.toArray(new Class[0]);
    }

    /**
     * Performs a set of partial injections starting at the base class and working its
     * way down.
     * <p>{@inheritDoc}</p>
     *
     * @return the object returned is the result of the last of the injectors delegated to
     */
    @Override
    public Object decorateComponentInstance(
        final PicoContainer container,
        final Type into,
        final T instance) {
      Object result = null;
      for (final Class<?> eachSuperClass : getListOfSupertypesToDecorate(instance.getClass())) {
        result = partiallyDecorateComponentInstance(container, into, instance, eachSuperClass);
      }

      return result;

    }

    @Override
    public Object partiallyDecorateComponentInstance(
        final PicoContainer container, final Type into, final T instance,
        final Class<?> classFilter) {
      Object result = null;

      for (final com.picocontainer.Injector<T> injector : injectors) {
        result = injector.partiallyDecorateComponentInstance(container, into, instance, classFilter);
      }
      return result;
    }

    @Override
    public void verify(final PicoContainer container) {
      for (final com.picocontainer.Injector<T> injector : injectors) {
        injector.verify(container);
      }
    }

    @Override
    public final void accept(final PicoVisitor visitor) {
      super.accept(visitor);

      for (final com.picocontainer.Injector<T> injector : injectors) {
        injector.accept(visitor);
      }
    }

    @Override
    public String getDescriptor() {
      final StringBuilder sb = new StringBuilder("CompositeInjector(");
      for (final com.picocontainer.Injector<T> injector : injectors) {
        sb.append(injector.getDescriptor());
      }

      if (sb.charAt(sb.length() - 1) == '-') {
        sb.deleteCharAt(sb.length() - 1); // remove last dash
      }

      return sb.toString().replace("-", "+") + ")-";
    }

    @Override
    public ComponentMonitor changeMonitor(final ComponentMonitor monitor) {
      final ComponentMonitor result = super.changeMonitor(monitor);

      for (final com.picocontainer.Injector<?> eachInjector : injectors) {
        if (eachInjector instanceof ComponentMonitorStrategy) {
          ((ComponentMonitorStrategy) eachInjector).changeMonitor(monitor);
        }
      }

      return result;
    }
  }
}
