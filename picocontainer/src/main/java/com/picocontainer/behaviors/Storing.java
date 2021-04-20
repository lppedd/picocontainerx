/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Idea by Rachel Davies, Original code by Aslak Hellesoy and Paul Hammant   *
 *****************************************************************************/

package com.picocontainer.behaviors;

import com.picocontainer.*;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;
import com.picocontainer.references.ThreadLocalMapObjectReference;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Paul Hammant
 */
@SuppressWarnings("serial")
public class Storing extends AbstractBehavior {

    @SuppressWarnings("rawtypes")
	private StoreThreadLocal mapThreadLocalObjectReference = new StoreThreadLocal();
    
	@Override
	public void dispose() {
		try {
			super.dispose();
		} finally {
			if (mapThreadLocalObjectReference != null) {
				mapThreadLocalObjectReference.remove();

			}
		}
	}

	protected <T> StoreThreadLocal<T> getThreadLocalStore() {
	
		return mapThreadLocalObjectReference;
	}
	
    @Override
	public <T> ComponentAdapter<T>  createComponentAdapter(final ComponentMonitor monitor, final LifecycleStrategy lifecycle, final Properties componentProps,
                                       final Object key, final Class<T> impl, final ConstructorParameters constructorParams, final FieldParameters[] fieldParams, final MethodParameters[] methodParams) throws PicoCompositionException {
        if (removePropertiesIfPresent(componentProps, Characteristics.NO_CACHE)) {
            return super.createComponentAdapter(monitor, lifecycle, componentProps, key, impl, constructorParams, fieldParams, methodParams);
        }
        removePropertiesIfPresent(componentProps, Characteristics.CACHE);
        ThreadLocalMapObjectReference threadLocalMapObjectReference = new ThreadLocalMapObjectReference(getThreadLocalStore(), key);

        return monitor.changedBehavior(new Stored<T>(
                super.createComponentAdapter(monitor, lifecycle, componentProps, key, impl, constructorParams, fieldParams, methodParams), threadLocalMapObjectReference));

    }

    @Override
	public <T> ComponentAdapter<T> addComponentAdapter(final ComponentMonitor monitor, final LifecycleStrategy lifecycle,
                                    final Properties componentProps, final ComponentAdapter<T> adapter) {
        if (removePropertiesIfPresent(componentProps, Characteristics.NO_CACHE)) {
            return super.addComponentAdapter(monitor, lifecycle, componentProps, adapter);
        }
        removePropertiesIfPresent(componentProps, Characteristics.CACHE);

        return monitor.changedBehavior(new Stored<T>(super.addComponentAdapter(monitor, lifecycle, componentProps, adapter),
                          new ThreadLocalMapObjectReference(getThreadLocalStore(), adapter.getComponentKey())));
    }

    public StoreWrapper getCacheForThread() {
        StoreWrapper wrappedMap = new StoreWrapper();
        wrappedMap.wrapped = (Map)getThreadLocalStore().get();
        return wrappedMap;
    }

    public void putCacheForThread(final StoreWrapper wrappedMap) {
    	getThreadLocalStore().set(wrappedMap.wrapped);
    }

    public StoreWrapper resetCacheForThread() {
        Map map = new HashMap();
        getThreadLocalStore().set(map);
        StoreWrapper storeWrapper = new StoreWrapper();
        storeWrapper.wrapped = map;
        return storeWrapper;
    }

    public void invalidateCacheForThread() {
    	getThreadLocalStore().set(Collections.emptyMap());
    }

    public int getCacheSize() {
        return ((Map)getThreadLocalStore().get()).size();
    }
    
    private void writeObject(final java.io.ObjectOutputStream stream)
            throws IOException {
    	stream.defaultWriteObject();
    }

    private void readObject(final java.io.ObjectInputStream stream)
            throws IOException, ClassNotFoundException {

    	stream.defaultReadObject();
    	mapThreadLocalObjectReference = new StoreThreadLocal();;
    }    

    public static class StoreThreadLocal<T> extends ThreadLocal<Map<Object, T>> implements Serializable {
        @Override
		protected Map<Object, T> initialValue() {
            return new HashMap<Object, T>();
        }
    }
    
    
    

    public static class StoreWrapper implements Serializable {
        private Map wrapped;
    }

    public static class Stored<T> extends AbstractChangedBehavior<T> {

        private final ObjectReference<Instance<T>> instanceReference;
        private final ComponentLifecycle lifecycleDelegate;

        public Stored(final ComponentAdapter<T> delegate, final ObjectReference<Instance<T>> reference) {
            super(delegate);
            instanceReference = reference;
            this.lifecycleDelegate = hasLifecycle(delegate)
                    ? new RealComponentLifecycle<T>() : new NoComponentLifecycle<T>();
        }

