/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/
package com.picocontainer.behaviors;

import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.LifecycleStrategy;
import com.picocontainer.PicoContainer;
import com.picocontainer.behaviors.ImplementationHiding.HiddenImplementation;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Paul Hammant
 */
@SuppressWarnings("serial")
public class Intercepting extends AbstractBehavior {
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
    return monitor.changedBehavior(
        new Intercepted<>(
            super.createComponentAdapter(
                monitor,
                lifecycle,
                componentProps,
                key,
                impl,
                constructorParams,
                fieldParams,
                methodParams
            )
        )
    );
  }

  /**
   * @author Paul Hammant
   */
  @SuppressWarnings("serial")
  public static class Intercepted<T> extends HiddenImplementation<T> {
    private final Map<Class<?>, Object> pres = new HashMap<>();
    private final Map<Class<?>, Object> posts = new HashMap<>();
    private final Controller controller = new ControllerWrapper(new InterceptorThreadLocal());

    public Intercepted(final ComponentAdapter<T> delegate) {
      super(delegate);
    }

    public void addPreInvocation(final Class<?> type, final Object interceptor) {
      pres.put(type, interceptor);
    }

    public void addPostInvocation(final Class<?> type, final Object interceptor) {
      posts.put(type, interceptor);
    }

    @Override
    protected Object invokeMethod(
        final Object componentInstance,
        final Method method,
        final Object[] args,
        final PicoContainer container) throws Throwable {
      try {
        controller.clear();
        controller.instance(componentInstance);

        final Object pre = pres.get(method.getDeclaringClass());

        if (pre != null) {
          final Object rv = method.invoke(pre, args);

          if (controller.isVetoed()) {
            return rv;
          }
        }

        final Object result = method.invoke(componentInstance, args);
        controller.setOriginalRetVal(result);
        final Object post = posts.get(method.getDeclaringClass());

        if (post != null) {
          final Object rv = method.invoke(post, args);

          if (controller.isOverridden()) {
            return rv;
          }
        }

        return result;
      } catch (final InvocationTargetException ite) {
        throw ite.getTargetException();
      }
    }

    public Controller getController() {
      return controller;
    }

    @Override
    public String getDescriptor() {
      return "Intercepted";
    }
  }

  public static class InterceptorThreadLocal extends ThreadLocal<Controller> implements Serializable {
    @Override
    protected Controller initialValue() {
      return new ControllerImpl();
    }
  }

  public interface Controller {
    void veto();

    void clear();

    boolean isVetoed();

    void setOriginalRetVal(Object retVal);

    boolean isOverridden();

    void instance(Object instance);

    Object getInstance();

    Object getOriginalRetVal();

    void override();
  }

  public static class ControllerImpl implements Controller {
    private boolean vetoed;

    @Nullable
    private Object retVal;
    private boolean overridden;

    @Nullable
    private Object instance;

    @Override
    public void veto() {
      vetoed = true;
    }

    @Override
    public void clear() {
      vetoed = false;
      overridden = false;
      retVal = null;
      instance = null;
    }

    @Override
    public boolean isVetoed() {
      return vetoed;
    }

    @Override
    public void setOriginalRetVal(final Object retVal) {
      this.retVal = retVal;
    }

    @Nullable
    @Override
    public Object getOriginalRetVal() {
      return retVal;
    }

    @Override
    public boolean isOverridden() {
      return overridden;
    }

    @Override
    public void instance(final Object instance) {
      this.instance = instance;
    }

    @Nullable
    @Override
    public Object getInstance() {
      return instance;
    }

    @Override
    public void override() {
      overridden = true;
    }
  }

  public static class ControllerWrapper implements Controller {
    private final ThreadLocal<? extends Controller> threadLocal;

    public ControllerWrapper(final ThreadLocal<? extends Controller> threadLocal) {
      this.threadLocal = threadLocal;
    }

    @Override
    public void veto() {
      threadLocal.get().veto();
    }

    @Override
    public void clear() {
      threadLocal.get().clear();
    }

    @Override
    public boolean isVetoed() {
      return threadLocal.get().isVetoed();
    }

    @Override
    public void setOriginalRetVal(final Object retVal) {
      threadLocal.get().setOriginalRetVal(retVal);
    }

    @Override
    public Object getOriginalRetVal() {
      return threadLocal.get().getOriginalRetVal();
    }

    @Override
    public boolean isOverridden() {
      return threadLocal.get().isOverridden();
    }

    @Override
    public void instance(final Object instance) {
      threadLocal.get().instance(instance);
    }

    @Override
    public Object getInstance() {
      return threadLocal.get().getInstance();
    }

    @Override
    public void override() {
      threadLocal.get().override();
    }
  }
}
