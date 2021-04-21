package com.picocontainer.converters;

import com.picocontainer.PicoCompositionException;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Converts values to {@link URL} data type objects.
 *
 * @author Paul Hammant
 * @author Michael Rimov
 */
public class UrlConverter implements Converter<URL> {
  @Override
  public URL convert(final String paramValue) {
    try {
      return new URL(paramValue);
    } catch (final MalformedURLException e) {
      throw new PicoCompositionException(e);
    }
  }
}
