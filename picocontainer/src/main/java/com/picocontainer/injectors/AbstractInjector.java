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

import com.googlecode.jtype.Generic;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentMonitor;
import com.picocontainer.ObjectReference;
import com.picocontainer.Parameter;
import com.picocontainer.PicoCompositionException;
import com.picocontainer.PicoContainer;
import com.picocontainer.PicoVisitor;
import com.picocontainer.adapters.AbstractAdapter;
import com.picocontainer.parameters.AccessibleObjectParameterSet;
import com.picocontainer.parameters.ComponentParameter;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * This {@link ComponentAdapter} will instantiate a new object for each call to
 * {@link ComponentAdapter#getComponentInstance(PicoContainer, Type)}.
 * <p>
 * That means that when used with a PicoContainer, {@link PicoContainer#getComponent} will
 * return a new object each time.
 *
 * @author Aslak Helles&oslash;y
 * @author Paul Hammant
 * @author J&ouml;rg Schaible
 * @author Mauro Talevi
 */
@SuppressWarnings("serial")
public abstract class AbstractInjector<T> extends AbstractAdapter<T> implements com.picocontainer.Injector<T> {
  /**
   * The cycle guard for the verification.
   */
  protected transient ThreadLocalCyclicDependencyGuard<?> verifyingGuard;

  /**
   * The parameters to use for initialization.
   */
  protected final transient AccessibleObjectParameterSet[] parameters;

  /**
   * The strategy used to control the lifecycle
   */
  private final boolean useNames;

  /**
   * Constructs a new ComponentAdapter for the given key and implementation.
   *
   * @param key the search key for this implementation
   * @param impl the concrete implementation
   * @param monitor the component monitor used by this ComponentAdapter
   * @param parameters the parameters to use for the initialization
   *
   * @throws NotConcreteRegistrationException if the implementation is not
   *     a concrete class
   * @throws NullPointerException if one of the parameters is {@code null}
   */
  protected AbstractInjector(
      @NotNull final Object key,
      @NotNull final Class<T> impl,
      @NotNull final ComponentMonitor monitor,
      final boolean useNames,
      final AccessibleObjectParameterSet... parameters) {
    super(key, impl, monitor);
    this.useNames = useNames;
    checkConcrete();

    if (parameters != null) {
      for (int i = 0; i < parameters.length; i++) {
        requireNonNull(parameters[i], "Parameter " + i + " cannot be null");

        final Parameter[] nestedParameters = parameters[i].getParams();

        if (nestedParameters != null) {
          for (int j = 0; j < nestedParameters.length; j++) {
            requireNonNull(nestedParameters[j], "Parameter " + j + " inside " + parameters[i] + " cannot be null");
          }
        }
      }
    }

    this.parameters = parameters;
  }

  protected static AccessibleObjectParameterSet[] toAccessibleObjectParameterSetArray(
      @Nullable final AccessibleObjectParameterSet singleParam) {
    // noinspection SimplifiableIfStatement
    if (singleParam == null) {
      return AccessibleObjectParameterSet.EMPTY;
    }

    return new AccessibleObjectParameterSet[]{
        singleParam
    };
  }

  public boolean useNames() {
    return useNames;
  }

  private void checkConcrete() {
    // Assert that the component class is concrete
    final boolean isAbstract = (getComponentImplementation().getModifiers() & Modifier.ABSTRACT) == Modifier.ABSTRACT;

    if (getComponentImplementation().isInterface() || isAbstract) {
      throw new NotConcreteRegistrationException(getComponentImplementation());
    }
  }

  @NotNull
  protected Parameter[] createDefaultParameters(final AccessibleObject member) {
    int length = 0;

    if (member instanceof Constructor) {
      length = ((Constructor<?>) member).getParameterTypes().length;
    } else if (member instanceof Field) {
      length = 1;
    } else if (member instanceof Method) {
      length = ((Method) member).getParameterTypes().length;
    } else {
      throwUnknownAccessibleObjectType(member);
    }

    return createDefaultParameters(length);
  }

  @Contract("_ -> fail")
  public static void throwUnknownAccessibleObjectType(final AccessibleObject member) {
    throw new IllegalArgumentException(
        "Object " + member + " doesn't appear to be a constructor, a field, or a method.  Don't know how to proceed."
    );
  }

  /**
   * Create default parameters for the given types.
   *
   * @param length parameter list length
   *
   * @return the array with the default parameters.
   */
  @NotNull
  protected Parameter[] createDefaultParameters(final int length) {
    final Parameter[] componentParameters = new Parameter[length];

    for (int i = 0; i < length; i++) {
      componentParameters[i] = constructDefaultComponentParameter();
    }

    return componentParameters;
  }

  /**
   * Allows different swapping of types.
   */
  protected Parameter constructDefaultComponentParameter() {
    return ComponentParameter.DEFAULT;
  }

  /**
   * Constructs an appropriate {@link AccessibleObjectParameterSet} based on the type of {@link AccessibleObject} sent.
   * If params are null or zero length then default parameter is used.
   */
  @NotNull
  protected AccessibleObjectParameterSet constructAccessibleObjectParameterSet(
      final AccessibleObject object,
      final Parameter... params) {
    if (object instanceof Constructor) {
      return new ConstructorParameters(params);
    }

    if (object instanceof Field) {
      return new FieldParameters(((Field) object).getDeclaringClass(), ((Field) object).getName(), params);
    }

    if (object instanceof Method) {
      return new MethodParameters(((Method) object).getDeclaringClass(), ((Method) object).getName(), params);
    }

    throwUnknownAccessibleObjectType(object);

    // Never gets here
    return null;
  }

  @NotNull
  protected Parameter[] createDefaultParamsBasedOnTypeOfAccessibleObject(final AccessibleObject object) {
    if (object instanceof Constructor) {
      return createDefaultParameters(((Constructor<?>) object).getParameterTypes().length);
    }

    if (object instanceof Field) {
      return createDefaultParameters(1);
    }

    if (object instanceof Method) {
      return createDefaultParameters(((Method) object).getParameterTypes().length);
    }

    throwUnknownAccessibleObjectType(object);

    //Never gets here
    return null;
  }

  /**
   * Allow modifications of the parameters to use for a target member.
   */
  protected Parameter[] interceptParametersToUse(final Parameter[] currentParameters, final AccessibleObject member) {
    return currentParameters;
  }

  /**
   * @return {@code null} if no parameter set for the given accessible object has been defined.
   */
  @NotNull
  protected final AccessibleObjectParameterSet getParameterToUseForObject(
      final AccessibleObject targetInjectionMember,
      final AccessibleObjectParameterSet... assignedParameters) {
    if (assignedParameters == null || assignedParameters.length == 0) {
      Parameter[] paramsToUse = createDefaultParameters(targetInjectionMember);
      paramsToUse = interceptParametersToUse(paramsToUse, targetInjectionMember);
      return constructAccessibleObjectParameterSet(targetInjectionMember, paramsToUse);
    }

    for (final AccessibleObjectParameterSet eachParameterSet : assignedParameters) {
      // If a target type is defined then we have to match against it.
      // This allows injection into private members of base classes of the same name
      // as the subclasses.
      final Class<?> targetType = eachParameterSet.getTargetType();

      if (targetType != null && !targetType.equals(getAccessibleObjectDefiningType(targetInjectionMember))) {
        continue;
      }

      if (eachParameterSet.getName().equals(getAccessibleObjectName(targetInjectionMember))) {
        // Allow parameter substitution
        final Parameter[] paramsToUse = interceptParametersToUse(eachParameterSet.getParams(), targetInjectionMember);
        return constructAccessibleObjectParameterSet(targetInjectionMember, paramsToUse);
      }
    }

    Parameter[] paramsToUse = createDefaultParameters(targetInjectionMember);
    paramsToUse = interceptParametersToUse(paramsToUse, targetInjectionMember);
    return constructAccessibleObjectParameterSet(targetInjectionMember, paramsToUse);
  }

  /**
   * Retrieves the enclosing class of the accessible object.
   * Constructor, Method, and Field all supply the method "getDeclaringClass()",
   * yet it isn't supplied in the AccessibleObject base class.
   *
   * @return the enclosing type of the accessible object.
   */
  protected Class<?> getAccessibleObjectDefiningType(final AccessibleObject targetAccessibleObject) {
    if (targetAccessibleObject == null) {
      throw new NullPointerException("targetAccessibleObject");
    }

    try {
      final Method declaringClassMethod = targetAccessibleObject.getClass().getMethod("getDeclaringClass");
      return (Class<?>) declaringClassMethod.invoke(targetAccessibleObject);
    } catch (final NoSuchMethodException e) {
      throw new PicoCompositionException(
          "Target Type '" + targetAccessibleObject.getClass()
              + "' does not appear to support getDeclaringClass(). "
              + "Please override getAccessibleObjectDefiningType() for your type of injection",
          e
      );
    } catch (final IllegalAccessException | InvocationTargetException e) {
      throw new PicoCompositionException(
          "Error invoking 'getDeclaringClass()' in type " + targetAccessibleObject.getClass(),
          e
      );
    }
  }

  /**
   * Retrieves the name of the accessible object or null if it doesn't have one (such as a constructor)
   */
  @Nullable
  public String getAccessibleObjectName(final AccessibleObject targetAccessibleObject) {
    if (targetAccessibleObject == null) {
      throw new NullPointerException("targetAccessibleObject");
    }

    if (targetAccessibleObject instanceof Constructor) {
      return null
          ;
    }

    try {
      final Method declaringClassMethod = targetAccessibleObject.getClass().getMethod("getName");
      return (String) declaringClassMethod.invoke(targetAccessibleObject);
    } catch (final NoSuchMethodException e) {
      throw new PicoCompositionException(
          "Target Type '" + targetAccessibleObject.getClass()
              + "' does not appear to support getName(). "
              + "Please override getAccessibleObjectDefiningType() for your type of injection",
          e
      );
    } catch (final IllegalAccessException | InvocationTargetException e) {
      throw new PicoCompositionException(
          "Error invoking 'getName()' in type " + targetAccessibleObject.getClass(),
          e
      );
    }
  }

  @SuppressWarnings("NoopMethodInAbstractClass")
  @Override
  public void verify(final PicoContainer container) {
    //
  }

  @Override
  public abstract T getComponentInstance(PicoContainer container, Type into);

  @Nullable
  @Override
  public Object decorateComponentInstance(final PicoContainer container, final Type into, final T instance) {
    return null;
  }

  @Nullable
  @Override
  public Object partiallyDecorateComponentInstance(
      final PicoContainer container,
      final Type into,
      final T instance,
      final Class<?> superclassPortion) {
    return null;
  }

  @Override
  public void accept(final PicoVisitor visitor) {
    super.accept(visitor);

    if (parameters != null) {
      for (final AccessibleObjectParameterSet parameter : parameters) {
        if (parameter.getParams() != null) {
          for (final Parameter eachContainedParameter : parameter.getParams()) {
            eachContainedParameter.accept(visitor);
          }
        }
      }
    }
  }

  @Override
  public String getDescriptor() {
    return "Abstract Injector";
  }

  /**
   * Instantiate an object with given parameters and respect the accessible flag.
   *
   * @param constructor the constructor to use
   * @param parameters the parameters for the constructor
   *
   * @return the new object.
   */
  protected T newInstance(final Constructor<? extends T> constructor, final Object[] parameters)
      throws InstantiationException,
             IllegalAccessException,
             InvocationTargetException {
    try {
      return constructor.newInstance(parameters);
    } catch (final IllegalArgumentException e) {
      //Chain it with the calling parameters to give us some real information.
      throw new IllegalArgumentException(
          "Type mismatch calling constructor "
              + constructor
              + " with parameters "
              + Arrays.deepToString(parameters),
          e
      );
    }
  }

  /**
   * inform monitor about component instantiation failure
   */
  protected T caughtInstantiationException(
      final ComponentMonitor monitor,
      final Constructor<T> constructor,
      final InstantiationException e,
      final PicoContainer container) {
    // can't get here because checkConcrete() will catch it earlier, but see PICO-191
    monitor.instantiationFailed(container, this, constructor, e);
    throw new PicoCompositionException("Should never get here");
  }

  /**
   * inform monitor about access exception.
   */
  protected T caughtIllegalAccessException(
      final ComponentMonitor monitor,
      final Constructor<T> constructor,
      final IllegalAccessException e,
      final PicoContainer container) {
    // can't get here because either filtered or access mode set
    monitor.instantiationFailed(container, this, constructor, e);
    throw new PicoCompositionException(e);
  }

  /**
   * Inform monitor about exception while instantiating component.
   */
  protected T caughtInvocationTargetException(
      final ComponentMonitor monitor,
      final Member member,
      final Object componentInstance,
      final InvocationTargetException e) {
    monitor.invocationFailed(member, componentInstance, e);

    if (e.getTargetException() instanceof RuntimeException) {
      throw (RuntimeException) e.getTargetException();
    }

    if (e.getTargetException() instanceof Error) {
      throw (Error) e.getTargetException();
    }

    throw new PicoCompositionException(e.getTargetException());
  }

  protected T caughtIllegalAccessException(
      final ComponentMonitor monitor,
      final Member member,
      final Object componentInstance,
      final IllegalAccessException e) {
    monitor.invocationFailed(member, componentInstance, e);
    throw new PicoCompositionException(
        "Illegal Access Exception for Injector "
            + getDescriptor()
            + " and target member "
            + (member != null ? member.toString() : " null"),
        e
    );
  }

  protected Type box(final Type parameterType) {
    if (parameterType instanceof Class && ((Class<?>) parameterType).isPrimitive()) {
      final String parameterTypeName = ((Class<?>) parameterType).getName();

      if ("int".equals(parameterTypeName)) {
        return Integer.class;
      }

      if ("boolean".equals(parameterTypeName)) {
        return Boolean.class;
      }

      if ("long".equals(parameterTypeName)) {
        return Long.class;
      }

      if ("float".equals(parameterTypeName)) {
        return Float.class;
      }

      if ("double".equals(parameterTypeName)) {
        return Double.class;
      }

      if ("char".equals(parameterTypeName)) {
        return Character.class;
      }

      if ("byte".equals(parameterTypeName)) {
        return Byte.class;
      }

      if ("short".equals(parameterTypeName)) {
        return Short.class;
      }
    }

    return parameterType;
  }

  /**
   * Returns {@code true} if all fields are static members
   *
   * @param fieldsToInject list of fields/methods to be injected
   *
   * @return {@code true} if all members are static
   */
  protected boolean isStaticInjection(final Member... fieldsToInject) {
    Boolean isStaticFields = null;

    for (final Member eachField : fieldsToInject) {
      if (Modifier.isStatic(eachField.getModifiers())) {
        if (isStaticFields != null && !isStaticFields) {
          throw new PicoCompositionException(
              "Please make SpecificFieldInjector inject either all non static fields or all non static fields"
          );
        }

        isStaticFields = Boolean.TRUE;
      } else {
        if (isStaticFields != null && isStaticFields) {
          throw new PicoCompositionException(
              "Please make SpecificFieldInjector inject either all non static fields or all non static fields"
          );
        }

        isStaticFields = Boolean.FALSE;
      }
    }

    return isStaticFields != null ? isStaticFields : Boolean.FALSE;
  }

  /**
   * Abstract utility class to detect recursion cycles.
   * Derive from this class and implement {@link ThreadLocalCyclicDependencyGuard#run}.
   * The method will be called by  {@link ThreadLocalCyclicDependencyGuard#observe}.
   * Select an appropriate guard for your scope: any {@link ObjectReference} can be
   * used as long as it is initialized with {@code Boolean.FALSE}.
   *
   * @author J&ouml;rg Schaible
   */
  abstract static class ThreadLocalCyclicDependencyGuard<T> extends ThreadLocal<Boolean> {
    protected PicoContainer guardedContainer;

    @Override
    protected Boolean initialValue() {
      return Boolean.FALSE;
    }

    /**
     * Derive from this class and implement this function with the functionality
     * to observe for a dependency cycle.
     *
     * @return a value, if the functionality result in an expression,
     *     otherwise just return {@code null}
     */
    public abstract T run(Object instance);

    /**
     * Call the observing function. The provided guard will hold the {@link Boolean} value.
     * If the guard is already {@code Boolean.TRUE} a {@link CyclicDependencyException}
     * will be  thrown.
     *
     * @param stackFrame the current stack frame
     *
     * @return the result of the {@code run} method
     */
    public final T observe(final Class<?> stackFrame, final Object instance) {
      if (Boolean.TRUE.equals(get())) {
        throw new CyclicDependencyException(stackFrame);
      }

      try {
        set(Boolean.TRUE);
        return run(instance);
      } catch (final CyclicDependencyException e) {
        e.push(stackFrame);
        throw e;
      } finally {
        set(Boolean.FALSE);
      }
    }

    public void setGuardedContainer(final PicoContainer container) {
      guardedContainer = container;
    }

  }

  public static class CyclicDependencyException extends PicoCompositionException {
    private final List<Class<?>> stack;

    public CyclicDependencyException(@NotNull final Class<?> element) {
      super((Throwable) null);
      stack = new LinkedList<>();
      push(element);
    }

    public void push(@NotNull final Class<?> element) {
      stack.add(element);
    }

    @NotNull
    public Class<?>[] getDependencies() {
      return stack.toArray(new Class[0]);
    }

    @Override
    public String getMessage() {
      return "Cyclic dependency: " + stack.toString();
    }
  }

  /**
   * Exception that is thrown as part of the introspection.
   * Raised if a PicoContainer cannot resolve a type dependency because the registered
   * {@link ComponentAdapter}s are not distinct.
   *
   * @author Paul Hammant
   * @author Aslak Helles&oslash;y
   * @author Jon Tirs&eacute;n
   */
  public static final class AmbiguousComponentResolutionException extends PicoCompositionException {
    private Class<?> component;
    private final Generic<?> ambiguousDependency;
    private final Object[] ambiguousComponentKeys;
    private AccessibleObject accessibleObject;

    /**
     * Zero-based parameter #
     */
    private int parameterNumber = -1;

    /**
     * Construct a new exception with the ambiguous class type and the ambiguous component keys.
     *
     * @param ambiguousDependency the unresolved dependency type
     * @param keys the ambiguous keys
     */
    public AmbiguousComponentResolutionException(final Generic<?> ambiguousDependency, final Object[] keys) {
      super("");
      this.ambiguousDependency = ambiguousDependency;
      ambiguousComponentKeys = new Object[keys.length];
      System.arraycopy(keys, 0, ambiguousComponentKeys, 0, keys.length);
    }

    /**
     * @return Returns a string containing the unresolved class type and the ambiguous keys.
     */
    @Override
    public String getMessage() {
      final StringBuilder msg = new StringBuilder();
      msg.append(component != null ? component : "<not-specified>");
      msg.append(" needs a '");
      msg.append(ambiguousDependency.toString());
      msg.append("' injected");

      if (parameterNumber > -1) {
        msg.append(" into parameter #");
        msg.append(parameterNumber);
        msg.append(" (zero based index) of");
      }

      if (parameterNumber == -1 && accessibleObject != null) {
        msg.append(" into");
      }

      if (accessibleObject != null) {
        if (accessibleObject instanceof Field) {
          msg.append(" field '");
        } else if (accessibleObject instanceof Constructor) {
          msg.append(" constructor '");
        } else if (accessibleObject instanceof Method) {
          msg.append(" method '");
        } else {
          msg.append(" '");
        }

        msg.append(accessibleObject);
      } else {
        msg.append(" through : <unknown>");
      }

      msg.append("', but there are too many choices to inject. These: ");
      msg.append(Arrays.asList(getAmbiguousComponentKeys()));
      msg.append(", refer to http://picocontainer.org/ambiguous-injectable-help.html");
      return msg.toString();
    }

    /**
     * @return Returns the ambiguous component keys as array.
     */
    public Object[] getAmbiguousComponentKeys() {
      return ambiguousComponentKeys;
    }

    public void setComponent(final Class<?> component) {
      this.component = component;
    }

    public void setMember(final AccessibleObject accessibleObject) {
      this.accessibleObject = accessibleObject;
    }

    public void setParameterNumber(final int parameterNumber) {
      this.parameterNumber = parameterNumber;
    }
  }

  /**
   * Exception thrown when some of the component's dependencies are not satisfiable.
   *
   * @author Aslak Helles&oslash;y
   * @author Mauro Talevi
   */
  public static class UnsatisfiableDependenciesException extends PicoCompositionException {
    public UnsatisfiableDependenciesException(final String message) {
      super(message);
    }
  }

  /**
   * @author Aslak Hellesoy
   */
  public static class NotConcreteRegistrationException extends PicoCompositionException {
    private final Class<?> impl;

    public NotConcreteRegistrationException(final Class<?> impl) {
      super("Bad Access: '" + impl.getName() + "' is not instantiable");
      this.impl = impl;
    }

    public Class<?> getComponentImplementation() {
      return impl;
    }
  }
}
