package com.picocontainer;

/**
 * TODO
 */
@SuppressWarnings("InterfaceMayBeAnnotatedFunctional")
public interface Decorator {
  Object decorate(final Object instance);
}
