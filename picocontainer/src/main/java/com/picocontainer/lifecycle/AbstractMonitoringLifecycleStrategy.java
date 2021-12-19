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
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

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
   * The monitor that receives lifecycle states.
   */
  private ComponentMonitor monitor;

  public AbstractMonitoringLifecycleStrategy(@NotNull final ComponentMonitor monitor) {
    changeMonitor(monitor);
  }

  /**
   * Swaps the current monitor with a replacement.
   */
  @Override
  public ComponentMonitor changeMonitor(@NotNull final ComponentMonitor newMonitor) {
    final ComponentMonitor oldValue = monitor;
    monitor = requireNonNull(newMonitor, "The monitor cannot be null");
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
