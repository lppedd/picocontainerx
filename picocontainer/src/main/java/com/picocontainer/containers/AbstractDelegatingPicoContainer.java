package com.picocontainer.containers;

import com.googlecode.jtype.Generic;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentAdapter.NOTHING;
import com.picocontainer.Converters;
import com.picocontainer.Converting;
import com.picocontainer.NameBinding;
import com.picocontainer.PicoContainer;
import com.picocontainer.PicoVisitor;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/**
 * Abstract base class for <i>immutable<i> delegation to a {@link PicoContainer}.
 *
 * @author Konstantin Pribluda
 */
@SuppressWarnings("serial")
public abstract class AbstractDelegatingPicoContainer implements PicoContainer, Converting, Serializable {
  private PicoContainer delegate;

  public AbstractDelegatingPicoContainer(final PicoContainer delegate) {
    if (delegate == null) {
      throw new NullPointerException("PicoContainer delegate must not be null");
    }

    this.delegate = delegate;
  }

  @Override
  public final void accept(final PicoVisitor visitor) {
    visitor.visitContainer(this);
    delegate.accept(visitor);
  }

  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  @Override
  public boolean equals(final Object obj) {
    // required to make it pass on both jdk 1.3 and jdk 1.4. Btw, what about
    // overriding hashCode()? (AH)
    return this == obj || delegate.equals(obj);
  }

  @Override
  public <T> T getComponentInto(final Class<T> componentType, final Type into) {
    return componentType.cast(getComponentInto((Object) componentType, into));
  }

  @Override
  public <T> T getComponentInto(final Generic<T> componentType, final Type into) {
    return (T) getComponentInto((Object) componentType, into);
  }

  @Override
  public <T> T getComponent(final Class<T> componentType, final Class<? extends Annotation> binding, final Type into) {
    return delegate.getComponent(componentType, binding, into);
  }

  @Override
  public <T> T getComponent(final Class<T> componentType, final Class<? extends Annotation> binding) {
    return delegate.getComponent(componentType, binding);
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
    return getComponentInto(Generic.get(componentType), NOTHING.class);
  }

  @Override
  public <T> T getComponent(final Generic<T> componentType) {
    return delegate.getComponent(componentType);
  }

  @Override
  public <T> ComponentAdapter<T> getComponentAdapter(
      final Class<T> componentType,
      final NameBinding componentNameBinding) {
    return delegate.getComponentAdapter(Generic.get(componentType), componentNameBinding);
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
  public ComponentAdapter<?> getComponentAdapter(final Object key) {
    return delegate.getComponentAdapter(key);
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
  public List<Object> getComponents() {
    return delegate.getComponents();
  }

  @Override
  public <T> List<T> getComponents(final Class<T> type) {
    return delegate.getComponents(type);
  }

  public PicoContainer getDelegate() {
    return delegate;
  }

  /**
   * Allows for swapping of delegate object to allow for temp proxies.
   *
   * @param newDelegate
   * @return the old delegate instance.
   */
  protected PicoContainer swapDelegate(final PicoContainer newDelegate) {
    if (newDelegate == null) {
      throw new NullPointerException("newDelegate");
    }

    final PicoContainer oldDelegate = delegate;
    delegate = newDelegate;
    return oldDelegate;
  }

  @Override
  public PicoContainer getParent() {
    return delegate.getParent();
  }

  @Override
  public String toString() {
    return "[Delegate]:" + delegate.toString();
  }

  @Override
  @Nullable
  public Converters getConverters() {
    // noinspection SimplifiableIfStatement
    if (delegate instanceof Converting) {
      return ((Converting) delegate).getConverters();
    }

    return null;
  }
}
