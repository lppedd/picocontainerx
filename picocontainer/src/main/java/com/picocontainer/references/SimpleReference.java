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

import java.io.Serializable;

/**
 * Simple instance implementation of {@link ObjectReference}.
 *
 * @author Aslak Helles&oslash;y
 * @author Konstantin Pribluda
 */
@SuppressWarnings("serial")
public class SimpleReference<T> implements ObjectReference<T>, Serializable {
  private T instance;

  @Override
  public T get() {
    return instance;
  }

  @Override
  public void set(final T instance) {
    this.instance = instance;
  }
}
