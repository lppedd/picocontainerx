/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/
package com.picocontainer.injectors;

import com.picocontainer.ComponentAdapter;
import com.picocontainer.PicoCompositionException;
import com.picocontainer.PicoContainer;
import com.picocontainer.PicoVisitor;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * An Injector which provides an custom instance in a factory style
 * </p>
 *
 * @author Paul Hammant
 */
public abstract class FactoryInjector<T> implements com.picocontainer.Injector<T> {

    private Class key;

    public FactoryInjector() throws PicoCompositionException {
        key = getTypeArguments(FactoryInjector.class, getClass()).get(0);
        if (key == null) {
            key = CantWorkItOut.class;
        }
    }

    public FactoryInjector(final Class<T> key) {
        this.key = key;
    }

    // from http://www.artima.com/weblogs/viewpost.jsp?thread=208860
    public static Class<?> getClass(final Type type) {
        if (type instanceof Class) {
            return (Class) type;
        } else if (type instanceof ParameterizedType) {
            return getClass(((ParameterizedType) type).getRawType());
        } else if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            Class<?> componentClass = getClass(componentType);
            if (componentClass != null) {
                return Array.newInstance(componentClass, 0).getClass();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
   * Get the actual type arguments a child class has used to extend a generic base class.
   *
   * @param class1 the base class
   * @param class2 the child class
   * @return a list of the raw classes for the actual type arguments.
   */
  public static <T> List<Class<?>> getTypeArguments(
    final Class<FactoryInjector> class1, final Class<? extends Object> class2) {
    Map<Type, Type> resolvedTypes = new HashMap<Type, Type>();
    Type type = class2;
    // start walking up the inheritance hierarchy until we hit baseClass
    while (! getClass(type).equals(class1)) {
      if (type instanceof Class) {
        // there is no useful information for us in raw types, so just keep going.
        type = ((Class) type).getGenericSuperclass();
      }
      else {
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Class<?> rawType = (Class) parameterizedType.getRawType();

        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
        for (int i = 0; i < actualTypeArguments.length; i++) {
          resolvedTypes.put(typeParameters[i], actualTypeArguments[i]);
        }

        if (!rawType.equals(class1)) {
          type = rawType.getGenericSuperclass();
        }
      }
    }

    // finally, for each actual type argument provided to baseClass, determine (if possible)
    // the raw class for that type argument.
    Type[] actualTypeArguments;
    if (type instanceof Class) {
      actualTypeArguments = ((Class) type).getTypeParameters();
    }
    else {
      actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
    }
    List<Class<?>> typeArgumentsAsClasses = new ArrayList<Class<?>>();
    // resolve types by chasing down type variables.
    for (Type baseType: actualTypeArguments) {
      while (resolvedTypes.containsKey(baseType)) {
        baseType = resolvedTypes.get(baseType);
      }
      typeArgumentsAsClasses.add(getClass(baseType));
    }
    return typeArgumentsAsClasses;
  }

    @Override
    public Object getComponentKey() {
        return key;
    }

    @Override
    public Class<? extends T> getComponentImplementation() {
        return key;
    }

    @Override
    public void accept(final PicoVisitor visitor) {
        visitor.visitComponentAdapter(this);
    }

    @Override
    public ComponentAdapter<T> getDelegate() {
        return null;
    }

    @Override
    public <U extends ComponentAdapter<?>> U findAdapterOfType(final Class<U> adapterType) {
        return null;
    }

    @Override
    public abstract T getComponentInstance(PicoContainer container, Type into);

    @Override
    public Object decorateComponentInstance(final PicoContainer container, final Type into, final T instance) {
        return null;
    }

    @Override
    public Object partiallyDecorateComponentInstance(final PicoContainer container, final Type into, final T instance, final Class<?> superclassPortion) {
        return null;
    }

    @Override
    public void verify(final PicoContainer container) {
    }

    @Override
    public String getDescriptor() {
        return "FactoryInjector-";
    }

    public void start(final PicoContainer container) {
    }

    public void stop(final PicoContainer container) {
    }

    public void dispose(final PicoContainer container) {
    }

    public boolean componentHasLifecycle() {
        return false;
    }

    public static class CantWorkItOut {
        private CantWorkItOut() {
        }
    }

}
