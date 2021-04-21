/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Idea by Rachel Davies, Original code by various                           *
 *****************************************************************************/
package com.picocontainer.containers;

import com.googlecode.jtype.Generic;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentAdapter.NOTHING;
import com.picocontainer.Converters;
import com.picocontainer.Converting;
import com.picocontainer.NameBinding;
import com.picocontainer.PicoContainer;
import com.picocontainer.PicoVisitor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Takes a var-args list of containers and will query them in turn for
 * getComponent(*) and getComponentAdapter(*) requests.
 * Methods returning lists and getParent/accept will not function.
 */
@SuppressWarnings("serial")
public class CompositePicoContainer implements PicoContainer, Converting, Serializable {
  private final PicoContainer[] containers;
  private final Converters compositeConverter = new CompositeConverters();

  public class CompositeConverters implements Converters {
    @Override
    public boolean canConvert(final Type type) {
      for (final PicoContainer container : containers) {
        if (container instanceof Converting && ((Converting) container).getConverters().canConvert(type)) {
          return true;
        }
      }

      return false;
    }

    @Nullable
    @Override
    public Object convert(final String paramValue, final Type type) {
      for (final PicoContainer container : containers) {
        if (container instanceof Converting) {
          final Converters converter = ((Converting) container).getConverters();
          if (converter.canConvert(type)) {
            return converter.convert(paramValue, type);
          }
        }
      }

      return null;
    }
  }

  public CompositePicoContainer(final PicoContainer... containers) {
    this.containers = containers;
  }

  @Nullable
  @Override
  public <T> T getComponentInto(final Class<T> componentType, final Type into) {
    for (final PicoContainer container : containers) {
      final T inst = container.getComponentInto(componentType, into);

      if (inst != null) {
        return inst;
      }
    }

    return null;
  }

  @Nullable
  @Override
  public <T> T getComponentInto(final Generic<T> componentType, final Type into) {
    for (final PicoContainer container : containers) {
      final T inst = container.getComponentInto(componentType, into);

      if (inst != null) {
        return inst;
      }
    }

    return null;
  }

  @Nullable
  @Override
  public Object getComponent(final Object keyOrType) {
    return getComponentInto(keyOrType, NOTHING.class);
  }

  @Nullable
  @Override
  public Object getComponentInto(final Object keyOrType, final Type into) {
    for (final PicoContainer container : containers) {
      final Object inst = container.getComponentInto(keyOrType, into);

      if (inst != null) {
        return inst;
      }
    }

    return null;
  }

  @Nullable
  @Override
  public <T> T getComponent(final Class<T> componentType) {
    return getComponent(Generic.get(componentType));
  }

  @Nullable
  @Override
  public <T> T getComponent(final Generic<T> componentType) {
    for (final PicoContainer container : containers) {
      final T inst = container.getComponent(componentType);

      if (inst != null) {
        return inst;
      }
    }

    return null;
  }

  @Nullable
  @Override
  public ComponentAdapter<?> getComponentAdapter(final Object key) {
    for (final PicoContainer container : containers) {
      final ComponentAdapter<?> inst = container.getComponentAdapter(key);

      if (inst != null) {
        return inst;
      }
    }

    return null;
  }

  @Nullable
  @Override
  public <T> ComponentAdapter<T> getComponentAdapter(
      final Class<T> componentType,
      final NameBinding nameBinding) {
    for (final PicoContainer container : containers) {
      final ComponentAdapter<T> inst = container.getComponentAdapter(Generic.get(componentType), nameBinding);

      if (inst != null) {
        return inst;
      }
    }

    return null;
  }

  @Nullable
  @Override
  public <T> ComponentAdapter<T> getComponentAdapter(
      final Generic<T> componentType,
      final NameBinding nameBinding) {
    for (final PicoContainer container : containers) {
      final ComponentAdapter<T> inst = container.getComponentAdapter(componentType, nameBinding);

      if (inst != null) {
        return inst;
      }
    }

    return null;
  }

  @Nullable
  @Override
  public <T> ComponentAdapter<T> getComponentAdapter(
      final Class<T> componentType,
      final Class<? extends Annotation> binding) {
    return getComponentAdapter(Generic.get(componentType), binding);
  }

  @Nullable
  @Override
  public <T> ComponentAdapter<T> getComponentAdapter(
      final Generic<T> componentType,
      final Class<? extends Annotation> binding) {
    for (final PicoContainer container : containers) {
      final ComponentAdapter<T> inst = container.getComponentAdapter(componentType, binding);

      if (inst != null) {
        return inst;
      }
    }

    return null;
  }

  @Nullable
  @Contract("_, _, _ -> null")
  @Override
  public <T> T getComponent(
      final Class<T> componentType,
      final Class<? extends Annotation> binding,
      final Type into) {
    return null;
  }

  @Nullable
  @Contract("_, _, -> null")
  @Override
  public <T> T getComponent(final Class<T> componentType, final Class<? extends Annotation> binding) {
    return null;
  }

  @Override
  public List<Object> getComponents() {
    return Collections.emptyList();
  }

  @Nullable
  @Contract("-> null")
  @Override
  public PicoContainer getParent() {
    return null;
  }

  @Override
  public Collection<ComponentAdapter<?>> getComponentAdapters() {
    return Collections.emptyList();
  }

  @Override
  public <T> List<ComponentAdapter<T>> getComponentAdapters(final Class<T> componentType) {
    return Collections.emptyList();
  }

  @Override
  public <T> List<ComponentAdapter<T>> getComponentAdapters(final Generic<T> componentType) {
    return Collections.emptyList();
  }

  @Override
  public <T> List<ComponentAdapter<T>> getComponentAdapters(
      final Class<T> componentType,
      final Class<? extends Annotation> binding) {
    return Collections.emptyList();
  }

  @Override
  public <T> List<ComponentAdapter<T>> getComponentAdapters(
      final Generic<T> componentType,
      final Class<? extends Annotation> binding) {
    return Collections.emptyList();
  }

  @Override
  public <T> List<T> getComponents(final Class<T> componentType) {
    return Collections.emptyList();
  }

  @Override
  public void accept(final PicoVisitor visitor) {}

  @Override
  public Converters getConverters() {
    return compositeConverter;
  }
}
