/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *****************************************************************************/
package com.picocontainer;

/**
 * An interface which specifies the lifecycle strategy on the component instance.
 * Lifecycle strategies are used by component adapters to delegate the lifecycle
 * operations on the component instances.
 *
 * @author Paul Hammant
 * @author Peter Royal
 * @author J&ouml;rg Schaible
 * @author Mauro Talevi
 * @see Startable
 * @see Disposable
 */
public interface LifecycleStrategy {
  /**
   * Invoke the "start" method on the component instance if this is startable.
   * It is up to the implementation of the strategy what "start" and "startable" means.
   *
   * @param component the instance of the component to start
   */
  void start(final Object component);

  /**
   * Invoke the "stop" method on the component instance if this is stoppable.
   * It is up to the implementation of the strategy what "stop" and "stoppable" means.
   *
   * @param component the instance of the component to stop
   */
  void stop(final Object component);

  /**
   * Invoke the "dispose" method on the component instance if this is disposable.
   * It is up to the implementation of the strategy what "dispose" and "disposable" means.
   *
   * @param component the instance of the component to dispose
   */
  void dispose(final Object component);

  /**
   * Test if a component instance has a lifecycle.
   *
   * @param type the component's type
   * @return {@code true} if the component has a lifecycle
   */
  boolean hasLifecycle(final Class<?> type);

  /**
   * Is a component eager (it will start when {@code start()} or equivalent is called),
   * or lazy (it will only start on the first {@code getComponent()})?
   * The default is the first of those two.
   *
   * @param adapter
   * @return {@code true} if lazy, {@code false} if eager
   */
  boolean isLazy(final ComponentAdapter<?> adapter);
}
