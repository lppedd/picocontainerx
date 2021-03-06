/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/
package com.picocontainer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

/**
 * Implementers are responsible for instantiating and injecting dependencies
 * into constructors, methods and fields.
 */
public interface Injector<T> extends ComponentAdapter<T> {
  /**
   * A preexisting component instance can be injected into after instantiation.
   *
   * @param container the container that can provide injectable dependencies
   * @param into
   * @param instance the instance to
   */
  Object decorateComponentInstance(
      @NotNull final PicoContainer container,
      @NotNull final Type into,
      @NotNull final T instance);

  /**
   * Does a partial decoration.
   * This is necessary since in composite injection, you need to inject
   * the superclass fields/methods first before injecting the subtype fields/methods.
   *
   * @param container
   * @param into
   * @param instance the instance we're decorating.
   * @param superclassPortion the portion of the object to decorate.
   */
  Object partiallyDecorateComponentInstance(
      @NotNull final PicoContainer container,
      @NotNull final Type into,
      @NotNull final T instance,
      @Nullable final Class<?> superclassPortion);
}
