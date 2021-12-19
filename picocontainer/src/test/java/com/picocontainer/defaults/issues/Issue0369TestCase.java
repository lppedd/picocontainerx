package com.picocontainer.defaults.issues;

import static junit.framework.Assert.assertSame;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.picocontainer.ComponentAdapter;
import com.picocontainer.PicoCompositionException;
import com.picocontainer.PicoContainer;
import com.picocontainer.PicoVisitor;

public class Issue0369TestCase {

    @Test
    public void simpleProofOfChangeInGetComponent() {
        MyAdapter mya = new MyAdapter();
        Class<? extends List> impl = mya.getComponentImplementation();
        assertSame(ArrayList.class, impl);
    }

    public class MyAdapter implements ComponentAdapter<List> {

        private final Class<? extends List> implementationclass = ArrayList.class;

        @Override
        public Class<? extends List> getComponentImplementation() {
            return implementationclass;
        }

        @Override
        public Object getComponentKey() {
            return null;
        }

        @Override
        public List getComponentInstance(final PicoContainer container, final Type into) throws PicoCompositionException {
            return null;
        }

        @Override
        public void verify(final PicoContainer container) throws PicoCompositionException {

        }

        @Override
        public void accept(final PicoVisitor visitor) {

        }

        @Override
        public ComponentAdapter<List> getDelegate() {
            return null;
        }

        @Override
        public <U extends ComponentAdapter<?>> U findAdapterOfType(final Class<U> adapterType) {
            return null;
        }

        @Override
        public String getDescriptor() {
            return null;
        }
    }

}
