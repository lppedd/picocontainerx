/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer.injectors;

import com.picocontainer.ComponentMonitor;
import com.picocontainer.Parameter;
import com.picocontainer.PicoCompositionException;
import com.picocontainer.PicoContainer;
import com.picocontainer.parameters.FieldParameters;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

/**
 * TODO
 */
@SuppressWarnings("serial")
public abstract class AbstractFieldInjector<T> extends IterativeInjector<T> {
  public AbstractFieldInjector(
      final Object componentKey,
      final Class<T> componentImplementation,
      final ComponentMonitor monitor,
      final boolean useNames,
      final boolean requireConsumptionOfAllParameters,
      final FieldParameters... parameters) {
    super(
        componentKey,
        componentImplementation,
        monitor,
        useNames,
        requireConsumptionOfAllParameters,
        parameters
    );
  }

  @Override
  protected final void unsatisfiedDependencies(
      final PicoContainer container,
      final Set<Type> unsatisfiableDependencyTypes,
      final List<? extends AccessibleObject> unsatisfiableDependencyMembers) {
    final StringBuilder sb = new StringBuilder(getComponentImplementation().getName());
    sb.append(" has unsatisfied dependency for fields [");

    for (final AccessibleObject accessibleObject : unsatisfiableDependencyMembers) {
      final Field field = (Field) accessibleObject;
      sb.append(" ")
          .append(field.getDeclaringClass().getName())
          .append(".")
          .append(field.getName())
          .append(" (field's type is ")
          .append(field.getType().getName())
          .append(") ");
    }

    throw new UnsatisfiableDependenciesException(sb + "] from " + container.toString());
  }

  @Override
  protected boolean isAccessibleObjectEqualToParameterTarget(
      final AccessibleObject accessibleObject,
      final Parameter currentParameter) {
    if (currentParameter.getTargetName() == null) {
      return false;
    }

    if (!(accessibleObject instanceof Field)) {
      throw new PicoCompositionException(accessibleObject + " must be a field to use setter injection");
    }

    final Field testField = (Field) accessibleObject;
    return testField.getName().equals(currentParameter.getTargetName());
  }
}
