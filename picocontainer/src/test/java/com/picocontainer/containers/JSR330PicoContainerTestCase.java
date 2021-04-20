package com.picocontainer.containers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.junit.Ignore;
import org.junit.Test;
import com.picocontainer.tck.AbstractPicoContainerTest;

import com.picocontainer.Characteristics;
import com.picocontainer.ComponentFactory;
import com.picocontainer.DefaultPicoContainer;
import com.picocontainer.MutablePicoContainer;
import com.picocontainer.PicoBuilder;
import com.picocontainer.PicoContainer;
import com.picocontainer.containers.JSR330PicoContainer;
import com.picocontainer.parameters.BeanParameters;
import com.picocontainer.parameters.ComponentParameter;
import com.picocontainer.parameters.ConstantParameter;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.JSR330ComponentParameter;
import com.picocontainer.parameters.MethodParameters;
import com.picocontainer.visitors.TraversalCheckingVisitor;

public class JSR330PicoContainerTestCase extends AbstractPicoContainerTest {


	/**
	 * The one test that fails the TCK because it uses J2EE5 Lifecycle annotations instead
	 */
	@Override
	@Test
	@Ignore
	public void testContainerCascadesDefaultLifecycle() {

	}


	/**
	 * This one fails because of PICO-398
	 */
	@Override
	@Test
	@Ignore
	public void testAcceptImplementsBreadthFirstStrategy() {

	}


	@Override
	protected MutablePicoContainer createPicoContainer(final PicoContainer parent) {
		return new JSR330PicoContainer(parent);
	}

	@Override
	protected Properties[] getProperties() {
		return new Properties[0];
	}

	public static class A {

	}

	@Named("test")
	public static class B {
		public int number = 0;

	}

	@SomeQualifier
	public static class C {

	}

	@Test
	public void testJSR330KeyDetermination() {
		MutablePicoContainer mpc = new JSR330PicoContainer(new PicoBuilder().withCaching().withJavaEE5Lifecycle().build());

		mpc.addComponent(A.class)
			.addComponent(B.class)
			.addComponent(C.class);

		assertNotNull(mpc.getComponentAdapter(A.class));
		assertNotNull(mpc.getComponentAdapter("test"));
		assertNull(mpc.getComponentAdapter(B.class));
		assertNull(mpc.getComponentAdapter(C.class));
		assertNotNull(mpc.getComponentAdapter(SomeQualifier.class.getName()));
	}




	public static class ProviderTest {
		public ProviderTest(
				@Named("test") final Provider<B> b,
				@SomeQualifier final Provider<C> c) {

		}

		public ProviderTest() {
			fail("Shouldn't be called");
		}
	}


	@Test
	public void testAddProvidersWithNonAnnotatedKeys() {

		Provider<B> provider1 = new Provider<B>() {
			 public B get() {
				 B returnValue =  new B();
				 returnValue.number = 2;
				 return returnValue;
			 }
		};

		Provider<B> provider2 = new Provider<B>() {

			public B get() {
				 B returnValue =  new B();
				 returnValue.number = 42;
				 return returnValue;
			}

		};

		MutablePicoContainer mpc = new JSR330PicoContainer(new PicoBuilder().withCaching().withJavaEE5Lifecycle().build());
		mpc.addProvider("provider1", provider1);
		mpc.addProvider("provider2", provider2);
		assertTrue(mpc.getComponentAdapter("provider1") != null);
		assertTrue(mpc.getComponentAdapter("provider2") != null);
	}



	@Named("provider1")
	public static class Provider1 implements javax.inject.Provider<B> {
		 public B get() {
			 B returnValue =  new B();
			 returnValue.number = 2;
			 return returnValue;
		 }
	}

	@Named("provider2")
	public static class Provider2 implements javax.inject.Provider<B> {
		public B get() {
			 B returnValue =  new B();
			 returnValue.number = 42;
			 return returnValue;
		}
	}

	@Test
	public void testAddProvidersWithAnnotatedKeys() {

		Provider<B> provider1 = new Provider1();
		Provider<B> provider2 = new Provider2();


		MutablePicoContainer mpc = new JSR330PicoContainer(new PicoBuilder().withCaching().withJavaEE5Lifecycle().build());
		mpc.addProvider(provider1);
		mpc.addProvider(provider2);
		assertTrue(mpc.getComponentAdapter("provider1") != null);
		assertTrue(mpc.getComponentAdapter("provider2") != null);
	}


	public static class ProviderTestTwo {
		public ProviderTestTwo(final A a,
				@Named("test") final Provider<B> b,
				@Named("test2") final Provider<B> c,
				@SomeQualifier final Provider<C> d,
				final Provider<C> e) {

		}

		public ProviderTestTwo() {
			fail("Shouldn't be called");
		}
	}


