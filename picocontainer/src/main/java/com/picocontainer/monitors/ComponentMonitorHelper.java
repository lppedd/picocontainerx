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

import com.picocontainer.ComponentMonitor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.text.MessageFormat;

/**
 * An abstract {@link ComponentMonitor} which supports all the message formats.
 *
 * @author Mauro Talevi
 */
public final class ComponentMonitorHelper {
  public static final String INSTANTIATING = "PicoContainer: instantiating {0}";
  public static final String INSTANTIATED = "PicoContainer: instantiated {0} [{1} ms], component {2}, injected [{3}]";
  public static final String INSTANTIATION_FAILED = "PicoContainer: instantiation failed: {0}, reason: {1}";
  public static final String INVOKING = "PicoContainer: invoking {0} on {1}";
  public static final String INVOKED = "PicoContainer: invoked {0} on {1} [{2} ms]";
  public static final String INVOCATION_FAILED = "PicoContainer: invocation failed: {0} on {1}, reason: {2}";
  public static final String LIFECYCLE_INVOCATION_FAILED = "PicoContainer: lifecycle invocation failed: {0} on {1}, reason: {2}";
  public static final String NO_COMPONENT = "PicoContainer: No component for key: {0}";

  public static String format(final String template, final Object... arguments) {
    return MessageFormat.format(template, arguments);
  }

  public static String parmsToString(final Object[] injected) {
    if (injected == null) {
      return "";
    }

    final StringBuilder sb = new StringBuilder();

    for (int i = 0; i < injected.length; i++) {
      final String s = injected[i].getClass().getName();
      sb.append(s);

      if (i < injected.length - 1) {
        sb.append(", ");
      }
    }

    return sb.toString();
  }

  public static String ctorToString(final Constructor<?> constructor) {
    if (constructor == null) {
      return "null";
    }

    final Class<?>[] params = constructor.getParameterTypes();
    final StringBuilder sb = new StringBuilder(constructor.getName());
    sb.append("(");

    for (int i = 0; i < params.length; i++) {
      final String s = params[i].getName();
      sb.append(s);

      if (i < params.length - 1) {
        sb.append(", ");
      }
    }

    sb.append(")");
    return sb.toString();
  }

  public static String methodToString(final Member member) {
    if (member == null) {
      return "null";
    }

    final StringBuilder sb = new StringBuilder(member.getName());

    if (member instanceof Method) {
      final Class<?>[] params = ((Method) member).getParameterTypes();
      sb.append("(");

      for (int i = 0; i < params.length; i++) {
        final String s = params[i].getName();
        sb.append(s);

        if (i < params.length - 1) {
          sb.append(", ");
        }
      }

      sb.append(")");
    }

    return sb.toString();
  }

  public static String getDeclaringTypeString(final Member member) {
    // noinspection SimplifiableIfStatement
    if (member == null) {
      return " null ";
    }

    return member.getDeclaringClass().getName();
  }

  public static String memberToString(final Member member) {
    if (member == null) {
      return "null";
    }

    // noinspection SimplifiableIfStatement
    if (member instanceof Field) {
      return getDeclaringTypeString(member) + "." + toString((Field) member);
    }

    return getDeclaringTypeString(member) + "." + methodToString(member);
  }

  public static String toString(final Field field) {
    // noinspection SimplifiableIfStatement
    if (field == null) {
      return "null";
    }

    return field.getName() + "(" + field.getName() + ")";
  }
}
