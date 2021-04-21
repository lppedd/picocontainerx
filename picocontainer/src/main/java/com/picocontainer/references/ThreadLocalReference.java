/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/
package com.picocontainer.references;

import com.picocontainer.ObjectReference;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Gets and sets references on {@link ThreadLocal}.
 *
 * @author Paul Hammant
 */
@SuppressWarnings("serial")
public class ThreadLocalReference<T> extends ThreadLocal<T> implements ObjectReference<T>, Serializable {
  private void writeObject(final ObjectOutputStream out) { }

  private void readObject(final ObjectInputStream in) { }
}
