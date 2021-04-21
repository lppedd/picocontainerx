/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/
package com.picocontainer.injectors;

import com.picocontainer.Characteristics;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.LifecycleStrategy;
import com.picocontainer.PicoCompositionException;
import com.picocontainer.PicoContainer;
import com.picocontainer.PicoVisitor;
import com.picocontainer.lifecycle.NullLifecycleStrategy;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Properties;

/**
 * <p>
 * Providers are a type of injector that can participate in injection via a custom method.
 * </p>
 * <p>
 * Implementers of this class must implement a single method called {@code provide}.
 * That method must return the component type intended to be provided.
 * The method can accept parameters that PicoContainer will satisfy.
 * </p>
 */
@SuppressWarnings("rawtypes")
public class ProviderAdapter implements com.picocontainer.Injector, Provider, LifecycleStrategy {
  private static final Method AT_INJECT_GET = javax.inject.Provider.class.getDeclaredMethods()[0];

  private final Object provider;
  private final Method provideMethod;
  private final Object key;
  private final Type providerReturnType;
  private Properties properties;
  private final LifecycleStrategy lifecycle;

  protected ProviderAdapter() {
    provider = this;
    provideMethod = getProvideMethod(getClass());
    key = provideMethod.getReturnType();
    providerReturnType = provideMethod.getReturnType();
    setUseNames(useNames());
    lifecycle = new NullLifecycleStrategy();
  }

  public ProviderAdapter(final Provider theProvider) {
    this(null, theProvider);
  }

  public ProviderAdapter(final javax.inject.Provider<?> theProvider) {
    this(null, theProvider);
  }

  public ProviderAdapter(@Nullable final LifecycleStrategy lifecycle, final Provider provider) {
    this(null, lifecycle, provider);
  }

  public ProviderAdapter(
      @Nullable final Object key,
      @Nullable final LifecycleStrategy lifecycle,
      final Provider provider) {
    this(lifecycle, key, provider, false);
  }

  public ProviderAdapter(
      @Nullable final Object key,
      final javax.inject.Provider<?> provider) {
    this(new NullLifecycleStrategy(), key, provider, false);
  }

  public ProviderAdapter(
      final javax.inject.Provider<?> provider,
      final boolean useNames) {
    this(null, provider, useNames);
  }

  public ProviderAdapter(
      final Object key,
      final javax.inject.Provider<?> provider,
      final boolean useNames) {
    this(new NullLifecycleStrategy(), key, provider, useNames);
  }

  public ProviderAdapter(
      @Nullable final Object key,
      @Nullable final LifecycleStrategy lifecycle,
      final javax.inject.Provider<?> provider,
      final boolean useNames) {
    this(lifecycle, key, provider, useNames);
  }

  public ProviderAdapter(
      @Nullable final LifecycleStrategy lifecycle,
      final javax.inject.Provider<?> provider,
      final boolean useNames) {
    this((Object) null, lifecycle, provider, useNames);
  }

  private ProviderAdapter(
      @Nullable final LifecycleStrategy lifecycle,
      @Nullable final Object providerKey,
      final Object provider,
      final boolean useNames) {
    this.lifecycle = lifecycle;
    this.provider = provider;
    provideMethod = getProvideMethod(provider.getClass());
    providerReturnType = determineProviderReturnType(provider);

    if (providerKey == null) {
      key = determineProviderReturnType(provider);
    } else {
      key = providerKey;
    }

    setUseNames(useNames);
  }

  private void setUseNames(final boolean useNames) {
    if (useNames) {
      properties = Characteristics.USE_NAMES;
    } else {
      properties = Characteristics.NONE;
    }
  }

  protected boolean useNames() {
    return false;
  }

  @Override
  public Object getComponentKey() {
    return key;
  }

  @Override
  public Class getComponentImplementation() {
    if (provider instanceof javax.inject.Provider) {
      return provider.getClass();
    }

    return (Class) key;
  }

