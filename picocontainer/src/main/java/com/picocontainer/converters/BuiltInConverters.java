package com.picocontainer.converters;

import com.picocontainer.Converters;
import com.picocontainer.DefaultPicoContainer;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Provides some built-in converters used by {@link DefaultPicoContainer}.
 * It supports by default primitive types (and boxed equivalents),
 * {@link File} and {@link URL} types.
 * </p>
 * <p>
 * Built-in converters can be changed by extending the class and overriding
 * the method {@link #addBuiltInConverters()}.
 * </p>
 */
@SuppressWarnings("serial")
public class BuiltInConverters implements Converters, Serializable {
  private final Map<Type, Converter<?>> converters = new HashMap<>();

  public BuiltInConverters() {
    addBuiltInConverters();
  }

  protected void addBuiltInConverters() {
    addMultiTypeConverter(new IntegerConverter(), Integer.class, Integer.TYPE);
    addMultiTypeConverter(new DoubleConverter(), Double.class, Double.TYPE);
    addMultiTypeConverter(new BooleanConverter(), Boolean.class, Boolean.TYPE);
    addMultiTypeConverter(new LongConverter(), Long.class, Long.TYPE);
    addMultiTypeConverter(new FloatConverter(), Float.class, Float.TYPE);
    addMultiTypeConverter(new CharacterConverter(), Character.class, Character.TYPE);
    addMultiTypeConverter(new ByteConverter(), Byte.class, Byte.TYPE);
    addMultiTypeConverter(new ShortConverter(), Short.class, Short.TYPE);
    addConverter(new FileConverter(), File.class);
    addConverter(new UrlConverter(), URL.class);
  }

  private void addMultiTypeConverter(final Converter<?> converter, final Class<?>... types) {
    for (final Class<?> type : types) {
      addConverter(converter, type);
    }
  }

  protected void addConverter(final Converter<?> converter, final Class<?> key) {
    converters.put(key, converter);
  }

  @Override
  public boolean canConvert(final Type type) {
    return converters.containsKey(type);
  }

  @Nullable
  @Override
  public Object convert(final String paramValue, final Type type) {
    final Converter<?> converter = converters.get(type);
    return converter == null ? null : converter.convert(paramValue);
  }
}
