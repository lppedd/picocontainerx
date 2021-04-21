/*******************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved. *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD * style
 * license a copy of which has been included with this distribution in * the
 * LICENSE.txt file. * * Original code by *
 ******************************************************************************/
package com.picocontainer.behaviors;

import com.picocontainer.*;
import com.picocontainer.behaviors.ImplementationHiding.HiddenImplementation;
import com.picocontainer.injectors.AdaptingInjection;
import com.picocontainer.monitors.NullComponentMonitor;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Enumeration;
import java.util.Properties;

/**
 * TODO
 */
@SuppressWarnings({"serial", "RedundantInterfaceDeclaration"})
public class AbstractBehavior implements ComponentFactory, Serializable, Behavior {
  private ComponentFactory delegate;

  @Override
  public ComponentFactory wrap(final ComponentFactory delegate) {
    this.delegate = delegate;
    return this;
  }

  @Override
  public <T> ComponentAdapter<T> createComponentAdapter(
      final ComponentMonitor monitor,
      final LifecycleStrategy lifecycle,
      final Properties componentProps,
      final Object key,
      final Class<T> impl,
      final ConstructorParameters constructorParams,
      final FieldParameters[] fieldParams,
      final MethodParameters[] methodParams) {
    if (delegate == null) {
      delegate = new AdaptingInjection();
    }

    final ComponentAdapter<T> compAdapter =
        delegate.createComponentAdapter(
            monitor,
            lifecycle,
            componentProps,
            key,
            impl,
            constructorParams,
            fieldParams,
            methodParams
        );

    final boolean enableCircular = removePropertiesIfPresent(componentProps, Characteristics.ENABLE_CIRCULAR);
    return enableCircular && delegate instanceof InjectionType
        ? monitor.changedBehavior(new HiddenImplementation<>(compAdapter))
        : compAdapter;
  }

  @Override
  public void verify(final PicoContainer container) {
    if (delegate != null) {
      delegate.verify(container);
    }
  }

  @Override
  public void accept(final PicoVisitor visitor) {
    visitor.visitComponentFactory(this);
    if (delegate != null) {
      delegate.accept(visitor);
    }
  }

  @Override
  public <T> ComponentAdapter<T> addComponentAdapter(
      final ComponentMonitor monitor,
      final LifecycleStrategy lifecycle,
      final Properties componentProps,
      final ComponentAdapter<T> adapter) {
    return delegate != null && delegate instanceof Behavior
        ? ((Behavior) delegate).addComponentAdapter(monitor, lifecycle, componentProps, adapter)
        : adapter;
  }

  /**
   * Checks to see if one or more properties in the parameter {@code present} are available in
   * the {@code current} parameter.
   *
   * @param current the current set of properties to check
   * @param present the properties to check for.
   * @param compareValueToo If set to {@code true}, then we also check the <em>value</em> of the property to make
   * sure it matches. Some items in {@link Characteristics} have both a {@code true} and a {@code false} value.
   * @return {@code true} if the property is present <em>and</em> the value that exists equals
   * the value of {@code compareValueToo}
   */
  public static boolean arePropertiesPresent(final Properties current, final Properties present, final boolean compareValueToo) {
    final Enumeration<?> keys = present.keys();

    while (keys.hasMoreElements()) {
      final String key = (String) keys.nextElement();
      final String presentValue = present.getProperty(key);
      final String currentValue = current.getProperty(key);

      if (currentValue == null) {
        return false;
      }

      if (!presentValue.equals(currentValue) && compareValueToo) {
        return false;
      }
    }

    return true;
  }

  public static boolean removePropertiesIfPresent(final Properties current, final Properties present) {
    if (!arePropertiesPresent(current, present, true)) {
      return false;
    }

    final Enumeration<?> keys = present.keys();

    while (keys.hasMoreElements()) {
      final Object key = keys.nextElement();
      current.remove(key);
    }

    return true;
  }

  @Nullable
  public static String getAndRemovePropertiesIfPresentByKey(final Properties current, final Properties present) {
    if (!arePropertiesPresent(current, present, false)) {
      return null;
    }

    final Enumeration<?> keys = present.keys();
    String value = null;

    while (keys.hasMoreElements()) {
      final Object key = keys.nextElement();
      value = (String) current.remove(key);
    }

    return value;
  }

  protected void mergeProperties(final Properties into, final Properties from) {
    final Enumeration<?> e = from.propertyNames();

    while (e.hasMoreElements()) {
      final String s = (String) e.nextElement();
      into.setProperty(s, from.getProperty(s));
    }
  }

  @Override
  public void dispose() {
    if (delegate != null) {
      delegate.dispose();
    }
  }

