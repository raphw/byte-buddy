package net.bytebuddy.utility.visitor;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;

public class ContextClassVisitorTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private DynamicType dynamicType;

    @Mock
    private LoadedTypeInitializer loadedTypeInitializer;

    @Test
    public void testPassive() {
        new TestContextClassVisitor(Collections.<DynamicType>emptyList(), loadedTypeInitializer).visitEnd();
    }

    @Test(expected = IllegalStateException.class)
    public void testPassiveLoadedTypeInitializer() {
        when(loadedTypeInitializer.isAlive()).thenReturn(true);
        new TestContextClassVisitor(Collections.<DynamicType>emptyList(), loadedTypeInitializer).visitEnd();
    }

    @Test(expected = IllegalStateException.class)
    public void testPassiveDynamicType() {
        new TestContextClassVisitor(Collections.singletonList(dynamicType), loadedTypeInitializer).visitEnd();
    }

    @Test
    public void testActive() {
        when(loadedTypeInitializer.isAlive()).thenReturn(true);
        new TestContextClassVisitor(Collections.singletonList(dynamicType), loadedTypeInitializer).active().visitEnd();
    }

    private static class TestContextClassVisitor extends ContextClassVisitor {

        private final List<DynamicType> dynamicTypes;

        private final LoadedTypeInitializer loadedTypeInitializer;

        private TestContextClassVisitor(List<DynamicType> dynamicTypes, LoadedTypeInitializer loadedTypeInitializer) {
            super(null);
            this.dynamicTypes = dynamicTypes;
            this.loadedTypeInitializer = loadedTypeInitializer;
        }

        @Override
        public List<DynamicType> getAuxiliaryTypes() {
            return dynamicTypes;
        }

        @Override
        public LoadedTypeInitializer getLoadedTypeInitializer() {
            return loadedTypeInitializer;
        }
    }
}