        private void guardInstRef() {
            if (instanceReference.get() == null) {
                instanceReference.set(new Instance<T>());
            }
        }

        @Override
		public boolean componentHasLifecycle() {
            return lifecycleDelegate.componentHasLifecycle();
        }

        /**
         * Disposes the cached component instance
         * {@inheritDoc}
         */
        @Override
		public void dispose(final PicoContainer container) {
            lifecycleDelegate.dispose(container);
        }

        /**
         * Retrieves the stored reference.  May be null if it has
         * never been set, or possibly if the reference has been
         * flushed.
         *
         * @return the stored object or null.
         */
        public T getStoredObject() {
            guardInstRef();
            return instanceReference.get().instance;
        }

        /**
         * Flushes the cache.
         * If the component instance is started is will stop and dispose it before
         * flushing the cache.
         */
        public void flush() {
            Instance<T> inst = instanceReference.get();
            if (inst != null) {
                Object instance = inst.instance;
                if (instance != null && instanceReference.get().started) {
                    stop(instance);
                    dispose(instance);
                }
                instanceReference.set(null);
            }
        }

        @Override
		public T getComponentInstance(final PicoContainer container, final Type into) throws PicoCompositionException {
            guardInstRef();
            T instance = instanceReference.get().instance;
            if (instance == null) {
                instance = super.getComponentInstance(container, into);
                instanceReference.get().instance = instance;
            }
            return instance;
        }

        public String getDescriptor() {
            return "Stored" + getLifecycleDescriptor();
        }

        protected String getLifecycleDescriptor() {
            return (lifecycleDelegate.componentHasLifecycle() ? "+Lifecycle" : "");
        }

        /**
         * Starts the cached component instance
         * {@inheritDoc}
         */
        @Override
		public void start(final PicoContainer container) {
            lifecycleDelegate.start(container);
        }

        /**
         * Stops the cached component instance
         * {@inheritDoc}
         */
        @Override
		public void stop(final PicoContainer container) {
            lifecycleDelegate.stop(container);
        }

        @Override
		public boolean isStarted() {
            return lifecycleDelegate.isStarted();
        }

        private class RealComponentLifecycle<T> implements ComponentLifecycle<T>, Serializable {

            public void start(final PicoContainer container) {
                guardInstRef();
                guardAlreadyDisposed();
                guardStartState(true, "already started");
                // Lazily make the component if applicable
                Stored.this.start(getComponentInstance(container, NOTHING.class));
                instanceReference.get().started = true;
            }

            public void stop(final PicoContainer container) {
                guardInstRef();
                guardAlreadyDisposed();
                guardNotInstantiated();
                guardStartState(false, "not started");
                Stored.this.stop(instanceReference.get().instance);
                instanceReference.get().started = false;

            }

            public void dispose(final PicoContainer container) {
                guardInstRef();
                Instance<?> instance = instanceReference.get();
                if (instance.instance != null) {
                    guardAlreadyDisposed();
                    Stored.this.dispose(instance.instance);
                    instance.disposed = true;
                }
            }


            private void guardNotInstantiated() {
                if (instanceReference.get().instance == null) {
					throw new IllegalStateException("'" + getComponentKey() + "' not instantiated");
				}
            }

            private void guardStartState(final boolean unexpectedStartState, final String message) {
                if (instanceReference.get().started == unexpectedStartState) {
					throw new IllegalStateException("'" + getComponentKey() + "' " + message);
				}
            }

            private void guardAlreadyDisposed() {
                if (instanceReference.get().disposed) {
					throw new IllegalStateException("'" + getComponentKey() + "' already disposed");
				}
            }

            public boolean componentHasLifecycle() {
                return true;
            }

            public boolean isStarted() {
                guardInstRef();
                return instanceReference.get().started;
            }
        }

        private static class NoComponentLifecycle<T> implements ComponentLifecycle<T>, Serializable {
            public void start(final PicoContainer container) {
            }

            public void stop(final PicoContainer container) {
            }

            public void dispose(final PicoContainer container) {
            }

            public boolean componentHasLifecycle() {
                return false;
            }

            public boolean isStarted() {
                return false;
            }
        }

        private static boolean hasLifecycle(final ComponentAdapter delegate) {
            return delegate instanceof LifecycleStrategy
                    && ((LifecycleStrategy) delegate).hasLifecycle(delegate.getComponentImplementation());
        }

        public static class Instance<T> implements Serializable {
            private T instance;
            protected boolean started;
            protected boolean disposed;
        }

    }

}
