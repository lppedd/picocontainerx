package com.picocontainer.injectors;

import com.picocontainer.*;
import com.picocontainer.lifecycle.NullLifecycleStrategy;
import com.picocontainer.monitors.NullComponentMonitor;

import java.io.Serializable;
import java.lang.reflect.Type;

@SuppressWarnings("serial")
public abstract class AbstractInjectionType implements InjectionType, Serializable {

    public void verify(final PicoContainer container) {
    }

    public final void accept(final PicoVisitor visitor) {
        visitor.visitComponentFactory(this);
    }


    protected <T> ComponentAdapter<T> wrapLifeCycle(final com.picocontainer.Injector<T> injector, final LifecycleStrategy lifecycle) {
        if (lifecycle instanceof NullLifecycleStrategy) {
            return injector;
        } else {
            return new LifecycleAdapter<T>(injector, lifecycle);
        }
    }
    

	public void dispose() {
		
	}    

	private static class LifecycleAdapter<T> implements com.picocontainer.Injector<T>, LifecycleStrategy, ComponentMonitorStrategy, Serializable {
        private final com.picocontainer.Injector<T> delegate;
        private final LifecycleStrategy lifecycle;

        public LifecycleAdapter(final com.picocontainer.Injector<T> delegate, final LifecycleStrategy lifecycle) {
            this.delegate = delegate;
            this.lifecycle = lifecycle;
        }

        public Object getComponentKey() {
            return delegate.getComponentKey();
        }

        public Class<? extends T> getComponentImplementation() {
            return delegate.getComponentImplementation();
        }

        public T getComponentInstance(final PicoContainer container, final Type into) throws PicoCompositionException {
            return delegate.getComponentInstance(container, into);
        }

        public void verify(final PicoContainer container) throws PicoCompositionException {
            delegate.verify(container);
        }

        public void accept(final PicoVisitor visitor) {
            delegate.accept(visitor);
        }

        public ComponentAdapter<T> getDelegate() {
            return delegate;
        }

        @SuppressWarnings("rawtypes")
		public <U extends ComponentAdapter> U findAdapterOfType(final Class<U> adapterType) {
            return delegate.findAdapterOfType(adapterType);
        }

        public String getDescriptor() {
            return "LifecycleAdapter";
        }

        @Override
		public String toString() {
            return getDescriptor() + ":" + delegate.toString();
        }

        public void start(final Object component) {
            lifecycle.start(component);
        }

        public void stop(final Object component) {
            lifecycle.stop(component);
        }

        public void dispose(final Object component) {
            lifecycle.dispose(component);
        }

        public boolean hasLifecycle(final Class<?> type) {
            return lifecycle.hasLifecycle(type);
        }

        public boolean isLazy(final ComponentAdapter<?> adapter) {
            return lifecycle.isLazy(adapter);
        }

        public ComponentMonitor changeMonitor(final ComponentMonitor monitor) {
            if (delegate instanceof ComponentMonitorStrategy) {
                return ((ComponentMonitorStrategy) delegate).changeMonitor(monitor);
            }
            
            return new NullComponentMonitor();
        }

        public ComponentMonitor currentMonitor() {
            if (delegate instanceof ComponentMonitorStrategy) {
                return ((ComponentMonitorStrategy) delegate).currentMonitor();
            }
            return null;
        }

        public Object decorateComponentInstance(final PicoContainer container, final Type into, final T instance) {
            return delegate.decorateComponentInstance(container, into, instance);
        }

		public Object partiallyDecorateComponentInstance(final PicoContainer container, final Type into, final T instance,
				final Class<?> superclassPortion) {
			return delegate.partiallyDecorateComponentInstance(container, into, instance, superclassPortion);
		}
    }

}
