/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by Paul Hammaant                                            *
 *****************************************************************************/

package com.picocontainer.monitors;

import com.picocontainer.ChangedBehavior;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.Injector;
import com.picocontainer.MutablePicoContainer;
import com.picocontainer.PicoContainer;

import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * A {@link ComponentMonitor} which writes to a {@link Writer}.
 *
 * @author Paul Hammant
 * @author Aslak Helles&oslash;y
 * @author Mauro Talevi
 */
public class WriterComponentMonitor implements ComponentMonitor {
  private final PrintWriter out;
  private final ComponentMonitor delegate;

  public WriterComponentMonitor(final Writer out) {
    this(out, new NullComponentMonitor());
  }

  public WriterComponentMonitor(final Writer out, final ComponentMonitor delegate) {
    this.out = new PrintWriter(out);
    this.delegate = delegate;
  }

  @Override
  public <T> Constructor<T> instantiating(
      final PicoContainer container,
      final ComponentAdapter<T> componentAdapter,
      final Constructor<T> constructor) {
    out.println(ComponentMonitorHelper.format(ComponentMonitorHelper.INSTANTIATING, ComponentMonitorHelper.ctorToString(constructor)));
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
    out.println(
        ComponentMonitorHelper.format(
            ComponentMonitorHelper.INSTANTIATED,
            ComponentMonitorHelper.ctorToString(constructor),
            duration,
            instantiated.getClass().getName(),
            ComponentMonitorHelper.parmsToString(injected)
        )
    );

    delegate.instantiated(container, componentAdapter, constructor, instantiated, injected, duration);
  }

  @Override
  public <T> void instantiationFailed(
      final PicoContainer container,
      final ComponentAdapter<T> componentAdapter,
      final Constructor<T> constructor,
      final Exception cause) {
    out.println(
        ComponentMonitorHelper.format(
            ComponentMonitorHelper.INSTANTIATION_FAILED,
            ComponentMonitorHelper.ctorToString(constructor),
            cause.getMessage()
        )
    );

    delegate.instantiationFailed(container, null, constructor, cause);
  }

  @Override
  public Object invoking(
      final PicoContainer container,
      final ComponentAdapter<?> componentAdapter,
      final Member member,
      final Object instance,
      final Object... args) {
    out.println(
        ComponentMonitorHelper.format(
            ComponentMonitorHelper.INVOKING,
            ComponentMonitorHelper.memberToString(member),
            instance
        )
    );

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
    out.println(
        ComponentMonitorHelper.format(
            ComponentMonitorHelper.INVOKED,
            ComponentMonitorHelper.methodToString(member),
            instance,
            duration
        )
    );

    delegate.invoked(container, componentAdapter, member, instance, duration, retVal, args);
  }

  @Override
  public void invocationFailed(final Member member, final Object instance, final Exception cause) {
    out.println(
        ComponentMonitorHelper.format(
            ComponentMonitorHelper.INVOCATION_FAILED,
            ComponentMonitorHelper.memberToString(member),
            instance,
            cause.getMessage()
        )
    );

    delegate.invocationFailed(member, instance, cause);
  }

  @Override
  public void lifecycleInvocationFailed(
      final MutablePicoContainer container,
      final ComponentAdapter<?> componentAdapter,
      final Method method,
      final Object instance,
      final RuntimeException cause) {
    out.println(
        ComponentMonitorHelper.format(
            ComponentMonitorHelper.LIFECYCLE_INVOCATION_FAILED,
            ComponentMonitorHelper.methodToString(method),
            instance,
            cause.getMessage()
        )
    );

    delegate.lifecycleInvocationFailed(container, componentAdapter, method, instance, cause);
  }

  @Override
  public Object noComponentFound(final MutablePicoContainer container, final Object key) {
    out.println(ComponentMonitorHelper.format(ComponentMonitorHelper.NO_COMPONENT, key));
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
}
