/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by Paul Hammant                                             *
 *****************************************************************************/
package com.picocontainer.containers;

import com.googlecode.jtype.Generic;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.Converters;
import com.picocontainer.Converting;
import com.picocontainer.NameBinding;
import com.picocontainer.PicoContainer;
import com.picocontainer.PicoVisitor;
import com.picocontainer.converters.ConvertsNothing;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Empty {@link PicoContainer} serving as recoil damper in situations where you
 * do not like to check whether container reference supplied to you
 * is {@code null} or not.
 *
 * @author Konstantin Pribluda
 */
@SuppressWarnings("serial")
public class EmptyPicoContainer implements PicoContainer, Converting, Serializable {
  @Nullable
  @Override
  public Object getComponent(final Object keyOrType) {
    return null;
  }

  @Nullable
  @Override
  public Object getComponentInto(final Object keyOrType, final Type into) {
    return null;
  }

  @Nullable
  @Override
  public <T> T getComponent(final Class<T> componentType) {
    return null;
  }

  @Nullable
  @Override
  public <T> T getComponent(final Generic<T> componentType) {
    return null;
  }

  @Nullable
  @Override
  public <T> T getComponentInto(final Class<T> componentType, final Type into) {
    return null;
  }

  @Nullable
  @Override
  public <T> T getComponentInto(final Generic<T> componentType, final Type into) {
    return null;
  }

  @Nullable
  @Override
  public <T> T getComponent(
      final Class<T> componentType,
      final Class<? extends Annotation> binding,
      final Type into) {
    return null;
  }

  @Nullable
  @Override
  public <T> T getComponent(
      final Class<T> componentType,
      final Class<? extends Annotation> binding) {
    return null;
  }

  @Nullable
  @Override
  public List<Object> getComponents() {
    return Collections.emptyList();
  }

  @Nullable
  @Override
  public PicoContainer getParent() {
    return null;
  }

  @Nullable
  @Override
  public ComponentAdapter<?> getComponentAdapter(final Object key) {
    return null;
  }

  @Nullable
  @Override
  public <T> ComponentAdapter<T> getComponentAdapter(
      final Class<T> componentType,
      final NameBinding nameBinding) {
    return null;
  }

  @Nullable
  @Override
  public <T> ComponentAdapter<T> getComponentAdapter(
      final Generic<T> componentType,
      final NameBinding nameBinding) {
    return null;
  }

  @Nullable
  @Override
  public <T> ComponentAdapter<T> getComponentAdapter(
      final Class<T> componentType,
      final Class<? extends Annotation> binding) {
    return null;
  }

  @Nullable
  @Override
  public <T> ComponentAdapter<T> getComponentAdapter(
      final Generic<T> componentType,
      final Class<? extends Annotation> binding) {
    return null;
  }

  @Override
  public Collection<ComponentAdapter<?>> getComponentAdapters() {
    return Collections.emptyList();
  }

  @Override
  public <T> List<ComponentAdapter<T>> getComponentAdapters(final Class<T> componentType) {
    return Collections.emptyList();
  }

  @Override
  public <T> List<ComponentAdapter<T>> getComponentAdapters(final Generic<T> componentType) {
    return Collections.emptyList();
  }

  @Override
  public <T> List<ComponentAdapter<T>> getComponentAdapters(
      final Class<T> componentType,
      final Class<? extends Annotation> binding) {
    return Collections.emptyList();
  }

  @Override
  public <T> List<ComponentAdapter<T>> getComponentAdapters(
      final Generic<T> componentType,
      final Class<? extends Annotation> binding) {
    return Collections.emptyList();
  }

  @Override
  public void accept(final PicoVisitor visitor) { }

  @Override
  public <T> List<T> getComponents(final Class<T> componentType) {
    return Collections.emptyList();
  }

  @Override
  public String toString() {
    return "(empty)";
  }

  @Override
  public Converters getConverters() {
    return new ConvertsNothing();
  }
}
