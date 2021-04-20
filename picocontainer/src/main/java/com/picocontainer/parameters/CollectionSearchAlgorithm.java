package com.picocontainer.parameters;

import com.googlecode.jtype.Generic;
import com.picocontainer.ComponentAdapter;
import com.picocontainer.PicoContainer;

import java.util.Map;

public interface CollectionSearchAlgorithm {

	Map<Object, ComponentAdapter<?>> getMatchingComponentAdapters(PicoContainer container, ComponentAdapter adapter,
                                 Class keyType, Generic<?> valueType);

}
