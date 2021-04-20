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

import java.util.Map;
import java.util.Properties;

/**
 * Collection of immutable properties, holding behaviour characteristics.  See
 * <a href="http://www.picocontainer.org/behaviors.html">The PicoContainer Website</a> for details on the usage
 * of Characteristics.
 *
 * @author Paul Hammant
 * @see com.picocontainer.ComponentAdapter
 * @see ChangedBehavior
 */
@SuppressWarnings("serial")
public final class Characteristics {

    private static final String _INJECTION = "injection";
    private static final String _NONE = "none";
    private static final String _CONSTRUCTOR = "constructor";
    private static final String _METHOD = "method";
    private static final String _SETTER = "setter";
    private static final String _CACHE = "cache";
    private static final String _SYNCHRONIZING = "synchronizing";
    private static final String _LOCKING = "locking";
    private static final String _HIDE_IMPL = "hide-impl";
    private static final String _PROPERTY_APPLYING = "property-applying";
    private static final String _AUTOMATIC = "automatic";
    private static final String _USE_NAMES = "use-parameter-names";
    private static final String  _USE_ALL_PARAMETERS = "use-all-parameters";
    private static final String _ENABLE_CIRCULAR = "enable-circular";
    private static final String _GUARD = "guard";
    private static final String _EMJECTION = "emjection_enabled";
    private static final String _STATIC_INJECTION = "static-injection";

    /**
     * Since properties use strings, we supply String constants for Boolean conditions.
     */
    public static final String FALSE = "false";

    /**
     * Since properties use strings, we supply String constants for Boolean conditions.
     */
    public static final String TRUE = "true";

    /**
     * Turns on constructor injection.
     * @see com.picocontainer.injectors.ConstructorInjection
     */
    public static final Properties CDI = immutable(_INJECTION, _CONSTRUCTOR);

    /**
     * Turns on Setter Injection.
     * @see com.picocontainer.injectors.SetterInjection
     */
    public static final Properties SDI = immutable(_INJECTION, _SETTER);

    /**
     * Turns on Method Injection.
     */
    public static final Properties METHOD_INJECTION = immutable(_INJECTION, _METHOD);


    /**
     * Turn on static injection.
     */
    public static final Properties STATIC_INJECTION = immutable(_STATIC_INJECTION, TRUE);

    /**
     * Turn off static injection.
     */
    public static final Properties NO_STATIC_INJECTION = immutable(_STATIC_INJECTION, FALSE);


    /**
     * Turns off Caching of component instances.  (Often referred to in other circles
     * as singleton).
     * @see com.picocontainer.behaviors.Caching
     */
    public static final Properties NO_CACHE = immutable(_CACHE, FALSE);

    /**
     * Turns on Caching of component instances.  (Often referred to in other circles
     * as singleton)
     * @see com.picocontainer.behaviors.Caching
     */
    public static final Properties CACHE = immutable(_CACHE, TRUE);

    /**
     * Turns on synchronized access to the component instance.  (Under JDK 1.5 conditions,
     * it will be better to use {@link #LOCK} instead.
     * @see com.picocontainer.behaviors.Synchronizing
     */
    public static final Properties SYNCHRONIZE = immutable(_SYNCHRONIZING, TRUE);


    /**
     * Turns off synchronized access to the component instance.
     * @see com.picocontainer.behaviors.Synchronizing
     */
    public static final Properties NO_SYNCHRONIZE = immutable(_SYNCHRONIZING, FALSE);

    /**
     * Uses a java.util.concurrent.Lock to provide faster access than synchronized.
     * @see com.picocontainer.behaviors.Locking
     */
    public static final Properties LOCK = immutable(_LOCKING, TRUE);

    /**
     * Turns off locking synchronization.
     * @see com.picocontainer.behaviors.Locking
     */
    public static final Properties NO_LOCK = immutable(_LOCKING, FALSE);

    /**
     * Synonym for {@link #CACHE CACHE}.
     * @see com.picocontainer.behaviors.Caching
     */
    public static final Properties SINGLE = CACHE;

