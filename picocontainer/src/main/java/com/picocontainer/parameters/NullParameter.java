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
import com.picocontainer.PicoCompositionException;
import com.picocontainer.PicoContainer;
import com.picocontainer.PicoVisitor;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Once in a great while, you actually want to pass in 'null' as an argument.
 * Instead of bypassing the type checking mechanisms available in {@link ConstantParameter},
 * we provide a special type geared to marking nulls.
 *
 * @author Michael Rimov
 */
@SuppressWarnings("serial")
public class NullParameter extends AbstractParameter implements Serializable {
  public static final NullParameter INSTANCE = new NullParameter();

  /**
   * Only one instance of Null parameter needed.
   */
  private NullParameter() {}

  /**
   * @see com.picocontainer.Parameter#accept(PicoVisitor)
   */
  @Override
  public void accept(final PicoVisitor visitor) {
    visitor.visitParameter(this);
  }

  /**
   * @see com.picocontainer.Parameter#resolve(PicoContainer, ComponentAdapter, ComponentAdapter, Type, NameBinding,
   * boolean, Annotation)
   */
  @Override
  public Resolver resolve(
      final PicoContainer container,
      final ComponentAdapter<?> forAdapter,
      final ComponentAdapter<?> injecteeAdapter,
      final Type expectedType,
      final NameBinding expectedNameBinding,
      final boolean useNames,
      final Annotation binding) {
    return new ValueResolver(isAssignable(expectedType), null, null);
  }

  /**
   * @see com.picocontainer.Parameter#verify(PicoContainer, ComponentAdapter, Type,
   * NameBinding, boolean, Annotation)
   */
  @Override
  public void verify(
      final PicoContainer container,
      final ComponentAdapter<?> adapter,
      final Type expectedType,
      final NameBinding expectedNameBinding,
      final boolean useNames,
      final Annotation binding) {
    if (!isAssignable(expectedType)) {
      throw new PicoCompositionException(expectedType + " cannot be assigned a null value");
    }
  }

  /**
   * Nulls cannot be assigned to primitives.
   *
   * @param expectedType
   */
  protected boolean isAssignable(final Type expectedType) {
    if (expectedType instanceof Class<?>) {
      final Class<?> expectedClass = (Class<?>) expectedType;
      return !expectedClass.isPrimitive();
    }

    return true;
  }

  @Override
  public String getTargetName() {
    return null;
  }
}
