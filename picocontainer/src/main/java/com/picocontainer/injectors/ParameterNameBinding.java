/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/
package com.picocontainer.injectors;

import com.picocontainer.NameBinding;
import com.thoughtworks.paranamer.Paranamer;

import java.lang.reflect.AccessibleObject;

/**
 * TODO
 */
public class ParameterNameBinding implements NameBinding {
  private final AccessibleObject member;
  private final int index;
  private final Paranamer paranamer;
  private String name;

  public ParameterNameBinding(
      final Paranamer paranamer,
      final AccessibleObject member,
      final int index) {
    this.member = member;
    this.paranamer = paranamer;
    this.index = index;
  }

  @Override
  public String getName() {
    if (name != null) {
      return name;
    }

    final String[] strings = paranamer.lookupParameterNames(member, false);
    name = strings.length == 0 ? "" : strings[index];
    return name;
  }
}
