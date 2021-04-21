/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer.monitors;

import com.picocontainer.ChangedBehavior;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.Injector;
import com.picocontainer.MutablePicoContainer;
import com.picocontainer.PicoContainer;
import com.picocontainer.PicoException;
import com.picocontainer.PicoLifecycleException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A {@link ComponentMonitor} which collects lifecycle failures
 * and rethrows them on demand after.
 *
 * @author Paul Hammant
 * @author Mauro Talevi
 */
@SuppressWarnings("serial")
public final class LifecycleComponentMonitor implements ComponentMonitor {
  /**
   * Delegate for chained component monitors.
   */
  private final ComponentMonitor delegate;
  private final List<RuntimeException> lifecycleFailures = new ArrayList<>();

  public LifecycleComponentMonitor(final ComponentMonitor delegate) {
    this.delegate = delegate;
  }

  public LifecycleComponentMonitor() {
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
      final Object[] parameters,
      final long duration) {
    delegate.instantiated(container, componentAdapter, constructor, instantiated, parameters, duration);
  }

  @Override
  public <T> void instantiationFailed(
      final PicoContainer container,
      final ComponentAdapter<T> componentAdapter,
      final Constructor<T> constructor,
      final Exception cause) {
    delegate.instantiationFailed(container, componentAdapter, constructor, cause);
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
  public void invocationFailed(final Member member, final Object instance, final Exception cause) {
    delegate.invocationFailed(member, instance, cause);
  }

  @Override
  public void lifecycleInvocationFailed(
      final MutablePicoContainer container,
      final ComponentAdapter<?> componentAdapter,
      final Method method,
      final Object instance,
      final RuntimeException cause) {
    lifecycleFailures.add(cause);

    try {
      delegate.lifecycleInvocationFailed(container, componentAdapter, method, instance, cause);
    } catch (final PicoLifecycleException e) {
      // do nothing, exception already logged for later rethrow
    }
  }

  @Override
  public Object noComponentFound(final MutablePicoContainer container, final Object key) {
    return delegate.noComponentFound(container, key);
  }

  @Override
  public <T> Injector<T> newInjector(final Injector<T> injector) {
    return delegate.newInjector(injector);
  }

  @Override
  public <T> ChangedBehavior<T> changedBehavior(final ChangedBehavior<T> changedBehavior) {
    return delegate.changedBehavior(changedBehavior);
  }

  public void rethrowLifecycleFailuresException() {
    throw new LifecycleFailuresException(lifecycleFailures);
  }

  /**
   * Subclass of {@link PicoException} that is thrown when the collected
   * lifecycle failures need to be be collectively rethrown.
   *
   * @author Paul Hammant
   * @author Mauro Talevi
   */
  public final class LifecycleFailuresException extends PicoException {
    private final List<RuntimeException> lifecycleFailures;

    public LifecycleFailuresException(final List<RuntimeException> lifecycleFailures) {
      this.lifecycleFailures = lifecycleFailures;
    }

    @Override
    public String getMessage() {
      final StringBuilder message = new StringBuilder();

      for (final Exception lifecycleFailure : lifecycleFailures) {
        message.append(lifecycleFailure.getMessage()).append(";  ");
      }

      return message.toString();
    }

    public Collection<RuntimeException> getFailures() {
      return lifecycleFailures;
    }
  }
}
