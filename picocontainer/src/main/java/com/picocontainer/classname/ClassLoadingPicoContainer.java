/*******************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.
 * ---------------------------------------------------------------------------
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 ******************************************************************************/
package com.picocontainer.classname;

import com.picocontainer.MutablePicoContainer;
import com.picocontainer.PicoContainer;

import java.net.URL;

/**
 * Extends {@link PicoContainer} with classloader juggling capabilities.
 *
 * @author Paul Hammant
 * @author Aslak Helles&oslash;y
 */
public interface ClassLoadingPicoContainer extends MutablePicoContainer {
  /**
   * Adds a new URL that will be used in classloading.
   *
   * @param url url of the jar to find components in
   * @return {@code ClassPathElement} to add permissions to (subject to security policy)
   */
  ClassPathElement addClassLoaderURL(final URL url);

  /**
   * Returns the {@link ClassLoader} that is the aggregate of the URLs added.
   *
   * @return A ClassLoader
   */
  ClassLoader getComponentClassLoader();

  /**
   * Makes a child container with a given name
   *
   * @param name the container name
   * @return The ScriptedPicoContainer
   */
  ClassLoadingPicoContainer makeChildContainer(final String name);

  /**
   * Adds a child container with a given name
   *
   * @param name the container name
   * @param child the child PicoContainer
   */
  ClassLoadingPicoContainer addChildContainer(final String name, final PicoContainer child);
}
