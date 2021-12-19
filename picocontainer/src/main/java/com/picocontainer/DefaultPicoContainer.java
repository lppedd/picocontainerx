/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Original code by                                                          *
 *****************************************************************************/
package com.picocontainer;

import com.googlecode.jtype.Generic;
import com.picocontainer.ComponentAdapter.NOTHING;
import com.picocontainer.adapters.AbstractAdapter;
import com.picocontainer.adapters.InstanceAdapter;
import com.picocontainer.behaviors.AbstractBehavior;
import com.picocontainer.behaviors.AdaptingBehavior;
import com.picocontainer.behaviors.Caching;
import com.picocontainer.behaviors.Caching.Cached;
import com.picocontainer.behaviors.ImplementationHiding.HiddenImplementation;
import com.picocontainer.containers.AbstractDelegatingMutablePicoContainer;
import com.picocontainer.containers.AbstractDelegatingPicoContainer;
import com.picocontainer.containers.EmptyPicoContainer;
import com.picocontainer.containers.ImmutablePicoContainer;
import com.picocontainer.converters.BuiltInConverters;
import com.picocontainer.converters.ConvertsNothing;
import com.picocontainer.injectors.AbstractInjector.AmbiguousComponentResolutionException;
import com.picocontainer.injectors.AbstractInjector.CyclicDependencyException;
import com.picocontainer.injectors.AdaptingInjection;
import com.picocontainer.injectors.FactoryInjector;
import com.picocontainer.injectors.ProviderAdapter;
import com.picocontainer.lifecycle.DefaultLifecycleState;
import com.picocontainer.lifecycle.LifecycleState;
import com.picocontainer.lifecycle.StartableLifecycleStrategy;
import com.picocontainer.monitors.NullComponentMonitor;
import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.DefaultConstructorParameter;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;
import org.jetbrains.annotations.Nullable;

import javax.inject.Provider;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.*;

/**
 * <p/>
 * The Standard {@link PicoContainer}/{@link MutablePicoContainer} implementation.
 * Constructing a container c with a parent p will cause c to look up components
 * in p if they cannot be found inside c itself.
 * </p>
 * <p/>
 * Using {@link Class} objects as keys to the various {@code registerXXX()} methods makes
 * a subtle semantic difference:
 * </p>
 * <p/>
 * If there are more than one registered components of the same type and one of them are
 * registered with a {@link Class} key of the corresponding type, this {@code addComponent}
 * will take precedence over other components during type resolution.
 * </p>
 * <p/>
 * Another place where keys that are classes make a subtle difference is in {@link HiddenImplementation}.
 * </p>
 * <p/>
 * This implementation of {@link MutablePicoContainer} also supports {@link ComponentMonitorStrategy}.
 * </p>
 *
 * @author Paul Hammant
 * @author Aslak Hellesøy
 * @author Jon Tirsén
 * @author Thomas Heller
 * @author Mauro Talevi
 */
@SuppressWarnings("serial")
public class DefaultPicoContainer implements MutablePicoContainer, Converting, ComponentMonitorStrategy, Serializable {
  private String name;

  /**
   * Component factory instance.
   */
  protected final ComponentFactory componentFactory;

  /**
   * Parent PicoContainer.
   */
  @Nullable
  private PicoContainer parent;

  /**
   * All PicoContainer children.
   */
  private final Set<PicoContainer> children = new HashSet<>();

  /**
   * Current state of the container.
   */
  private LifecycleState lifecycleState = new DefaultLifecycleState();

  /**
   * Keeps track of child containers started status.
   */
  private final Collection<WeakReference<PicoContainer>> childrenStarted = new HashSet<>();

  /**
   * Lifecycle strategy instance.
   */
  protected final LifecycleStrategy lifecycle;

  /**
   * Properties set at the container level, that will affect subsequent components added.
   */
  private final Properties containerProperties = new Properties();

  /**
   * Component monitor instance.  Receives event callbacks.
   */
  protected ComponentMonitor monitor;

  /**
   * Map used for looking up component adapters by their key.
   */
  private final Map<Object, ComponentAdapter<?>> keyToAdapterCache = new HashMap<>();

  private final List<ComponentAdapter<?>> componentAdapters = new ArrayList<>();

  protected final List<ComponentAdapter<?>> orderedComponentAdapters = new ArrayList<>();

  private Converters converters;

  /**
   * Creates a new container with a custom ComponentFactory and no parent container.
   *
   * @param componentFactory the ComponentFactory to use.
   */
  public DefaultPicoContainer(final ComponentFactory componentFactory) {
    this((PicoContainer) null, componentFactory);
  }

  /**
   * Creates a new container with the AdaptingInjection using a
   * custom ComponentMonitor
   *
   * @param monitor the ComponentMonitor to use
   */
  public DefaultPicoContainer(final ComponentMonitor monitor) {
    this(null, new StartableLifecycleStrategy(monitor), monitor);
  }

  /**
   * Creates a new container with a (caching) {@link AdaptingInjection}
   * and a parent container.
   *
   * @param parent the parent container (used for component dependency lookups).
   */
  public DefaultPicoContainer(final PicoContainer parent) {
    this(parent, new AdaptingBehavior());
  }

  /**
   * Creates a new container with a {@link AdaptingBehavior} and no parent container.
   */
  public DefaultPicoContainer() {
    this((PicoContainer) null, new AdaptingBehavior());
  }

  /**
   * Creates a new container with a custom ComponentFactory and a parent container.
   * <p/>
   * <em>
   * Important note about caching: If you intend the components to be cached, you should pass
   * in a factory that creates {@link Cached} instances, such as for example
   * {@link Caching}. Caching can delegate to
   * other ComponentAdapterFactories.
   * </em>
   *
   * @param parent the parent container (used for component dependency lookups).
   * @param componentFactories the factory to use for creation of ComponentAdapters.
   */
  public DefaultPicoContainer(
      @Nullable final PicoContainer parent,
      final ComponentFactory... componentFactories) {
    this(parent, new StartableLifecycleStrategy(new NullComponentMonitor()), new NullComponentMonitor(), componentFactories);
  }

