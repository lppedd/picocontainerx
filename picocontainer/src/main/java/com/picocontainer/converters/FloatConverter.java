package com.picocontainer.converters;

/**
 * Converts values to {@link Float} data type objects.
 *
 * @author Paul Hammant
 * @author Michael Rimov
 */
class FloatConverter implements Converter<Float> {
  @Override
  public Float convert(final String paramValue) {
    return Float.valueOf(paramValue);
  }
}
