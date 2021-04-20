package com.picocontainer.injectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import com.picocontainer.injectors.packageseparatetests.PackagePrivateDerivedTest1;

import com.picocontainer.injectors.InjectableMethodSelector;

public class InjectableMethodSelectorTestCase {




    public static class PackageTestParent {
    	boolean injected = false;
    	boolean otherInjected = false;
		private boolean epicFail;

    	void dontInject(final String something) {
    		epicFail = true;
    	}

    	@Inject
    	void doSomething(final String something) {
    		injected = true;
    	}

    	@Inject
    	void doSomethingElse(final String somethingElse) {
    		otherInjected = true;
    	}

    }

    public static class PackageTestChild extends PackageTestParent {
    	@Override
    	void doSomething(final String something) {
    		injected = true;
    	}
    }


    public static class PackageTestChildTwo extends PackageTestChild {
    	@Inject
    	@Override
    	void doSomething(final String something) {
    		super.doSomething(something);
    	}

    	@Inject
    	@Override
    	void doSomethingElse(final String somethingElse) {
    		super.doSomethingElse(somethingElse);
    	}

    	void alsoDontInject(final String somethingElse) {
    		super.dontInject(somethingElse);
    	}
    }

    @Test
    public void testPackageTestParentRetrievsAllInjectableMethods() throws NoSuchMethodException {
		InjectableMethodSelector selector = new InjectableMethodSelector(Inject.class);
		List<Method> methods = selector.retreiveAllInjectableMethods(PackageTestParent.class);

		Method doSomething = PackageTestParent.class.getDeclaredMethod("doSomething", String.class);
		Method doSomethingElse =  PackageTestParent.class.getDeclaredMethod("doSomethingElse", String.class);
		Method dontInject = PackageTestParent.class.getDeclaredMethod("dontInject", String.class);
		assertTrue("Got " + Arrays.deepToString(methods.toArray()) ,methods.contains(doSomething));
		assertTrue("Got " + Arrays.deepToString(methods.toArray()), methods.contains(doSomethingElse));
		assertFalse("Got " + Arrays.deepToString(methods.toArray()), methods.contains(dontInject));
	}


	@Test
	public void testChildMissingInjectMethodResultsInSkippedMethod() throws NoSuchMethodException {
		InjectableMethodSelector selector = new InjectableMethodSelector(Inject.class);
		selector.retreiveAllInjectableMethods(PackageTestChild.class);
		List<Method> methods = selector.retreiveAllInjectableMethods(PackageTestParent.class);

		Method doSomething = PackageTestChild.class.getDeclaredMethod("doSomething", String.class);
		Method doSomethingElse =  PackageTestParent.class.getDeclaredMethod("doSomethingElse", String.class);
		Method dontInject = PackageTestParent.class.getDeclaredMethod("dontInject", String.class);

		assertFalse(methods.contains(doSomething));
		assertTrue(methods.contains(doSomethingElse));
		assertFalse("Got " + Arrays.deepToString(methods.toArray()), methods.contains(dontInject));
	}

	@Test
	public void testChildOfChildWithReinsertedInjectMethodIsINcluded() throws NoSuchMethodException {
		InjectableMethodSelector selector = new InjectableMethodSelector(Inject.class);
		selector.retreiveAllInjectableMethods(PackageTestChild.class);
		List<Method> methods = selector.retreiveAllInjectableMethods(PackageTestChildTwo.class);

		Method doSomething = PackageTestChildTwo.class.getDeclaredMethod("doSomething", String.class);
		Method doSomethingElse =  PackageTestChildTwo.class.getDeclaredMethod("doSomethingElse", String.class);
		Method dontInject = PackageTestParent.class.getDeclaredMethod("dontInject", String.class);
		Method alsoDontInject = PackageTestChildTwo.class.getDeclaredMethod("alsoDontInject", String.class);

		assertTrue(methods.contains(doSomething));
		assertTrue(methods.contains(doSomethingElse));
		assertFalse("Got " + Arrays.deepToString(methods.toArray()), methods.contains(dontInject));
		assertFalse("Got " + Arrays.deepToString(methods.toArray()), methods.contains(alsoDontInject));

	}


	public static class PublicTestParent {

		public void dontInject(final String something) {
			//shouldn't be injecting.
		}

		@Inject
		public void doSomething(final String something) {

		}


		@Inject
		public void doSomethingElse(final String somethingElse) {

		}
	}


	public static class PublicChild {

		public void doSomething(final String something) {
			//Shouln't inject
		}

		@Inject
		public void doSomethingElse(final String somethingElse) {
			//should inject
		}
	}


