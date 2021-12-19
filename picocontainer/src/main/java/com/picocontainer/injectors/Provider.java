/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/
package com.picocontainer.injectors;

/**
 * Providers are a type of injector that can participate in injection via a custom method.
 * <p>
 * Implementers of this class must implement a single method called {@code provide}.
 * That method must return the component type intended to be provided.
 * The method can accept parameters that PicoContainer will satisfy.
 */
@SuppressWarnings("MarkerInterface")
public interface Provider {}
