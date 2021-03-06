/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer.injectors;

import com.picocontainer.DefaultPicoContainer;
import com.picocontainer.MutablePicoContainer;
import com.picocontainer.monitors.NullComponentMonitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TypedFieldInjectorTestCase {

    public static class Helicopter {
        private PogoStick pogo;
    }


    public static class PogoStick {
    }

    public static class Hulahoop {
    }

    @Test public void testFieldInjectionByTypeWhereMatch() {
        MutablePicoContainer pico = new DefaultPicoContainer();
        pico.addAdapter(new TypedFieldInjection.TypedFieldInjector<Helicopter>(Helicopter.class,
        				Helicopter.class,
        				new NullComponentMonitor(),
        				Integer.class.getName() + " "
        				+ PogoStick.class.getName()
        				+ " " + Float.class.getName(), true) );
        pico.addComponent(PogoStick.class, new PogoStick());
        Helicopter chopper = pico.getComponent(Helicopter.class);
        Assertions.assertNotNull(chopper);
        Assertions.assertNotNull(chopper.pogo);
    }



    @Test public void testFieldInjectionByTypeWhereNoMatch() {
        MutablePicoContainer pico = new DefaultPicoContainer();
        pico.setName("parent");
        pico.addAdapter(new TypedFieldInjection.TypedFieldInjector<Helicopter>(Helicopter.class, Helicopter.class, new NullComponentMonitor(),
                Integer.class.getName() + " " + PogoStick.class.getName() + " " + Float.class.getName(), true));
        pico.addComponent(Hulahoop.class, new Hulahoop());
        try {
            pico.getComponent(Helicopter.class);
            Assertions.fail("should have barfed");
        } catch (AbstractInjector.UnsatisfiableDependenciesException e) {
        	e.printStackTrace();
            String expected = "Helicopter has unsatisfied dependency for fields [ Helicopter.pogo (field's type is PogoStick) ] from parent:2<|";
            String actual = e.getMessage();
            actual = actual.replace(TypedFieldInjectorTestCase.class.getName() + "$", "");
            Assertions.assertEquals(expected, actual);
        }
    }
}
