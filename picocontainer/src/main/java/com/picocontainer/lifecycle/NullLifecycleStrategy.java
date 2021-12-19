/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer.lifecycle;

import com.picocontainer.ComponentAdapter;
import com.picocontainer.LifecycleStrategy;

import java.io.Serializable;

/**
 * {@link LifecycleStrategy} that does nothing.
 */
@SuppressWarnings("serial")
public class NullLifecycleStrategy implements LifecycleStrategy, Serializable {
  @Override
  public void start(final Object component) {}

  @Override
  public void stop(final Object component) {}

  @Override
  public void dispose(final Object component) {}

  @Override
  public boolean hasLifecycle(final Class<?> type) {
    return false;
  }

  @Override
  public boolean isLazy(final ComponentAdapter<?> adapter) {
    return false;
  }
}
