/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/
package com.picocontainer.injectors;

import com.picocontainer.PicoCompositionException;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Comparator;

/**
 * Allows sort on fields and methods.
 * <p>
 * Sorting rules:
 * <ol>
 * 	<li>Base class AccessibleObject come first</li>
 *  <li>Static AccessibleObject are used first before non static accessible objects if they're in the same class</li>
 * </ol>
 *
 * @author Michael Rimov
 */
public class JSR330AccessibleObjectOrderComparator implements Comparator<AccessibleObject> {
  @Override
  public int compare(final AccessibleObject o1, final AccessibleObject o2) {
    if (o1 == o2) {
      return 0;
    }

    if (o1 == null) {
      return -1;
    }

    if (o2 == null) {
      return 1;
    }

    if (!o1.getClass().equals(o2.getClass())) {
      throw new IllegalArgumentException("Both arguments need to be the same type");
    }

    final Integer o1Distance = getDistanceToJavaLangObject(o1);
    final Integer o2Distance = getDistanceToJavaLangObject(o2);
    int comparisonResult = o1Distance.compareTo(o2Distance);

    if (comparisonResult != 0) {
      return comparisonResult;
    }

    comparisonResult = compareFieldMethodOrder(o1.getClass(), o2.getClass());

    // noinspection SimplifiableIfStatement
    if (comparisonResult != 0) {
      return comparisonResult;
    }

    return compareStatics(o1, o2);
  }

  /**
   * In JSR-330, if they're in the same class, fields are injected
   * before methods.
   *
   * @param o1
   * @param o2
   *
   * @return
   */
  private int compareFieldMethodOrder(final Class<?> o1, final Class<?> o2) {
    if (Field.class.isAssignableFrom(o1) && Method.class.isAssignableFrom(o2)) {
      return -1;
    }

    return Method.class.isAssignableFrom(o1) && Field.class.isAssignableFrom(o2) ? 1 : 0;
  }

  // Currently, this comparator only handles fields and methods.
  private boolean isComparableOrderType(final Class<?> type) {
    return Field.class.isAssignableFrom(type) || Method.class.isAssignableFrom(type);
  }

  /**
   * Computes a number that represents the # of classes between the owning class
   * of the member being checked and java.lang.Object.  Further away gets a
   * higher score.
   *
   * @param ao
   *
   * @return
   */
  private int getDistanceToJavaLangObject(final AccessibleObject ao) {
    Class<?> currentType = getDeclaringClass(ao);
    int count = 0;

    while (!Object.class.equals(currentType)) {
      count++;
      currentType = currentType.getSuperclass();
    }

    return count;
  }

  private Class<?> getDeclaringClass(final AccessibleObject ao) {
    if (ao instanceof Member) {
      return ((Member) ao).getDeclaringClass();
    }

    throw new PicoCompositionException(
        ao.getClass() +
            " does not appear to be a field, method," +
            " or constructor (or anything that implements Member interface)");

  }

  private int getModifiers(final AccessibleObject ao) {
    if (ao instanceof Member) {
      return ((Member) ao).getModifiers();
    }

    throw new PicoCompositionException(
        ao.getClass() +
            " does not appear to be a field, method," +
            " or constructor (or anything that implements the Member interface)"
    );
  }

  private int compareStatics(final AccessibleObject o1, final AccessibleObject o2) {
    final int o1Modifiers = getModifiers(o1);
    final int o2Modifiers = getModifiers(o2);
    final boolean o1Static = Modifier.isStatic(o1Modifiers);
    final boolean o2Static = Modifier.isStatic(o2Modifiers);
    return o1Static && !o2Static ? -1 : !o1Static && o2Static ? 1 : 0;
  }
}
