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

import com.picocontainer.*;

/**
 * Static collection of factory methods for different BehaviourFactory implementations.
 *
 * @author Paul Hammant
 * @author Mauro Talevi
 */
public class Behaviors {

    /**
     * Prevents instantiation
     */
    private Behaviors() {
        // no-op
    }

    public static Behavior implementationHiding() {
        return new ImplementationHiding();
    }

    public static Behavior caching() {
        return new Caching();
    }

    public static Behavior synchronizing() {
        return new Synchronizing();
    }

    public static Behavior locking() {
        return new Locking();
    }

    public static Behavior propertyApplying() {
        return new PropertyApplying();
    }

    public static Behavior automatic() {
        return new Automating();
    }

    public static <T> ChangedBehavior<T> cached(final ComponentAdapter<T> delegate) {
        return new Caching.Cached<T>(delegate);
    }

    public static <T> ChangedBehavior<T> cached(final ComponentAdapter<T> delegate, final ObjectReference instanceReference) {
        return new Caching.Cached<T>(delegate, instanceReference);
    }

    public static <T> ChangedBehavior<T> decorated(final ComponentAdapter<T> delegate, final Decorator decorator) {
        return new Decorating.Decorated<T>(delegate, decorator);
    }
}
