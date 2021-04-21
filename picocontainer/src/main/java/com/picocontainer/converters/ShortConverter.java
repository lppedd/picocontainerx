package com.picocontainer.converters;

/**
 * Converts values to {@link Short} data type objects.
 *
 * @author Paul Hammant
 * @author Michael Rimov
 */
class ShortConverter implements Converter<Short> {
  @Override
  public Short convert(final String paramValue) {
    return Short.valueOf(paramValue);
  }
}
