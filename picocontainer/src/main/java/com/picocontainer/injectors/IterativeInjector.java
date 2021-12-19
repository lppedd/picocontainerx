/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *****************************************************************************/
package com.picocontainer.injectors;

import com.picocontainer.ComponentMonitor;
import com.picocontainer.NameBinding;
import com.picocontainer.Parameter;
import com.picocontainer.PicoCompositionException;
import com.picocontainer.PicoContainer;
import com.picocontainer.annotations.Bind;
import com.picocontainer.parameters.AccessibleObjectParameterSet;
import com.thoughtworks.paranamer.AdaptiveParanamer;
import com.thoughtworks.paranamer.AnnotationParanamer;
import com.thoughtworks.paranamer.CachingParanamer;
import com.thoughtworks.paranamer.Paranamer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Injection will happen iteratively after component instantiation.  This class deals with injection types that only
 * have one argument.  Examples would be single-argument methods such as setters, or an object's fields.
 */
@SuppressWarnings("serial")
public abstract class IterativeInjector<T> extends AbstractInjector<T> {
  private static final Object[] NONE = new Object[0];

  private transient ThreadLocalCyclicDependencyGuard<T> instantiationGuard;

  protected transient volatile List<AccessibleObject> injectionMembers;
  protected transient Type[] injectionTypes;
  protected transient Annotation[] bindings;

  private transient Paranamer paranamer;
  private transient volatile boolean initialized;

  private final boolean requireConsumptionOfAllParameters;

  /**
   * Constructs a IterativeInjector
   *
   * @param key the search key for this implementation
   * @param impl the concrete implementation
   * @param monitor the component monitor used by this addAdapter
   * @param useNames use argument names when looking up dependencies
   * @param staticsInitializedReferenceSet (Optional) A data structure that keeps track of
   *     static intializations.  If null, then static members will not be injected.
   * @param parameters the parameters to use for the initialization
   *
   * @throws NotConcreteRegistrationException if the implementation is
   *     not a concrete class.
   * @throws NullPointerException if one of the parameters is <code>null</code>
   */
  public IterativeInjector(
      final Object key,
      final Class<T> impl,
      final ComponentMonitor monitor,
      final boolean useNames,
      @Nullable final StaticsInitializedReferenceSet staticsInitializedReferenceSet,
      final AccessibleObjectParameterSet... parameters) {
    this(key, impl, monitor, useNames, true, parameters);
  }

  /**
   * Constructs a IterativeInjector for use in a composite injection environment.
   *
   * @param key the search key for this implementation
   * @param impl the concrete implementation
   * @param monitor the component monitor used by this addAdapter
   * @param useNames use argument names when looking up dependencies
   * @param requireConsumptionOfAllParameters If set to true, then all parameters (ie:
   *     ComponentParameter/ConstantParameter) must be
   *     used by this injector.  If set to false, then no error occurs if all parameters don't match this type of
   *     injection.  It is assumed
   *     that another type of injection will be using them.
   * @param parameters the parameters to use for the initialization
   *
   * @throws NotConcreteRegistrationException if the implementation is not a concrete class
   * @throws NullPointerException if one of the parameters is {@code null}
   */
  public IterativeInjector(
      final Object key,
      final Class<T> impl,
      final ComponentMonitor monitor,
      final boolean useNames,
      final boolean requireConsumptionOfAllParameters,
      final AccessibleObjectParameterSet... parameters) {
    super(key, impl, monitor, useNames, parameters);
    this.requireConsumptionOfAllParameters = requireConsumptionOfAllParameters;
  }

