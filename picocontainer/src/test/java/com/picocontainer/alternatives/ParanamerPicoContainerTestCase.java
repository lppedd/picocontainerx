/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer.alternatives;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;

import org.junit.Test;

import com.picocontainer.injectors.ParameterNameBinding;
import com.thoughtworks.paranamer.AdaptiveParanamer;
import com.thoughtworks.paranamer.CachingParanamer;
import com.thoughtworks.paranamer.DefaultParanamer;
import com.thoughtworks.paranamer.Paranamer;

public class ParanamerPicoContainerTestCase {

    @Test
    public void testCanInstantiateParanamer() {
        Paranamer paranamer = new DefaultParanamer();
        assertNotNull(paranamer);
    }


    public void methodToFind(final String name) {
		assert name != null;
	}


    @Test
    public void
           testNameBindingShouldNotThrowWhenAreParameterNamesAreNotAvailable()
            throws Exception {
        Paranamer paranamer = new CachingParanamer(new AdaptiveParanamer());

        Method method = getClass().getMethod("methodToFind", String.class);
        ParameterNameBinding binding = new ParameterNameBinding(paranamer, method, 0);

        assertEquals("name", binding.getName());
    }


}