  /**
   * Creates a new container with a custom ComponentFactory, LifecycleStrategy for instance registration,
   * and a parent container.
   * <p/>
   * <em>
   * Important note about caching: If you intend the components to be cached, you should pass
   * in a factory that creates {@link Cached} instances, such as for example
   * {@link Caching}. Caching can delegate to
   * other ComponentAdapterFactories.
   * </em>
   *
   * @param parent the parent container (used for component dependency lookups).
   * @param lifecycle the lifecycle strategy chosen for registered
   *     instance (not implementations!)
   * @param componentFactories the factory to use for creation of ComponentAdapters.
   */
  public DefaultPicoContainer(
      @Nullable final PicoContainer parent,
      final LifecycleStrategy lifecycle,
      final ComponentFactory... componentFactories) {
    this(parent, lifecycle, new NullComponentMonitor(), componentFactories);
  }

  public DefaultPicoContainer(
      @Nullable final PicoContainer parent,
      final LifecycleStrategy lifecycle,
      final ComponentMonitor monitor,
      final ComponentFactory... componentFactories) {
    if (componentFactories.length == 0) {
      throw new NullPointerException("at least one componentFactory");
    }

    int i = componentFactories.length - 1;
    ComponentFactory componentFactory = componentFactories[i];

    while (i > 0) {
      try {
        componentFactory = ((Behavior) componentFactories[i - 1]).wrap(componentFactory);
      } catch (final ClassCastException e) {
        throw new PicoCompositionException("Check the order of the BehaviorFactories " +
            "in the varargs list of ComponentFactories. Index " + (i - 1)
            + " (" + componentFactories[i - 1].getClass().getName() + ") should be a BehaviorFactory but is not.");
      }

      i--;
    }

    if (componentFactory == null) {
      throw new NullPointerException("one of the componentFactories");
    }

    if (lifecycle == null) {
      throw new NullPointerException("lifecycle");
    }

    this.componentFactory = componentFactory;
    this.lifecycle = lifecycle;
    this.parent = parent;

    if (parent != null && !(parent instanceof EmptyPicoContainer)) {
      this.parent = new ImmutablePicoContainer(parent);
    }

    this.monitor = monitor;
  }

  /**
   * Creates a new container with the AdaptingInjection using a
   * custom ComponentMonitor
   *
   * @param parent the parent container (used for component dependency lookups).
   * @param monitor the ComponentMonitor to use
   */
  public DefaultPicoContainer(
      @Nullable final PicoContainer parent,
      final ComponentMonitor monitor) {
    this(parent, new StartableLifecycleStrategy(monitor), monitor, new AdaptingBehavior());
  }

  /**
   * Creates a new container with the AdaptingInjection using a
   * custom ComponentMonitor and lifecycle strategy
   *
   * @param parent the parent container (used for component dependency lookups).
   * @param lifecycle the lifecycle strategy to use.
   * @param monitor the ComponentMonitor to use
   */
  public DefaultPicoContainer(
      @Nullable final PicoContainer parent,
      final LifecycleStrategy lifecycle,
      final ComponentMonitor monitor) {
    this(parent, lifecycle, monitor, new AdaptingBehavior());
  }

  /**
   * Creates a new container with the AdaptingInjection using a
   * custom lifecycle strategy
   *
   * @param parent the parent container (used for component dependency lookups).
   * @param lifecycle the lifecycle strategy to use.
   */
  public DefaultPicoContainer(final PicoContainer parent, final LifecycleStrategy lifecycle) {
    this(parent, lifecycle, new NullComponentMonitor());
  }

  /**
   * Creates a new container with a custom ComponentFactory and no parent container.
   *
   * @param componentFactories the ComponentFactory to use.
   */
  public DefaultPicoContainer(final ComponentFactory... componentFactories) {
    this(null, componentFactories);
  }

