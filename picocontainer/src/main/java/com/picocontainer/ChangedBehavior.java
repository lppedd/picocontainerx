/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by Paul Hammant                                             *
 *****************************************************************************/

package com.picocontainer;

/**
 * Behaviors modify the components created by an Injector with additional behaviors.
 *
 * @author Paul Hammant
 * @author Jörg Schaible
 * @author Mauro Talevi
 * @see LifecycleStrategy
 */
public interface ChangedBehavior<T> extends ComponentAdapter<T>, ComponentLifecycle<T> {}
