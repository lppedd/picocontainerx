package com.picocontainer;

/**
 * Interface for containers that can handle string-to-object conversion in object parameters.
 *
 * @author Paul Hammant
 */
@SuppressWarnings("InterfaceMayBeAnnotatedFunctional")
public interface Converting {
  /**
   * Retrieve the set of converters for transforming string parameters into objects.
   */
  Converters getConverters();
}
