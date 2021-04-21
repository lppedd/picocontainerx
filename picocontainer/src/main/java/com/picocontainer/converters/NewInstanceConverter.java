package com.picocontainer.converters;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Converts a value to an object via its single-String constructor.
 */
public class NewInstanceConverter implements Converter<Object> {
  private Constructor<?> c;

  public NewInstanceConverter(final Class<?> clazz) {
    try {
      c = clazz.getConstructor(String.class);
    } catch (final NoSuchMethodException ignored) {
      //
    }
  }

  @Nullable
  @Override
  public Object convert(final String paramValue) {
    if (c == null) {
      return null;
    }

    try {
      return c.newInstance(paramValue);
    } catch (final IllegalAccessException | InstantiationException | InvocationTargetException ignored) {
      //
    }

    return null;
  }
}
