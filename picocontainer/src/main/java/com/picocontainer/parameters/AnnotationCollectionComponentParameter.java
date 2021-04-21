package com.picocontainer.parameters;

import com.googlecode.jtype.Generic;
import com.picocontainer.ComponentAdapter;

import java.io.Serializable;
import java.lang.annotation.Annotation;

/**
 * <p>
 * Will match all objects as parameters that have the given class-based annotation marker.<br>
 * Example, assuming the given classes:
 * </p>
 * <pre>
 * {@literal @Singleton}
 * class A {
 *
 * }
 *
 * {@literal @Singleton}
 * class B {
 *
 * }
 *
 * class C {
 *
 * }
 *
 * public class SingletonRegistry {
 *    public Object[] allSingletons;
 *
 *    public SingletonRegistry(Object[] allSingletons) {
 *       this.allSingletons = allSingletons;
 *    }
 * }
 * </pre>
 * <p>In your assembly script then use:</p>
 * <pre>
 * MutablePicoContainer pico = new PicoBuilder().build();
 * pico.addComponent(A.class)
 *    .addClass(B.class)
 *    .addClass(C.class)
 *    .addClass(
 *        SingletonRegistry.class,
 *        SingletonRegistry.class,
 *        // Collect all classes marked singleton.
 *        new AnnotationCollectionParameter(Singleton.class)
 *     );
 *
 * SingletonRegistry registry = pico.getComponent(SingletonRegistry.class);
 *
 * // Should have A &amp; B, but not C.
 * assertEquals(2, registry.allSingletons.length);
 * </pre>
 *
 * @author Michael Rimov
 */
@SuppressWarnings("serial")
public class AnnotationCollectionComponentParameter extends CollectionComponentParameter implements Serializable {
  private final Class<? extends Annotation> annotationType;

  public AnnotationCollectionComponentParameter(final Class<? extends Annotation> annotationType) {
    this.annotationType = annotationType;
  }

  public AnnotationCollectionComponentParameter(
      final Class<? extends Annotation> annotationType,
      final boolean emptyCollection) {
    super(emptyCollection);
    this.annotationType = annotationType;
  }

  public AnnotationCollectionComponentParameter(
      final Class<? extends Annotation> annotationType,
      final Class<?> keyType,
      final Generic<?> componentValueType,
      final boolean emptyCollection) {
    super(keyType, componentValueType, emptyCollection);
    this.annotationType = annotationType;
  }

  public AnnotationCollectionComponentParameter(
      final Class<? extends Annotation> annotationType,
      final Generic<?> componentValueType,
      final boolean emptyCollection) {
    super(componentValueType, emptyCollection);
    this.annotationType = annotationType;
  }

  @Override
  protected boolean evaluate(final ComponentAdapter adapter) {
    return adapter != null && adapter.getComponentImplementation().isAnnotationPresent(annotationType);
  }
}
