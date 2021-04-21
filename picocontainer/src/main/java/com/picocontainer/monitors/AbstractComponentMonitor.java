/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by Mauro Talevi                                             *
 *****************************************************************************/

package com.picocontainer.monitors;

import com.picocontainer.ChangedBehavior;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.ComponentMonitorStrategy;
import com.picocontainer.Injector;
import com.picocontainer.MutablePicoContainer;
import com.picocontainer.PicoContainer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * <p>
 * A {@link ComponentMonitor} which delegates to another monitor.
 * It provides a {@link NullComponentMonitor} as default,
 * but does not allow using {@code null} for the delegate.
 * </p>
 * <p>
 * It also supports a {@link ComponentMonitorStrategy} that allows to change the delegate.
 * </p>
 *
 * @author Mauro Talevi
 */
@SuppressWarnings("serial")
public class AbstractComponentMonitor implements ComponentMonitor, ComponentMonitorStrategy, Serializable {
  /**
   * Delegate monitor to allow for component monitor chaining.
   */
  private ComponentMonitor delegate;

  /**
   * @param delegate the {@link ComponentMonitor} to which this monitor delegates
   */
  public AbstractComponentMonitor(final ComponentMonitor delegate) {
    checkMonitor(delegate);
    this.delegate = delegate;
  }

  /**
   * Uses {@link NullComponentMonitor} as delegate.
   */
  public AbstractComponentMonitor() {
    this(new NullComponentMonitor());
  }

  @Override
  public <T> Constructor<T> instantiating(
      final PicoContainer container,
      final ComponentAdapter<T> componentAdapter,
      final Constructor<T> constructor) {
    return delegate.instantiating(container, componentAdapter, constructor);
  }

  @Override
  public <T> void instantiated(
      final PicoContainer container,
      final ComponentAdapter<T> componentAdapter,
      final Constructor<T> constructor,
      final Object instantiated,
      final Object[] injected,
      final long duration) {
    delegate.instantiated(container, componentAdapter, constructor, instantiated, injected, duration);
  }

  @Override
  public <T> void instantiationFailed(
      final PicoContainer container,
      final ComponentAdapter<T> componentAdapter,
      final Constructor<T> constructor,
      final Exception e) {
    delegate.instantiationFailed(container, componentAdapter, constructor, e);
  }

  @Override
  public Object invoking(
      final PicoContainer container,
      final ComponentAdapter<?> componentAdapter,
      final Member member,
      final Object instance,
      final Object... args) {
    return delegate.invoking(container, componentAdapter, member, instance, args);
  }

  @Override
  public void invoked(
      final PicoContainer container,
      final ComponentAdapter<?> componentAdapter,
      final Member member,
      final Object instance,
      final long duration,
      final Object retVal,
      final Object[] args) {
    delegate.invoked(container, componentAdapter, member, instance, duration, retVal, args);
  }

  @Override
  public void invocationFailed(final Member member, final Object instance, final Exception e) {
    delegate.invocationFailed(member, instance, e);
  }

  @Override
  public void lifecycleInvocationFailed(
      final MutablePicoContainer container,
      final ComponentAdapter<?> componentAdapter,
      final Method method,
      final Object instance,
      final RuntimeException cause) {
    delegate.lifecycleInvocationFailed(container, componentAdapter, method, instance, cause);
  }

  @Override
  public Object noComponentFound(final MutablePicoContainer container, final Object key) {
    return delegate.noComponentFound(container, key);
  }

  @Override
  public <T> Injector<T> newInjector(final Injector<T> injector) {
    return injector;
  }

  @Override
  public <T> ChangedBehavior<T> changedBehavior(final ChangedBehavior<T> changedBehavior) {
    return changedBehavior;
  }

  /**
   * If the delegate supports a {@link ComponentMonitorStrategy}, this is used to changed
   * the monitor while keeping the same delegate. Else the delegate is replaced by the new monitor.
   */
  @Override
  public ComponentMonitor changeMonitor(final ComponentMonitor monitor) {
    checkMonitor(monitor);

    if (delegate instanceof ComponentMonitorStrategy) {
      return ((ComponentMonitorStrategy) delegate).changeMonitor(monitor);
    }

    final ComponentMonitor result = delegate;
    delegate = monitor;
    return result;
  }

  @Override
  public ComponentMonitor currentMonitor() {
    // noinspection SimplifiableIfStatement
    if (delegate instanceof ComponentMonitorStrategy) {
      return ((ComponentMonitorStrategy) delegate).currentMonitor();
    }

    return delegate;
  }

  @Contract("null -> fail")
  private void checkMonitor(@Nullable final ComponentMonitor monitor) {
    if (monitor == null) {
      throw new NullPointerException("monitor");
    }
  }
}
