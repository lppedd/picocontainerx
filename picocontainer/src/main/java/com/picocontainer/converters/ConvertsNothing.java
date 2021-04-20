package com.picocontainer.converters;

import com.picocontainer.Converters;

import java.lang.reflect.Type;

/**
 * Null-object implementation of Converters
 */
public class ConvertsNothing implements Converters {
    public boolean canConvert(final Type type) {
        return false;
    }

    public Object convert(final String paramValue, final Type type) {
        return null;
    }
}
