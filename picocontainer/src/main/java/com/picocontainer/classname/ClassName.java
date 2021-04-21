/*******************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.
 * ---------------------------------------------------------------------------
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 ******************************************************************************/
package com.picocontainer.classname;

/**
 * A simple wrapper for a class name which is used as a key in
 * the registration of components in PicoContainer.
 *
 * @author Paul Hammant
 */
public class ClassName implements CharSequence {
  private final String className;

  public ClassName(final String className) {
    this.className = className;
  }

  @Override
  public int length() {
    return className.length();
  }

  @Override
  public char charAt(final int ix) {
    return className.charAt(ix);
  }

  @Override
  public CharSequence subSequence(final int from, final int to) {
    return className.subSequence(from, to);
  }

  @Override
  public String toString() {
    return className;
  }

  @Override
  public int hashCode() {
    return className.hashCode();
  }

  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  @Override
  public boolean equals(final Object o) {
    return className.equals(o);
  }
}
