/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *****************************************************************************/
package com.picocontainer.lifecycle;

import com.picocontainer.PicoCompositionException;

import java.io.Serializable;

/**
 * Bean-like implementation of {@link LifecycleState}.
 *
 * @author Paul Hammant
 * @author Michael Rimov
 */
@SuppressWarnings("serial")
public class DefaultLifecycleState implements LifecycleState, Serializable {
  /**
   * Default state of a container once it has been built.
   */
  private static final String CONSTRUCTED = "CONSTRUCTED";

  /**
   * 'Start' Lifecycle has been called.
   */
  private static final String STARTED = "STARTED";

  /**
   * 'Stop' lifecycle has been called.
   */
  private static final String STOPPED = "STOPPED";

  /**
   * 'Dispose' lifecycle has been called.
   */
  private static final String DISPOSED = "DISPOSED";

  /**
   * Initial state.
   */
  private String state = CONSTRUCTED;

  @Override
  public void removingComponent() {
    if (isStarted()) {
      throw new PicoCompositionException("Cannot remove components after the container has started");
    }

    if (isDisposed()) {
      throw new PicoCompositionException("Cannot remove components after the container has been disposed");
    }
  }

  @Override
  public void starting(final String containerName) {
    if (isConstructed() || isStopped()) {
      state = STARTED;
      return;
    }

    throw new IllegalStateException(
        "Cannot start container '" + containerName + "'.  Current container state was: " + state
    );
  }

  @Override
  public void stopping(final String containerName) {
    if (!isStarted()) {
      throw new IllegalStateException(
          "Cannot stop container '" + containerName + "'.  Current container state was: " + state
      );
    }
  }

  @Override
  public void stopped() {
    state = STOPPED;
  }

  @Override
  public boolean isStarted() {
    return STARTED.equals(state);
  }

  @Override
  public void disposing(final String containerName) {
    if (!(isStopped() || isConstructed())) {
      throw new IllegalStateException(
          "Cannot dispose container '" + containerName + "'.  Current lifecycle state is: " + state
      );
    }
  }

  @Override
  public void disposed() {
    state = DISPOSED;
  }

  @Override
  public boolean isDisposed() {
    return DISPOSED.equals(state);
  }

  @Override
  public boolean isStopped() {
    return STOPPED.equals(state);
  }

  /**
   * Returns {@code true} if no other state has been triggered so far.
   */
  public boolean isConstructed() {
    return CONSTRUCTED.equals(state);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + ".state=" + state;
  }
}
