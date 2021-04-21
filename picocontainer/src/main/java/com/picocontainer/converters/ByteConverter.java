package com.picocontainer.converters;

/**
 * Converts values to {@link Byte} data type objects.
 *
 * @author Paul Hammant
 * @author Michael Rimov
 */
class ByteConverter implements Converter<Byte> {
  @Override
  public Byte convert(final String paramValue) {
    return Byte.valueOf(paramValue);
  }
}
