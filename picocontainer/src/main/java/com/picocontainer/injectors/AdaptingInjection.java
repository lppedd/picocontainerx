/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Idea by Rachel Davies, Original code by Aslak Hellesoy and Paul Hammant   *
 *****************************************************************************/

package com.picocontainer.injectors;

import com.picocontainer.*;
import com.picocontainer.annotations.Inject;
import com.picocontainer.behaviors.AbstractBehavior;
import com.picocontainer.parameters.AccessibleObjectParameterSet;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

import static com.picocontainer.injectors.AnnotatedMethodInjection.getInjectionAnnotation;

/**
 * Creates injector instances, depending on the injection characteristics of the component class.
 * It will attempt to create a component adapter with - in order of priority:
 * <ol>
 *  <li>Annotated field injection: if annotation {@link com.picocontainer.annotations.Inject} is found for field</li>
 *  <li>Annotated method injection: if annotation {@link com.picocontainer.annotations.Inject} is found for method</li>
 *  <li>Setter injection: if {@link Characteristics.SDI} is found</li>
 *  <li>Method injection: if {@link Characteristics.METHOD_INJECTION} if found</li>
 *  <li>Constructor injection (the default, must find {@link Characteristics.CDI})</li>
 * </ol>
 *
 * @author Paul Hammant
 * @author Mauro Talevi
 * @see AnnotatedFieldInjection
 * @see AnnotatedMethodInjection
 * @see SetterInjection
 * @see MethodInjection
 * @see ConstructorInjection
 */
@SuppressWarnings("serial")
public class AdaptingInjection extends AbstractInjectionType {

	private final ConstructorInjection constructorInjection;

	private final MethodInjection methodInjection;

	private final SetterInjection setterInjection;

	private final AnnotatedMethodInjection annotatedMethodInjection;

	private final AnnotatedFieldInjection annotatedFieldInjection;

	public AdaptingInjection() {
		constructorInjection = new Jsr330ConstructorInjection();
		methodInjection = new MethodInjection();
		setterInjection = new SetterInjection();
		annotatedMethodInjection = new AnnotatedMethodInjection();
		annotatedFieldInjection = new AnnotatedFieldInjection();
	}



	public <T> ComponentAdapter<T> createComponentAdapter(final ComponentMonitor monitor, final LifecycleStrategy lifecycle,
                                                   final Properties componentProps, final Object key, final Class<T> impl,
                                                   final ConstructorParameters constructorParams, final FieldParameters[] fieldParams, final MethodParameters[] methodParams) throws PicoCompositionException {


		verifyNamedParameters(impl, fieldParams, methodParams);

		ArrayList<InjectionType> injectors = new ArrayList<InjectionType>();

        InjectionType componentAdapter = null;

        componentProps.putAll(Characteristics.ALLOW_UNUSED_PARAMETERS);

        //Pico Style Injections
        //Must specifically set Characteristics.SDI
        componentAdapter = setterInjectionAdapter(componentProps);
        if (componentAdapter != null) {
        	injectors.add(componentAdapter);
        	componentAdapter = null;
        }

        //Must specifically set Characteristics.METHOD_INJECTION
        componentAdapter = methodInjectionAdapter(componentProps);
        if (componentAdapter != null) {
        	injectors.add(componentAdapter);
        	componentAdapter = null;
        }



        //JSR 330 injection
        //Turned on by default
        componentAdapter = methodAnnotatedInjectionAdapter(impl);
        if (componentAdapter != null) {
        	injectors.add(componentAdapter);
        	componentAdapter = null;
        }


        //JSR 330 injection
        //Turned on by default
        componentAdapter = fieldAnnotatedInjectionAdapter(impl);
        if (componentAdapter != null) {
        	injectors.add(componentAdapter);
        	componentAdapter = null;
        }

        injectors.add(defaultInjectionAdapter(componentProps));



        Collections.reverse(injectors);
        //return defaultInjectionAdapter(componentProps, monitor, lifecycle, key, impl, parameters);
        InjectionType[] aArray = injectors.toArray(new InjectionType[injectors.size()]);


        //Wrap the static injection behavior.
        ComponentFactory caf = new CompositeInjection(aArray);

        ComponentAdapter<T> result =  caf
        	.createComponentAdapter(monitor, lifecycle,
        			componentProps, key, impl,
        			constructorParams, fieldParams, methodParams);


        AbstractBehavior.removePropertiesIfPresent(componentProps, Characteristics.ALLOW_UNUSED_PARAMETERS);
        return result;
    }


