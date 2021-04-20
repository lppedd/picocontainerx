/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the license.txt file.                                                    *
 *                                                                           *
 * Idea by Rachel Davies, Original code by Aslak Hellesoy and Paul Hammant   *
 *****************************************************************************/

package com.picocontainer;

/**
 * <p>
 * An interface which is implemented by components that can be started and stopped.
 * The {@link Startable#start()} must be called at the begin of the component lifecycle,
 * and it can be called again only after a call to {@link Startable#stop()}.
 * <p>
 * The {@link Startable#stop()} method must be called at the end of the component lifecycle,
 * and it can further be called after every {@link Startable#start()}.
 * <p>
 * If a component implements the {@link Disposable} interface as well,
 * {@link Startable#stop()} should be called before {@link Disposable#dispose()}.
 * </p>
 * <p>For more advanced and pluggable lifecycle support, see the functionality offered by
 * {@code picocontainer-gems} subproject.
 * </p>
 *
 * @author Paul Hammant
 * @author Aslak Helles&oslash;y
 * @see Disposable the Disposable interface if you need to dispose() semantics.
 */
public interface Startable {
  /**
   * Start this component.
   * Called initially at the beginning of the lifecycle, it can be called again after a stop.
   */
  void start();

  /**
   * Stop this component.
   * Called near the end of the lifecycle, it can be called again after a further start.
   * Implement {@link Disposable} if you need a single call at the definitive end of the lifecycle.
   */
  void stop();
}
