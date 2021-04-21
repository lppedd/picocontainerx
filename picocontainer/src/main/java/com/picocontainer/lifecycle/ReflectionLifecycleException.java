/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *****************************************************************************/
package com.picocontainer.lifecycle;

import com.picocontainer.PicoException;

/**
 * Subclass of {@link PicoException} that is thrown when there is a problem
 * invoking lifecycle methods via Reflection.
 *
 * @author Paul Hammant
 * @author Mauro Talevi
 */
@SuppressWarnings("serial")
public class ReflectionLifecycleException extends PicoException {
  protected ReflectionLifecycleException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
