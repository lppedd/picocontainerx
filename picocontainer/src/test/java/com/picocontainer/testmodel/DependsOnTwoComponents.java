/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Idea by Rachel Davies, Original code by Aslak Hellesoy and Paul Hammant   *
 *****************************************************************************/

package com.picocontainer.testmodel;

import junit.framework.Assert;

/**
 * @author steve.freeman@m3p.co.uk
 *         was FlintstoneImpl
 */
public class DependsOnTwoComponents {
    public DependsOnTwoComponents(final Touchable Touchable, final DependsOnTouchable fred) {
        Assert.assertNotNull("Touchable cannot be passed in as null", Touchable);
        Assert.assertNotNull("DependsOnTouchable cannot be passed in as null", fred);
    }
}
