package com.picocontainer.injectors;

import com.picocontainer.Parameter;
import com.picocontainer.PicoCompositionException;
import com.picocontainer.containers.JSR330PicoContainer;
import com.picocontainer.parameters.ComponentParameter;
import com.picocontainer.parameters.JSR330ComponentParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Named;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;

public class AnnotationInjectionUtils {
  private AnnotationInjectionUtils() {}

  /**
   * If a default ComponentParameter() is being used for a particular argument for the given method, then
   * this function may substitute what would normally be resolved based on JSR-330 annotations.
   */
  public static Parameter[] interceptParametersToUse(
      final Parameter[] currentParameters,
      final AccessibleObject member) {
    final Annotation[][] allAnnotations = getParameterAnnotations(member);

    if (currentParameters.length != allAnnotations.length) {
      throw new PicoCompositionException("Internal error, parameter lengths, not the same as the annotation lengths");
    }

    // Make this function side effect free.
    final Parameter[] returnValue = Arrays.copyOf(currentParameters, currentParameters.length);

    for (int i = 0; i < returnValue.length; i++) {
      // Allow composition scripts to override annotations
      // See comment in com.picocontainer.injectors.AnnotatedFieldInjection.AnnotatedFieldInjector.getParameterToUseForObject(AccessibleObject, AccessibleObjectParameterSet...)
      // for possible issues with this
      if (returnValue[i] != ComponentParameter.DEFAULT && returnValue[i] != JSR330ComponentParameter.DEFAULT) {
        continue;
      }

      final Named namedAnnotation = getNamedAnnotation(allAnnotations[i]);

      if (namedAnnotation != null) {
        returnValue[i] = new JSR330ComponentParameter(namedAnnotation.value());
      } else {
        final Annotation qualifier = JSR330PicoContainer.getQualifier(allAnnotations[i]);

        if (qualifier != null) {
          returnValue[i] = new JSR330ComponentParameter(qualifier.annotationType().getName());
        }
      }

      // Otherwise, don't modify it
    }

    return returnValue;
  }

  @NotNull
  private static Annotation[][] getParameterAnnotations(final AccessibleObject member) {
    if (member instanceof Constructor) {
      return ((Constructor<?>) member).getParameterAnnotations();
    }

    if (member instanceof Field) {
      return new Annotation[][]{member.getAnnotations()};
    }

    if (member instanceof Method) {
      return ((Method) member).getParameterAnnotations();
    }

    AbstractInjector.throwUnknownAccessibleObjectType(member);

    // Never gets here
    return null;
  }

  @Nullable
  private static Named getNamedAnnotation(@NotNull final Annotation[] annotations) {
    for (final Annotation eachAnnotation : annotations) {
      if (eachAnnotation.annotationType().equals(Named.class)) {
        return (Named) eachAnnotation;
      }
    }

    return null;
  }

  /**
   * Allows private method/constructor injection on fields/methods.
   */
  public static void setMemberAccessible(final AccessibleObject target) {
    // Don't run a privileged block if we don't have to
    if (target.isAccessible()) {
      return;
    }

    AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
      target.setAccessible(true);
      return null;
    });
  }
}
