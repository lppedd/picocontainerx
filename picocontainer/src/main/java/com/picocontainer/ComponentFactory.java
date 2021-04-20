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

import com.picocontainer.parameters.ConstructorParameters;
import com.picocontainer.parameters.FieldParameters;
import com.picocontainer.parameters.MethodParameters;

import java.util.Properties;

/**
 * A component factory is responsible for creating {@link ComponentAdapter} component adapters.
 * The main use of the component factory is inside {@link DefaultPicoContainer#DefaultPicoContainer(ComponentFactory)},
 * where it can be used to customize the default component adapter that is used when none is specified explicitly.
 *
 * @author Paul Hammant
 * @author Mauro Talevi
 * @author Jon Tirs&eacute;n
 */
public interface ComponentFactory {
  /**
   * Create a new component adapter based on the specified arguments.
   *
   * @param monitor the component monitor
   * @param lifecycle te lifecycle strategy
   * @param componentProps the component properties
   * @param key the key to be associated with this adapter. This
   * value should be returned from a call to
   * {@link ComponentAdapter#getComponentKey()} on the created
   * adapter.
   * @param impl the implementation class to be associated
   * with this adapter. This value should be returned from a call
   * to {@link ComponentAdapter#getComponentImplementation()} on
   * the created adapter. Should not be null.
   * @param constructorParams TODO
   * @param fieldParams TODO
   * @param methodParams TODO
   * @return a new component adapter based on the specified arguments. Should
   * not return null.
   */
  <T> ComponentAdapter<T> createComponentAdapter(
      final ComponentMonitor monitor,
      final LifecycleStrategy lifecycle,
      final Properties componentProps,
      final Object key,
      final Class<T> impl,
      final ConstructorParameters constructorParams,
      final FieldParameters[] fieldParams,
      final MethodParameters[] methodParams
  );

  /**
   * Verification for the {@code ComponentFactory} - subject to implementation.
   *
   * @param container the {@link PicoContainer} that is used for verification.
   */
  void verify(final PicoContainer container);

  /**
   * Accepts a visitor for this {@code ComponentFactory}.
   * The method is normally called by visiting a {@link PicoContainer},
   * that cascades the visitor also down to all its ComponentFactory instances.
   *
   * @param visitor the visitor.
   */
  void accept(final PicoVisitor visitor);

  void dispose();
}
