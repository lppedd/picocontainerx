package com.picocontainer.adapters;

import java.lang.reflect.Type;

import com.picocontainer.ComponentAdapter;
import com.picocontainer.PicoContainer;
import com.picocontainer.PicoVisitor;

public class NullCA implements ComponentAdapter {

    private final Object key;

    public NullCA(final Object key) {
        this.key = key;
    }

    @Override
    public Object getComponentKey() {
        return key;
    }

    @Override
    public Class getComponentImplementation() {
        return NOTHING.class;
    }

    @Override
    public Object getComponentInstance(final PicoContainer container, final Type into)  {
        return null;
    }

    @Override
    public void verify(final PicoContainer container)  {
    }

    @Override
    public void accept(final PicoVisitor visitor) {
    }

    @Override
    public ComponentAdapter getDelegate() {
        return null;
    }

    @Override
    public ComponentAdapter findAdapterOfType(final Class adapterType) {
        return null;
    }

    @Override
    public String getDescriptor() {
        return "Null-CA";
    }
}
