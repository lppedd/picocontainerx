/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/
package com.picocontainer;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * TODO
 */
public abstract class TypeOf<T> {
  public static final TypeOf<Integer> INTEGER = TypeOf.fromClass(Integer.class);
  public static final TypeOf<Long> LONG = TypeOf.fromClass(Long.class);
  public static final TypeOf<Float> FLOAT = TypeOf.fromClass(Float.class);
  public static final TypeOf<Double> DOUBLE = TypeOf.fromClass(Double.class);
  public static final TypeOf<Boolean> BOOLEAN = TypeOf.fromClass(Boolean.class);
  public static final TypeOf<Character> CHARACTER = TypeOf.fromClass(Character.class);
  public static final TypeOf<Short> SHORT = TypeOf.fromClass(Short.class);
  public static final TypeOf<Byte> BYTE = TypeOf.fromClass(Byte.class);
  public static final TypeOf<Void> VOID = TypeOf.fromClass(Void.TYPE);

  private Type type;

  protected TypeOf() {
    type = getTypeFromSuperOfSubclass();
  }

  protected Type getTypeFromSuperOfSubclass() {
    final ParameterizedType type = (ParameterizedType) getClass().getGenericSuperclass();
    return type.getActualTypeArguments()[0];
  }

  public static <T> TypeOf<T> fromClass(final Class<T> type) {
    final TypeOf<T> typeOf = new ClassType<>();
    typeOf.type = type;
    return typeOf;
  }

  public static TypeOf<?> fromParameterizedType(final ParameterizedType parameterizedType) {
    final TypeOf<?> typeOf = new Dummy<>();
    typeOf.type = parameterizedType.getRawType();
    return typeOf;
  }

  public Type getType() {
    return type;
  }

  public boolean isPrimitive() {
    return type instanceof Class && ((Class<?>) type).isPrimitive();
  }

  public String getName() {
    return type instanceof Class ? ((Class<?>) type).getName() : type.toString();
  }

  public boolean isAssignableFrom(final Class<?> aClass) {
    return type instanceof Class && ((Class<?>) type).isAssignableFrom(aClass);
  }

  public boolean isAssignableTo(final Class<?> aClass) {
    return type instanceof Class && aClass.isAssignableFrom((Class<?>) type);
  }

  private static class ClassType<T> extends TypeOf<T> {
    @Nullable
    @Override
    protected Type getTypeFromSuperOfSubclass() {
      return null;
    }
  }

  private static class Dummy<T> extends TypeOf<T> {
    @Nullable
    @Override
    protected Type getTypeFromSuperOfSubclass() {
      return null;
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof TypeOf)) {
      return false;
    }

    final TypeOf<?> typeOf = (TypeOf<?>) o;
    return Objects.equals(type, typeOf.type);
  }

  @Override
  public int hashCode() {
    return type != null ? type.hashCode() : 0;
  }
}
