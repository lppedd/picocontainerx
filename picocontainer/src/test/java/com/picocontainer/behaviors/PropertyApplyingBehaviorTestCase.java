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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import com.picocontainer.testmodel.CoupleBean;

import com.picocontainer.behaviors.PropertyApplying;

/**
 *
 * @author greg
 */
public class PropertyApplyingBehaviorTestCase {
    @Test public void testBeanPropertyComponentAdapterCanUsePropertyEditors() {
        Object c = PropertyApplying.PropertyApplicator.convert(CoupleBean.class.getName(), "a's name:Camilla;b's name:Charles;", this.getClass().getClassLoader());
        assertNotNull(c);
        assertTrue(c instanceof CoupleBean);
        assertEquals("Camilla", ((CoupleBean) c).getPersonA().getName());
        assertEquals("Charles", ((CoupleBean) c).getPersonB().getName());
    }

}