	/**
	 * Quick
	 * @throws PicoCompositionException if there's a
	 * @param parameters
	 * @todo This only verifies that the names exist, it doesn't check for name hiding from base class to
	 * sub class.
	 */
    private void verifyNamedParameters(final Class<?> impl, final FieldParameters[] parameters, final MethodParameters[] methodParameters) {
    	if (parameters == null) {
    		return;
    	}
    	Set<String> allNames = AccessController.doPrivileged(new PrivilegedAction<Set<String>>() {

			public Set<String> run() {
				HashSet<String> result = new HashSet<String>(30);
				Class<?> currentImpl = impl;
				while (!Object.class.getName().equals(currentImpl.getName())) {

					for (Field eachField : currentImpl.getDeclaredFields()) {
						result.add(eachField.getName());
					}

					for (Method eachMethod : currentImpl.getDeclaredMethods()) {
						result.add(eachMethod.getName());
					}

					currentImpl = currentImpl.getSuperclass();
				}
				return result;
			}});


    	for (FieldParameters eachParam : parameters) {
			if (!allNames.contains(eachParam.getName())) {
				throwCompositionException(impl, eachParam);
			}
    	}


    	for (MethodParameters eachParam : methodParameters) {
			if (!allNames.contains(eachParam.getName())) {
				throwCompositionException(impl, eachParam);
			}
    	}
	}


	private void throwCompositionException(final Class<?> impl, final AccessibleObjectParameterSet eachParam) {
		throw new PicoCompositionException("Cannot locate field or method '"
					+ eachParam.getName()
					+ "' in type "
					+ impl
					+ ". \n\tParameter in error: "
					+ eachParam);
	}




	private  <T> InjectionType defaultInjectionAdapter(final Properties componentProps) {
        AbstractBehavior.removePropertiesIfPresent(componentProps, Characteristics.CDI);
        return constructorInjection;
    }

    private <T> InjectionType setterInjectionAdapter(final Properties componentProps) {
        if (AbstractBehavior.removePropertiesIfPresent(componentProps, Characteristics.SDI)) {
        	return setterInjection;
        }
        return null;
    }

    private <T> InjectionType methodInjectionAdapter(final Properties componentProps) {
        if (AbstractBehavior.removePropertiesIfPresent(componentProps, Characteristics.METHOD_INJECTION)) {
            return methodInjection;
        }
        return null;
    }


    private <T> InjectionType methodAnnotatedInjectionAdapter(final Class<T> impl) {
        if (injectionMethodAnnotated(impl)) {
        	return annotatedMethodInjection;
        }
        return null;
    }

    private <T> InjectionType fieldAnnotatedInjectionAdapter(final Class<T> impl) {
        if (injectionFieldAnnotated(impl)) {
        	return this.annotatedFieldInjection;
        }

        return null;
    }

    private boolean injectionMethodAnnotated(final Class<?> impl) {
        return  AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @SuppressWarnings("synthetic-access")
            public Boolean run() {
            	InjectableMethodSelector methodSelector = new InjectableMethodSelector(Inject.class);
            	return (methodSelector.retreiveAllInjectableMethods(impl).size() > 0);
            }
        });
    }

    private boolean injectionFieldAnnotated(final Class<?> impl) {
        return (Boolean) AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @SuppressWarnings("synthetic-access")
            public Object run() {
                if (impl.isInterface()) {
                    return false;
                }
                Class impl2 = impl;
                while (impl2 != Object.class) {
                    if (injectionAnnotated(impl2.getDeclaredFields())) {
                        return true;
                    }
                    impl2 = impl2.getSuperclass();
                }
                return false;
            }
        });
    }

    private boolean injectionAnnotated(final AccessibleObject[] objects) {
        for (AccessibleObject object : objects) {
            if (object.getAnnotation(Inject.class) != null
                    || object.getAnnotation(getInjectionAnnotation("javax.inject.Inject")) != null) {
                return true;
            }
        }
        return false;
    }

}
