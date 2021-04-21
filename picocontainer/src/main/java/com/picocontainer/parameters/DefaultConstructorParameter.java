/*******************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.
 * ---------------------------------------------------------------------------
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 ******************************************************************************/
package com.picocontainer.parameters;

import com.picocontainer.ComponentAdapter;
import com.picocontainer.NameBinding;
import com.picocontainer.PicoContainer;
import com.picocontainer.PicoVisitor;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

/**
 * Part of the replacement construct for Parameter.ZERO
 *
 * @since PicoContainer 2.8
 */
@SuppressWarnings("serial")
public final class DefaultConstructorParameter extends AbstractParameter implements Serializable {
  public static final DefaultConstructorParameter INSTANCE = new DefaultConstructorParameter();

  @Override
  public void accept(final PicoVisitor visitor) {
    visitor.visitParameter(this);
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
    return new NotResolved();
  }

  @Override
  public void verify(
      final PicoContainer container,
      final ComponentAdapter<?> adapter,
      final Type expectedType,
      final NameBinding expectedNameBinding,
      final boolean useNames,
      final Annotation binding) {
    if (!(expectedType instanceof Class)) {
      throw new ClassCastException("Unable to use except for class types.  Offending type: " + expectedType);
    }

    final Class<?> type = (Class<?>) expectedType;

    try {
      final Constructor<?> constructor = type.getConstructor();
    } catch (final NoSuchMethodException e) {
      throw new IllegalArgumentException("No default constructor for type " + expectedType, e);
    }
  }

  @Override
  public String toString() {
    return "Force Default Constructor Parameter";
  }

  /**
   * Returns {@code true} if the object is a DEFAULT_CONSTRUCTOR object.
   */
  @Override
  public boolean equals(final Object other) {
    if (other == null) {
      return false;
    }

    return other.getClass().getName().equals(getClass().getName());
  }
}