  @Override
  public Collection<ComponentAdapter<?>> getComponentAdapters() {
    return Collections.unmodifiableList(getModifiableComponentAdapterList());
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public final ComponentAdapter<?> getComponentAdapter(Object key) {
    if (key instanceof Generic) {
      key = ((Generic) key).getType();
    }

    ComponentAdapter<?> adapter = getComponentKeyToAdapterCache().get(key);

    if (adapter == null && parent != null) {
      adapter = getParent().getComponentAdapter(key);
      if (adapter != null) {
        adapter = new KnowsContainerAdapter(adapter, getParent());
      }
    }

    if (adapter == null) {
      final Object inst = monitor.noComponentFound(this, key);
      if (inst != null) {
        adapter = new LateInstance(key, inst);
      }
    }

    return adapter;
  }

  /**
   * <tt>Special Case</tt> class that is an adapter instantiated when a component monitor
   * returns a &quot;late resolution&quot; to finding a container.
   *
   * @param <T>
   */
  public static class LateInstance<T> extends AbstractAdapter<T> {
    private final T instance;

    private LateInstance(final Object key, final T instance) {
      super(key, (Class<T>) instance.getClass());
      this.instance = instance;
    }

    public T getComponentInstance() {
      return instance;
    }

    @Override
    public T getComponentInstance(final PicoContainer container, final Type into) {
      return instance;
    }

    @Override
    public void verify(final PicoContainer container) {}

    @Override
    public String getDescriptor() {
      return "LateInstance";
    }
  }

  /**
   * {@code Decorator} that binds an adapter to a particular PicoContainer instance.
   *
   * @param <T>
   */
  public static class KnowsContainerAdapter<T> implements ComponentAdapter<T> {
    private final ComponentAdapter<T> ca;
    private final PicoContainer ctr;

    public KnowsContainerAdapter(final ComponentAdapter<T> ca, final PicoContainer ctr) {
      this.ca = ca;
      this.ctr = ctr;
    }

    public T getComponentInstance(final Type into) {
      return getComponentInstance(ctr, into);
    }

    @Override
    public Object getComponentKey() {
      return ca.getComponentKey();
    }

    @Override
    public Class<? extends T> getComponentImplementation() {
      return ca.getComponentImplementation();
    }

    @Override
    public T getComponentInstance(final PicoContainer container, final Type into) {
      return ca.getComponentInstance(container, into);
    }

    @Override
    public void verify(final PicoContainer container) {
      ca.verify(container);
    }

    @Override
    public void accept(final PicoVisitor visitor) {
      ca.accept(visitor);
    }

    @Override
    public ComponentAdapter<T> getDelegate() {
      return ca.getDelegate();
    }

    @Override
    public <U extends ComponentAdapter<?>> U findAdapterOfType(final Class<U> adapterType) {
      return ca.findAdapterOfType(adapterType);
    }

    @Override
    public String getDescriptor() {
      return null;
    }
  }

  @Override
  public <T> ComponentAdapter<T> getComponentAdapter(final Class<T> componentType, final NameBinding nameBinding) {
    return getComponentAdapter(Generic.get(componentType), nameBinding, null);
  }

  @Override
  public <T> ComponentAdapter<T> getComponentAdapter(final Generic<T> componentType, final NameBinding nameBinding) {
    return getComponentAdapter(componentType, nameBinding, null);
  }

  private <T> ComponentAdapter<T> getComponentAdapter(
      final Generic<T> componentType,
      final NameBinding componentNameBinding,
      final Class<? extends Annotation> binding) {
    // See http://jira.codehaus.org/secure/ViewIssue.jspa?key=PICO-115
    final ComponentAdapter<T> adapterByKey = (ComponentAdapter<T>) getComponentAdapter(componentType);

    if (adapterByKey != null) {
      return adapterByKey;
    }

    final List<ComponentAdapter<T>> found = binding == null ? getComponentAdapters(componentType) : getComponentAdapters(componentType, binding);

    if (found.size() == 1) {
      return found.get(0);
    }

    if (found.isEmpty()) {
      if (parent != null) {
        return getParent().getComponentAdapter(componentType, componentNameBinding);
      }

      return null;
    }

    if (componentNameBinding != null) {
      final String parameterName = componentNameBinding.getName();
      if (parameterName != null) {
        final ComponentAdapter<?> ca = getComponentAdapter(parameterName);
        if (ca != null && JTypeHelper.isAssignableFrom(componentType, ca.getComponentImplementation())) {
          return (ComponentAdapter<T>) ca;
        }
      }
    }

    final Class<?>[] foundClasses = new Class[found.size()];

    for (int i = 0; i < foundClasses.length; i++) {
      foundClasses[i] = found.get(i).getComponentImplementation();
    }

    throw new AmbiguousComponentResolutionException(componentType, foundClasses);
  }

  @Override
  public <T> ComponentAdapter<T> getComponentAdapter(
      final Class<T> componentType,
      final Class<? extends Annotation> binding) {
    return getComponentAdapter(Generic.get(componentType), null, binding);
  }

  @Override
  public <T> ComponentAdapter<T> getComponentAdapter(
      final Generic<T> componentType,
      final Class<? extends Annotation> binding) {
    return getComponentAdapter(componentType, null, binding);
  }

  @Override
  public <T> List<ComponentAdapter<T>> getComponentAdapters(final Class<T> componentType) {
    return getComponentAdapters(Generic.get(componentType), null);
  }

  @Override
  public <T> List<ComponentAdapter<T>> getComponentAdapters(final Generic<T> componentType) {
    return getComponentAdapters(componentType, null);
  }

  @Override
  public <T> List<ComponentAdapter<T>> getComponentAdapters(
      final Class<T> componentType,
      final Class<? extends Annotation> binding) {
    return getComponentAdapters(Generic.get(componentType), binding);
  }

  @Override
  public <T> List<ComponentAdapter<T>> getComponentAdapters(
      final Generic<T> componentType,
      final Class<? extends Annotation> binding) {
    if (componentType == null) {
      return Collections.emptyList();
    }

    final List<ComponentAdapter<T>> found = new ArrayList<>();

    for (final ComponentAdapter<?> componentAdapter : getComponentAdapters()) {
      final Object key = componentAdapter.getComponentKey();

      //JSR 330 Provider compatibility... we have to be able to return both the providers that provide
      //the type as well as the actual types themselves.
      final Class<?> implementation = componentAdapter.getComponentImplementation();
      boolean compatible = JTypeHelper.isAssignableFrom(componentType, implementation);

      if (!compatible && componentAdapter.findAdapterOfType(ProviderAdapter.class) != null) {
        //If provider
        //Todo: Direct access of provider adapter... work around.
        final ProviderAdapter adapter = componentAdapter.findAdapterOfType(ProviderAdapter.class);
        compatible = JTypeHelper.isAssignableFrom(componentType, adapter.getProviderReturnType());
      }

      if (compatible &&
          (!(key instanceof Key) ||
              ((Key<?>) key).getAnnotation() == null ||
              binding == null ||
              ((Key<?>) key).getAnnotation() == binding)) {
        found.add((ComponentAdapter<T>) componentAdapter);
      }
    }

    return found;
  }

  protected MutablePicoContainer addAdapterInternal(final ComponentAdapter<?> componentAdapter) {
    final Object key = componentAdapter.getComponentKey();

    if (getComponentKeyToAdapterCache().containsKey(key)) {
      throw new PicoCompositionException("Duplicate Keys not allowed. Duplicate for '" + key + "'");
    }

    getModifiableComponentAdapterList().add(componentAdapter);
    getComponentKeyToAdapterCache().put(key, componentAdapter);
    return this;
  }

  /**
   * This method can be used to override the ComponentAdapter created by the {@link ComponentFactory}
   * passed to the constructor of this container.
   */
  @Override
  public MutablePicoContainer addAdapter(final ComponentAdapter<?> componentAdapter) {
    return addAdapter(componentAdapter, containerProperties);
  }

  @Override
  public MutablePicoContainer addProvider(final Provider<?> provider) {
    return addAdapter(new ProviderAdapter(provider), containerProperties);
  }

  @Override
  public MutablePicoContainer addProvider(final Object key, final Provider<?> provider) {
    return addAdapter(new ProviderAdapter(key, provider), containerProperties);
  }

  public MutablePicoContainer addAdapter(final ComponentAdapter<?> componentAdapter, final Properties properties) {
    final Properties tmpProperties = (Properties) properties.clone();
    removeGenericPropertiesThatWeDontCareAbout(tmpProperties);

    if (!AbstractBehavior.removePropertiesIfPresent(tmpProperties, Characteristics.NONE) && componentFactory instanceof Behavior) {
      final MutablePicoContainer container =
          addAdapterInternal(
              ((Behavior) componentFactory).addComponentAdapter(
                  monitor,
                  lifecycle,
                  tmpProperties,
                  componentAdapter
              )
          );

      throwIfPropertiesLeft(tmpProperties);
      return container;
    }

    return addAdapterInternal(componentAdapter);
  }

  @Override
  public <T> ComponentAdapter<T> removeComponent(final Object key) {
    lifecycleState.removingComponent();

    final ComponentAdapter<T> adapter = (ComponentAdapter<T>) getComponentKeyToAdapterCache().remove(key);
    getModifiableComponentAdapterList().remove(adapter);
    getOrderedComponentAdapters().remove(adapter);
    return adapter;
  }

  @Override
  public <T> BindWithOrTo<T> bind(final Class<T> type) {
    return new DpcBindWithOrTo<>(this, type);
  }

  /**
   * The returned ComponentAdapter will be an {@link InstanceAdapter}.
   */
  @Override
  public MutablePicoContainer addComponent(final Object implOrInstance) {
    return addComponent(implOrInstance, containerProperties);
  }

  private MutablePicoContainer addComponent(final Object implOrInstance, final Properties props) {
    if (implOrInstance instanceof String) {
      return addComponent(implOrInstance, implOrInstance);
    }

    final Class<?> clazz;

    if (implOrInstance instanceof Class) {
      clazz = (Class<?>) implOrInstance;
    } else {
      clazz = implOrInstance.getClass();
    }

    return addComponent(clazz, implOrInstance, props, null, null, null);
  }

  @Override
  public MutablePicoContainer addConfig(final String name, final Object val) {
    return addAdapterInternal(new InstanceAdapter<>(name, val, lifecycle, monitor));
  }

  /**
   * The returned ComponentAdapter will be instantiated by the {@link ComponentFactory}
   * passed to the container's constructor.
   */
  @Override
  public MutablePicoContainer addComponent(
      final Object key,
      final Object implOrInstance,
      final Parameter... constructorParameters) {
    return addComponent(
        key,
        implOrInstance,
        containerProperties,
        new ConstructorParameters(constructorParameters),
        null,
        null
    );
  }

  @Override
  public MutablePicoContainer addComponent(
      final Object key,
      final Object implOrInstance,
      final ConstructorParameters constructorParams,
      final FieldParameters[] fieldParameters,
      final MethodParameters[] methodParams) {
    return addComponent(
        key,
        implOrInstance,
        containerProperties,
        constructorParams,
        fieldParameters,
        methodParams
    );
  }

  private MutablePicoContainer addComponent(
      Object key,
      final Object implOrInstance,
      final Properties properties,
      final ConstructorParameters constructorParameters,
      final FieldParameters @Nullable [] fieldParameters,
      final MethodParameters @Nullable [] methodParameters) {
    Parameter[] tweakedParameters = constructorParameters != null ? constructorParameters.getParams() : null;

    if (key instanceof Generic) {
      key = Generic.get(((Generic) key).getType());
    }

    if (tweakedParameters != null && tweakedParameters.length == 0) {
      tweakedParameters = null; // backwards compatibility!  solve this better later - Paul
    }

    //New replacement for Parameter.ZERO.
    if (tweakedParameters != null && tweakedParameters.length == 1 && DefaultConstructorParameter.INSTANCE.equals(tweakedParameters[0])) {
      tweakedParameters = new Parameter[0];
    }

    if (implOrInstance instanceof Class) {
      final Properties tmpProperties = (Properties) properties.clone();
      final ComponentAdapter<?> adapter =
          componentFactory.createComponentAdapter(monitor,
              lifecycle,
              tmpProperties,
              key,
              (Class<?>) implOrInstance,
              new ConstructorParameters(tweakedParameters),
              fieldParameters,
              methodParameters
          );

      removeGenericPropertiesThatWeDontCareAbout(tmpProperties);
      throwIfPropertiesLeft(tmpProperties);

      if (lifecycleState.isStarted()) {
        addAdapterIfStartable(adapter);
        potentiallyStartAdapter(adapter);
      }

      return addAdapterInternal(adapter);
    }

    final ComponentAdapter<?> adapter = new InstanceAdapter<>(key, implOrInstance, lifecycle, monitor);

    if (lifecycleState.isStarted()) {
      addAdapterIfStartable(adapter);
      potentiallyStartAdapter(adapter);
    }

    return addAdapter(adapter, properties);
  }

  private void removeGenericPropertiesThatWeDontCareAbout(final Properties tmpProperties) {
    AbstractBehavior.removePropertiesIfPresent(tmpProperties, Characteristics.USE_NAMES);
    AbstractBehavior.removePropertiesIfPresent(tmpProperties, Characteristics.STATIC_INJECTION);
  }

  public static class DpcBindWithOrTo<T> extends DpcBindTo<T> implements BindWithOrTo<T> {

    public DpcBindWithOrTo(final MutablePicoContainer mutablePicoContainer, final Class<T> type) {
      super(mutablePicoContainer, type);
    }

    @Override
    public <T> BindTo<T> withAnnotation(final Class<? extends Annotation> annotation) {
      return new DpcBindTo<T>(mutablePicoContainer, (Class<T>) type).withAnno(annotation);
    }

    @Override
    public <T> BindTo<T> named(final String name) {
      return new DpcBindTo<T>(mutablePicoContainer, (Class<T>) type).named(name);
    }
  }

  public static class DpcBindTo<T> implements BindTo<T> {
    final MutablePicoContainer mutablePicoContainer;
    final Class<T> type;

    @SuppressWarnings("FieldCanBeLocal")
    private Class<? extends Annotation> annotation;

    @SuppressWarnings("FieldCanBeLocal")
    private String name;

    private DpcBindTo(final MutablePicoContainer mutablePicoContainer, final Class<T> type) {
      this.mutablePicoContainer = mutablePicoContainer;
      this.type = type;
    }

    @Override
    public MutablePicoContainer to(final Class<? extends T> impl) {
      return mutablePicoContainer.addComponent(type, impl);
    }

    @Override
    public MutablePicoContainer to(final T instance) {
      return mutablePicoContainer.addComponent(type, instance);
    }

    @Override
    public MutablePicoContainer toProvider(final Provider<? extends T> provider) {
      return mutablePicoContainer.addAdapter(new ProviderAdapter(provider));
    }

    @Override
    public MutablePicoContainer toProvider(final com.picocontainer.injectors.Provider provider) {
      return mutablePicoContainer.addAdapter(new ProviderAdapter(provider));
    }

    private BindTo<T> withAnno(final Class<? extends Annotation> annotation) {
      this.annotation = annotation;
      return this;
    }

    private BindTo<T> named(final String name) {
      this.name = name;
      return this;
    }
  }

  private void throwIfPropertiesLeft(final Properties tmpProperties) {
    if (!tmpProperties.isEmpty()) {
      throw new PicoCompositionException("Unprocessed Characteristics:" + tmpProperties + ", please refer to http://picocontainer.org/unprocessed-properties-help.html");
    }
  }

  private synchronized void addOrderedComponentAdapter(final ComponentAdapter<?> componentAdapter) {
    if (!getOrderedComponentAdapters().contains(componentAdapter)) {
      getOrderedComponentAdapters().add(componentAdapter);
    }
  }

  @Override
  public List<Object> getComponents() {
    return getComponents(Object.class);
  }

  @Override
  public <T> List<T> getComponents(final Class<T> componentType) {
    if (componentType == null) {
      return Collections.emptyList();
    }

    final List<T> result = new ArrayList<>();

    synchronized (this) {
      final Map<ComponentAdapter<T>, T> adapterToInstanceMap = new HashMap<>();

      for (final ComponentAdapter<?> componentAdapter : getModifiableComponentAdapterList()) {
        if (componentType.isAssignableFrom(componentAdapter.getComponentImplementation())) {
          final ComponentAdapter<T> typedComponentAdapter = (ComponentAdapter<T>) componentAdapter;
          final T componentInstance = getLocalInstance(typedComponentAdapter);
          adapterToInstanceMap.put(typedComponentAdapter, componentInstance);
        }
      }

      for (final ComponentAdapter<?> componentAdapter : getOrderedComponentAdapters()) {
        final T componentInstance = adapterToInstanceMap.get(componentAdapter);

        if (componentInstance != null) {
          // may be null in the case of the "implicit" addAdapter
          // representing "this".
          result.add(componentInstance);
        }
      }
    }

    return result;
  }

  private <T> T getLocalInstance(final ComponentAdapter<T> typedComponentAdapter) {
    final T componentInstance = typedComponentAdapter.getComponentInstance(this, NOTHING.class);

    // This is to ensure all are added. (Indirect dependencies will be added
    // from InstantiatingComponentAdapter).
    addOrderedComponentAdapter(typedComponentAdapter);
    return componentInstance;
  }

  @Override
  public Object getComponent(final Object keyOrType) {
    return getComponent(keyOrType, null, NOTHING.class);
  }

  @Override
  public Object getComponentInto(final Object keyOrType, final Type into) {
    return getComponent(keyOrType, null, into);
  }

  @Override
  public <T> T getComponent(final Class<T> componentType) {
    return getComponent(Generic.get(componentType));
  }

  @Override
  public <T> T getComponent(final Generic<T> componentType) {
    final Object o = getComponent(componentType, null, NOTHING.class);
    return (T) o;
  }

  public Object getComponent(final Object keyOrType, final Class<? extends Annotation> annotation, final Type into) {
    final ComponentAdapter<?> componentAdapter;
    final Object component;

    if (annotation != null) {
      componentAdapter = getComponentAdapter((Generic<?>) keyOrType, annotation);
      component = componentAdapter == null ? null : getInstance(componentAdapter, null, into);
    } else if (keyOrType instanceof Generic && ((Generic) keyOrType).getType() instanceof Class) {
      componentAdapter = getComponentAdapter((Generic<?>) keyOrType, (NameBinding) null);
      component = componentAdapter == null ? null : getInstance(componentAdapter, (Generic<?>) keyOrType, into);
    } else {
      componentAdapter = getComponentAdapter(keyOrType);
      component = componentAdapter == null ? null : getInstance(componentAdapter, null, into);
    }

    return decorateComponent(component, componentAdapter);
  }

  /**
   * This is invoked when getComponent(..) is called.  It allows extendees to decorate a
   * component before it is returned to the caller.
   *
   * @param component the component that will be returned for getComponent(..)
   * @param componentAdapter the component adapter that made that component
   *
   * @return the component (the same as that passed in by default)
   */
  protected Object decorateComponent(final Object component, final ComponentAdapter<?> componentAdapter) {
    if (componentAdapter instanceof ComponentLifecycle<?>
        && lifecycle.isLazy(componentAdapter) // is Lazy
        && !((ComponentLifecycle<?>) componentAdapter).isStarted()) {
      ((ComponentLifecycle<?>) componentAdapter).start(this);
    }

    return component;
  }

  @Override
  public <T> T getComponent(final Class<T> componentType, final Class<? extends Annotation> binding, final Type into) {
    final Object o = getComponent(Generic.get(componentType), binding, into);
    return componentType.cast(o);
  }

  @Override
  public <T> T getComponent(final Class<T> componentType, final Class<? extends Annotation> binding) {
    return getComponent(componentType, binding, NOTHING.class);
  }

  @Override
  public <T> T getComponentInto(final Class<T> componentType, final Type into) {
    final Object o = getComponent((Object) componentType, null, into);
    return componentType.cast(o);
  }

  @Override
  public <T> T getComponentInto(final Generic<T> componentType, final Type into) {
    final Object o = getComponent(componentType, null, into);
    return (T) o;
  }

  private Object getInstance(final ComponentAdapter<?> componentAdapter, final Generic<?> key, final Type into) {
    // check whether this is our adapter
    // we need to check this to ensure up-down dependencies cannot be followed
    final boolean isLocal = getModifiableComponentAdapterList().contains(componentAdapter);

    if (isLocal || componentAdapter instanceof LateInstance) {
      Object instance;

      try {
        if (componentAdapter instanceof FactoryInjector) {
          instance = ((FactoryInjector) componentAdapter).getComponentInstance(this, into);
        } else {
          instance = componentAdapter.getComponentInstance(this, into);
        }
      } catch (final CyclicDependencyException e) {
        if (parent != null) {
          instance = getParent().getComponentInto(componentAdapter.getComponentKey(), into);
          if (instance != null) {
            return instance;
          }
        }

        throw e;
      }

      addOrderedComponentAdapter(componentAdapter);
      return instance;
    }

    if (parent != null) {
      Object componentKey = key;

      if (componentKey == null) {
        componentKey = componentAdapter.getComponentKey();
      }

      return getParent().getComponentInto(componentKey, into);
    }

    return null;
  }

  @Override
  public PicoContainer getParent() {
    return parent;
  }

  @Override
  public <T> ComponentAdapter<T> removeComponentByInstance(final T componentInstance) {
    for (final ComponentAdapter<?> componentAdapter : getModifiableComponentAdapterList()) {
      if (getLocalInstance(componentAdapter).equals(componentInstance)) {
        return removeComponent(componentAdapter.getComponentKey());
      }
    }

    return null;
  }

  /**
   * Start the components of this PicoContainer and all its logical child containers.
   * The starting of the child container is only attempted if the parent
   * container start successfully.  The child container for which start is attempted
   * is tracked so that upon stop, only those need to be stopped.
   * The lifecycle operation is delegated to the component adapter,
   * if it is an instance of {@link ChangedBehavior lifecycle manager}.
   * The actual {@link LifecycleStrategy lifecycle strategy} supported
   * depends on the concrete implementation of the adapter.
   *
   * @see ChangedBehavior
   * @see LifecycleStrategy
   * @see #makeChildContainer()
   * @see #addChildContainer(PicoContainer)
   * @see #removeChildContainer(PicoContainer)
   */
  @Override
  public synchronized void start() {
    lifecycleState.starting(getName());

    startAdapters();
    childrenStarted.clear();

    for (final PicoContainer child : children) {
      childrenStarted.add(new WeakReference<>(child));

      if (child instanceof Startable) {
        ((Startable) child).start();
      }
    }
  }

  /**
   * Stop the components of this PicoContainer and all its logical child containers.
   * The stopping of the child containers is only attempted for those that have been
   * started, possibly not successfully.
   * The lifecycle operation is delegated to the component adapter,
   * if it is an instance of {@link ChangedBehavior lifecycle manager}.
   * The actual {@link LifecycleStrategy lifecycle strategy} supported
   * depends on the concrete implementation of the adapter.
   *
   * @see ChangedBehavior
   * @see LifecycleStrategy
   * @see #makeChildContainer()
   * @see #addChildContainer(PicoContainer)
   * @see #removeChildContainer(PicoContainer)
   */
  @Override
  public synchronized void stop() {
    lifecycleState.stopping(getName());

    try {
      for (final PicoContainer child : children) {
        if (childStarted(child)) {
          if (child instanceof Startable) {
            ((Startable) child).stop();
          }
        }
      }
    } finally {
      try {
        stopAdapters();
      } finally {
        lifecycleState.stopped();
      }
    }
  }

  /**
   * Checks the status of the child container to see if it's been started
   * to prevent IllegalStateException upon stop
   *
   * @param child the child PicoContainer
   *
   * @return A boolean, {@code true} if the container is started
   */
  private boolean childStarted(final PicoContainer child) {
    for (final WeakReference<PicoContainer> eachChild : childrenStarted) {
      final PicoContainer ref = eachChild.get();

      if (ref == null) {
        continue;
      }

      if (child.equals(ref)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Dispose the components of this PicoContainer and all its logical child containers.
   * The lifecycle operation is delegated to the component adapter,
   * if it is an instance of {@link ChangedBehavior lifecycle manager}.
   * The actual {@link LifecycleStrategy lifecycle strategy} supported
   * depends on the concrete implementation of the adapter.
   *
   * @see ChangedBehavior
   * @see LifecycleStrategy
   * @see #makeChildContainer()
   * @see #addChildContainer(PicoContainer)
   * @see #removeChildContainer(PicoContainer)
   */
  @Override
  public synchronized void dispose() {
    if (lifecycleState.isStarted()) {
      stop();
    }

    lifecycleState.disposing(getName());

    try {
      for (final PicoContainer child : children) {
        if (child instanceof MutablePicoContainer) {
          ((Disposable) child).dispose();
        }
      }
    } finally {
      try {
        disposeAdapters();
        componentFactory.dispose();
      } finally {
        lifecycleState.disposed();
      }
    }
  }

  @Override
  public synchronized void setLifecycleState(final LifecycleState lifecycleState) {
    this.lifecycleState = lifecycleState;
  }

  @Override
  public synchronized LifecycleState getLifecycleState() {
    return lifecycleState;
  }

  @Override
  public MutablePicoContainer makeChildContainer() {
    // noinspection TypeMayBeWeakened
    final DefaultPicoContainer pc = new DefaultPicoContainer(this, lifecycle, monitor, componentFactory);
    addChildContainer(pc);
    return pc;
  }

  /**
   * Checks for identical references in the child container.  It doesn't
   * traverse an entire hierarchy, namely it simply checks for child containers
   * that are equal to the current container.
   *
   * @param child
   */
  private void checkCircularChildDependencies(final PicoContainer child) {
    final String MESSAGE = "Cannot have circular dependency between parent %s and child: %s";
    if (child == this) {
      throw new IllegalArgumentException(String.format(MESSAGE, this, child));
    }

    //Todo: Circular Import Dependency on AbstractDelegatingPicoContainer
    if (child instanceof AbstractDelegatingPicoContainer) {
      AbstractDelegatingPicoContainer delegateChild = (AbstractDelegatingPicoContainer) child;

      while (delegateChild != null) {
        final PicoContainer delegateInstance = delegateChild.getDelegate();

        if (this == delegateInstance) {
          throw new IllegalArgumentException(String.format(MESSAGE, this, child));
        }

        if (delegateInstance instanceof AbstractDelegatingPicoContainer) {
          delegateChild = (AbstractDelegatingPicoContainer) delegateInstance;
        } else {
          delegateChild = null;
        }
      }
    }
  }

  @Override
  public MutablePicoContainer addChildContainer(final PicoContainer child) {
    checkCircularChildDependencies(child);

    if (children.add(child)) {
      // TODO Should only be added if child container has also be started
      if (lifecycleState.isStarted()) {
        childrenStarted.add(new WeakReference<>(child));
      }
    }

    return this;
  }

  @Override
  public boolean removeChildContainer(final PicoContainer child) {
    final boolean result = children.remove(child);
    WeakReference<PicoContainer> foundRef = null;

    for (final WeakReference<PicoContainer> eachChild : childrenStarted) {
      final PicoContainer ref = eachChild.get();

      if (ref.equals(child)) {
        foundRef = eachChild;
        break;
      }
    }

    if (foundRef != null) {
      childrenStarted.remove(foundRef);
    }

    return result;
  }

  @Override
  public MutablePicoContainer change(final Properties... properties) {
    for (final Properties c : properties) {
      final Enumeration<String> e = (Enumeration<String>) c.propertyNames();

      while (e.hasMoreElements()) {
        final String s = e.nextElement();
        containerProperties.setProperty(s, c.getProperty(s));
      }
    }

    return this;
  }

  @Override
  public MutablePicoContainer as(final Properties... properties) {
    return new AsPropertiesPicoContainer(properties);
  }

  @Override
  public void accept(final PicoVisitor visitor) {
    //TODO Pico 3 : change accept signatures to allow abort at any point in the traversal.
    final boolean shouldContinue = visitor.visitContainer(this);

    if (!shouldContinue) {
      return;
    }

    componentFactory.accept(visitor); // will cascade through behaviors
    final Collection<ComponentAdapter<?>> componentAdapters = new ArrayList<>(getComponentAdapters());

    for (final ComponentAdapter<?> componentAdapter : componentAdapters) {
      componentAdapter.accept(visitor);
    }

    final Collection<PicoContainer> allChildren = new ArrayList<>(children);

    for (final PicoContainer child : allChildren) {
      child.accept(visitor);
    }
  }

  /**
   * Changes monitor in the ComponentFactory, the component adapters
   * and the child containers, if these support a ComponentMonitorStrategy.
   */
  @Override
  public ComponentMonitor changeMonitor(final ComponentMonitor newMonitor) {
    final ComponentMonitor returnValue = monitor;
    monitor = newMonitor;

    if (lifecycle instanceof ComponentMonitorStrategy) {
      ((ComponentMonitorStrategy) lifecycle).changeMonitor(newMonitor);
    }

    for (final ComponentAdapter<?> adapter : getModifiableComponentAdapterList()) {
      if (adapter instanceof ComponentMonitorStrategy) {
        ((ComponentMonitorStrategy) adapter).changeMonitor(newMonitor);
      }
    }

    for (final PicoContainer child : children) {
      if (child instanceof ComponentMonitorStrategy) {
        ((ComponentMonitorStrategy) child).changeMonitor(newMonitor);
      }
    }

    return returnValue;
  }

  /**
   * Returns the first current monitor found in the ComponentFactory, the component adapters
   * and the child containers, if these support a ComponentMonitorStrategy.
   *
   * @throws PicoCompositionException if no component monitor is found in container or its children
   */
  @Override
  public ComponentMonitor currentMonitor() {
    return monitor;
  }

  /**
   * Loops over all component adapters and invokes
   * start(PicoContainer) method on the ones which are LifecycleManagers
   */
  private void startAdapters() {
    Collection<ComponentAdapter<?>> adapters = getComponentAdapters();

    for (final ComponentAdapter<?> adapter : adapters) {
      addAdapterIfStartable(adapter);
    }

    adapters = getOrderedComponentAdapters();

    // clone the adapters
    final Collection<ComponentAdapter<?>> adaptersClone = new ArrayList<>(adapters);

    for (final ComponentAdapter<?> adapter : adaptersClone) {
      potentiallyStartAdapter(adapter);
    }
  }

  protected void potentiallyStartAdapter(final ComponentAdapter<?> adapter) {
    if (adapter instanceof ComponentLifecycle) {
      if (!lifecycle.isLazy(adapter)) {
        ((ComponentLifecycle<?>) adapter).start(this);
      }
    }
  }

  private void addAdapterIfStartable(final ComponentAdapter<?> adapter) {
    if (adapter instanceof ComponentLifecycle) {
      final ComponentLifecycle<?> componentLifecycle = (ComponentLifecycle<?>) adapter;

      if (componentLifecycle.componentHasLifecycle()) {
        // create an instance, it will be added to the ordered CA list
        instantiateComponentAsIsStartable(adapter);
        addOrderedComponentAdapter(adapter);
      }
    }
  }

  protected void instantiateComponentAsIsStartable(final ComponentAdapter<?> adapter) {
    if (!lifecycle.isLazy(adapter)) {
      adapter.getComponentInstance(this, NOTHING.class);
    }
  }

  /**
   * Loops over started component adapters (in inverse order) and invokes
   * stop(PicoContainer) method on the ones which are LifecycleManagers
   */
  private void stopAdapters() {
    for (int i = getOrderedComponentAdapters().size() - 1; 0 <= i; i--) {
      final ComponentAdapter<?> adapter = getOrderedComponentAdapters().get(i);

      if (adapter instanceof ComponentLifecycle) {
        final ComponentLifecycle<?> componentLifecycle = (ComponentLifecycle<?>) adapter;

        if (componentLifecycle.componentHasLifecycle() && componentLifecycle.isStarted()) {
          componentLifecycle.stop(this);
        }
      }
    }
  }

  /**
   * Loops over all component adapters (in inverse order) and invokes
   * dispose(PicoContainer) method on the ones which are LifecycleManagers
   */
  private void disposeAdapters() {
    for (int i = getOrderedComponentAdapters().size() - 1; 0 <= i; i--) {
      final ComponentAdapter<?> adapter = getOrderedComponentAdapters().get(i);

      if (adapter instanceof ComponentLifecycle) {
        final ComponentLifecycle<?> componentLifecycle = (ComponentLifecycle<?>) adapter;
        componentLifecycle.dispose(this);
      }
    }
  }

  /**
   * @return the orderedComponentAdapters
   */
  protected List<ComponentAdapter<?>> getOrderedComponentAdapters() {
    return orderedComponentAdapters;
  }

  /**
   * @return the keyToAdapterCache
   */
  protected Map<Object, ComponentAdapter<?>> getComponentKeyToAdapterCache() {
    return keyToAdapterCache;
  }

  /**
   * @return the componentAdapters
   */
  protected List<ComponentAdapter<?>> getModifiableComponentAdapterList() {
    return componentAdapters;
  }

  @Override
  public synchronized void setName(final String name) {
    this.name = name;
  }

  @Override
  public synchronized String getName() {
    return name;
  }

  @Override
  public String toString() {
    return String.format(
        "%s:%d<%s",
        name != null ? name : super.toString(),
        componentAdapters.size(),
        parent != null && !(parent instanceof EmptyPicoContainer) ? parent.toString() : "|"
    );
  }

  /**
   * If this container has a set of converters, then return it.
   * If it does not, and the parent (or their parent ..) does, use that
   * If they do not, return a NullObject implementation (ConversNothing)
   *
   * @return the converters
   */
  @Override
  public synchronized Converters getConverters() {
    if (converters == null) {
      if (parent == null || parent instanceof Converting && ((Converting) parent).getConverters() instanceof ConvertsNothing) {
        converters = new BuiltInConverters();
      } else {
        return ((Converting) parent).getConverters();
      }
    }

    return converters;
  }

  @SuppressWarnings("synthetic-access")
  private class AsPropertiesPicoContainer extends AbstractDelegatingMutablePicoContainer {
    private final Properties properties;

    AsPropertiesPicoContainer(final Properties... props) {
      super(DefaultPicoContainer.this);
      properties = (Properties) containerProperties.clone();

      for (final Properties eachProperty : props) {
        properties.putAll(eachProperty);
      }
    }

    @Override
    public MutablePicoContainer as(final Properties... props) {
      throw new PicoCompositionException("Syntax 'as(FOO).as(BAR)' not allowed, do 'as(FOO, BAR)' instead");
    }

    @Override
    public MutablePicoContainer makeChildContainer() {
      return getDelegate().makeChildContainer();
    }

    @Override
    public <T> BindWithOrTo<T> bind(final Class<T> type) {
      return new DpcBindWithOrTo<>(this, type);
    }

    @Override
    public MutablePicoContainer addComponent(
        final Object key,
        final Object implOrInstance,
        final Parameter... parameters) {
      return DefaultPicoContainer.this.addComponent(key,
          implOrInstance,
          properties,
          new ConstructorParameters(parameters),
          null,
          null
      );
    }

    @Override
    public MutablePicoContainer addComponent(
        final Object key,
        final Object implOrInstance,
        final ConstructorParameters constructorParams,
        final FieldParameters[] fieldParams,
        final MethodParameters[] methodParams) {
      return DefaultPicoContainer.this.addComponent(
          key,
          implOrInstance,
          properties,
          constructorParams,
          fieldParams,
          methodParams
      );
    }

    @Override
    public MutablePicoContainer addComponent(final Object implOrInstance) {
      return DefaultPicoContainer.this.addComponent(implOrInstance, properties);
    }

    @Override
    public MutablePicoContainer addAdapter(final ComponentAdapter<?> componentAdapter) {
      return DefaultPicoContainer.this.addAdapter(componentAdapter, properties);
    }

    @Override
    public MutablePicoContainer addProvider(final Provider<?> provider) {
      return DefaultPicoContainer.this.addAdapter(new ProviderAdapter(provider), properties);
    }

    /**
     * @see MutablePicoContainer#getLifecycleState()
     */
    @Override
    public LifecycleState getLifecycleState() {
      return DefaultPicoContainer.this.getLifecycleState();
    }

    /**
     * @see MutablePicoContainer#getName()
     */
    @Override
    public String getName() {
      return DefaultPicoContainer.this.getName();
    }

    @Override
    public ComponentMonitor changeMonitor(final ComponentMonitor monitor) {
      return DefaultPicoContainer.this.changeMonitor(monitor);
    }

    @Override
    public void start() {
      throw new PicoCompositionException("Cannot have  .as().start()  Register a component or delete the as() statement");
    }

    @Override
    public void stop() {
      throw new PicoCompositionException("Cannot have  .as().stop()  Register a component or delete the as() statement");
    }

    @Override
    public void dispose() {
      throw new PicoCompositionException("Cannot have  .as().dispose()  Register a component or delete the as() statement");
    }
  }
}
