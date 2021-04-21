package com.picocontainer.converters;

/**
 * Converts values to {@link Double} data type objects.
 *
 * @author Paul Hammant
 * @author Michael Rimov
 */
class DoubleConverter implements Converter<Double> {
  @Override
  public Double convert(final String paramValue) {
    return Double.valueOf(paramValue);
  }
}
