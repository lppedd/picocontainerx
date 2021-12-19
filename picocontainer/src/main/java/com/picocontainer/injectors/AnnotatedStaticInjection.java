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
import com.picocontainer.PicoContainer;
import com.picocontainer.behaviors.AbstractBehavior;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@SuppressWarnings("serial")
public class AnnotatedStaticInjection extends AbstractBehavior {

  private final StaticsInitializedReferenceSet referenceSet;

  public AnnotatedStaticInjection() {
    this(new StaticsInitializedReferenceSet());
  }

  public AnnotatedStaticInjection(final StaticsInitializedReferenceSet referenceSet) {
    this.referenceSet = referenceSet;
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
    final boolean requireConsumptionOfAllParameters = !AbstractBehavior.arePropertiesPresent(componentProps, Characteristics.ALLOW_UNUSED_PARAMETERS, false);

    ComponentAdapter<T> result = null;
    final boolean noStatic = removePropertiesIfPresent(componentProps, Characteristics.NO_STATIC_INJECTION);

    // NO_STATIC_INJECTION takes precedence
    if (removePropertiesIfPresent(componentProps, Characteristics.STATIC_INJECTION) && !noStatic) {
      result = monitor.changedBehavior(new StaticInjection<>(referenceSet,
          super.createComponentAdapter(monitor, lifecycle, componentProps, key, impl, constructorParams, fieldParams, methodParams)
          , useNames,
          requireConsumptionOfAllParameters,
          fieldParams, methodParams));
    }

    // noinspection SimplifiableIfStatement
    if (result == null) {
      // Static injection wasn't specified
      return super.createComponentAdapter(monitor, lifecycle, componentProps, key, impl, constructorParams, fieldParams, methodParams);
    }

    return result;
  }

  public static class StaticInjection<T> extends AbstractChangedBehavior<T> {
    @Nullable
    private transient StaticsInitializedReferenceSet referenceSet;
    private final boolean useNames;
    private final boolean consumeAllParameters;
    private final FieldParameters[] fieldParams;
    private final MethodParameters[] methodParams;
    private final List<StaticInjector<?>> wrappedInjectors;

    public StaticInjection(
        @Nullable final StaticsInitializedReferenceSet referenceSet,
        @NotNull final ComponentAdapter<T> delegate,
        final boolean useNames,
        final boolean consumeAllParameters,
        @NotNull final FieldParameters[] fieldParams,
        @NotNull final MethodParameters[] methodParams) {
      super(delegate);
      this.referenceSet = referenceSet;
      this.useNames = useNames;
      this.consumeAllParameters = consumeAllParameters;
      this.fieldParams = fieldParams;
      this.methodParams = methodParams;
      wrappedInjectors = createListOfStaticInjectors(getComponentImplementation());
    }

    private List<StaticInjector<?>> createListOfStaticInjectors(@NotNull final Class<?> componentImplementation) {
      final List<StaticInjector<?>> injectors = new ArrayList<>();
      Class<?> currentClass = componentImplementation;
      final Class<? extends Annotation> injectionAnnotation = AnnotatedMethodInjection.getInjectionAnnotation("javax.inject.Inject");

      while (!currentClass.equals(Object.class)) {
        // Method first because we're going to reverse the entire collection after building
        final StaticInjector<?> methodInjector = constructStaticMethodInjections(injectionAnnotation, currentClass);

        if (methodInjector != null) {
          injectors.add(methodInjector);
        }

        final StaticInjector<?> fieldInjector = constructStaticFieldInjections(injectionAnnotation, currentClass);

        if (fieldInjector != null) {
          injectors.add(fieldInjector);
        }

        currentClass = currentClass.getSuperclass();
      }

      Collections.reverse(injectors);
      return injectors;
    }

    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    private StaticInjector<?> constructStaticMethodInjections(
        final Class<? extends Annotation> injectionAnnotation,
        final Class<?> currentClass) {
      final List<Method> methodsToInject = new ArrayList<>(8);

      for (final Method eachMethod : currentClass.getDeclaredMethods()) {
        if (!Modifier.isStatic(eachMethod.getModifiers()) ||
            getReferenceSet().isMemberAlreadyInitialized(eachMethod)) {
          continue;
        }

        if (eachMethod.isAnnotationPresent(injectionAnnotation)) {
          methodsToInject.add(eachMethod);
        }
      }

      // noinspection SimplifiableIfStatement
      if (methodsToInject.isEmpty()) {
        return null;
      }

      return new SpecificMethodInjector(
          getComponentKey(),
          getComponentImplementation(),
          currentMonitor(),
          useNames,
          consumeAllParameters,
          methodParams,
          methodsToInject.toArray(new Method[0])
      );
    }

    @Nullable
    @SuppressWarnings({"rawtypes", "unchecked"})
    private StaticInjector<?> constructStaticFieldInjections(
        final Class<? extends Annotation> injectionAnnotation,
        final Class<?> currentClass) {
      final List<Field> fieldsToInject = new ArrayList<>();

      for (final Field eachField : currentClass.getDeclaredFields()) {
        if (!Modifier.isStatic(eachField.getModifiers())) {
          continue;
        }

        if (getReferenceSet().isMemberAlreadyInitialized(eachField)) {
          continue;
        }

        if (eachField.isAnnotationPresent(injectionAnnotation)) {
          fieldsToInject.add(eachField);
        }
      }

      // noinspection SimplifiableIfStatement
      if (fieldsToInject.isEmpty()) {
        return null;
      }

      return new SpecificFieldInjector(
          getComponentKey(),
          getComponentImplementation(),
          currentMonitor(),
          useNames,
          consumeAllParameters,
          fieldParams,
          fieldsToInject.toArray(new Field[0])
      );
    }

    @Override
    public T getComponentInstance(final PicoContainer container, final Type into) {
      if (getReferenceSet() != null) {

        // The individual static injectors decide if a static member
        // has already been injected or not
        for (final StaticInjector<?> staticInjectors : wrappedInjectors) {
          staticInjectors.injectStatics(container, into, getReferenceSet());
        }
      }

      return super.getComponentInstance(container, into);
    }

    @Override
    public String getDescriptor() {
      return "StaticAnnotationInjector";
    }

    /**
     * If we've been serialized, we'll have to recreate from scratch and re-inject static members.
     */
    private StaticsInitializedReferenceSet getReferenceSet() {
      if (referenceSet == null) {
        referenceSet = new StaticsInitializedReferenceSet();
      }

      return referenceSet;
    }
  }
}
