package com.picocontainer.containers;

import static com.picocontainer.Key.annotatedKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;
import com.picocontainer.testmodel.DependsOnTouchable;
import com.picocontainer.testmodel.SimpleTouchable;

import com.picocontainer.MutablePicoContainer;
import com.picocontainer.annotations.Bind;
import com.picocontainer.containers.TieringPicoContainer;
import com.picocontainer.injectors.AbstractInjector;

public class TieringPicoContainerTestCase {

    public static class Couch {
    }

    public static class TiredPerson {
        private final Couch couchToSitOn;

        public TiredPerson(final Couch couchToSitOn) {
            this.couchToSitOn = couchToSitOn;
        }
    }

    @Test
    public void testThatGrandparentTraversalForComponentsCanBeBlocked() {
        MutablePicoContainer grandparent = new TieringPicoContainer();
        MutablePicoContainer parent = grandparent.makeChildContainer();
        MutablePicoContainer child = parent.makeChildContainer();
        grandparent.addComponent(Couch.class);
        child.addComponent(TiredPerson.class);

        TiredPerson tp = null;
        try {
            tp = child.getComponent(TiredPerson.class);
            fail("should have barfed");
        } catch (AbstractInjector.UnsatisfiableDependenciesException e) {
            // expected
        }

    }

    @Test
    public void testThatParentTraversalIsOkForTiering() {
        MutablePicoContainer parent = new TieringPicoContainer();
        MutablePicoContainer child = parent.makeChildContainer();
        parent.addComponent(Couch.class);
        child.addComponent(TiredPerson.class);

        TiredPerson tp = child.getComponent(TiredPerson.class);
        assertNotNull(tp);
        assertNotNull(tp.couchToSitOn);

    }

    public static class Doctor {
        private final TiredPerson tiredPerson;

        public Doctor(final TiredPerson tiredPerson) {
            this.tiredPerson = tiredPerson;
        }
    }

    public static class TiredDoctor {
        private final Couch couchToSitOn;
        private final TiredPerson tiredPerson;

        public TiredDoctor(final Couch couchToSitOn, final TiredPerson tiredPerson) {
            this.couchToSitOn = couchToSitOn;
            this.tiredPerson = tiredPerson;
        }
    }

    @Test
    public void testThatParentTraversalIsOnlyBlockedOneTierAtATime() {
        MutablePicoContainer gp = new TieringPicoContainer();
        MutablePicoContainer p = gp.makeChildContainer();
        MutablePicoContainer c = p.makeChildContainer();
        gp.addComponent(Couch.class);
        p.addComponent(TiredPerson.class);
        c.addComponent(Doctor.class);
        c.addComponent(TiredDoctor.class);
        Doctor d = c.getComponent(Doctor.class);
        assertNotNull(d);
        assertNotNull(d.tiredPerson);
        assertNotNull(d.tiredPerson.couchToSitOn);
        try {
            TiredDoctor td = c.getComponent(TiredDoctor.class);
            fail("should have barfed");
        } catch (AbstractInjector.UnsatisfiableDependenciesException e) {
            // expected
        }

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Bind
    public static @interface Grouchy {}

    public static class GrouchyTiredPerson extends TiredPerson {
        public GrouchyTiredPerson(final Couch couchToSitOn) {
            super(couchToSitOn);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Bind
    public static @interface Polite {}

    public static class PoliteTiredPerson extends TiredPerson {
        public PoliteTiredPerson(final Couch couchToSitOn) {
            super(couchToSitOn);
        }
    }

    public static class DiscerningDoctor {
        private final TiredPerson tiredPerson;

        public DiscerningDoctor(@Polite final TiredPerson tiredPerson) {
            this.tiredPerson = tiredPerson;
        }
    }

    @Test
    public void testThatGrandparentTraversalForComponentsCanBeBlockedEvenForAnnotatedInjections() {
        MutablePicoContainer grandparent = new TieringPicoContainer();
        MutablePicoContainer parent = grandparent.makeChildContainer();
        MutablePicoContainer child = parent.makeChildContainer();
        grandparent.addComponent(Couch.class);
        grandparent.addComponent(annotatedKey(TiredPerson.class, Polite.class), PoliteTiredPerson.class);
        grandparent.addComponent(annotatedKey(TiredPerson.class, Grouchy.class), GrouchyTiredPerson.class);
        child.addComponent(DiscerningDoctor.class);

        assertNotNull(grandparent.getComponent(TiredPerson.class, Polite.class));
        assertNotNull(grandparent.getComponent(TiredPerson.class, Grouchy.class));

        DiscerningDoctor dd = null;
        try {
            dd = child.getComponent(DiscerningDoctor.class);
            fail("should have barfed");
        } catch (AbstractInjector.UnsatisfiableDependenciesException e) {
            // expected
        }

    }

    @Test
    public void testThatGrandparentTraversalForComponentsCanBeBlockedEvenForAnnotatedInjections2() {
        MutablePicoContainer grandparent = new TieringPicoContainer();
        grandparent.addComponent(Couch.class);
        grandparent.addComponent(annotatedKey(TiredPerson.class, Polite.class), PoliteTiredPerson.class);
        grandparent.addComponent(annotatedKey(TiredPerson.class, Grouchy.class), GrouchyTiredPerson.class);
        grandparent.addComponent(DiscerningDoctor.class);

        assertNotNull(grandparent.getComponent(TiredPerson.class, Polite.class));
        assertNotNull(grandparent.getComponent(TiredPerson.class, Grouchy.class));

        DiscerningDoctor dd = grandparent.getComponent(DiscerningDoctor.class);
        assertNotNull(dd.tiredPerson);
        assertTrue(dd.tiredPerson instanceof PoliteTiredPerson);

    }

    @Test public void testRepresentationOfContainerTree() {
		TieringPicoContainer parent = new TieringPicoContainer();
        parent.setName("parent");
        TieringPicoContainer child = new TieringPicoContainer(parent);
        child.setName("child");
		parent.addComponent("st", SimpleTouchable.class);
		child.addComponent("dot", DependsOnTouchable.class);
		assertEquals("child:1<[Immutable]:parent:1<|", child.toString());
    }




}
