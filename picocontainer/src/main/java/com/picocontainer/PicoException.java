/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer;

/**
 * <p>
 * Superclass for all exceptions in PicoContainer.
 * You can use this if you want to catch all exceptions thrown by PicoContainer.
 * </p>
 * <p>
 * Be aware that some parts of the PicoContainer API will also throw {@link NullPointerException}
 * when {@code null} values are provided for method arguments and this is not allowed.
 * </p>
 *
 * @author Paul Hammant
 * @author Aslak Helles&oslash;y
 */
@SuppressWarnings("serial")
public abstract class PicoException extends RuntimeException {
  protected PicoException() { }

  protected PicoException(final String message) {
    super(message);
  }

  protected PicoException(final Throwable cause) {
    super(cause);
  }

  protected PicoException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