    /**
     * Synonym for {@link #NO_CACHE NO_CACHE}.
     * @see com.picocontainer.behaviors.Caching
     */
    public static final Properties NO_SINGLE = NO_CACHE;

    /**
     * Turns on implementation hiding.  You may use the JDK Proxy implementation included
     * in this version, <strong>or</strong> the ASM-based implementation hiding method
     * included in PicoContainer Gems.  However, you cannot use both in a single PicoContainer
     * instance.
     */
    public static final Properties HIDE_IMPL = immutable(_HIDE_IMPL, TRUE);

    /**
     * Turns off implementation hiding.
     * @see #HIDE_IMPL for more information.
     */
    public static final Properties NO_HIDE_IMPL = immutable(_HIDE_IMPL, FALSE);

    public static final Properties ENABLE_CIRCULAR = immutable(_ENABLE_CIRCULAR, TRUE);

    public static final Properties NONE = immutable(_NONE, "");

    /**
     * Turns on bean-setting property applications where certain simple properties are set
     * after the object is created based.
     */
    public static final Properties PROPERTY_APPLYING = immutable(_PROPERTY_APPLYING, TRUE);

    /**
     * Turns off bean-setting property applications.
     * @see com.picocontainer.behaviors.PropertyApplying
     */
    public static final Properties NO_PROPERTY_APPLYING = immutable(_PROPERTY_APPLYING, FALSE);

    public static final Properties AUTOMATIC = immutable(_AUTOMATIC, TRUE);

    public static final Properties USE_NAMES = immutable(_USE_NAMES, TRUE);

    public static final Properties ALLOW_UNUSED_PARAMETERS = immutable(_USE_ALL_PARAMETERS, FALSE);

    public static final Properties EMJECTION_ENABLED = immutable(_EMJECTION, TRUE);

    public static final Properties GUARD = immutable(_GUARD, "guard");

    public static final Properties GUARD(final String with) {
        return immutable(_GUARD, with);
    };

    /**
     * Transforms a single name value pair unto a <em>read only</em> {@linkplain Properties}
     * instance.
     * <p>Example Usage:</p>
     * <pre>
     * 		Properties readOnly = immutable("oneKey","oneValue"};
     * 		assert readOnly.getProperty("oneKey") != null);
     * </pre>
     * @param name the property key.
     * @param value the property value.
     * @return Read Only properties instance.
     */
    public static Properties immutable(final String name, final String value) {
        return new ImmutableProperties(name, value);
    }

    /**
     * Read only property set.  Once constructed, all methods that modify state will
     * throw UnsupportedOperationException.
     * @author Paul Hammant.
     */
    public static class ImmutableProperties extends Properties {

        private boolean sealed = false;

        public ImmutableProperties(final String name, final String value) {
            super.setProperty(name, value);
            sealed = true;
        }

        /**
         * Read Only Object:  will throw UnsupportedOperationException.
         */
        @Override
        public Object remove(final Object o) {
            throw new UnsupportedOperationException("immutable properties are read only");
        }

        /**
         * Read Only Object:  will throw UnsupportedOperationException.
         */
        @Override
        public synchronized Object setProperty(final String string, final String string1) {
            throw new UnsupportedOperationException("immutable properties are read only");
        }

        /**
         * Read Only Object:  will throw UnsupportedOperationException.
         */
		@Override
		public synchronized void clear() {
            throw new UnsupportedOperationException("immutable properties are read only");
		}

		/**
		 * Once object is constructed, this will throw UnsupportedOperationException because
		 * this class is a read only wrapper.
		 */
		@Override
		public synchronized Object put(final Object key, final Object value) {
			if (!sealed) {
				//setProperty calls put, so until the object is fully constructed, we
				//cannot seal it.
				return super.put(key, value);
			}

            throw new UnsupportedOperationException("immutable properties are read only");
		}

        /**
         * Read Only Object:  will throw UnsupportedOperationException.
         */
		@Override
		public synchronized void putAll(final Map<? extends Object, ? extends Object> t) {
            throw new UnsupportedOperationException("immutable properties are read only");
		}


    }

}
