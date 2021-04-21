/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by Paul Hammant & Obie Fernandez & Aslak Helles&oslash;y    *
 *****************************************************************************/

package com.picocontainer.monitors;

import com.picocontainer.ChangedBehavior;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.Injector;
import com.picocontainer.MutablePicoContainer;
import com.picocontainer.PicoContainer;
import com.picocontainer.PicoLifecycleException;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * A {@link ComponentMonitor} that does nothing.
 *
 * @author Paul Hammant
 * @author Obie Fernandez
 */
@SuppressWarnings("serial")
public class NullComponentMonitor implements ComponentMonitor, Serializable {
  @Override
  public <T> Constructor<T> instantiating(
      final PicoContainer container,
      final ComponentAdapter<T> componentAdapter,
      final Constructor<T> constructor) {
    return constructor;
  }

  @Override
  public <T> void instantiationFailed(
      final PicoContainer container,
      final ComponentAdapter<T> componentAdapter,
      final Constructor<T> constructor,
      final Exception e) {}

  @Override
  public <T> void instantiated(
      final PicoContainer container,
      final ComponentAdapter<T> componentAdapter,
      final Constructor<T> constructor,
      final Object instantiated,
      final Object[] injected,
      final long duration) {}

  @Override
  public Object invoking(
      final PicoContainer container,
      final ComponentAdapter<?> componentAdapter,
      final Member member,
      final Object instance,
      final Object... args) {
    return KEEP;
  }

  @Override
  public void invoked(
      final PicoContainer container,
      final ComponentAdapter<?> componentAdapter,
      final Member member,
      final Object instance,
      final long duration,
      final Object retVal,
      final Object... args) {}

  @Override
  public void invocationFailed(final Member member, final Object instance, final Exception e) {}

  @Override
  public void lifecycleInvocationFailed(
      final MutablePicoContainer container,
      final ComponentAdapter<?> componentAdapter,
      final Method method,
      final Object instance,
      final RuntimeException cause) {
    if (cause instanceof PicoLifecycleException) {
      throw cause;
    }

    throw new PicoLifecycleException(method, instance, cause);
  }

  @Nullable
  @Override
  public Object noComponentFound(final MutablePicoContainer container, final Object key) {
    return null;
  }

  @Override
  public <T> Injector<T> newInjector(final Injector<T> injector) {
    return injector;
  }

  @Override
  public <T> ChangedBehavior<T> changedBehavior(final ChangedBehavior<T> changedBehavior) {
    return changedBehavior;
  }
}
