package com.picocontainer.converters;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Converts a value to an object via its single-String constructor.
 */
public class NewInstanceConverter implements Converter<Object> {
    private Constructor<?> c;

    public NewInstanceConverter(final Class<?> clazz) {
        try {
            c = clazz.getConstructor(String.class);
        } catch (NoSuchMethodException e) {
        }
    }
    public Object convert(final String paramValue) {
        if (c == null) {
            return null;
        }
        try {
            return c.newInstance(paramValue);
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        } catch (InstantiationException e) {
        }
        return null;
    }
}
