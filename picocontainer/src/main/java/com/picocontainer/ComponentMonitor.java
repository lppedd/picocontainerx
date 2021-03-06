/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by Paul Hammant & Obie Fernandez & Aslak                    *
 *****************************************************************************/

package com.picocontainer;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * A component monitor is responsible for monitoring the component instantiation and method invocation.
 *
 * @author Paul Hammant
 * @author Obie Fernandez
 * @author Aslak Helles&oslash;y
 * @author Mauro Talevi
 */
public interface ComponentMonitor {
  Object KEEP = new Object();

  /**
   * Event thrown as the component is being instantiated using the given constructor.
   *
   * @param container
   * @param componentAdapter
   * @param constructor the Constructor used to instantiate the addComponent
   *
   * @return the constructor to use in instantiation (nearly always the same one as passed in)
   */
  <T> Constructor<T> instantiating(
      final PicoContainer container,
      final ComponentAdapter<T> componentAdapter,
      final Constructor<T> constructor
  );

  /**
   * Event thrown after the component has been instantiated using the given constructor.
   * This should be called for both Constructor and Setter DI.
   *
   * @param container
   * @param componentAdapter
   * @param constructor the Constructor used to instantiate the addComponent
   * @param instantiated the component that was instantiated by PicoContainer
   * @param injected the components during instantiation.
   * @param duration the duration in milliseconds of the instantiation
   */
  <T> void instantiated(
      final PicoContainer container,
      final ComponentAdapter<T> componentAdapter,
      final Constructor<T> constructor,
      final Object instantiated,
      final Object[] injected,
      final long duration
  );

  /**
   * Event thrown if the component instantiation failed using the given constructor.
   *
   * @param container
   * @param componentAdapter
   * @param constructor the Constructor used to instantiate the addComponent
   * @param cause the Exception detailing the cause of the failure
   */
  <T> void instantiationFailed(
      final PicoContainer container,
      final ComponentAdapter<T> componentAdapter,
      final Constructor<T> constructor,
      final Exception cause
  );

  /**
   * Event thrown as the component method is being invoked on the given instance.
   *
   * @param container
   * @param componentAdapter
   * @param member
   * @param instance the component instance
   * @param args
   */
  Object invoking(
      @Nullable final PicoContainer container,
      @Nullable final ComponentAdapter<?> componentAdapter,
      final Member member,
      final Object instance,
      final Object... args
  );

  /**
   * Event thrown after the component method has been invoked on the given instance.
   *
   * @param container current container that the invoking component adapter has access to.
   * @param componentAdapter the component adapter making the invocation.
   * @param member Method/Field/etc being invoked
   * @param instance the component instance
   * @param duration duration of the invocation
   * @param retVal the return value from the invocation, most often null, may be non-null if a method was invoked.
   * @param args Arguments invoked on the member
   */
  void invoked(
      @Nullable final PicoContainer container,
      @Nullable final ComponentAdapter<?> componentAdapter,
      final Member member,
      final Object instance,
      final long duration,
      @Nullable final Object retVal,
      final Object... args
  );

  /**
   * Event thrown if the component method invocation failed on the given instance.
   *
   * @param member
   * @param instance the component instance
   * @param cause the Exception detailing the cause of the failure
   */
  void invocationFailed(final Member member, final Object instance, final Exception cause);

  /**
   * Event thrown if a lifecycle method invocation - start, stop or dispose - failed on the given instance.
   *
   * @param container
   * @param componentAdapter
   * @param method the lifecycle Method invoked on the component instance
   * @param instance the component instance
   * @param cause the {@link RuntimeException} detailing the cause of the failure
   */
  void lifecycleInvocationFailed(
      final @Nullable MutablePicoContainer container,
      final @Nullable ComponentAdapter<?> componentAdapter,
      final Method method,
      final Object instance,
      final RuntimeException cause
  );

  /**
   * No Component has been found for the key in question.
   * Implementers of this have a last chance opportunity to specify something for the need.
   * This is only relevant to component dependencies, and not to {@code container.getComponent(key)}
   * in your user code.
   *
   * @param container
   * @param key
   */
  Object noComponentFound(final MutablePicoContainer container, Object key);

  /**
   * A mechanism to monitor or override the {@link Injector}s being made for components.
   *
   * @param injector
   *
   * @return an {@link Injector}. For most implementations, the same one as was passed in.
   */
  <T> Injector<T> newInjector(final Injector<T> injector);

  /**
   * A mechanism to monitor or override the {@code Behaviors} being made for components.
   *
   * @param changedBehavior
   *
   * @return an {@link Behavior}. For most implementations, the same one as was passed in.
   */
  <T> ChangedBehavior<T> changedBehavior(final ChangedBehavior<T> changedBehavior);
}
