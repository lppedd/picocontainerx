/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by Joerg Schaibe                                            *
 *****************************************************************************/
package com.picocontainer;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * TODO
 *
 * @author Paul Hammant
 */
@SuppressWarnings("serial")
public class Key<T> implements Serializable {
  private final Class<T> type;
  private final Class<? extends Annotation> annotation;

  public Key(final Class<T> type, final Class<? extends Annotation> annotation) {
    this.type = type;
    this.annotation = annotation;
  }

  public Class<T> getType() {
    return type;
  }

  public Class<? extends Annotation> getAnnotation() {
    return annotation;
  }

  @Override
  public String toString() {
    return type.getName() + ":" + annotation.getName();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final Key<?> key = (Key<?>) o;
    return annotation.equals(key.annotation) && type.equals(key.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, annotation);
  }

  public static <T> Key<T> annotatedKey(final Class<T> type, final Class<? extends Annotation> annotation) {
    return new Key<>(type, annotation);
  }
}