  /**
   * <p>
   * {@link ComponentAdapter} which decorates another adapter.
   * </p>
   * <p>
   * This adapter supports a {@link ComponentMonitorStrategy} and will propagate change of monitor
   * to the delegate if the delegate itself support the monitor strategy.
   * </p>
   * <p>
   * This adapter also supports a {@link Behavior} and a {@link LifecycleStrategy} if the delegate does.
   * </p>
   *
   * @author Jon Tirsen
   * @author Aslak Hellesoy
   * @author Mauro Talevi
   */
  public abstract static class AbstractChangedBehavior<T>
      implements ChangedBehavior<T>,
                 ComponentMonitorStrategy,
                 LifecycleStrategy,
                 Serializable {
    protected final ComponentAdapter<T> delegate;

    public AbstractChangedBehavior(final ComponentAdapter<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public Object getComponentKey() {
      return delegate.getComponentKey();
    }

    @Override
    public Class<? extends T> getComponentImplementation() {
      return delegate.getComponentImplementation();
    }

    @Override
    public T getComponentInstance(final PicoContainer container, final Type into) {
      return delegate.getComponentInstance(container, into);
    }

    @Override
    public void verify(final PicoContainer container) {
      delegate.verify(container);
    }

    @Override
    public final ComponentAdapter<T> getDelegate() {
      return delegate;
    }

    @Override
    public final <U extends ComponentAdapter> U findAdapterOfType(final Class<U> adapterType) {
      return adapterType.isAssignableFrom(getClass())
          ? (U) this
          : delegate.findAdapterOfType(adapterType);
    }

    @Override
    public void accept(final PicoVisitor visitor) {
      visitor.visitComponentAdapter(this);
      delegate.accept(visitor);
    }

    /**
     * Delegates change of monitor if the delegate supports a component monitor strategy.
     */
    @Override
    public ComponentMonitor changeMonitor(final ComponentMonitor monitor) {
      return delegate instanceof ComponentMonitorStrategy
          ? ((ComponentMonitorStrategy) delegate).changeMonitor(monitor)
          : new NullComponentMonitor();
    }

    /**
     * Returns delegate's current monitor if the delegate supports a component monitor strategy.
     */
    @Override
    public ComponentMonitor currentMonitor() {
      if (delegate instanceof ComponentMonitorStrategy) {
        return ((ComponentMonitorStrategy) delegate).currentMonitor();
      }

      throw new PicoCompositionException("No component monitor found in delegate");
    }

    /**
     * Invokes delegate {@code start} method if the delegate is a {@link Behavior}.
     */
    @Override
    public void start(final PicoContainer container) {
      if (delegate instanceof ChangedBehavior) {
        ((ChangedBehavior<?>) delegate).start(container);
      }
    }

    /**
     * Invokes delegate {@code stop} method if the delegate is a {@link Behavior}.
     */
    @Override
    public void stop(final PicoContainer container) {
      if (delegate instanceof ChangedBehavior) {
        ((ChangedBehavior<?>) delegate).stop(container);
      }
    }

    /**
     * Invokes delegate {@code dispose} method if the delegate is a {@link Behavior}.
     */
    @Override
    public void dispose(final PicoContainer container) {
      if (delegate instanceof ChangedBehavior) {
        ((ChangedBehavior<?>) delegate).dispose(container);
      }
    }

    /**
     * Invokes delegate {@code hasLifecycle} method if the delegate is a {@link Behavior}.
     */
    @Override
    public boolean componentHasLifecycle() {
      return delegate instanceof ChangedBehavior && ((ChangedBehavior<?>) delegate).componentHasLifecycle();
    }

    @Override
    public boolean isStarted() {
      return delegate instanceof ChangedBehavior && ((ChangedBehavior<?>) delegate).isStarted();
    }

    /**
     * Invokes delegate {@code start} method if the delegate is a {@link LifecycleStrategy}.
     */
    @Override
    public void start(final Object component) {
      if (delegate instanceof LifecycleStrategy) {
        ((LifecycleStrategy) delegate).start(component);
      }
    }

    /**
     * Invokes delegate {@code stop} method if the delegate is a {@link LifecycleStrategy}.
     */
    @Override
    public void stop(final Object component) {
      if (delegate instanceof LifecycleStrategy) {
        ((LifecycleStrategy) delegate).stop(component);
      }
    }

    /**
     * Invokes delegate {@code dispose} method if the delegate is a {@link LifecycleStrategy}.
     */
    @Override
    public void dispose(final Object component) {
      if (delegate instanceof LifecycleStrategy) {
        ((LifecycleStrategy) delegate).dispose(component);
      }
    }

    /**
     * Invokes delegate {@code hasLifecycle(Class)} method if the delegate is a {@link LifecycleStrategy}.
     */
    @Override
    public boolean hasLifecycle(final Class<?> type) {
      return delegate instanceof LifecycleStrategy && ((LifecycleStrategy) delegate).hasLifecycle(type);
    }

    @Override
    public boolean isLazy(final ComponentAdapter<?> adapter) {
      return delegate instanceof LifecycleStrategy && ((LifecycleStrategy) delegate).isLazy(adapter);
    }

    @Override
    public String toString() {
      return getDescriptor() + ":" + delegate.toString();
    }
  }
}
