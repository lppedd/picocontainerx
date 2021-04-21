/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by the committers                                           *
 *****************************************************************************/
package com.picocontainer.containers;

import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.MutablePicoContainer;
import com.picocontainer.Parameter;
import com.picocontainer.PicoCompositionException;
import com.picocontainer.PicoContainer;
import com.picocontainer.lifecycle.LifecycleState;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;

import javax.inject.Provider;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Properties;

/**
 * Abstract base class for delegating to mutable containers.
 *
 * @author Paul Hammant
 */
@SuppressWarnings("serial")
public abstract class AbstractDelegatingMutablePicoContainer
    extends AbstractDelegatingPicoContainer
    implements MutablePicoContainer {
  public AbstractDelegatingMutablePicoContainer(final MutablePicoContainer delegate) {
    super(delegate);
  }

  @Override
  public <T> BindWithOrTo<T> bind(final Class<T> type) {
    return getDelegate().bind(type);
  }

  @Override
  public MutablePicoContainer addComponent(
      final Object key,
      final Object implOrInstance,
      final Parameter... parameters) {
    getDelegate().addComponent(key, implOrInstance, parameters);
    return this;
  }

  @Override
  public MutablePicoContainer addComponent(final Object implOrInstance) {
    getDelegate().addComponent(implOrInstance);
    return this;
  }

  @Override
  public MutablePicoContainer addComponent(
      final Object key,
      final Object implOrInstance,
      final ConstructorParameters constructorParams,
      final FieldParameters[] fieldParams,
      final MethodParameters[] methodParams) {
    getDelegate().addComponent(key, implOrInstance, constructorParams, fieldParams, methodParams);
    return this;
  }

  @Override
  public MutablePicoContainer addConfig(final String name, final Object val) {
    getDelegate().addConfig(name, val);
    return this;
  }

  @Override
  public MutablePicoContainer addAdapter(final ComponentAdapter<?> componentAdapter) {
    getDelegate().addAdapter(componentAdapter);
    return this;
  }

  @Override
  public MutablePicoContainer addProvider(final Provider<?> provider) {
    getDelegate().addProvider(provider);
    return this;
  }

  @Override
  public MutablePicoContainer addProvider(final Object key, final Provider<?> provider) {
    getDelegate().addProvider(key, provider);
    return this;
  }

  @Override
  public <T> ComponentAdapter<T> removeComponent(final Object key) {
    return getDelegate().removeComponent(key);
  }

  @Override
  public <T> ComponentAdapter<T> removeComponentByInstance(final T componentInstance) {
    return getDelegate().removeComponentByInstance(componentInstance);
  }

  @Override
  public MutablePicoContainer addChildContainer(final PicoContainer child) {
    getDelegate().addChildContainer(child);
    return this;
  }

  @Override
  public boolean removeChildContainer(final PicoContainer child) {
    return getDelegate().removeChildContainer(child);
  }

  @Override
  public MutablePicoContainer change(final Properties... properties) {
    getDelegate().change(properties);
    return this;
  }

  @Override
  public MutablePicoContainer as(final Properties... properties) {
    // DefaultMutablePicoContainer.as() returns a different container instance
    // For as() to work, we need to swap to the new container
    final MutablePicoContainer resultingDelegate = getDelegate().as(properties);
    final InvocationHandler tempInvocationHandler = new OneRegistrationSwappingInvocationHandler(this, resultingDelegate);
    final MutablePicoContainer proxiedDelegate =
        (MutablePicoContainer) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class[]{MutablePicoContainer.class},
            tempInvocationHandler
        );

    swapDelegate(proxiedDelegate);
    return proxiedDelegate;
  }

  @Override
  public void dispose() {
    getDelegate().dispose();
  }

  @Override
  public abstract MutablePicoContainer makeChildContainer();

  @Override
  public void start() {
    getDelegate().start();
  }

  @Override
  public void stop() {
    getDelegate().stop();
  }

  @Override
  public MutablePicoContainer getDelegate() {
    return (MutablePicoContainer) super.getDelegate();
  }

  @Override
  public void setName(final String name) {
    getDelegate().setName(name);
  }

  @Override
  public void setLifecycleState(final LifecycleState lifecycleState) {
    getDelegate().setLifecycleState(lifecycleState);
  }

  @Override
  public LifecycleState getLifecycleState() {
    return getDelegate().getLifecycleState();
  }

  @Override
  public String getName() {
    return getDelegate().getName();
  }

  @Override
  public ComponentMonitor changeMonitor(final ComponentMonitor monitor) {
    return getDelegate().changeMonitor(monitor);
  }

  @Override
  protected MutablePicoContainer swapDelegate(final PicoContainer newDelegate) {
    return (MutablePicoContainer) super.swapDelegate(newDelegate);
  }

  /**
   * Allows invocation of {@link MutablePicoContainer#addComponent} once
   * and then reverts the delegate back to the old instance.
   *
   * @author Michael Rimov
   */
  public static class OneRegistrationSwappingInvocationHandler implements InvocationHandler {
    private final AbstractDelegatingMutablePicoContainer owner;
    private final MutablePicoContainer oldDelegate;
    private final MutablePicoContainer oneShotPico;

    public OneRegistrationSwappingInvocationHandler(
        final AbstractDelegatingMutablePicoContainer owner,
        final MutablePicoContainer oneShotPico) {
      this.owner = owner;
      this.oneShotPico = oneShotPico;
      oldDelegate = owner.getDelegate();
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      // Invoke delegate no matter what. No problem there.
      try {
        final Object result = method.invoke(oneShotPico, args);
        final String methodName = method.getName();

        // If the method is one of the addComponent() methods, then swap the delegate back to the
        // old value.
        if (methodName.startsWith("addComponent")
            || methodName.startsWith("addAdapter")
            || methodName.startsWith("addProvider")) {
          owner.swapDelegate(oldDelegate);
        }

        // Swap back to the original owner now
        return owner;
      } catch (final InvocationTargetException e) {
        final Throwable nestedException = e.getTargetException();

        if (nestedException instanceof RuntimeException) {
          throw nestedException;
        }

        // Otherwise
        throw new PicoCompositionException("Error in proxy", e);
      } catch (final Throwable e) {
        // Gotta catch to avoid endless loops :(
        if (e instanceof RuntimeException) {
          throw e;
        }

        // Make sure we don't have checked exceptions propagating up for some reason
        throw new PicoCompositionException("Error in proxy", e);
      }
    }
  }
}
