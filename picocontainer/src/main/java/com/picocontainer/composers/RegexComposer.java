/*****************************************************************************
 * Copyright (C) 2003-2011 PicoContainer Committers. All rights reserved.    *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *****************************************************************************/
package com.picocontainer.composers;

import com.picocontainer.ComponentAdapter;
import com.picocontainer.ComponentAdapter.NOTHING;
import com.picocontainer.PicoContainer;
import com.picocontainer.monitors.ComposingMonitor.Composer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Subsets components in a container, the keys for which match a regular expression.
 */
public class RegexComposer implements Composer {
  private final Pattern pattern;
  private final String forNamedComponent;

  public RegexComposer(@NotNull final String pattern, final String forNamedComponent) {
    this.pattern = Pattern.compile(pattern);
    this.forNamedComponent = forNamedComponent;
  }

  public RegexComposer() {
    pattern = null;
    forNamedComponent = null;
  }

  @Nullable
  @Override
  public Object compose(final PicoContainer container, final Object key) {
    if (key instanceof String && (forNamedComponent == null || forNamedComponent.equals(key))) {
      final Pattern pat = pattern == null ? Pattern.compile((String) key) : pattern;
      final Collection<ComponentAdapter<?>> cas = container.getComponentAdapters();
      final Collection<Object> retVal = new ArrayList<>();

      for (final ComponentAdapter<?> componentAdapter : cas) {
        final Object key2 = componentAdapter.getComponentKey();

        if (key2 instanceof String) {
          final Matcher matcher = pat.matcher((String) key2);

          if (matcher.find()) {
            retVal.add(componentAdapter.getComponentInstance(container, NOTHING.class));
          }
        }
      }

      return retVal;
    }

    return null;
  }
}
