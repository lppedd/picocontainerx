package com.picocontainer.injectors;

import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.ComponentMonitorStrategy;
import com.picocontainer.InjectionType;
import com.picocontainer.LifecycleStrategy;
import com.picocontainer.PicoContainer;
import com.picocontainer.PicoVisitor;
import com.picocontainer.lifecycle.NullLifecycleStrategy;
import com.picocontainer.monitors.NullComponentMonitor;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Type;

/**
 * TODO
 */
@SuppressWarnings("serial")
public abstract class AbstractInjectionType implements InjectionType, Serializable {
  @Override
  public void verify(final PicoContainer container) {}

  @Override
  public final void accept(final PicoVisitor visitor) {
    visitor.visitComponentFactory(this);
  }

  protected <T> ComponentAdapter<T> wrapLifeCycle(
      final com.picocontainer.Injector<T> injector,
      final LifecycleStrategy lifecycle) {
    // noinspection SimplifiableIfStatement
    if (lifecycle instanceof NullLifecycleStrategy) {
      return injector;
    }

    return new LifecycleAdapter<T>(injector, lifecycle);
  }

  @Override
  public void dispose() {}

  private static class LifecycleAdapter<T>
      implements com.picocontainer.Injector<T>,
                 LifecycleStrategy,
                 ComponentMonitorStrategy,
                 Serializable {
    private final com.picocontainer.Injector<T> delegate;
    private final LifecycleStrategy lifecycle;

    LifecycleAdapter(final com.picocontainer.Injector<T> delegate, final LifecycleStrategy lifecycle) {
      this.delegate = delegate;
      this.lifecycle = lifecycle;
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
    public void accept(final PicoVisitor visitor) {
      delegate.accept(visitor);
    }

    @Override
    public ComponentAdapter<T> getDelegate() {
      return delegate;
    }

    @Override
    public <U extends ComponentAdapter> U findAdapterOfType(final Class<U> adapterType) {
      return delegate.findAdapterOfType(adapterType);
    }

    @Override
    public String getDescriptor() {
      return "LifecycleAdapter";
    }

    @Override
    public String toString() {
      return getDescriptor() + ":" + delegate.toString();
    }

    @Override
    public void start(final Object component) {
      lifecycle.start(component);
    }

    @Override
    public void stop(final Object component) {
      lifecycle.stop(component);
    }

    @Override
    public void dispose(final Object component) {
      lifecycle.dispose(component);
    }

    @Override
    public boolean hasLifecycle(final Class<?> type) {
      return lifecycle.hasLifecycle(type);
    }

    @Override
    public boolean isLazy(final ComponentAdapter<?> adapter) {
      return lifecycle.isLazy(adapter);
    }

    @Override
    public ComponentMonitor changeMonitor(final ComponentMonitor monitor) {
      // noinspection SimplifiableIfStatement
      if (delegate instanceof ComponentMonitorStrategy) {
        return ((ComponentMonitorStrategy) delegate).changeMonitor(monitor);
      }

      return new NullComponentMonitor();
    }

    @Nullable
    @Override
    public ComponentMonitor currentMonitor() {
      // noinspection SimplifiableIfStatement
      if (delegate instanceof ComponentMonitorStrategy) {
        return ((ComponentMonitorStrategy) delegate).currentMonitor();
      }

      return null;
    }

    @Override
    public Object decorateComponentInstance(final PicoContainer container, final Type into, final T instance) {
      return delegate.decorateComponentInstance(container, into, instance);
    }

    @Override
    public Object partiallyDecorateComponentInstance(
        final PicoContainer container,
        final Type into,
        final T instance,
        final Class<?> superclassPortion) {
      return delegate.partiallyDecorateComponentInstance(container, into, instance, superclassPortion);
    }
  }
}
