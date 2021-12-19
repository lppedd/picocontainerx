/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer.adapters;

import com.picocontainer.ChangedBehavior;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentLifecycle;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.LifecycleStrategy;
import com.picocontainer.PicoContainer;
import com.picocontainer.lifecycle.NullLifecycleStrategy;
import com.picocontainer.monitors.NullComponentMonitor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

/**
 * {@link ComponentAdapter} which wraps a component instance.
 * <p>
 * This component adapter supports both a {@link ChangedBehavior}
 * and a {@link LifecycleStrategy} to control the lifecycle of the component.
 * The lifecycle manager methods simply delegate to the lifecycle strategy methods
 * on the component instance.
 *
 * @author Aslak Helles&oslash;y
 * @author Paul Hammant
 * @author Mauro Talevi
 */
@SuppressWarnings("serial")
public final class InstanceAdapter<T> extends AbstractAdapter<T> implements ComponentLifecycle<T>, LifecycleStrategy {
  /**
   * The actual instance of the component.
   */
  @NotNull
  private final T componentInstance;

  /**
   * Lifecycle Strategy for the component adapter.
   */
  @NotNull
  private final LifecycleStrategy lifecycle;
  private boolean started;

  public InstanceAdapter(
      @NotNull final Object key,
      @NotNull final T componentInstance,
      @NotNull final LifecycleStrategy lifecycle,
      @NotNull final ComponentMonitor monitor) {
    super(key, (Class<T>) componentInstance.getClass(), monitor);
    this.componentInstance = componentInstance;
    this.lifecycle = lifecycle;
  }

  public InstanceAdapter(
      @NotNull final Object key,
      @NotNull final T componentInstance) {
    this(key, componentInstance, new NullLifecycleStrategy(), new NullComponentMonitor());
  }

  public InstanceAdapter(
      @NotNull final Object key,
      @NotNull final T componentInstance,
      @NotNull final LifecycleStrategy lifecycle) {
    this(key, componentInstance, lifecycle, new NullComponentMonitor());
  }

  public InstanceAdapter(
      @NotNull final Object key,
      @NotNull final T componentInstance,
      @NotNull final ComponentMonitor monitor) {
    this(key, componentInstance, new NullLifecycleStrategy(), monitor);
  }

  @Override
  public T getComponentInstance(final PicoContainer container, final Type into) {
    return componentInstance;
  }

  @Override
  public void verify(final PicoContainer container) {}

  @Override
  public String getDescriptor() {
    return "Instance-";
  }

  @Override
  public void start(final PicoContainer container) {
    start(componentInstance);
  }

  @Override
  public void stop(final PicoContainer container) {
    stop(componentInstance);
  }

  @Override
  public void dispose(final PicoContainer container) {
    dispose(componentInstance);
  }

  @Override
  public boolean componentHasLifecycle() {
    return hasLifecycle(componentInstance.getClass());
  }

  @Override
  public boolean isStarted() {
    return started;
  }

  @Override
  public void start(final Object component) {
    lifecycle.start(componentInstance);
    started = true;
  }

  @Override
  public void stop(final Object component) {
    lifecycle.stop(componentInstance);
    started = false;
  }

  @Override
  public void dispose(final Object component) {
    lifecycle.dispose(componentInstance);
  }

  @Override
  public boolean hasLifecycle(final Class<?> type) {
    return lifecycle.hasLifecycle(type);
  }

  @Override
  public boolean isLazy(final ComponentAdapter<?> adapter) {
    return lifecycle.isLazy(adapter);
  }
}
