/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer.defaults;

import org.junit.Test;
import com.picocontainer.tck.AbstractMultipleConstructorTest;

import com.picocontainer.DefaultPicoContainer;
import com.picocontainer.MutablePicoContainer;

/**
 * @author Aslak Helles&oslash;y
 */
public class DefaultMultipleConstructorTestCase extends AbstractMultipleConstructorTest {
    @Override
	protected MutablePicoContainer createPicoContainer() {
        return new DefaultPicoContainer();
    }

    // Eclipse need at least *one* fixture as direct class member ...
    @Test public void testEclipseDummy() {
    }

    @Override
	@Test
    public void testMultiWithSatisfyingDependencyAndParametersWorks() {
        super.testMultiWithSatisfyingDependencyAndParametersWorks();
    }


}
