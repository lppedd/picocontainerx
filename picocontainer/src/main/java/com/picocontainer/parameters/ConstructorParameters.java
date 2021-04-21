/**
 *
 */
package com.picocontainer.parameters;

import com.picocontainer.Parameter;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * @author Mike
 *
 */
@SuppressWarnings("serial")
public class ConstructorParameters extends AccessibleObjectParameterSet {
  /**
   * Reference this in your constructor parameters if you wish the no-arg constructor to be used.
   */
  public static final ConstructorParameters NO_ARG_CONSTRUCTOR = new ConstructorParameters(new Parameter[0]);

  /**
   * Constructs constructor parameters with the given component and constant parameters.
   *
   * @param params the parameters in constructor parameter order
   */
  public ConstructorParameters(@Nullable final Parameter @Nullable [] params) {
    super(null, params);
  }

  public ConstructorParameters(final Parameter parameter) {
    super(null, parameter);
  }

  public ConstructorParameters() {
    this((Parameter[]) null);
  }

  @Override
  public String toString() {
    return "ConstructorParameters Parameters = " + Arrays.deepToString(getParams());
  }
}
