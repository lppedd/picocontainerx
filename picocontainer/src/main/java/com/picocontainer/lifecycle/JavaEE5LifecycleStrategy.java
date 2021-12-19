/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *****************************************************************************/
package com.picocontainer.lifecycle;

import com.picocontainer.ComponentMonitor;
import com.picocontainer.PicoLifecycleException;
import com.picocontainer.injectors.AnnotationInjectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Java EE 5 has some annotations {@link PreDestroy} and {@link PostConstruct}
 * that map to start() and dispose() in our world.
 *
 * @author Paul Hammant
 */
@SuppressWarnings("serial")
public final class JavaEE5LifecycleStrategy extends AbstractMonitoringLifecycleStrategy {
  /**
   * @param monitor the monitor to use
   *
   * @throws NullPointerException if the monitor is {@code null}
   */
  public JavaEE5LifecycleStrategy(@NotNull final ComponentMonitor monitor) {
    super(monitor);
  }

  @Override
  public void start(final Object component) {
    doLifecycleMethod(component, PostConstruct.class, true);
  }

  @Override
  public void stop(final Object component) {}

  @Override
  public void dispose(final Object component) {
    doLifecycleMethod(component, PreDestroy.class, false);
  }

  private void doLifecycleMethod(
      final Object component,
      final Class<? extends Annotation> annotation,
      final boolean superFirst) {
    doLifecycleMethod(component, annotation, component.getClass(), superFirst, new HashSet<>());
  }

  private void doLifecycleMethod(
      final Object component,
      final Class<? extends Annotation> annotation,
      final Class<?> clazz,
      final boolean superFirst,
      final Set<? super String> doneAlready) {
    final Class<?> parent = clazz.getSuperclass();

    if (superFirst && parent != Object.class) {
      doLifecycleMethod(component, annotation, parent, superFirst, doneAlready);
    }

    final Method[] methods = clazz.getDeclaredMethods();

    for (final Method method : methods) {
      final String signature = signature(method);

      if (method.isAnnotationPresent(annotation) && !doneAlready.contains(signature)) {
        try {
          final long str = System.currentTimeMillis();
          currentMonitor().invoking(null, null, method, component);
          AnnotationInjectionUtils.setMemberAccessible(method);
          method.invoke(component);
          doneAlready.add(signature);
          currentMonitor().invoked(null, null, method, component, System.currentTimeMillis() - str, null);
        } catch (final IllegalAccessException | InvocationTargetException e) {
          throw new PicoLifecycleException(method, component, e);
        }
      }
    }

    if (!superFirst && parent != Object.class) {
      // noinspection ConstantConditions
      doLifecycleMethod(component, annotation, parent, superFirst, doneAlready);
    }
  }

  private static String signature(final Method method) {
    final StringBuilder sb = new StringBuilder(method.getName());
    final Class<?>[] pt = method.getParameterTypes();

    for (final Class<?> objectClass : pt) {
      sb.append(objectClass.getName());
    }

    return sb.toString();
  }

  @Override
  public boolean hasLifecycle(final Class<?> type) {
    final Method[] methods = type.getDeclaredMethods();

    for (final Method method : methods) {
      if (method.isAnnotationPresent(PreDestroy.class) || method.isAnnotationPresent(PostConstruct.class)) {
        return true;
      }
    }

    return false;
  }
}
