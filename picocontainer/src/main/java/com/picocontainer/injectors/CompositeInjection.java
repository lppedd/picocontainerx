/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/
package com.picocontainer.injectors;

import com.picocontainer.behaviors.AbstractBehavior;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * A Composite of other types on InjectionFactories - pass them into the varargs constructor.
 *
 * @author Paul Hammant
 */
@SuppressWarnings("serial")
public class CompositeInjection extends AbstractInjectionType {

    private final com.picocontainer.InjectionType[] injectionTypes;

    public CompositeInjection(final com.picocontainer.InjectionType... injectionTypes) {
        this.injectionTypes = injectionTypes;
    }

    public <T> com.picocontainer.ComponentAdapter<T> createComponentAdapter(final com.picocontainer.ComponentMonitor monitor,
                                                                            final com.picocontainer.LifecycleStrategy lifecycle,
                                                                            final Properties componentProps,
                                                                            final Object key,
                                                                            final Class<T> impl,
                                                                            final ConstructorParameters constructorParams,
                                                                            final FieldParameters[] fieldParams,
                                                                            final MethodParameters[] methodParams) throws com.picocontainer.PicoCompositionException {

        @SuppressWarnings("unchecked")
		com.picocontainer.Injector<T>[] injectors = new com.picocontainer.Injector[injectionTypes.length];

        for (int i = 0; i < injectionTypes.length; i++) {
            com.picocontainer.InjectionType injectionType = injectionTypes[i];
            injectors[i] = (com.picocontainer.Injector<T>) injectionType.createComponentAdapter(monitor,
                    lifecycle, componentProps, key, impl, constructorParams, fieldParams, methodParams);
        }

        boolean useNames = AbstractBehavior.arePropertiesPresent(componentProps, com.picocontainer.Characteristics.USE_NAMES, true);
        return wrapLifeCycle(monitor.newInjector(new CompositeInjector<T>(key, impl, monitor, useNames, injectors)), lifecycle);
    }

    public static class CompositeInjector<T> extends AbstractInjector<T> {

        private final com.picocontainer.Injector<T>[] injectors;

        public CompositeInjector(final Object key, final Class<?> impl, final com.picocontainer.ComponentMonitor monitor,
                                 final boolean useNames, final com.picocontainer.Injector<T>... injectors) {
            super(key, impl, monitor, useNames);
            this.injectors = injectors;
        }

        @Override
        public T getComponentInstance(final com.picocontainer.PicoContainer container, final Type into) throws com.picocontainer.PicoCompositionException {
	            T instance = null;

	            for (Class<?> eachSuperClass : this.getListOfSupertypesToDecorate(getComponentImplementation())) {
		            for (com.picocontainer.Injector<T> injector : injectors) {
		                if (instance == null) {
		                    instance = injector.getComponentInstance(container, NOTHING.class);
		                } else {
		                    injector.partiallyDecorateComponentInstance(container, into, instance, eachSuperClass);
		                }
		            }
	            }
	            return instance;
        }

        protected Class<?>[] getListOfSupertypesToDecorate(final Class<?> startClass) {
        	if (startClass == null) {
        		throw new NullPointerException("startClass");
        	}

        	List<Class<?>> result = new ArrayList<Class<?>>();

        	Class<?> current = startClass;
        	while (!Object.class.getName().equals(current.getName())) {
        		result.add(current);
        		current = current.getSuperclass();
        	}

        	//Needed for: com.picocontainer.injectors.AdaptingInjectionTestCase.testSingleUsecanBeInstantiatedByDefaultComponentAdapter()
        	if (result.size() == 0) {
        		result.add(Object.class);
        	}

        	//Start with base class, not derived class.
        	Collections.reverse(result);

        	return result.toArray(new Class[result.size()]);
        }


        /**
         * Performs a set of partial injections starting at the base class and working its
         * way down.
         * <p>{@inheritDoc}</p>
         * @return the object returned is the result of the last of the injectors delegated to
         */
        @Override
        public Object decorateComponentInstance(final com.picocontainer.PicoContainer container, final Type into, final T instance) {
        	Object result = null;
        	for (Class<?> eachSuperClass : this.getListOfSupertypesToDecorate(instance.getClass())) {
        		result = partiallyDecorateComponentInstance(container, into, instance, eachSuperClass);
        	}

        	return result;

        }

		@Override
		public Object partiallyDecorateComponentInstance(final com.picocontainer.PicoContainer container, final Type into, final T instance,
                                                     final Class<?> classFilter) {
			Object result = null;

            for (com.picocontainer.Injector<T> injector : injectors) {
            	result = injector.partiallyDecorateComponentInstance(container, into, instance, classFilter);
            }
            return result;
		}

        @Override
        public void verify(final com.picocontainer.PicoContainer container) throws com.picocontainer.PicoCompositionException {
            for (com.picocontainer.Injector<T> injector : injectors) {
                injector.verify(container);
            }
        }

        @Override
        public final void accept(final com.picocontainer.PicoVisitor visitor) {
            super.accept(visitor);
            for (com.picocontainer.Injector<T> injector : injectors) {
                injector.accept(visitor);
            }
        }

        @Override
        public String getDescriptor() {
            StringBuilder sb = new StringBuilder("CompositeInjector(");
            for (com.picocontainer.Injector<T> injector : injectors) {
                sb.append(injector.getDescriptor());
            }

            if (sb.charAt(sb.length() - 1) == '-') {
            	sb.deleteCharAt(sb.length()-1); // remove last dash
            }

            return sb.toString().replace("-", "+") + ")-";
        }

		@Override
		public com.picocontainer.ComponentMonitor changeMonitor(final com.picocontainer.ComponentMonitor monitor) {
			com.picocontainer.ComponentMonitor result = super.changeMonitor(monitor);
			for (com.picocontainer.Injector<?> eachInjector : injectors) {
				if (eachInjector instanceof com.picocontainer.ComponentMonitorStrategy) {
					((com.picocontainer.ComponentMonitorStrategy)eachInjector).changeMonitor(monitor);
				}
			}
			return result;
		}

    }
}
