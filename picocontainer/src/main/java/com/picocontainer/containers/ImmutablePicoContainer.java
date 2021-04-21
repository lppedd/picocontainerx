/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer.containers;

import com.googlecode.jtype.Generic;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentAdapter.NOTHING;
import com.picocontainer.Converters;
import com.picocontainer.Converting;
import com.picocontainer.NameBinding;
import com.picocontainer.PicoContainer;
import com.picocontainer.PicoVisitor;
import com.picocontainer.converters.ConvertsNothing;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/**
 * Wraps a {@link PicoContainer} to achieve immutability.
 * Typically used to mock a parent container.
 *
 * @author Konstantin Pribluda
 */
@SuppressWarnings("serial")
public final class ImmutablePicoContainer implements PicoContainer, Converting, Serializable {
  private final PicoContainer delegate;

  public ImmutablePicoContainer(final PicoContainer delegate) {
    if (delegate == null) {
      throw new NullPointerException();
    }

    this.delegate = delegate;
  }

  @Override
  public Object getComponent(final Object keyOrType) {
    return getComponentInto(keyOrType, NOTHING.class);
  }

  @Override
  public Object getComponentInto(final Object keyOrType, final Type into) {
    return delegate.getComponentInto(keyOrType, into);
  }

  @Override
  public <T> T getComponent(final Class<T> componentType) {
    return delegate.getComponent(Generic.get(componentType));
  }

  @Override
  public <T> T getComponent(final Generic<T> componentType) {
    return delegate.getComponent(componentType);
  }

  @Override
  public <T> T getComponentInto(final Class<T> componentType, final Type into) {
    return delegate.getComponentInto(componentType, into);
  }

  @Override
  public <T> T getComponentInto(final Generic<T> componentType, final Type into) {
    return delegate.getComponentInto(componentType, into);
  }

  @Override
  public <T> T getComponent(
      final Class<T> componentType,
      final Class<? extends Annotation> binding,
      final Type into) {
    return delegate.getComponent(componentType, binding, into);
  }

  @Override
  public <T> T getComponent(final Class<T> componentType, final Class<? extends Annotation> binding) {
    return delegate.getComponent(componentType, binding);
  }

  @Override
  public List<Object> getComponents() {
    return delegate.getComponents();
  }

  @Override
  public PicoContainer getParent() {
    return delegate.getParent();
  }

  @Override
  public ComponentAdapter<?> getComponentAdapter(final Object key) {
    return delegate.getComponentAdapter(key);
  }

  @Override
  public <T> ComponentAdapter<T> getComponentAdapter(
      final Class<T> componentType,
      final NameBinding nameBinding) {
    return delegate.getComponentAdapter(Generic.get(componentType), nameBinding);
  }

  @Override
  public <T> ComponentAdapter<T> getComponentAdapter(
      final Generic<T> componentType,
      final NameBinding componentNameBinding) {
    return delegate.getComponentAdapter(componentType, componentNameBinding);
  }

  @Override
  public <T> ComponentAdapter<T> getComponentAdapter(
      final Class<T> componentType,
      final Class<? extends Annotation> binding) {
    return delegate.getComponentAdapter(Generic.get(componentType), binding);
  }

  @Override
  public <T> ComponentAdapter<T> getComponentAdapter(
      final Generic<T> componentType,
      final Class<? extends Annotation> binding) {
    return delegate.getComponentAdapter(componentType, binding);
  }

  @Override
  public Collection<ComponentAdapter<?>> getComponentAdapters() {
    return delegate.getComponentAdapters();
  }

  @Override
  public <T> List<ComponentAdapter<T>> getComponentAdapters(final Class<T> componentType) {
    return delegate.getComponentAdapters(Generic.get(componentType));
  }

  @Override
  public <T> List<ComponentAdapter<T>> getComponentAdapters(final Generic<T> componentType) {
    return delegate.getComponentAdapters(componentType);
  }

  @Override
  public <T> List<ComponentAdapter<T>> getComponentAdapters(
      final Class<T> componentType,
      final Class<? extends Annotation> binding) {
    return delegate.getComponentAdapters(Generic.get(componentType), binding);
  }

  @Override
  public <T> List<ComponentAdapter<T>> getComponentAdapters(
      final Generic<T> componentType,
      final Class<? extends Annotation> binding) {
    return delegate.getComponentAdapters(componentType, binding);
  }

  @Override
  public <T> List<T> getComponents(final Class<T> componentType) {
    return delegate.getComponents(componentType);
  }

  @Override
  public final void accept(final PicoVisitor visitor) {
    delegate.accept(visitor);
  }

  @Override
  public boolean equals(final Object obj) {
    return obj == this
        || obj != null && obj == delegate
        || obj instanceof ImmutablePicoContainer && ((ImmutablePicoContainer) obj).delegate == delegate;
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public String toString() {
    return "[Immutable]:" + delegate;
  }

  @Override
  public Converters getConverters() {
    // noinspection SimplifiableIfStatement
    if (delegate instanceof Converting) {
      return ((Converting) delegate).getConverters();
    }

    return new ConvertsNothing();
  }
}
