/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *****************************************************************************/
package com.picocontainer.lifecycle;

import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.ComponentMonitorStrategy;
import com.picocontainer.LifecycleStrategy;
import org.jetbrains.annotations.Contract;

import java.io.Serializable;

/**
 * Abstract base class for lifecycle strategy implementations supporting a {@link ComponentMonitor}.
 *
 * @author J&ouml;rg Schaible
 */
@SuppressWarnings("serial")
public abstract class AbstractMonitoringLifecycleStrategy
    implements LifecycleStrategy,
               ComponentMonitorStrategy,
               Serializable {
  /**
   * Component monitor that receives lifecycle state.
   */
  private ComponentMonitor monitor;

  /**
   * @param monitor the monitor to use
   * @throws NullPointerException if the monitor is {@code null}
   */
  public AbstractMonitoringLifecycleStrategy(final ComponentMonitor monitor) {
    changeMonitor(monitor);
  }

  /**
   * Swaps the current monitor with a replacement.
   *
   * @param newMonitor the new monitor
   * @throws NullPointerException if the passed in monitor is null
   */
  @Contract("null -> fail")
  @Override
  public ComponentMonitor changeMonitor(final ComponentMonitor newMonitor) {
    if (newMonitor == null) {
      throw new NullPointerException("Monitor is null");
    }

    final ComponentMonitor oldValue = monitor;
    monitor = newMonitor;

    return oldValue;
  }

  @Override
  public ComponentMonitor currentMonitor() {
    return monitor;
  }

  @Override
  public boolean isLazy(final ComponentAdapter<?> adapter) {
    return false;
  }
}