	public static class RegistrationTypeProvider1 implements Provider<B> {
		 public B get() {
			 B returnValue =  new B();
			 returnValue.number = 2;
			 return returnValue;
		 }
	}

	@Named("test2")
	public static class RegistrationTypeProvider2 implements Provider<B> {
		public B get() {
			 B returnValue =  new B();
			 returnValue.number = 42;
			 return returnValue;
		}

	}

	@SomeQualifier
	public static class CProvider implements Provider<C> {

		public C get() {
			C returnValue = new JSR330PicoContainerTestCase.C();
			return returnValue;
		}

	}

	public static class C2Provider implements Provider<C> {
		public C get() {
			C returnValue = new C();
			return returnValue;
		}
	}


	@Test
	public void testConstructorInjectionAndMixedProviders() {

		MutablePicoContainer mpc = new JSR330PicoContainer();


		 mpc
		  						//Mix of normal components and Providers
		 	.addComponent(A.class)
			.addComponent(ProviderTestTwo.class, ProviderTestTwo.class)  //The Test
			.addProvider("test",  new RegistrationTypeProvider1())	//Manual key name "test"
			.addProvider(new RegistrationTypeProvider2()) //@Named "test2"
			.addProvider(new CProvider())  //@SomeQualifier
			.addProvider(new C2Provider()) //No Qualifier: In case of ambiguity, this is the one that's chosen.
			;

		ProviderTestTwo testObject = mpc.getComponent(ProviderTestTwo.class);
		assertTrue(testObject != null);
	}


	@Named("test2")
	public static class NamedCProvider implements Provider<C> {

		public C get() {
			return new C();
		}

	}


	public static class StaticProviderTest {

		@Inject
		@Named("test2")
		public static C one;

		@Inject
		@SomeQualifier
		private static C two;

		public static C three;

		public static C four;


		/**
		 * Test resolution of no qualifier
		 */
		@Inject
		public static C five;

		@Inject
		@Named("test2")
		public static void injectThree(final C value) {
			three = value;
		}



		/**
		 * Test resolution of no qualifier -- means we're using JSR330 Componentparameters
		 * instead of normal default component parameters.
		 * @param value
		 */
		@Inject
		private static void injectFour(final C value) {
			four = value;
		}

	}


    @Test
    public void testStaticMethodsAndFieldsCanHandleAnnotationBinding() {
    	StaticProviderTest.one = null;
    	StaticProviderTest.two = null;
    	StaticProviderTest.three = null;
    	StaticProviderTest.four = null;
    	StaticProviderTest.five = null;

    	MutablePicoContainer mpc = new JSR330PicoContainer()
    					.as(Characteristics.STATIC_INJECTION).addComponent(StaticProviderTest.class)
    					.addProvider(new CProvider())
    					.addProvider(new NamedCProvider())
    					.addProvider(new C2Provider()); //No Qualifier: In case of ambiguity, this is the one that's chosen.


    	StaticProviderTest test = mpc.getComponent(StaticProviderTest.class);
    	assertNotNull(test);
    	assertNotNull(StaticProviderTest.one);
    	assertNotNull(StaticProviderTest.two);
    	assertNotNull(StaticProviderTest.three);
    	assertNotNull(StaticProviderTest.four);
    	assertNotNull(StaticProviderTest.five);

    	assertNotSame(StaticProviderTest.one, StaticProviderTest.two);
    	assertNotSame(StaticProviderTest.three, StaticProviderTest.four);
    	assertNotSame(StaticProviderTest.one, StaticProviderTest.five);

    }

    @Test
    public void testShowStaticInjectionIsTurnedOffByDefault() {
    	StaticProviderTest.one = null;
    	StaticProviderTest.two = null;
    	StaticProviderTest.three = null;
    	StaticProviderTest.four = null;
    	StaticProviderTest.five = null;

    	MutablePicoContainer mpc = new JSR330PicoContainer()
    	//No as(Characteristics.STATIC_INJECTION)
		.addComponent(StaticProviderTest.class)
		.addProvider(new CProvider())
		.addProvider(new RegistrationTypeProvider2());

		StaticProviderTest test = mpc.getComponent(StaticProviderTest.class);
		assertNotNull(test);
		assertNull(StaticProviderTest.one);
		assertNull(StaticProviderTest.two);
		assertNull(StaticProviderTest.three);
		assertNull(StaticProviderTest.four);

    }


	public static class ProvderTestThree {
		public ProvderTestThree(final Provider<C> arg) {
			assertNotNull(arg);
		}
	}

	public static class ThreeCProvider implements Provider<C> {

		public C get() {
			return new C();
		}

	}

	public static class ThreeAProvider implements Provider<A> {

		public A get() {
			return new A();
		}

	}

