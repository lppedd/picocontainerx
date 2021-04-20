/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *****************************************************************************/
package com.picocontainer;

import java.lang.reflect.Type;

/**
 * A facade for a collection of converters that provides string-to-type conversions.
 *
 * @author Paul Hammant
 * @author Michael Rimov
 */
public interface Converters {
  /**
   * Returns {@code true} if a converter is available to convert to the given object type.
   *
   * @param type the object type to convert to
   */
  boolean canConvert(final Type type);

  /**
   * Converts a particular string value into the target type.
   *
   * @param value the string value to convert
   * @param type the object type to convert to
   * @return The converted object instance
   */
  Object convert(final String value, final Type type);
}
