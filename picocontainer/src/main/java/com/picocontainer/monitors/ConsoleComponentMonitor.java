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

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * A {@link ComponentMonitor} which writes to a {@link OutputStream}.
 * This is typically used to write to a console.
 * <p>
 * TODO: after serialization, the output PrintStream is null
 *
 * @author Paul Hammant
 * @author Aslak Helles&oslash;y
 * @author Mauro Talevi
 */
@SuppressWarnings("serial")
public class ConsoleComponentMonitor implements ComponentMonitor, Serializable {
  /**
   * The outgoing print stream.
   */
  private final transient PrintStream out;

  /**
   * Delegate component monitor (for component monitor chains).
   */
  private final ComponentMonitor delegate;

  /**
   * Constructs a console component monitor that sends output to {@link System#out}.
   */
  public ConsoleComponentMonitor() {
    // noinspection UseOfSystemOutOrSystemErr
    this(System.out);
  }

  /**
   * Constructs a console component monitor that sends output to the specified output stream.
   *
   * @param out the designated output stream
   * Options include System.out, Socket streams, File streams, etc
   */
  public ConsoleComponentMonitor(final OutputStream out) {
    this(out, new NullComponentMonitor());
  }

  /**
   * Constructs a console component monitor chain that sends output to the specified output stream
   * and then sends all events to the delegate component monitor.
   *
   * @param out the output stream of choice
   * @param delegate the next monitor in the component monitor chain to receive event information
   */
  public ConsoleComponentMonitor(final OutputStream out, final ComponentMonitor delegate) {
    this.out = new PrintStream(out);
    this.delegate = delegate;
  }

  @Override
  public <T> Constructor<T> instantiating(
      final PicoContainer container,
      final ComponentAdapter<T> componentAdapter,
      final Constructor<T> constructor) {
    out.println(
        ComponentMonitorHelper.format(
            ComponentMonitorHelper.INSTANTIATING,
            ComponentMonitorHelper.ctorToString(constructor)
        )
    );

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
    out.println(
        ComponentMonitorHelper.format(
            ComponentMonitorHelper.INSTANTIATED,
            ComponentMonitorHelper.ctorToString(constructor),
            duration,
            instantiated.getClass().getName(),
            ComponentMonitorHelper.parmsToString(parameters)
        )
    );

    delegate.instantiated(container, componentAdapter, constructor, instantiated, parameters, duration);
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

    delegate.instantiationFailed(container, componentAdapter, constructor, cause);
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
      final Object... args) {
    out.println(
        ComponentMonitorHelper.format(
            ComponentMonitorHelper.INVOKED,
            ComponentMonitorHelper.memberToString(member),
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
            ComponentMonitorHelper.memberToString(method),
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
