/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/
package com.picocontainer.injectors;

import com.googlecode.jtype.Generic;
import com.picocontainer.ComponentAdapter.NOTHING;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.ComponentMonitorStrategy;
import com.picocontainer.InjectionType;
import com.picocontainer.LifecycleStrategy;
import com.picocontainer.PicoCompositionException;
import com.picocontainer.PicoContainer;
import com.picocontainer.lifecycle.NullLifecycleStrategy;
import com.picocontainer.monitors.NullComponentMonitor;
import com.picocontainer.parameters.MethodParameters;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

/**
 * A Re-injector allows methods on pre-instantiated classes to be invoked,
 * with appropriately scoped parameters.
 */
public class Reinjector {
  private static final LifecycleStrategy NO_LIFECYCLE = new NullLifecycleStrategy();
  private static final Properties NO_PROPERTIES = new Properties();

  private final PicoContainer parent;
  private final ComponentMonitor monitor;

  /**
   * Make a re-injector with a parent container from which to pull components to be re-injected to.
   * With this constructor, a NullComponentMonitor is used.
   *
   * @param parent the parent container
   */
  public Reinjector(@NotNull final PicoContainer parent) {
    this(
        parent,
        parent instanceof ComponentMonitorStrategy
            ? ((ComponentMonitorStrategy) parent).currentMonitor()
            : new NullComponentMonitor()
    );
  }

  /**
   * Make a re-injector with a parent container from which to pull components to be re-injected to.
   *
   * @param parent the parent container
   * @param monitor the monitor to use for 'instantiating' events
   */
  public Reinjector(@NotNull final PicoContainer parent, final ComponentMonitor monitor) {
    this.parent = parent;
    this.monitor = monitor;
  }

  /**
   * Re-injecting into a method.
   *
   * @param key the component-key from the parent set of components to inject into
   * @param reInjectionMethod the reflection method to use for injection
   *
   * @return the result of the re-injection method invocation
   */
  public Object reInject(final Class<?> key, final Method reInjectionMethod) {
    return reInject(
        key,
        key,
        parent.getComponentInto(Generic.get(key), NOTHING.class),
        NO_PROPERTIES,
        new MethodInjection(reInjectionMethod)
    );
  }

  /**
   * Re-injecting into a method.
   *
   * @param key the component-key from the parent set of components to inject into
   * @param reInjectionMethodEnum the enum for the reflection method to use for injection.
   *
   * @return the result of the re-injection method invocation
   */
  public Object reInject(final Class<?> key, final Enum<?> reInjectionMethodEnum) {
    return reInject(
        key,
        key,
        parent.getComponentInto(Generic.get(key), NOTHING.class),
        NO_PROPERTIES,
        new MethodInjection(toMethod(reInjectionMethodEnum))
    );
  }

  private Method toMethod(final Enum<?> reInjectionMethodEnum) {
    final Object methodOrException = AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
      try {
        return reInjectionMethodEnum.getClass().getMethod("toMethod").invoke(reInjectionMethodEnum);
      } catch (final IllegalAccessException e) {
        return new PicoCompositionException("Illegal access to " + reInjectionMethodEnum.name());
      } catch (final InvocationTargetException e) {
        return new PicoCompositionException("Invocation Target Exception " + reInjectionMethodEnum.name(), e.getCause());
      } catch (final NoSuchMethodException e) {
        return new PicoCompositionException("Expected generated method toMethod() on enum");
      }
    });

    if (methodOrException instanceof Method) {
      return (Method) methodOrException;
    }

    throw (PicoCompositionException) methodOrException;
  }

  /**
   * Re-injecting into a method.
   *
   * @param key the component-key from the parent set of components to inject into (key and impl are the same)
   * @param reInjectionType the InjectionFactory to use for re-injection
   *
   * @return the result of the re-injection method invocation
   */
  public Object reInject(final Class<?> key, final InjectionType reInjectionType) {
    return reInject(
        key,
        key,
        parent.getComponentInto(Generic.get(key), NOTHING.class),
        NO_PROPERTIES,
        reInjectionType
    );
  }

  /**
   * Re-injecting into a method.
   *
   * @param key the component-key from the parent set of components to inject into
   * @param impl the implementation of the component that is going to result
   * @param reInjectionType the InjectionFactory to use for re-injection
   */
  public Object reInject(final Class<?> key, final Class<?> impl, final InjectionType reInjectionType) {
    return reInject(
        key,
        impl,
        parent.getComponentInto(key, NOTHING.class),
        NO_PROPERTIES,
        reInjectionType
    );
  }

  /**
   * Re-injecting into a method.
   *
   * @param key the component-key from the parent set of components to inject into
   * @param implementation the implementation of the component that is going to result
   * @param instance the object that has the provider method to be invoked
   * @param reInjectionType the InjectionFactory to use for re-injection
   *
   * @return the result of the re-injection method invocation
   */
  public Object reInject(
      final Class<?> key,
      final Class<?> implementation,
      final Object instance,
      final InjectionType reInjectionType) {
    return reInject(key, implementation, instance, NO_PROPERTIES, reInjectionType);
  }

  /**
   * Re-injecting into a method.
   *
   * @param key the component-key from the parent set of components to inject into
   * @param implementation the implementation of the component that is going to result
   * @param instance the object that has the provider method to be invoked
   * @param properties for re-injection
   * @param reInjectionType the InjectionFactory to use for re-injection
   *
   * @return the result of the re-injection method invocation
   */
  public Object reInject(
      final Object key,
      final Class<?> implementation,
      final Object instance,
      final Properties properties,
      final InjectionType reInjectionType,
      final MethodParameters... methodParams) {
    final Reinjection reInjection = new Reinjection(reInjectionType, parent);
    final com.picocontainer.Injector<Object> injector =
        (com.picocontainer.Injector<Object>) reInjection.createComponentAdapter(
            monitor,
            NO_LIFECYCLE,
            properties,
            key,
            implementation,
            null,
            null,
            methodParams != null && methodParams.length > 0 ? methodParams : null
        );

    return injector.decorateComponentInstance(parent, NOTHING.class, instance);
  }
}
