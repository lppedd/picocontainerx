package com.picocontainer.converters;

/**
 * Converts values to {@link Boolean} data type objects.
 *
 * @author Paul Hammant
 * @author Michael Rimov
 */
class BooleanConverter implements Converter<Boolean> {
  @Override
  public Boolean convert(final String paramValue) {
    return Boolean.valueOf(paramValue);
  }
}
