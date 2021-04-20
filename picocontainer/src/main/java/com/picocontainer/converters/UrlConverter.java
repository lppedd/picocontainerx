package com.picocontainer.converters;

import com.picocontainer.PicoCompositionException;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Converts values to URL data type objects
 *
 * @author Paul Hammant
 * @author Michael Rimov
 */
public class UrlConverter implements Converter<URL> {

    public URL convert(final String paramValue) {
        try {
            return new URL(paramValue);
        } catch (MalformedURLException e) {
            throw new PicoCompositionException(e);
        }
    }
}
