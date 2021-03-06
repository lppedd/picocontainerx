/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/
package com.picocontainer.references;

import com.picocontainer.ObjectReference;

import java.util.Map;

/**
 * Gets and sets references on a map stored in {@link ThreadLocal}.
 *
 * @author Paul Hammant
 */
public class ThreadLocalMapObjectReference<T> implements ObjectReference<T> {
  private final ThreadLocal<? extends Map<Object, T>> threadLocal;
  private final Object key;

  public ThreadLocalMapObjectReference(
      final ThreadLocal<? extends Map<Object, T>> threadLocal,
      final Object key) {
    this.threadLocal = threadLocal;
    this.key = key;
  }

  @Override
  public T get() {
    return threadLocal.get().get(key);
  }

  @Override
  public void set(final T item) {
    threadLocal.get().put(key, item);
  }
}
