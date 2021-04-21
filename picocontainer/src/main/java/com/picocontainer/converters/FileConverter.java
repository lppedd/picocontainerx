package com.picocontainer.converters;

import java.io.File;

/**
 * Converts values to {@link File} data type objects
 *
 * @author Paul Hammant
 * @author Michael Rimov
 */
class FileConverter implements Converter<File> {
  @Override
  public File convert(final String paramValue) {
    return new File(paramValue);
  }
}