	@Test
    public void testPublicTestParentRetrievsAllInjectableMethods() throws NoSuchMethodException {
		InjectableMethodSelector selector = new InjectableMethodSelector(Inject.class);
		List<Method> methods = selector.retreiveAllInjectableMethods(PublicTestParent.class);

		Method doSomething = PublicTestParent.class.getDeclaredMethod("doSomething", String.class);
		Method doSomethingElse =  PublicTestParent.class.getDeclaredMethod("doSomethingElse", String.class);
		Method dontInject = PublicTestParent.class.getDeclaredMethod("dontInject", String.class);


		assertTrue("Got " + Arrays.deepToString(methods.toArray()) ,methods.contains(doSomething));
		assertTrue("Got " + Arrays.deepToString(methods.toArray()), methods.contains(doSomethingElse));
		assertFalse("Got " + Arrays.deepToString(methods.toArray()), methods.contains(dontInject));
	}

	@Test
    public void testPublicTestChildCanOverrideParentMethod() throws NoSuchMethodException {
		InjectableMethodSelector selector = new InjectableMethodSelector(Inject.class);
		List<Method> methods = selector.retreiveAllInjectableMethods(PublicChild.class);

		Method doSomething = PublicChild.class.getDeclaredMethod("doSomething", String.class);
		Method doSomethingElse =  PublicChild.class.getDeclaredMethod("doSomethingElse", String.class);
		Method dontInject = PublicTestParent.class.getDeclaredMethod("dontInject", String.class);


		assertFalse("Got " + Arrays.deepToString(methods.toArray()) ,methods.contains(doSomething));
		assertTrue("Got " + Arrays.deepToString(methods.toArray()), methods.contains(doSomethingElse));
		assertFalse("Got " + Arrays.deepToString(methods.toArray()), methods.contains(dontInject));
    }


	public static class PrivateBase1 {

		@Inject
		private void doSomething() {}
	}


	public static class PrivateChild1 extends PrivateBase1 {

		//See if the system thinks its an override.
		public void doSomething() {}
	}


	@Test
	public void testPrivateInjectionMethodsAreInjected() {
		InjectableMethodSelector selector = new InjectableMethodSelector();
		List<Method> methods = selector.retreiveAllInjectableMethods(PrivateBase1.class);

		assertEquals(1, methods.size());
		assertTrue(PrivateBase1.class.equals(methods.get(0).getDeclaringClass()));
		assertEquals("doSomething", methods.get(0).getName());
	}


	@Test
	public void testPrivateInjectionMethodsCannotBeOverridden() {
		InjectableMethodSelector selector = new InjectableMethodSelector();
		List<Method> methods = selector.retreiveAllInjectableMethods(PrivateChild1.class);
		assertEquals(1, methods.size());
		assertTrue(PrivateBase1.class.equals(methods.get(0).getDeclaringClass()));
		assertEquals("doSomething", methods.get(0).getName());

	}


	public static class OverloadingBase {


		@Inject
		public void doSomething() {
		}

	}

	public static class OverloadingChild extends OverloadingBase {

		@Inject
		public void doSomething(final String value) {

		}
	}


	@Test
	public void testOverloadingMethodsDontMaskParentPublicInjectionsWithDifferentArgs() {
		@SuppressWarnings("unchecked")
		InjectableMethodSelector selector = new InjectableMethodSelector();
		List<Method> methods = selector.retreiveAllInjectableMethods(OverloadingChild.class);
		assertEquals(2, methods.size());
		assertTrue(OverloadingChild.class.equals(methods.get(0).getDeclaringClass()));
		assertEquals("doSomething", methods.get(0).getName());

		assertTrue(OverloadingBase.class.equals(methods.get(1).getDeclaringClass()));
		assertEquals("doSomething", methods.get(1).getName());
	}


	public static class PackagePrivateBase1 {
		@Inject
		void doSomething() {}
	}


	@Test
	public void testOverloadingFromPackagePrivateToPrivateOutsidePackageGetsBothTypesOfMethodsInjected() {
		@SuppressWarnings("unchecked")
		InjectableMethodSelector selector = new InjectableMethodSelector();
		List<Method> methods = selector.retreiveAllInjectableMethods(PackagePrivateDerivedTest1.class);
		assertEquals("Got " + Arrays.deepToString(methods.toArray()), 2, methods.size());
		assertTrue(PackagePrivateDerivedTest1.class.equals(methods.get(0).getDeclaringClass()));
		assertEquals("doSomething", methods.get(0).getName());

		assertTrue(PackagePrivateBase1.class.equals(methods.get(1).getDeclaringClass()));
		assertEquals("doSomething", methods.get(1).getName());
	}


}
