package net.bytebuddy.agent.builder;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.lang.annotation.Annotation;
import java.security.ProtectionDomain;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderInitializationStrategyTest {

    private static final byte[] QUX = new byte[]{1, 2, 3}, BAZ = new byte[]{4, 5, 6};

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private DynamicType.Builder<?> builder;

    @Mock
    private DynamicType dynamicType;

    @Mock
    private ClassLoader classLoader;

    @Mock
    private ProtectionDomain protectionDomain;

    @Mock
    private AgentBuilder.InjectionStrategy injectionStrategy;

    @Test
    public void testNoOp() throws Exception {
        assertThat(AgentBuilder.Default.InitializationStrategy.NoOp.INSTANCE.dispatcher(),
                is((AgentBuilder.InitializationStrategy.Dispatcher) AgentBuilder.Default.InitializationStrategy.NoOp.INSTANCE));
    }

    @Test
    public void testNoOpApplication() throws Exception {
        assertThat(AgentBuilder.Default.InitializationStrategy.NoOp.INSTANCE.apply(builder), is((DynamicType.Builder) builder));
    }

    @Test
    public void testNoOpRegistration() throws Exception {
        AgentBuilder.Default.InitializationStrategy.NoOp.INSTANCE.register(dynamicType, classLoader, protectionDomain, injectionStrategy);
        verifyNoMoreInteractions(dynamicType);
        verifyNoMoreInteractions(classLoader);
        verifyNoMoreInteractions(injectionStrategy);
    }

    @Test
    public void testPremature() throws Exception {
        assertThat(AgentBuilder.InitializationStrategy.Minimal.INSTANCE.dispatcher(),
                is((AgentBuilder.InitializationStrategy.Dispatcher) AgentBuilder.InitializationStrategy.Minimal.INSTANCE));
    }

    @Test
    public void testPrematureApplication() throws Exception {
        assertThat(AgentBuilder.InitializationStrategy.Minimal.INSTANCE.apply(builder), is((DynamicType.Builder) builder));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testMinimalRegistrationIndependentType() throws Exception {
        Annotation eagerAnnotation = mock(AuxiliaryType.SignatureRelevant.class);
        when(eagerAnnotation.annotationType()).thenReturn((Class) AuxiliaryType.SignatureRelevant.class);
        TypeDescription independent = mock(TypeDescription.class), dependent = mock(TypeDescription.class);
        when(independent.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(eagerAnnotation));
        when(dependent.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(dynamicType.getAuxiliaryTypeDescriptions()).thenReturn(new HashSet<TypeDescription>(Arrays.asList(independent, dependent)));
        Map<TypeDescription, byte[]> auxiliaryTypes = new HashMap<TypeDescription, byte[]>();
        auxiliaryTypes.put(independent, QUX);
        auxiliaryTypes.put(dependent, BAZ);
        when(dynamicType.getAuxiliaryTypes()).thenReturn(auxiliaryTypes);
        ClassInjector classInjector = mock(ClassInjector.class);
        when(injectionStrategy.resolve(classLoader, protectionDomain)).thenReturn(classInjector);
        when(classInjector.inject(Collections.singleton(independent), dynamicType))
                .thenReturn(Collections.<TypeDescription, Class<?>>singletonMap(independent, Foo.class));
        LoadedTypeInitializer loadedTypeInitializer = mock(LoadedTypeInitializer.class);
        when(dynamicType.getLoadedTypeInitializers()).thenReturn(Collections.singletonMap(independent, loadedTypeInitializer));
        AgentBuilder.InitializationStrategy.Minimal.INSTANCE.register(dynamicType, classLoader, protectionDomain, injectionStrategy);
        verify(classInjector).inject(Collections.singleton(independent), dynamicType);
        verifyNoMoreInteractions(classInjector);
        verify(loadedTypeInitializer).onLoad(Foo.class);
        verifyNoMoreInteractions(loadedTypeInitializer);
    }

    @Test
    public void testMinimalRegistrationDependentType() throws Exception {
        TypeDescription dependent = mock(TypeDescription.class);
        when(dependent.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(dynamicType.getAuxiliaryTypes()).thenReturn(Collections.singletonMap(dependent, BAZ));
        AgentBuilder.InitializationStrategy.Minimal.INSTANCE.register(dynamicType, classLoader, protectionDomain, injectionStrategy);
        verifyNoMoreInteractions(injectionStrategy);
    }

    private static class Foo {
        /* empty */
    }
}
