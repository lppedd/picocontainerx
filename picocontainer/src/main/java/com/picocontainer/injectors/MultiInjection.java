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
import com.picocontainer.LifecycleStrategy;
import com.picocontainer.annotations.Inject;
import com.picocontainer.behaviors.AbstractBehavior;
import com.picocontainer.injectors.AnnotatedFieldInjection.AnnotatedFieldInjector;
import com.picocontainer.injectors.AnnotatedMethodInjection.AnnotatedMethodInjector;
import com.picocontainer.injectors.CompositeInjection.CompositeInjector;
import com.picocontainer.injectors.ConstructorInjection.ConstructorInjector;
import com.picocontainer.injectors.SetterInjection.SetterInjector;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;

import java.util.Properties;

import static com.picocontainer.injectors.AnnotatedMethodInjection.getInjectionAnnotation;

/**
 * @author Paul Hammant
 */
@SuppressWarnings("serial")
public class MultiInjection extends AbstractInjectionType {
  private final String setterPrefix;

  public MultiInjection(final String setterPrefix) {
    this.setterPrefix = setterPrefix;
  }

  public MultiInjection() {
    this("set");
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
    final boolean useNames =
        AbstractBehavior.arePropertiesPresent(componentProps, Characteristics.USE_NAMES, true);
    final boolean requireConsumptionOfAllParameters =
        !AbstractBehavior.arePropertiesPresent(componentProps, Characteristics.ALLOW_UNUSED_PARAMETERS, false);

    return wrapLifeCycle(
        new MultiInjector<>(
            key,
            impl,
            monitor,
            setterPrefix,
            useNames,
            requireConsumptionOfAllParameters,
            constructorParams,
            fieldParams,
            methodParams
        ),
        lifecycle
    );
  }

  /**
   * @author Paul Hammant
   */
  @SuppressWarnings("serial")
  public static class MultiInjector<T> extends CompositeInjector<T> {
    @SuppressWarnings("unchecked")
    public MultiInjector(
        final Object key,
        final Class<T> impl,
        final ComponentMonitor monitor,
        final String setterPrefix,
        final boolean useNames,
        final boolean useAllParameter,
        final ConstructorParameters constructorParams,
        final FieldParameters[] fieldParams,
        final MethodParameters[] methodParams) {
      super(key, impl, monitor, useNames,
          monitor.newInjector(new ConstructorInjector<>(monitor, useNames, key, impl, constructorParams)),
          monitor.newInjector(new SetterInjector<>(key, impl, monitor, setterPrefix, useNames, "", false, methodParams)),
          monitor.newInjector(new AnnotatedMethodInjector<>(key, impl, methodParams, monitor, useNames, useAllParameter, Inject.class, getInjectionAnnotation("javax.inject.Inject"))),
          monitor.newInjector(new AnnotatedFieldInjector<>(key, impl, fieldParams, monitor, useNames, useAllParameter, Inject.class, getInjectionAnnotation("javax.inject.Inject")))
      );
    }

    @Override
    public String getDescriptor() {
      return "MultiInjector";
    }
  }
}
