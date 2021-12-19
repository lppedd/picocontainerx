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

/**
 * Interface responsible for changing monitoring strategy.
 * It may be implemented by {@link PicoContainer containers} and
 * single {@link ComponentAdapter component adapters}.
 * <p>
 * The choice of supporting the monitor strategy is left to the
 * implementers of the container and adapters.
 *
 * @author Paul Hammant
 * @author Joerg Schaible
 * @author Mauro Talevi
 */
public interface ComponentMonitorStrategy {
  /**
   * Changes the {@code ComponentMonitor} used.
   *
   * @param monitor the new {@code ComponentMonitor} to use
   *
   * @return the old {@code ComponentMonitor}
   */
  ComponentMonitor changeMonitor(@NotNull final ComponentMonitor monitor);

  /**
   * Returns the monitor currently used.
   *
   * @return The {@code ComponentMonitor} currently used
   */
  @NotNull
  ComponentMonitor currentMonitor();
}
