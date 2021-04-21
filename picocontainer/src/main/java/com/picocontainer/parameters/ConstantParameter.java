/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Idea by Rachel Davies, Original code by Jon Tirsen                        *
 *****************************************************************************/

package com.picocontainer.parameters;

import com.picocontainer.ComponentAdapter;
import com.picocontainer.NameBinding;
import com.picocontainer.PicoCompositionException;
import com.picocontainer.PicoContainer;
import com.picocontainer.PicoVisitor;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Should be used to pass in "constant" arguments to constructors.
 * This includes {@link String}s, {@link Integer}s or any other object
 * that is not registered in the container.
 *
 * @author Jon Tirs&eacute;n
 * @author Aslak Helles&oslash;y
 * @author J&ouml;rg Schaible
 * @author Thomas Heller
 */
@SuppressWarnings("serial")
public class ConstantParameter extends AbstractParameter implements Serializable {
  private final Object value;

  public ConstantParameter(final Object value) {
    this.value = value;
  }

  @Override
  public Resolver resolve(
      final PicoContainer container,
      final ComponentAdapter<?> forAdapter,
      final ComponentAdapter<?> injecteeAdapter,
      final Type expectedType,
      final NameBinding expectedNameBinding,
      final boolean useNames,
      final Annotation binding) {
    if (expectedType instanceof Class) {
      return new ValueResolver(isAssignable(expectedType), value, null);
    }

    if (expectedType instanceof ParameterizedType) {
      return new ValueResolver(isAssignable(((ParameterizedType) expectedType).getRawType()), value, null);
    }

    return new ValueResolver(true, value, null);
  }

  @Override
  public void verify(
      final PicoContainer container,
      final ComponentAdapter<?> adapter,
      final Type expectedType,
      final NameBinding expectedNameBinding,
      final boolean useNames,
      final Annotation binding) {
    if (!isAssignable(expectedType)) {
      throw new PicoCompositionException(
          expectedType + " is not assignable from " + (value != null ? value.getClass().getName() : "null")
      );
    }
  }

  protected boolean isAssignable(final Type expectedType) {
    if (expectedType instanceof Class) {
      final Class<?> expectedClass = (Class<?>) expectedType;
      return checkPrimitive(expectedClass) || expectedClass.isInstance(value);
    }

    return false;
  }

  @Override
  public void accept(final PicoVisitor visitor) {
    visitor.visitParameter(this);
  }

  private boolean checkPrimitive(final Class<?> expectedType) {
    try {
      if (expectedType.isPrimitive()) {
        final Field field = value.getClass().getField("TYPE");
        final Class<?> type = (Class<?>) field.get(value);
        return expectedType.isAssignableFrom(type);
      }
    } catch (final NoSuchFieldException | IllegalAccessException ignored) {
      //
    }

    return false;
  }

  public Object getValue() {
    return value;
  }
}
