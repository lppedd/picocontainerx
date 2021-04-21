package com.picocontainer.converters;

import com.picocontainer.Converters;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

/**
 * Does nothing.
 */
public class ConvertsNothing implements Converters {
  @Override
  public boolean canConvert(final Type type) {
    return false;
  }

  @Nullable
  @Override
  public Object convert(final String paramValue, final Type type) {
    return null;
  }
}
