package net.bytebuddy.agent.builder;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class AgentBuilderDefaultTransformationSimpleResolutionAuxiliaryTypeInitializerTest {

    private static final byte[] FOO = new byte[]{1, 2, 3};

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private DynamicType dynamicType;

    @Mock
    private AgentBuilder.Default.BootstrapInjectionStrategy bootstrapInjectionStrategy;

    @Mock
    private ClassLoader classLoader;

    @Mock
    private ProtectionDomain protectionDomain;

    @Mock
    private ClassInjector classInjector;

    @Mock
    private TypeDescription instrumentedType, auxiliaryType;

    @Mock
    private LoadedTypeInitializer loadedTypeInitializer, auxiliaryInitializer;

    private AccessControlContext accessControlContext;

    @Before
    public void setUp() throws Exception {
        accessControlContext = AccessController.getContext();
    }

    @Test
    public void testCreationWithAuxiliaryTypes() throws Exception {
        Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers = new HashMap<TypeDescription, LoadedTypeInitializer>();
        loadedTypeInitializers.put(instrumentedType, loadedTypeInitializer);
        loadedTypeInitializers.put(auxiliaryType, auxiliaryInitializer);
        when(dynamicType.getTypeDescription()).thenReturn(instrumentedType);
        when(dynamicType.getLoadedTypeInitializers()).thenReturn(loadedTypeInitializers);
        when(dynamicType.getRawAuxiliaryTypes()).thenReturn(Collections.singletonMap(auxiliaryType, FOO));
        assertThat(AgentBuilder.Default.Transformation.Simple.Resolution.AuxiliaryTypeInitializer.of(bootstrapInjectionStrategy,
                dynamicType,
                classLoader,
                protectionDomain,
                accessControlContext), is((AgentBuilder.InitializationStrategy.Dispatcher.LazyInitializer)
                new AgentBuilder.Default.Transformation.Simple.Resolution.AuxiliaryTypeInitializer(bootstrapInjectionStrategy,
                        instrumentedType,
                        classLoader,
                        protectionDomain,
                        accessControlContext,
                        Collections.singletonMap(auxiliaryType, FOO),
                        loadedTypeInitializers)));
    }

    @Test
    public void testCreationWithoutAuxiliaryTypes() throws Exception {
        when(dynamicType.getTypeDescription()).thenReturn(instrumentedType);
        when(dynamicType.getLoadedTypeInitializers()).thenReturn(Collections.singletonMap(instrumentedType, loadedTypeInitializer));
        when(dynamicType.getRawAuxiliaryTypes()).thenReturn(Collections.<TypeDescription, byte[]>emptyMap());
        assertThat(AgentBuilder.Default.Transformation.Simple.Resolution.AuxiliaryTypeInitializer.of(bootstrapInjectionStrategy,
                dynamicType,
                classLoader,
                protectionDomain,
                accessControlContext),
                is((AgentBuilder.InitializationStrategy.Dispatcher.LazyInitializer)
                        new AgentBuilder.InitializationStrategy.Dispatcher.LazyInitializer.Simple(loadedTypeInitializer)));
    }

    @Test
    public void testInjection() throws Exception {
        when(classInjector.inject(Collections.singletonMap(auxiliaryType, FOO)))
                .thenReturn(Collections.<TypeDescription, Class<?>>singletonMap(auxiliaryType, Bar.class));
        Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers = new HashMap<TypeDescription, LoadedTypeInitializer>();
        loadedTypeInitializers.put(instrumentedType, loadedTypeInitializer);
        loadedTypeInitializers.put(auxiliaryType, auxiliaryInitializer);
        LoadedTypeInitializer loadedTypeInitializer = new AgentBuilder.Default.Transformation.Simple.Resolution.AuxiliaryTypeInitializer.InjectingInitializer(instrumentedType,
                Collections.singletonMap(auxiliaryType, FOO),
                loadedTypeInitializers,
                classInjector);
        assertThat(loadedTypeInitializer.isAlive(), is(true));
        loadedTypeInitializer.onLoad(Foo.class);
        verify(this.loadedTypeInitializer).onLoad(Foo.class);
        verifyNoMoreInteractions(this.loadedTypeInitializer);
        verify(auxiliaryInitializer).onLoad(Bar.class);
        verifyNoMoreInteractions(auxiliaryInitializer);
        verify(classInjector).inject(Collections.singletonMap(auxiliaryType, FOO));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.Default.Transformation.Simple.Resolution.AuxiliaryTypeInitializer.class)
                .create(new ObjectPropertyAssertion.Creator<AccessControlContext>() {
                    @Override
                    public AccessControlContext create() {
                        return new AccessControlContext(new ProtectionDomain[]{mock(ProtectionDomain.class)});
                    }
                }).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.Transformation.Simple.Resolution.AuxiliaryTypeInitializer.InjectingInitializer.class)
                .create(new ObjectPropertyAssertion.Creator<AccessControlContext>() {
                    @Override
                    public AccessControlContext create() {
                        return new AccessControlContext(new ProtectionDomain[]{mock(ProtectionDomain.class)});
                    }
                }).apply();
    }

    private static class Foo {
        /* empty */
    }

    private static class Bar {
        /* empty */
    }
}