  protected Constructor<?> getConstructor() {
    final Object retVal = AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
      try {
        return getComponentImplementation().getConstructor((Class<?>[]) null);
      } catch (final NoSuchMethodException | SecurityException e) {
        return new PicoCompositionException(e);
      }
    });

    if (retVal instanceof Constructor) {
      return (Constructor<?>) retVal;
    }

    throw (PicoCompositionException) retVal;
  }

  /**
   * Key-Value Paired parameter/accessible object
   *
   * @author Michael Rimov
   */
  public static class ParameterToAccessibleObjectPair {
    private final AccessibleObject accessibleObject;

    private final AccessibleObjectParameterSet parameter;

    /**
     * @param accessibleObject
     * @param parameter set to null if there was no resolution for this accessible object.
     */
    public ParameterToAccessibleObjectPair(
        final AccessibleObject accessibleObject,
        final AccessibleObjectParameterSet parameter) {
      this.accessibleObject = accessibleObject;
      this.parameter = parameter;
    }

    public AccessibleObject getAccessibleObject() {
      return accessibleObject;
    }

    public AccessibleObjectParameterSet getAccessibleObjectParameters() {
      return parameter;
    }

    public boolean isResolved() {
      return parameter != null && parameter.getParams() != null;
    }

  }

  ParameterToAccessibleObjectPair[] getMatchingParameterListForMembers(final PicoContainer container) throws PicoCompositionException {
    if (initialized == false) {
      synchronized (this) {
        if (initialized == false) {
          initializeInjectionMembersAndTypeLists();
        }
      }
    }

    final List<Object> matchingParameterList = new ArrayList<Object>(Collections.nCopies(injectionMembers.size(), null));

    final Set<AccessibleObjectParameterSet> notMatchingParameters = matchParameters(container, matchingParameterList, parameters);

    final Set<Type> unsatisfiableDependencyTypes = new HashSet<Type>();
    final List<AccessibleObject> unsatisfiableDependencyMembers = new ArrayList<AccessibleObject>();

    for (int i = 0; i < matchingParameterList.size(); i++) {
      final ParameterToAccessibleObjectPair param = (ParameterToAccessibleObjectPair) matchingParameterList.get(i);
      if (param == null || !param.isResolved()) {
        unsatisfiableDependencyTypes.add(injectionTypes[i]);
        unsatisfiableDependencyMembers.add(injectionMembers.get(i));
      }
    }
    if (unsatisfiableDependencyTypes.size() > 0) {
      unsatisfiedDependencies(container, unsatisfiableDependencyTypes, unsatisfiableDependencyMembers);
    } else if (notMatchingParameters.size() > 0 && requireConsumptionOfAllParameters) {
      throw new PicoCompositionException("Following parameters do not match any of the injectionMembers for " + getComponentImplementation() + ": " + notMatchingParameters);
    }
    return matchingParameterList.toArray(new ParameterToAccessibleObjectPair[matchingParameterList.size()]);
  }

  /**
   * Returns a set of integers that point to where in the Parameter array unmatched parameters exist.
   *
   * @param container
   * @param matchingParameterList
   * @param assignedParameters {@link Parameter} for the current object being instantiated.
   *
   * @return set of integers pointing to the index in the parameter array things went awry.
   */
  private Set<AccessibleObjectParameterSet> matchParameters(
      final PicoContainer container,
      final List<Object> matchingParameterList,
      final AccessibleObjectParameterSet... assignedParameters) {

    final Set<AccessibleObjectParameterSet> unmatchedParameters = new HashSet<AccessibleObjectParameterSet>();

    for (final AccessibleObject eachObject : injectionMembers) {
      AccessibleObjectParameterSet currentParameter = getParameterToUseForObject(eachObject, assignedParameters);

      if (currentParameter == null) {
        currentParameter = constructAccessibleObjectParameterSet(eachObject, constructDefaultComponentParameter());
      }

      if (!matchParameter(container, matchingParameterList, currentParameter)) {
        unmatchedParameters.add(currentParameter);
      }
    }

    return unmatchedParameters;
  }

  private boolean matchParameter(
      final PicoContainer container,
      final List<Object> matchingParameterList,
      final AccessibleObjectParameterSet parameter) {
    for (int j = 0; j < injectionTypes.length; j++) {

      final Object o = matchingParameterList.get(j);
      final AccessibleObject targetInjectionMember = getTargetInjectionMember(injectionMembers, j, parameter.getParams()[0]);
      if (targetInjectionMember == null) {
        return false;
      }

      AccessibleObjectParameterSet paramToUse = getParameterToUseForObject(targetInjectionMember, parameter);
      if (paramToUse == null) {
        paramToUse = constructAccessibleObjectParameterSet(targetInjectionMember);
      }

      try {
        if (o == null
            && paramToUse.getParams()[0].resolve(container, this, null, injectionTypes[j],
            makeParameterNameImpl(targetInjectionMember),
            useNames(), bindings[j]).isResolved()) {
          matchingParameterList.set(j, new ParameterToAccessibleObjectPair(targetInjectionMember, paramToUse));
          return true;
        }
      } catch (final AmbiguousComponentResolutionException e) {
        e.setComponent(getComponentImplementation());
        e.setMember(injectionMembers.get(j));
        throw e;
      }
    }
    return false;
  }

  protected abstract boolean isAccessibleObjectEqualToParameterTarget(
      @NotNull final AccessibleObject testObject,
      @NotNull final Parameter currentParameter);

  /**
   * Retrieves the appropriate injection member or null if the parameter doesn't match anything we know about and
   * {@linkplain #requireConsumptionOfAllParameters}
   * is set to false.
   *
   * @param injectionMembers
   * @param currentIndex
   * @param parameter
   *
   * @return Might return null if the parameter doesn't apply to this target.
   */
  private AccessibleObject getTargetInjectionMember(
      final List<AccessibleObject> injectionMembers, final int currentIndex,
      final Parameter parameter) {

    if (parameter.getTargetName() == null) {
      return injectionMembers.get(currentIndex);
    }

    for (final AccessibleObject eachObject : injectionMembers) {
      if (isAccessibleObjectEqualToParameterTarget(eachObject, parameter)) {
        return eachObject;
      }
    }

    if (requireConsumptionOfAllParameters) {
      throw new PicoCompositionException("There was no matching target field/method for target name "
          + parameter.getTargetName()
          + " using injector " + getDescriptor());
    }

    return null;
  }

  protected NameBinding makeParameterNameImpl(final AccessibleObject member) {
    if (member == null) {
      throw new NullPointerException("member");
    }

    if (paranamer == null) {
      paranamer = new CachingParanamer(new AnnotationParanamer(new AdaptiveParanamer()));
    }
    return new ParameterNameBinding(paranamer, member, 0);
  }

  protected abstract void unsatisfiedDependencies(
      final PicoContainer container,
      final Set<Type> unsatisfiableDependencyTypes,
      final List<? extends AccessibleObject> unsatisfiableDependencyMembers);

  @Override
  public T getComponentInstance(final PicoContainer container, final Type into) throws PicoCompositionException {
    final Constructor<?> constructor = getConstructor();
    boolean iInstantiated = false;
    T result;
    try {
      if (instantiationGuard == null) {
        iInstantiated = true;
        instantiationGuard = new ThreadLocalCyclicDependencyGuard<T>() {
          @Override
          public T run(final Object instance) {
            final ParameterToAccessibleObjectPair[] matchingParameters = getMatchingParameterListForMembers(guardedContainer);
            final Object componentInstance = makeInstance(container, constructor, currentMonitor());
            return decorateComponentInstance(matchingParameters, currentMonitor(), componentInstance, container, guardedContainer, into, null);
          }
        };
      }
      instantiationGuard.setGuardedContainer(container);
      result = instantiationGuard.observe(getComponentImplementation(), null);
    } finally {
      if (iInstantiated) {
        instantiationGuard.remove();
        instantiationGuard = null;
      }
    }
    return result;
  }

  T decorateComponentInstance(
      final ParameterToAccessibleObjectPair[] matchingParameters,
      final ComponentMonitor monitor,
      final Object componentInstance,
      final PicoContainer container,
      final PicoContainer guardedContainer,
      final Type into,
      final Class<?> partialDecorationFilter) {
    AccessibleObject member = null;
    final Object[] injected = new Object[injectionMembers.size()];
    Object lastReturn = null;
    try {
      for (int i = 0; i < matchingParameters.length; i++) {
        if (matchingParameters[i] != null) {
          member = matchingParameters[i].getAccessibleObject();
        }

        //Skip it, we're only doing a partial injection
        if (partialDecorationFilter != null && !partialDecorationFilter.equals(((Member) member).getDeclaringClass())) {
          continue;
        }

        if (matchingParameters[i] != null && matchingParameters[i].isResolved()) {
          //Again, interative injector only supports 1 parameter
          //per method to inject.
          final Object toInject = matchingParameters[i].getAccessibleObjectParameters().getParams()[0].resolve(guardedContainer, this, null, injectionTypes[i],
              makeParameterNameImpl(injectionMembers.get(i)),
              useNames(), bindings[i]).resolveInstance(into);
          final Object rv = monitor.invoking(container, this, (Member) member, componentInstance, toInject);
          if (rv == ComponentMonitor.KEEP) {
            final long str = System.currentTimeMillis();
            lastReturn = injectIntoMember(member, componentInstance, toInject);
            monitor.invoked(container, this, (Member) member, componentInstance, System.currentTimeMillis() - str, lastReturn, toInject);
          } else {
            lastReturn = rv;
          }
          injected[i] = toInject;
        }
      }
      return (T) memberInvocationReturn(lastReturn, member, componentInstance);
    } catch (final InvocationTargetException e) {
      return caughtInvocationTargetException(monitor, (Member) member, componentInstance, e);
    } catch (final IllegalAccessException e) {
      return caughtIllegalAccessException(monitor, (Member) member, componentInstance, e);
    }
  }

  protected abstract Object memberInvocationReturn(Object lastReturn, AccessibleObject member, Object instance);

  private Object makeInstance(
      final PicoContainer container,
      final Constructor constructor,
      final ComponentMonitor monitor) {
    final long startTime = System.currentTimeMillis();
    final Constructor constructorToUse = monitor.instantiating(container,
        this, constructor);
    final Object componentInstance;
    try {
      componentInstance = newInstance(constructorToUse, null);
    } catch (final InvocationTargetException e) {
      monitor.instantiationFailed(container, this, constructorToUse, e);
      if (e.getTargetException() instanceof RuntimeException) {
        throw (RuntimeException) e.getTargetException();
      } else if (e.getTargetException() instanceof Error) {
        throw (Error) e.getTargetException();
      }
      throw new PicoCompositionException(e.getTargetException());
    } catch (final InstantiationException e) {
      return caughtInstantiationException(monitor, constructor, e, container);
    } catch (final IllegalAccessException e) {
      return caughtIllegalAccessException(monitor, constructor, e, container);
    }
    monitor.instantiated(container,
        this,
        constructorToUse,
        componentInstance,
        NONE,
        System.currentTimeMillis() - startTime);
    return componentInstance;
  }

  @Override
  public Object decorateComponentInstance(final PicoContainer container, final Type into, final T instance) {
    return partiallyDecorateComponentInstance(container, into, instance, null);
  }

  @Override
  public Object partiallyDecorateComponentInstance(
      final PicoContainer container,
      final Type into,
      final T instance,
      final Class<?> superclassPortion) {
    boolean iInstantiated = false;
    T result;
    try {
      if (instantiationGuard == null) {
        iInstantiated = true;
        instantiationGuard = new ThreadLocalCyclicDependencyGuard<T>() {
          @Override
          public T run(final Object inst) {
            final ParameterToAccessibleObjectPair[] matchingParameters = getMatchingParameterListForMembers(guardedContainer);
            return decorateComponentInstance(matchingParameters, currentMonitor(), inst, container, guardedContainer, into, superclassPortion);
          }
        };
      }
      instantiationGuard.setGuardedContainer(container);
      result = instantiationGuard.observe(getComponentImplementation(), instance);
    } finally {
      if (iInstantiated) {
        instantiationGuard.remove();
        instantiationGuard = null;
      }
    }
    return result;
  }

  protected abstract Object injectIntoMember(
      AccessibleObject member,
      Object componentInstance,
      Object toInject) throws IllegalAccessException, InvocationTargetException;

  @Override
  @SuppressWarnings("unchecked")
  public void verify(final PicoContainer container) throws PicoCompositionException {
    boolean i_Instantiated = false;
    try {
      if (verifyingGuard == null) {
        i_Instantiated = true;
        verifyingGuard = new ThreadLocalCyclicDependencyGuard<T>() {
          @Override
          public T run(final Object inst) {
            final ParameterToAccessibleObjectPair[] currentParameters = getMatchingParameterListForMembers(guardedContainer);
            for (int i = 0; i < currentParameters.length; i++) {
              currentParameters[i].getAccessibleObjectParameters().getParams()[0].verify(container, IterativeInjector.this, injectionTypes[i],
                  makeParameterNameImpl(currentParameters[i].getAccessibleObject()), useNames(), bindings[i]);
            }
            return null;
          }
        };
      }
      verifyingGuard.setGuardedContainer(container);
      verifyingGuard.observe(getComponentImplementation(), null);
    } finally {
      if (i_Instantiated) {
        verifyingGuard.remove();
        verifyingGuard = null;
      }
    }
  }

  protected void initializeInjectionMembersAndTypeLists() {
    injectionMembers = new ArrayList<AccessibleObject>();
    final Set<String> injectionMemberNames = new HashSet<String>();
    final List<Annotation> bingingIds = new ArrayList<Annotation>();
    final List<String> nameList = new ArrayList<String>();
    final List<Type> typeList = new ArrayList<Type>();
    final Method[] methods = getMethods();
    for (final Method method : methods) {
      final Type[] parameterTypes = method.getGenericParameterTypes();
      fixGenericParameterTypes(method, parameterTypes);

      final String methodSignature = crudeMethodSignature(method);

      // We're only interested if there is only one parameter and the method name is bean-style.
      if (parameterTypes.length == 1) {
        final boolean isInjector = isInjectorMethod(method);
        // ... and the method name is bean-style.
        // We're also not interested in dupes from parent classes (not all JDK impls)
        if (isInjector && !injectionMemberNames.contains(methodSignature)) {
          injectionMembers.add(method);
          injectionMemberNames.add(methodSignature);
          nameList.add(getName(method));
          typeList.add(box(parameterTypes[0]));
          bingingIds.add(getBindings(method, 0));
        }
      }
    }
    injectionTypes = typeList.toArray(new Type[0]);
    bindings = bingingIds.toArray(new Annotation[0]);
    initialized = true;
  }

  public static String crudeMethodSignature(final Method method) {
    final StringBuilder sb = new StringBuilder();
    sb.append(method.getReturnType().getName());
    sb.append(method.getName());
    for (final Class<?> pType : method.getParameterTypes()) {
      sb.append(pType.getName());
    }
    return sb.toString();
  }

  protected String getName(final Method method) {
    return null;
  }

  private void fixGenericParameterTypes(final Method method, final Type[] parameterTypes) {
    for (int i = 0; i < parameterTypes.length; i++) {
      final Type parameterType = parameterTypes[i];
      if (parameterType instanceof TypeVariable) {
        parameterTypes[i] = method.getParameterTypes()[i];
      }
    }
  }

  private Annotation getBindings(final Method method, final int i) {
    final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
    if (parameterAnnotations.length >= i + 1) {
      final Annotation[] o = parameterAnnotations[i];
      for (final Annotation annotation : o) {
        if (annotation.annotationType().getAnnotation(Bind.class) != null) {
          return annotation;
        }
      }
      return null;

    }
    //TODO - what's this ?
    if (parameterAnnotations != null) {
      //return ((Bind) method.getAnnotation(Bind.class)).id();
    }
    return null;

  }

  protected boolean isInjectorMethod(final Method method) {
    return false;
  }

  private Method[] getMethods() {
    return AccessController.doPrivileged(new PrivilegedAction<Method[]>() {
      @Override
      public Method[] run() {
        return getComponentImplementation().getMethods();
      }
    });
  }

}