	@Test
	public void testConstructorInjectionCanDifferentiateDifferentGenericTypesOnProviders() {
		MutablePicoContainer mpc = new JSR330PicoContainer(new PicoBuilder().withCaching().withJavaEE5Lifecycle().build());


		 mpc.addComponent(ProvderTestThree.class, ProvderTestThree.class,
						new JSR330ComponentParameter())  //The Test
			.addProvider(new ThreeCProvider())  //No Qualifier
			.addProvider(new ThreeAProvider()) //No Qualifier  Generic type provided should short it out
			;

		 ProvderTestThree testThree = mpc.getComponent(ProvderTestThree.class);
		 assertNotNull(testThree);

	}


	public static class ProviderTestFour {

		public static boolean injectMethodCalled;

		@Inject
		public static void inject(final Provider<C> arg) {
			assertNotNull(arg);
			injectMethodCalled = true;
		}

		@Inject
		public static Provider<C> anotherValue;
	}


    @Test
    public void testStaticMethodAndFieldParametersGetAppropriateParametersWhenMixedWithProviders() {

    	ProviderTestFour.injectMethodCalled = false;
    	ProviderTestFour.anotherValue = null;

		MutablePicoContainer mpc = new JSR330PicoContainer();
		 mpc.as(Characteristics.STATIC_INJECTION).addComponent(ProviderTestFour.class, ProviderTestFour.class)  //The Test
		.addProvider(new ThreeCProvider())  //No Qualifier
		.addProvider(new ThreeAProvider()) //No Qualifier  Generic type provided should short it out
		;


		 ProviderTestFour testFour = mpc.getComponent(ProviderTestFour.class);
		 assertNotNull(testFour);

		 assertTrue(ProviderTestFour.injectMethodCalled);
		 assertNotNull(ProviderTestFour.anotherValue);

		 assertTrue(ProviderTestFour.anotherValue.get() instanceof C);

    }




	public static class ParameterTest {

		public Integer constructorArg;

		@Inject
		public String fieldArg;

		public String methodarg;

		@Inject
		public ParameterTest(final Integer constructorArg) {
			this.constructorArg = constructorArg;
		}

		@Inject
		public void applyMethodArg(final String value) {
			this.methodarg = value;
		}
	}

    @Test
    public void testConstructorAndFieldParametersGetTheAppropriateParameters() {
		MutablePicoContainer mpc = new JSR330PicoContainer(new PicoBuilder().withCaching().withJavaEE5Lifecycle().build());
		mpc.addComponent(ParameterTest.class, ParameterTest.class,
				new ConstructorParameters(new ConstantParameter(new Integer(3))),
				new FieldParameters[] {
					new FieldParameters("fieldArg", new ConstantParameter("Arg 1"))
				},
				new MethodParameters[] {
					new MethodParameters("applyMethodArg",new ConstantParameter("Arg 2"))
				});


		ParameterTest test = mpc.getComponent(ParameterTest.class);
		assertNotNull(test);
		assertEquals(3, test.constructorArg.intValue());
		assertEquals("Arg 1", test.fieldArg);
		assertEquals("Arg 2", test.methodarg);
    }

    @Test
    public void testCachingIsTurnedOffByDefault() {
    	MutablePicoContainer mpc = new JSR330PicoContainer();
    	mpc.addComponent(Provider1.class);

    	assertNotSame(mpc.getComponent(Provider1.class), mpc.getComponent(Provider1.class));
    }

    @Test
    public void testYouMayOptInCachingWithDefaultContainer() {
    	MutablePicoContainer mpc = new JSR330PicoContainer();
    	mpc.as(Characteristics.CACHE).addComponent(Provider1.class);

    	assertSame(mpc.getComponent(Provider1.class), mpc.getComponent(Provider1.class));

    }



    @Singleton
    public static class TestSingletonAnnotation {

    }


    @Test
    public void testSingletonAnnotationResultsInCacheProperty() {
    	MutablePicoContainer mpc = new JSR330PicoContainer()
    		.addComponent(TestSingletonAnnotation.class);

    	assertSame(mpc.getComponent(TestSingletonAnnotation.class),
    			mpc.getComponent(TestSingletonAnnotation.class));

    }


    @Test
    public void testSingletonWithDefinedPredefinedKey() {
    	MutablePicoContainer mpc = new JSR330PicoContainer()
		.addComponent("test",TestSingletonAnnotation.class)
		.addComponent("test2", TestSingletonAnnotation.class);

    	assertSame(mpc.getComponent("test"),
			mpc.getComponent("test"));

    }



    public static class AdapterFactoryExaminingVisitor extends TraversalCheckingVisitor {

        private final List<Object> list;

        int containerCount = 0;

        public AdapterFactoryExaminingVisitor(final List<Object> list) {
            this.list = list;
        }

