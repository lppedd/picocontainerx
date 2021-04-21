/**
 *
 */
package com.picocontainer.parameters;

import com.googlecode.jtype.Generic;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.JTypeHelper;
import com.picocontainer.Parameter;
import com.picocontainer.injectors.AbstractInjector.AmbiguousComponentResolutionException;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Extension to {@link ComponentParameter} that attempts to sort out "ambiguous components"
 * in a way compatible with JSR330.
 * Currently if there is a {@link javax.inject.Named} annotation or a qualifier,
 * they are registered with a string key while default components (no qualifier)
 * will have the key of the implementation of the class (usually).
 * </p>
 * <p>
 * If we get a situation where {@link AmbiguousComponentResolutionException} would be thrown,
 * then we try this sorting algorithm first.
 * </p>
 *
 * @author Michael Rimov
 */
@SuppressWarnings("serial")
public class JSR330ComponentParameter extends ComponentParameter {
  public static final JSR330ComponentParameter DEFAULT = new JSR330ComponentParameter();

  public JSR330ComponentParameter() { }

  public JSR330ComponentParameter(final Object key) {
    super(key);
  }

  public JSR330ComponentParameter(final boolean emptyCollection) {
    super(emptyCollection);
  }

  public JSR330ComponentParameter(
      final Generic<?> componentValueType,
      final boolean emptyCollection) {
    super(componentValueType, emptyCollection);
  }

  public JSR330ComponentParameter(
      final Class<?> keyType,
      final Generic<?> componentValueType,
      final boolean emptyCollection) {
    super(keyType, componentValueType, emptyCollection);
  }

  public JSR330ComponentParameter(final Parameter mapDefiningParameter) {
    super(mapDefiningParameter);
  }

  /**
   * Override that looks to see if there is only one component adapter with a class as the
   * key since Providers and qualifiers will automatically have a string value.
   */
  @Override
  protected <T> ComponentAdapter<T> sortThroughTooManyAdapters(
      final Generic<T> expectedType,
      final List<ComponentAdapter<T>> found) {
    ComponentAdapter<T> lastAdapterWithClassKey = null;

    for (final ComponentAdapter<T> eachAdapter : found) {
      if (eachAdapter.getComponentKey() instanceof Class<?>) {
        // More than one found, bail
        if (lastAdapterWithClassKey != null) {
          lastAdapterWithClassKey = null;
          break;
        }

        lastAdapterWithClassKey = eachAdapter;
      }
    }

    if (lastAdapterWithClassKey == null) {
      return checkForMatchingGenericParameterTypes(expectedType, found);
    }

    return lastAdapterWithClassKey;
  }

  @Nullable
  protected <T> ComponentAdapter<T> checkForMatchingGenericParameterTypes(
      final Generic<T> expectedType,
      final Collection<? extends ComponentAdapter<T>> found) {
    ComponentAdapter<T> lastFound = null;

    for (final ComponentAdapter<T> eachCA : found) {
      if (JTypeHelper.isAssignableFrom(expectedType, eachCA.getComponentImplementation())) {
        // More than one found, bail
        if (lastFound != null) {
          return null;
        }

        lastFound = eachCA;
      }
    }

    return lastFound;
  }
}
