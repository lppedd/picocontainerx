/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *****************************************************************************/
package com.picocontainer.visitors;

import com.picocontainer.PicoException;
import com.picocontainer.PicoVisitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Abstract PicoVisitor implementation.
 * A generic traverse method is implemented, that accepts any object which has a method
 * named {@code accept} that takes a {@link PicoVisitor} as argument, and invokes it.
 * Additionally it provides the {@link #checkTraversal()} method, that throws
 * a {@link PicoVisitorTraversalException} if currently no traversal is running.
 *
 * @author J&ouml;rg Schaible
 */
@SuppressWarnings("serial")
public abstract class AbstractPicoVisitor implements PicoVisitor {
  private boolean traversal;

  @Override
  public Object traverse(final Object node) {
    traversal = true;

    final Object retval = AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
      try {
        return node.getClass().getMethod("accept", PicoVisitor.class);
      } catch (final NoSuchMethodException e) {
        return e;
      }
    });

    try {
      if (retval instanceof NoSuchMethodException) {
        throw (NoSuchMethodException) retval;
      }

      final Method accept = (Method) retval;
      accept.invoke(node, this);

      return Void.TYPE;
    } catch (final NoSuchMethodException | IllegalAccessException ignored) {
      //
    } catch (final InvocationTargetException e) {
      final Throwable cause = e.getTargetException();

      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }

      if (cause instanceof Error) {
        throw (Error) cause;
      }
    } finally {
      traversal = false;
    }

    throw new IllegalArgumentException(node.getClass().getName() + " is not a valid type for traversal");
  }

  /**
   * Checks the traversal flag, indicating a currently running traversal of the visitor.
   *
   * @throws PicoVisitorTraversalException if no traversal is active.
   */
  protected void checkTraversal() {
    if (!traversal) {
      throw new PicoVisitorTraversalException(this);
    }
  }

  /**
   * Exception for a {@code PicoVisitor}, that is dependent on a defined starting point of the traversal.
   * If the traversal is not initiated with a call of {@link PicoVisitor#traverse}
   *
   * @author joehni
   */
  public static class PicoVisitorTraversalException extends PicoException {
    /**
     * @param visitor The visitor causing the exception.
     */
    public PicoVisitorTraversalException(final PicoVisitor visitor) {
      super(
          "Traversal for PicoVisitor of type "
              + visitor.getClass().getName()
              + " must start with the visitor's traverse method"
      );
    }
  }
}
