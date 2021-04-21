/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/
package com.picocontainer.containers;

import com.picocontainer.DefaultPicoContainer;
import com.picocontainer.MutablePicoContainer;
import com.picocontainer.PicoContainer;

import java.util.Properties;

/**
 * Immutable {@link PicoContainer} constructed from properties.
 * Intended to be used with config parameter.
 *
 * @author Konstantin Pribluda
 */
@SuppressWarnings("serial")
public class PropertiesPicoContainer extends AbstractDelegatingPicoContainer {
  public PropertiesPicoContainer(final Properties properties, final PicoContainer parent) {
    super(new DefaultPicoContainer(parent));

    // Populate container from properties
    for (final Object key : properties.keySet()) {
      ((MutablePicoContainer) getDelegate()).addComponent(key, properties.get(key));
    }
  }

  public PropertiesPicoContainer(final Properties properties) {
    this(properties, null);
  }

  public void setName(final String name) {
    ((MutablePicoContainer) getDelegate()).setName(name);
  }

  @Override
  public String toString() {
    return "[Properties]:" + getDelegate().toString();
  }
}
