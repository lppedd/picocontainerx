package com.picocontainer.parameters;

import com.googlecode.jtype.Generic;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.PicoContainer;

import java.util.Map;

/**
 * TODO
 */
@SuppressWarnings("InterfaceMayBeAnnotatedFunctional")
public interface CollectionSearchAlgorithm {
  Map<Object, ComponentAdapter<?>> getMatchingComponentAdapters(
      final PicoContainer container,
      final ComponentAdapter<?> adapter,
      final Class<?> keyType,
      final Generic<?> valueType
  );
}
