package com.picocontainer.converters;

/**
 * Converts values to {@link Integer} data type objects.
 *
 * @author Paul Hammant
 * @author Michael Rimov
 */
class IntegerConverter implements Converter<Integer> {
  @Override
  public Integer convert(final String paramValue) {
    return Integer.valueOf(paramValue);
  }
}
