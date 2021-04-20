/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 *****************************************************************************/
package com.picocontainer.containers;

import com.picocontainer.PicoContainer;

/**
 * A container backed by system properties (is a PropertiesPicoContainer)
 * @author Konstantin Pribluda
 */
@SuppressWarnings("serial")
public class SystemPropertiesPicoContainer extends PropertiesPicoContainer {

	public SystemPropertiesPicoContainer() {
		this(null);
	}
	public SystemPropertiesPicoContainer(final PicoContainer parent) {
		super(System.getProperties(),parent);
	}

    @Override
    public String toString() {
        return "[SysProps]:" + super.getDelegate().toString();
    }
}
