/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *****************************************************************************/
package com.picocontainer.lifecycle;

import com.picocontainer.ComponentMonitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Reflection lifecycle strategy.
 * Starts, stops, disposes of component if appropriate methods are present.
 * The component may implement only one of the three methods.
 *
 * @author Paul Hammant
 * @author Mauro Talevi
 * @author J&ouml;rg Schaible
 * @see com.picocontainer.Startable
 * @see com.picocontainer.Disposable
 * @see StartableLifecycleStrategy
 */
@SuppressWarnings("serial")
public class ReflectionLifecycleStrategy extends AbstractMonitoringLifecycleStrategy {
  /**
   * Index in the {@link ReflectionLifecycleStrategy#methodNames} array that contains the name of the 'start' method.
   */
  private static final int START = 0;

  /**
   * Index in the {@link ReflectionLifecycleStrategy#methodNames} array that contains the name of the 'stop' method.
   */
  private static final int STOP = 1;

  /**
   * Index in the {@link ReflectionLifecycleStrategy#methodNames} array that contains the name of the 'dispose' method.
   */
  private static final int DISPOSE = 2;

  /**
   * An array of method names that are part of the lifecycle functions.
   */
  private final String[] methodNames;

  /**
   * Map of classes mapped to method arrays that are cached for reflection.
   */
  private final transient Map<Class<?>, Method[]> methodMap = new HashMap<>();

  /**
   * @param monitor the monitor to use
   * @throws NullPointerException if the monitor is {@code null}
   */
  public ReflectionLifecycleStrategy(final ComponentMonitor monitor) {
    this(monitor, "start", "stop", "dispose");
  }

  /**
   * Construct a {@code ReflectionLifecycleStrategy} with individual method names.
   * Note, that a lifecycle method does not have any arguments.
   *
   * @param monitor the monitor to use
   * @param startMethodName the name of the start method
   * @param stopMethodName the name of the stop method
   * @param disposeMethodName the name of the dispose method
   */
  public ReflectionLifecycleStrategy(
      final ComponentMonitor monitor,
      final String startMethodName,
      final String stopMethodName,
      final String disposeMethodName) {
    super(monitor);
    methodNames = new String[]{startMethodName, stopMethodName, disposeMethodName};
  }

  @Override
  public void start(final Object component) {
    final Method[] methods = init(component.getClass());
    invokeMethod(component, methods[START]);

  }

  @Override
  public void stop(final Object component) {
    final Method[] methods = init(component.getClass());
    invokeMethod(component, methods[STOP]);
  }

  @Override
  public void dispose(final Object component) {
    final Method[] methods = init(component.getClass());
    invokeMethod(component, methods[DISPOSE]);
  }

  private void invokeMethod(final Object component, final Method method) {
    if (component != null && method != null) {
      try {
        final long str = System.currentTimeMillis();
        currentMonitor().invoking(null, null, method, component);
        method.invoke(component);
        currentMonitor().invoked(null, null, method, component, System.currentTimeMillis() - str, null);
      } catch (final IllegalAccessException e) {
        monitorAndThrowReflectionLifecycleException(method, e, component);
      } catch (final InvocationTargetException e) {
        monitorAndThrowReflectionLifecycleException(method, e.getCause(), component);
      }
    }
  }

  protected void monitorAndThrowReflectionLifecycleException(
      final Method method,
      final Throwable e,
      final Object component) {
    final RuntimeException re;

    // noinspection SimplifiableIfStatement
    if (e.getCause() instanceof RuntimeException) {
      re = (RuntimeException) e.getCause();
      // TODO - change lifecycleInvocationFailed to take a throwable in future version
      //        } else if (e.getCause() instanceof Error) {
      //            re = (Error) e.getCause();
    } else {
      re = new RuntimeException("wrapper", e);
    }

    currentMonitor().lifecycleInvocationFailed(null, null, method, component, re);
  }

  /**
   * The component has a lifecycle if at least one of the three methods is present.
   */
  @Override
  public boolean hasLifecycle(final Class<?> type) {
    final Method[] methods = init(type);

    for (final Method method : methods) {
      if (method != null) {
        return true;
      }
    }

    return false;
  }

  /**
   * Initializes the method array with the given type.
   *
   * @param type the type to examine for reflection lifecycle methods.
   * @return Method array containing start/stop/dispose methods.
   */
  private Method[] init(final Class<?> type) {
    Method[] methods;

    synchronized (methodMap) {
      methods = methodMap.get(type);

      if (methods == null) {
        methods = new Method[methodNames.length];

        for (int i = 0; i < methods.length; i++) {
          try {
            final String methodName = methodNames[i];

            if (methodName == null) {
              // skipping, we're not interested in this lifecycle method.
              continue;
            }

            methods[i] = type.getMethod(methodName);
          } catch (final NoSuchMethodException ignored) {
            // continue
          }
        }

        methodMap.put(type, methods);
      }
    }

    return methods;
  }
}
