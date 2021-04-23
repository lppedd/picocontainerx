/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer.injectors;

import com.picocontainer.Characteristics;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.LifecycleStrategy;
import com.picocontainer.NameBinding;
import com.picocontainer.Parameter;
import com.picocontainer.annotations.Bind;
import com.picocontainer.annotations.Inject;
import com.picocontainer.behaviors.AbstractBehavior;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.JSR330ComponentParameter;
import com.picocontainer.parameters.MethodParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static com.picocontainer.injectors.AnnotatedMethodInjection.AnnotatedMethodInjector.makeAnnotationNames;
import static com.picocontainer.injectors.AnnotatedMethodInjection.getInjectionAnnotation;

/**
 * A {@link com.picocontainer.InjectionType} for Guice-style annotated fields.
 * The factory creates {@link AnnotatedFieldInjector}.
 *
 * @author Paul Hammant
 */
@SuppressWarnings("serial")
public class AnnotatedFieldInjection extends AbstractInjectionType {
  private final Class<? extends Annotation>[] injectionAnnotations;

  @SafeVarargs
  public AnnotatedFieldInjection(final Class<? extends Annotation>... injectionAnnotations) {
    this.injectionAnnotations = injectionAnnotations;
  }

  public AnnotatedFieldInjection() {
    this(
        getInjectionAnnotation("javax.inject.Inject"),
        getInjectionAnnotation("com.picocontainer.annotations.Inject")
    );
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
        monitor.newInjector(
            new AnnotatedFieldInjector<>(
                key,
                impl,
                fieldParams,
                monitor,
                useNames,
                requireConsumptionOfAllParameters,
                injectionAnnotations
            )
        ),
        lifecycle
    );
  }

  /**
   * Injection happens after instantiation, and through fields marked as injection points via an Annotation.
   * The default annotation {@link Inject} can be overridden.
   */
  public static class AnnotatedFieldInjector<T> extends AbstractFieldInjector<T> {
    private final Class<? extends Annotation>[] injectionAnnotations;
    private String injectionAnnotationNames;

    @SafeVarargs
    public AnnotatedFieldInjector(
        final Object key,
        final Class<T> impl,
        final FieldParameters[] parameters,
        final ComponentMonitor monitor,
        final boolean useNames,
        final boolean requireConsumptionOfAllParameters,
        final Class<? extends Annotation>... injectionAnnotations) {
      super(key, impl, monitor, useNames, requireConsumptionOfAllParameters, parameters);
      this.injectionAnnotations = injectionAnnotations;
    }

    @Override
    protected void initializeInjectionMembersAndTypeLists() {
      injectionMembers = new ArrayList<>();
      Class<?> drillInto = getComponentImplementation();

      while (drillInto != Object.class) {
        final Field[] fields = getFields(drillInto);

        for (final Field field : fields) {
          if (Modifier.isStatic(field.getModifiers())) {
            continue;
          }

          if (isAnnotatedForInjection(field)) {
            injectionMembers.add(field);
          }
        }

        drillInto = drillInto.getSuperclass();
      }

      // Sort for injection
      injectionMembers.sort(new JSR330AccessibleObjectOrderComparator());

      final List<Annotation> bindingIds = new ArrayList<>();
      final List<Type> typeList = new ArrayList<>();

      for (final AccessibleObject eachMember : injectionMembers) {
        final Field field = (Field) eachMember;
        typeList.add(box(field.getGenericType()));
        bindingIds.add(getBinding(field));
      }

      injectionTypes = typeList.toArray(new Type[0]);
      bindings = bindingIds.toArray(new Annotation[0]);
    }

    /**
     * Sorry, can't figure out how else to test injection member order without
     * this function or some other ugly hack to get at the private data structure.
     * At least I made it read only?  :D  -MR
     */
    public List<AccessibleObject> getInjectionMembers() {
      return injectionMembers != null
          ? Collections.unmodifiableList(injectionMembers)
          : Collections.emptyList();
    }

    @Nullable
    public static Annotation getBinding(final Field field) {
      final Annotation[] annotations = field.getAnnotations();

      for (final Annotation annotation : annotations) {
        if (annotation.annotationType().isAnnotationPresent(Bind.class)) {
          return annotation;
        }
      }

      return null;
    }

    protected final boolean isAnnotatedForInjection(final Field field) {
      for (final Class<? extends Annotation> injectionAnnotation : injectionAnnotations) {
        if (field.isAnnotationPresent(injectionAnnotation)) {
          return true;
        }
      }
      return false;
    }

    private Field[] getFields(final Class<?> clazz) {
      // noinspection Convert2MethodRef
      return AccessController.doPrivileged((PrivilegedAction<Field[]>) () -> clazz.getDeclaredFields());
    }

    /**
     * Allows different swapping of types.
     */
    @Override
    protected Parameter constructDefaultComponentParameter() {
      return JSR330ComponentParameter.DEFAULT;
    }

    /**
     * Performs the actual injection.
     */
    @Nullable
    @Contract("_, _, _ -> null")
    @Override
    protected Object injectIntoMember(
        final AccessibleObject member,
        final Object componentInstance,
        final Object toInject) throws IllegalAccessException {
      final Field field = (Field) member;
      AnnotationInjectionUtils.setMemberAccessible(member);
      field.set(componentInstance, toInject);
      return null;
    }

    @Override
    protected Parameter[] interceptParametersToUse(
        final Parameter[] currentParameters,
        final AccessibleObject member) {
      return AnnotationInjectionUtils.interceptParametersToUse(currentParameters, member);
    }

    @Override
    public String getDescriptor() {
      if (injectionAnnotationNames == null) {
        injectionAnnotationNames = makeAnnotationNames(injectionAnnotations);
      }

      return "AnnotatedFieldInjector[" + injectionAnnotationNames + "]-";
    }

    @Override
    protected NameBinding makeParameterNameImpl(final AccessibleObject member) {
      // noinspection Convert2MethodRef
      return () -> ((Field) member).getName();
    }

    @Override
    protected Object memberInvocationReturn(
        final Object lastReturn,
        final AccessibleObject member,
        final Object instance) {
      return instance;
    }
  }
}