        @Override
		public void visitComponentFactory(final ComponentFactory componentFactory) {
            list.add(componentFactory.getClass());
        }

		@Override
		public boolean visitContainer(final PicoContainer pico) {
			//Don't hang up on wrapped containers
			if (! (pico instanceof DefaultPicoContainer) ) {
				return CONTINUE_TRAVERSAL;
			}

			if (containerCount == 0) {
				containerCount++;
				return CONTINUE_TRAVERSAL;
			}

			return ABORT_TRAVERSAL;
		}



    }

    @Test
    public void testMakeChildContainerPropagatesAdapterFactories() {
    	JSR330PicoContainer pico = new JSR330PicoContainer();
    	MutablePicoContainer child = pico.makeChildContainer();

    	assertTrue(child != null);
    	assertTrue(child instanceof JSR330PicoContainer);

    	List<Object> parentList = new ArrayList<Object>();
    	List<Object> childList = new ArrayList<Object>();

    	AdapterFactoryExaminingVisitor visitor = new AdapterFactoryExaminingVisitor(parentList);
    	visitor.traverse(pico);

    	visitor = new AdapterFactoryExaminingVisitor(childList);
    	visitor.traverse(child);

    	assertTrue(parentList.size() > 0);
    	assertEquals(Arrays.deepToString(parentList.toArray()), Arrays.deepToString(childList.toArray()));
    }


    public static class InjectionOrder2Parent {

    	public static boolean injectSomethingCalled = false;

		public static String injectedValue;

    	@Inject
    	public static void injectSomthing(final String injectedValue) {
    		InjectionOrder2Parent.injectedValue = injectedValue;
			assertFalse(InjectionOrder2Child.isInjected());
    		injectSomethingCalled = true;
    	}
    }


    public static class InjectionOrder2Child extends InjectionOrder2Parent {
    	@Inject
    	private static String something = null;

    	public static boolean isInjected() {
    		return something != null;
    	}


    	@Inject
    	public void injectSomethingElse() {
    		assertNotNull(something);
    		assertNotNull(InjectionOrder2Parent.injectedValue);
    		assertTrue(InjectionOrder2Parent.injectSomethingCalled);
    	}
    }

    @Test
    public void testParentStaticJSRMethodsAreInjectedBeforeChildJSRFields() {
    	JSR330PicoContainer pico = new JSR330PicoContainer();

    	pico.addComponent("Test", "This is a test")
    		.as(Characteristics.STATIC_INJECTION).addComponent(InjectionOrder2Child.class);


    	InjectionOrder2Child child = pico.getComponent(InjectionOrder2Child.class);
    	assertNotNull(child);

    	assertNotNull(InjectionOrder2Parent.injectedValue);
    	assertTrue(InjectionOrder2Child.isInjected());

    }


	public static class Injected {

	}

	public static class TheInjectee {

		@Inject
		@Named("donotuse")
		public Injected fieldInjection;

		private Injected setterInjection;

		private Injected methodInjection;

		private final Injected constructorInjection;

		@Inject
		public TheInjectee(
				@Named("donotuse") final
				Injected constructorInjection) {
			this.constructorInjection = constructorInjection;

		}


		public Injected getSetterInjection() {
			return setterInjection;
		}

		@Inject
		public void setSetterInjection(@Named("donotuse") final Injected setterInjection) {
			this.setterInjection = setterInjection;
		}


		@Inject
		public void inject(@Named("donotuse") final Injected methodInjection) {
			this.methodInjection = methodInjection;
		}

		public Injected getMethodInjection() {
			return methodInjection;
		}

		public Injected getConstructorInjection() {
			return constructorInjection;
		}

	}


	@Test
	public void testSpecificParametersOverridesNamedAnnotations() {
		Injected a = new Injected();
		Injected b = new Injected();
		Injected c = new Injected();
		Injected d = new Injected();
		Injected donotuse = new Injected();

		MutablePicoContainer mpc = createPicoContainer(null)
				.addComponent("a", a)
				.addComponent("b", b)
				.addComponent("c", c)
				.addComponent("d", d)
				.addComponent("donotuse", donotuse)
				.addComponent(TheInjectee.class, TheInjectee.class,
							new ConstructorParameters(new ComponentParameter("a")),
							new FieldParameters[] {
									new FieldParameters("fieldInjection", new ComponentParameter("b"))
							},
							new MethodParameters[] {
									new MethodParameters("inject", new ComponentParameter("c")),
									new BeanParameters("setterInjection", new ComponentParameter("d"))
							});



		TheInjectee result = mpc.getComponent(TheInjectee.class);
		assertNotNull(result);

		assertSame(a, result.getConstructorInjection());
		assertSame(c, result.getMethodInjection());
		assertSame(d, result.getSetterInjection());
	}


}
