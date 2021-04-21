package com.picocontainer.converters;

/**
 * Converts values to {@link Long} data type objects.
 *
 * @author Paul Hammant
 * @author Michael Rimov
 */
class LongConverter implements Converter<Long> {
  @Override
  public Long convert(final String paramValue) {
    return Long.valueOf(paramValue);
  }
}
