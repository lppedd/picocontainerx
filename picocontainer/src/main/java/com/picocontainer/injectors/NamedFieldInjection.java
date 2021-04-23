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

import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.InjectionType;
import com.picocontainer.LifecycleStrategy;
import com.picocontainer.NameBinding;
import com.picocontainer.annotations.Bind;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static com.picocontainer.Characteristics.immutable;

/**
 * <p>
 * An {@link InjectionType} for named fields.
 * </p>
 * <p>
 * Use like so:
 * </p>
 * <pre>
 * pico.as(injectionFieldNames("field1", "field2")).addComponent(...)
 * </pre>
 * The factory creates {@link NamedFieldInjector}.
 *
 * @author Paul Hammant
 */
@SuppressWarnings("serial")
public class NamedFieldInjection extends AbstractInjectionType {
  private static final String INJECTION_FIELD_NAMES = "injectionFieldNames";
  private final boolean requireConsumptionOfallParameters;

  public NamedFieldInjection() {
    requireConsumptionOfallParameters = true;
  }

  public NamedFieldInjection(final boolean requireConsumptionOfallParameters) {
    this.requireConsumptionOfallParameters = requireConsumptionOfallParameters;
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
    String fieldNames = (String) componentProps.remove(INJECTION_FIELD_NAMES);

    if (fieldNames == null) {
      fieldNames = "";
    }

    return wrapLifeCycle(
        monitor.newInjector(
            new NamedFieldInjector<>(
                key,
                impl,
                monitor,
                fieldNames,
                requireConsumptionOfallParameters,
                fieldParams
            )
        ),
        lifecycle
    );
  }

  public static Properties injectionFieldNames(final String... fieldNames) {
    final StringBuilder sb = new StringBuilder();

    for (final String fieldName : fieldNames) {
      sb.append(" ").append(fieldName);
    }

    final Properties retVal = new Properties();
    return immutable(INJECTION_FIELD_NAMES, sb.toString().trim());
  }

  /**
   * Injection happens after instantiation, and fields are marked as
   * injection points via a named field.
   */
  public static class NamedFieldInjector<T> extends AbstractFieldInjector<T> {
    private final List<String> fieldNames;

    public NamedFieldInjector(
        final Object key,
        final Class<T> impl,
        final ComponentMonitor monitor,
        final String fieldNames,
        final boolean requireConsumptionOfAllParameters,
        final FieldParameters... parameters) {
      super(key, impl, monitor, true, requireConsumptionOfAllParameters, parameters);
      this.fieldNames = Arrays.asList(fieldNames.trim().split(" "));
    }

    @Override
    protected void initializeInjectionMembersAndTypeLists() {
      injectionMembers = new ArrayList<>();
      final List<Annotation> bindingIds = new ArrayList<>();
      final List<Type> typeList = new ArrayList<>();
      final Field[] fields = getFields();

      for (final Field field : fields) {
        if (isNamedForInjection(field)) {
          injectionMembers.add(field);
          typeList.add(box(field.getType()));
          bindingIds.add(getBinding(field));
        }
      }

      injectionTypes = typeList.toArray(new Type[0]);
      bindings = bindingIds.toArray(new Annotation[0]);
    }

    @Nullable
    private Annotation getBinding(final Field field) {
      final Annotation[] annotations = field.getAnnotations();

      for (final Annotation annotation : annotations) {
        if (annotation.annotationType().isAnnotationPresent(Bind.class)) {
          return annotation;
        }
      }

      return null;
    }

    protected boolean isNamedForInjection(final Field field) {
      return fieldNames.contains(field.getName());
    }

    private Field[] getFields() {
      return AccessController.doPrivileged((PrivilegedAction<Field[]>) () ->
          getComponentImplementation().getDeclaredFields()
      );
    }

    @Nullable
    @Contract("_, _, _ -> null")
    @Override
    protected Object injectIntoMember(
        final AccessibleObject member,
        final Object componentInstance,
        final Object toInject) throws IllegalAccessException {
      final Field field = (Field) member;
      field.setAccessible(true);
      field.set(componentInstance, toInject);
      return null;
    }

    @Override
    public String getDescriptor() {
      return "NamedFieldInjector-";
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

    List<String> getInjectionFieldNames() {
      return Collections.unmodifiableList(fieldNames);
    }
  }
}
