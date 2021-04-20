/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer.behaviors;

import com.picocontainer.Behavior;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.behaviors.Locking;

/** @author Paul Hammant */
public final class LockedTestCase extends SynchronizedTestCase {

    @Override
	protected ComponentAdapter makeComponentAdapter(final ComponentAdapter componentAdapter) {
        return new Locking.Locked(componentAdapter);
    }

    @Override
	protected Behavior makeBehaviorFactory() {
        return new Locking();
    }


}