  public javax.inject.Provider<?> getProvider() {
    return (javax.inject.Provider<?>) provider;
  }

  /**
   * The return type that the provider creates.
   */
  public Class<?> getProviderReturnType() {
    if (providerReturnType instanceof Class<?>) {
      return (Class<?>) providerReturnType;
    }

    throw new PicoCompositionException(
        "Unexpected condition, Provider Return type was not a class type, instead it was a : " + providerReturnType
    );
  }

  public Class<?> getProviderImplementation() {
    return provider.getClass();
  }

  @Override
  public Object getComponentInstance(final PicoContainer container, final Type into) {
    if (provideMethod == AT_INJECT_GET) {
      try {
        return provideMethod.invoke(provider);
      } catch (final Exception e) {
        throw new PicoCompositionException("Error invoking provider " + provider + " to inject into " + into, e);
      }
    }

    final Reinjector reinjector = new Reinjector(container);
    return reinjector.reinject(key, provider.getClass(), provider, properties, new MethodInjection(provideMethod));
  }

  public static Type determineProviderReturnType(final Object provider) {
    final Method provideMethod = getProvideMethod(provider.getClass());

    if (provideMethod == AT_INJECT_GET) {
      final Type paramType = provider.getClass().getGenericInterfaces()[0];

      if (paramType instanceof Class<?>) {
        return paramType.getClass();
      }

      return ((ParameterizedType) paramType).getActualTypeArguments()[0];
    }

    return provideMethod.getReturnType();
  }

  public static Method getProvideMethod(final Class<?> clazz) {
    if (javax.inject.Provider.class.isAssignableFrom(clazz)) {
      return AT_INJECT_GET;
    }

    Method provideMethod = null;

    // TODO doPrivileged
    for (final Method method : clazz.getDeclaredMethods()) {
      if ("provide".equals(method.getName())) {
        if (provideMethod != null) {
          throw newProviderMethodException("only one");
        }

        provideMethod = method;
      }
    }

    if (provideMethod == null) {
      throw newProviderMethodException("a");
    }

    if (provideMethod.getReturnType() == void.class) {
      throw newProviderMethodException("a non void returning");
    }

    return provideMethod;
  }

  private static PicoCompositionException newProviderMethodException(final String str) {
    return new PicoCompositionException(
        "There must be " + str + " method named 'provide' in the AbstractProvider implementation"
    );
  }

  @Override
  public void verify(final PicoContainer container) { }

  @Override
  public void accept(final PicoVisitor visitor) {
    visitor.visitComponentAdapter(this);
  }

  /**
   * Last one in the chain, no delegate.
   */
  @Nullable
  @Contract("-> null")
  @Override
  public ComponentAdapter<?> getDelegate() {
    return null;
  }

  @Override
  public String getDescriptor() {
    return "ProviderAdapter";
  }

  @Override
  public String toString() {
    return getClass().getName() +
        "@" +
        System.identityHashCode(this) +
        " (key = " +
        getComponentKey() +
        " ; implementation = " +
        getProviderImplementation() +
        " ; provided type = " +
        providerReturnType +
        " )";
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

  /**
   * Providers don't decorate component instances.
   */
  @Nullable
  @Contract("_, _, _ -> null")
  @Override
  public Object decorateComponentInstance(
      final PicoContainer container,
      final Type into,
      final Object instance) {
    return null;
  }

  /**
   * Providers don't decorate component instances.
   */
  @Nullable
  @Contract("_, _, _, _ -> null")
  @Override
  @SuppressWarnings("rawtypes")
  public Object partiallyDecorateComponentInstance(
      final PicoContainer container,
      final Type into,
      final Object instance,
      final Class superclassPortion) {
    return null;
  }

  @Nullable
  @Override
  public ComponentAdapter findAdapterOfType(final Class adapterType) {
    if (getClass().isAssignableFrom(adapterType)) {
      return this;
    }

    return null;
  }
}
