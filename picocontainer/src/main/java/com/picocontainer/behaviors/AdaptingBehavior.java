/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer.behaviors;

import com.picocontainer.Behavior;
import com.picocontainer.Characteristics;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentFactory;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.LifecycleStrategy;
import com.picocontainer.PicoContainer;
import com.picocontainer.PicoVisitor;
import com.picocontainer.annotations.Cache;
import com.picocontainer.injectors.AdaptingInjection;
import com.picocontainer.injectors.AnnotatedStaticInjection;
import com.picocontainer.injectors.StaticsInitializedReferenceSet;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * TODO
 */
@SuppressWarnings({"serial", "RedundantInterfaceDeclaration"})
public class AdaptingBehavior extends AbstractBehavior implements Behavior, Serializable {
  @Nullable
  private transient StaticsInitializedReferenceSet referenceSet;

  public AdaptingBehavior() {
    this(null);
  }

  public AdaptingBehavior(@Nullable final StaticsInitializedReferenceSet referenceSet) {
    this.referenceSet = referenceSet;
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
    final List<Behavior> list = new ArrayList<>();
    ComponentFactory lastFactory = makeInjectionFactory();
    processSynchronizing(componentProps, list);
    processLocking(componentProps, list);
    processPropertyApplying(componentProps, list);
    processAutomatic(componentProps, list);
    processImplementationHiding(componentProps, list);
    processCaching(componentProps, impl, list);
    processGuarding(componentProps, impl, list);

    //Instantiate Chain of ComponentFactories
    for (final Behavior componentFactory : list) {
      if (lastFactory != null && componentFactory != null) {
        componentFactory.wrap(lastFactory);
      }

      lastFactory = componentFactory;
    }

    final ComponentFactory completedFactory = createStaticInjection(lastFactory);

    return completedFactory.createComponentAdapter(monitor,
        lifecycle,
        componentProps,
        key,
        impl,
        constructorParams, fieldParams, methodParams);
  }

  /**
   * Override to return lastFactory parameter to completely disable static injection.
   *
   * @param lastFactory
   */
  protected ComponentFactory createStaticInjection(final ComponentFactory lastFactory) {
    return new AnnotatedStaticInjection(referenceSet).wrap(lastFactory);
  }

  @Override
  public <T> ComponentAdapter<T> addComponentAdapter(
      final ComponentMonitor monitor,
      final LifecycleStrategy lifecycle,
      final Properties componentProps,
      final ComponentAdapter<T> adapter) {
    final List<Behavior> list = new ArrayList<Behavior>();
    processSynchronizing(componentProps, list);
    processImplementationHiding(componentProps, list);
    processCaching(componentProps, adapter.getComponentImplementation(), list);
    processGuarding(componentProps, adapter.getComponentImplementation(), list);

    //Instantiate Chain of ComponentFactories
    Behavior lastFactory = null;

    for (final Behavior componentFactory : list) {
      if (lastFactory != null) {
        componentFactory.wrap(lastFactory);
      }

      lastFactory = componentFactory;
    }

    if (lastFactory == null) {
      return adapter;
    }

    return lastFactory.addComponentAdapter(monitor, lifecycle, componentProps, adapter);
  }

  @Override
  public void verify(final PicoContainer container) {}

  @Override
  public void accept(final PicoVisitor visitor) {
    visitor.visitComponentFactory(this);
  }

  protected AdaptingInjection makeInjectionFactory() {
    return new AdaptingInjection();
  }

  protected void processSynchronizing(final Properties componentProps, final List<? super Behavior> list) {
    if (AbstractBehavior.removePropertiesIfPresent(componentProps, Characteristics.SYNCHRONIZE)) {
      list.add(new Synchronizing());
    }
  }

  protected void processLocking(final Properties componentProps, final List<? super Behavior> list) {
    if (AbstractBehavior.removePropertiesIfPresent(componentProps, Characteristics.LOCK)) {
      list.add(new Locking());
    }
  }

  protected void processCaching(final Properties componentProps, final Class<?> impl, final List<? super Behavior> list) {
    if (AbstractBehavior.removePropertiesIfPresent(componentProps, Characteristics.CACHE)
        || impl.getAnnotation(Cache.class) != null) {
      list.add(new Caching());
    }

    AbstractBehavior.removePropertiesIfPresent(componentProps, Characteristics.NO_CACHE);
  }

  protected void processGuarding(final Properties componentProps, final Class<?> impl, final List<? super Behavior> list) {
    if (AbstractBehavior.arePropertiesPresent(componentProps, Characteristics.GUARD, false)) {
      list.add(new Guarding());
    }
  }

  protected void processImplementationHiding(final Properties componentProps, final List<? super Behavior> list) {
    if (AbstractBehavior.removePropertiesIfPresent(componentProps, Characteristics.HIDE_IMPL)) {
      list.add(new ImplementationHiding());
    }

    AbstractBehavior.removePropertiesIfPresent(componentProps, Characteristics.NO_HIDE_IMPL);
  }

  protected void processPropertyApplying(final Properties componentProps, final List<? super Behavior> list) {
    if (AbstractBehavior.removePropertiesIfPresent(componentProps, Characteristics.PROPERTY_APPLYING)) {
      list.add(new PropertyApplying());
    }
  }

  protected void processAutomatic(final Properties componentProps, final List<? super Behavior> list) {
    if (AbstractBehavior.removePropertiesIfPresent(componentProps, Characteristics.AUTOMATIC)) {
      list.add(new Automating());
    }
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    referenceSet = new StaticsInitializedReferenceSet();
  }
}
