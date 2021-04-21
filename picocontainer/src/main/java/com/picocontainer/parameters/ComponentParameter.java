/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer.parameters;

import com.googlecode.jtype.Generic;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.NameBinding;
import com.picocontainer.Parameter;
import com.picocontainer.PicoContainer;
import com.picocontainer.PicoVisitor;
import com.picocontainer.injectors.AbstractInjector.UnsatisfiableDependenciesException;
import org.jetbrains.annotations.Nullable;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Should be used to pass in a particular component as argument to a
 * different component's constructor.
 * This is particularly useful in cases where several components of the same type have been registered,
 * but with a different key.
 * Passing a {@code ComponentParameter} as a parameter when registering a component
 * will give PicoContainer a hint about what other component to use in the constructor.
 * Collecting parameter types are supported for {@link java.lang.reflect.Array}, {@link java.util.Collection}
 * and {@link java.util.Map}.
 *
 * @author Jon Tirs&eacute;n
 * @author Aslak Helles&oslash;y
 * @author J&ouml;rg Schaible
 * @author Thomas Heller
 */
@SuppressWarnings("serial")
public class ComponentParameter extends BasicComponentParameter {
  /**
   * {@code DEFAULT} is an instance of ComponentParameter using the default constructor.
   * <p>
   * TODO: clean up the circular reference
   */
  public static final ComponentParameter DEFAULT = new ComponentParameter();

  /**
   * Use {@code ARRAY} as {@link Parameter}for an Array that must have elements.
   */
  public static final ComponentParameter ARRAY = new ComponentParameter(false);

  /**
   * Use {@code ARRAY_ALLOW_EMPTY} as {@link Parameter}for an Array that may have no elements.
   */
  public static final ComponentParameter ARRAY_ALLOW_EMPTY = new ComponentParameter(true);

  private final Parameter collectionParameter;

  /**
   * Expect a parameter matching a component of a specific key.
   *
   * @param key the key of the desired addComponent
   */
  public ComponentParameter(final Object key) {
    this(key, null);
  }

  /**
   * Expect any scalar parameter of the appropriate type or an {@link java.lang.reflect.Array}.
   */
  public ComponentParameter() {
    this(false);
  }

  /**
   * Expect any scalar parameter of the appropriate type or an {@link java.lang.reflect.Array}.
   * Resolve the parameter even if no compnoent is of the array's component type.
   *
   * @param emptyCollection {@code true} allows an Array to be empty
   */
  public ComponentParameter(final boolean emptyCollection) {
    this(null, emptyCollection ? CollectionComponentParameter.ARRAY_ALLOW_EMPTY : CollectionComponentParameter.ARRAY);
  }

  /**
   * Expect any scalar parameter of the appropriate type or the collecting type
   * {@link java.lang.reflect.Array},{@link java.util.Collection}or {@link java.util.Map}.
   * The components in the collection will be of the specified type.
   *
   * @param componentValueType the component's type (ignored for an Array)
   * @param emptyCollection {@code true} allows the collection to be empty
   */
  public ComponentParameter(final Generic<?> componentValueType, final boolean emptyCollection) {
    this(null, new CollectionComponentParameter(componentValueType, emptyCollection));
  }

  /**
   * Expect any scalar parameter of the appropriate type or the collecting type
   * {@link java.lang.reflect.Array},{@link java.util.Collection}or {@link java.util.Map}.
   * The components in the collection will be of the specified type and their adapter's key
   * must have a particular type.
   *
   * @param keyType the component adapter's key type
   * @param componentValueType the component's type (ignored for an Array)
   * @param emptyCollection {@code true} allows the collection to be empty
   */
  public ComponentParameter(final Class<?> keyType, final Generic<?> componentValueType, final boolean emptyCollection) {
    this(null, new CollectionComponentParameter(keyType, componentValueType, emptyCollection));
  }

  /**
   * Use this constructor if you are using CollectionComponentParameter
   */
  public ComponentParameter(final Parameter mapDefiningParameter) {
    this(null, mapDefiningParameter);
  }

  private ComponentParameter(final Object key, final Parameter collectionParameter) {
    super(key);
    this.collectionParameter = collectionParameter;
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
    return new Resolver() {
      final Resolver resolver =
          ComponentParameter.super.resolve(
              container,
              forAdapter,
              injecteeAdapter,
              expectedType,
              expectedNameBinding,
              useNames,
              binding
          );

      @Override
      public boolean isResolved() {
        final boolean superResolved = resolver.isResolved();

        if (!superResolved) {
          return collectionParameter != null &&
              collectionParameter.resolve(
                  container,
                  forAdapter,
                  null,
                  expectedType,
                  expectedNameBinding,
                  useNames,
                  binding
              ).isResolved();
        }

        return true;
      }

      @Override
      @Nullable
      public Object resolveInstance(final Type into) {
        Object result = null;

        if (expectedType instanceof Class
            || expectedType instanceof ParameterizedType
            && ((ParameterizedType) expectedType).getRawType() == Provider.class) {
          result = ComponentParameter.super.resolve(
              container,
              forAdapter,
              injecteeAdapter,
              expectedType,
              expectedNameBinding,
              useNames,
              binding
          ).resolveInstance(into);
        } else if (expectedType instanceof ParameterizedType) {
          result = ComponentParameter.super.resolve(
              container,
              forAdapter,
              injecteeAdapter,
              ((ParameterizedType) expectedType).getRawType(),
              expectedNameBinding,
              useNames,
              binding
          ).resolveInstance(into);
        }

        if (result == null && collectionParameter != null) {
          return collectionParameter.resolve(container, forAdapter, injecteeAdapter, expectedType, expectedNameBinding,
              useNames, binding).resolveInstance(into);
        }

        return result;
      }

      @Override
      public ComponentAdapter<?> getComponentAdapter() {
        return resolver.getComponentAdapter();
      }
    };
  }

  @Override
  public void verify(
      final PicoContainer container,
      final ComponentAdapter<?> adapter,
      final Type expectedType,
      final NameBinding expectedNameBinding,
      final boolean useNames,
      final Annotation binding) {
    try {
      super.verify(container, adapter, expectedType, expectedNameBinding, useNames, binding);
    } catch (final UnsatisfiableDependenciesException e) {
      if (collectionParameter != null) {
        collectionParameter.verify(container, adapter, expectedType, expectedNameBinding, useNames, binding);
        return;
      }

      throw e;
    }
  }

  /**
   * Accept the visitor for the current {@link Parameter}.
   * If internally a {@link CollectionComponentParameter} is used, it is visited too.
   */
  @Override
  public void accept(final PicoVisitor visitor) {
    super.accept(visitor);

    if (collectionParameter != null) {
      collectionParameter.accept(visitor);
    }
  }
}
