package net.bytebuddy.agent.builder;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.Nexus;
import net.bytebuddy.dynamic.NexusAccessor;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.stubbing.Answer;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.test.utility.FieldByFieldComparison.matchesPrototype;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderInitializationStrategySelfInjectionDispatcherTest {

    private static final int IDENTIFIER = 42;

    private static final byte[] FOO = new byte[]{1, 2, 3}, BAR = new byte[]{4, 5, 6};

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private DynamicType.Builder<?> builder, appendedBuilder;

    @Mock
    private DynamicType dynamicType;

    @Mock
    private AgentBuilder.InjectionStrategy injectionStrategy;

    @Mock
    private ClassInjector classInjector;

    @Mock
    private TypeDescription instrumented, dependent, independent;

    @Mock
    private LoadedTypeInitializer instrumentedInitializer, dependentInitializer, independentInitializer;

    private NexusAccessor nexusAccessor = new NexusAccessor();

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(builder.initializer((any(ByteCodeAppender.class)))).thenReturn((DynamicType.Builder) appendedBuilder);
        when(injectionStrategy.resolve(Qux.class.getClassLoader(), Qux.class.getProtectionDomain())).thenReturn(classInjector);
        when(dynamicType.getTypeDescription()).thenReturn(instrumented);
        Map<TypeDescription, byte[]> auxiliaryTypes = new HashMap<TypeDescription, byte[]>();
        auxiliaryTypes.put(dependent, FOO);
        auxiliaryTypes.put(independent, BAR);
        when(dynamicType.getAuxiliaryTypes()).thenReturn(auxiliaryTypes);
        Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers = new HashMap<TypeDescription, LoadedTypeInitializer>();
        loadedTypeInitializers.put(instrumented, instrumentedInitializer);
        loadedTypeInitializers.put(dependent, dependentInitializer);
        loadedTypeInitializers.put(independent, independentInitializer);
        when(dynamicType.getLoadedTypeInitializers()).thenReturn(loadedTypeInitializers);
        when(instrumented.getName()).thenReturn(Qux.class.getName());
        when(classInjector.inject(any(Map.class))).then(new Answer<Map<TypeDescription, Class<?>>>() {
            public Map<TypeDescription, Class<?>> answer(InvocationOnMock invocationOnMock) throws Throwable {
                Map<TypeDescription, Class<?>> loaded = new HashMap<TypeDescription, Class<?>>();
                for (TypeDescription typeDescription : ((Map<TypeDescription, byte[]>) invocationOnMock.getArguments()[0]).keySet()) {
                    if (typeDescription.equals(dependent)) {
                        loaded.put(dependent, Foo.class);
                    } else if (typeDescription.equals(independent)) {
                        loaded.put(independent, Bar.class);
                    } else {
                        throw new AssertionError();
                    }
                }
                return loaded;
            }
        });
        Annotation eagerAnnotation = mock(AuxiliaryType.SignatureRelevant.class);
        when(eagerAnnotation.annotationType()).thenReturn((Class) AuxiliaryType.SignatureRelevant.class);
        when(independent.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(eagerAnnotation));
        when(dependent.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(instrumentedInitializer.isAlive()).thenReturn(true);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSplitInitialization() throws Exception {
        AgentBuilder.InitializationStrategy.Dispatcher dispatcher = new AgentBuilder.InitializationStrategy.SelfInjection.Split.Dispatcher(nexusAccessor, IDENTIFIER);
        assertThat(dispatcher.apply(builder), is((DynamicType.Builder) appendedBuilder));
        verify(builder).initializer(matchesPrototype(new NexusAccessor.InitializationAppender(IDENTIFIER)));
        verifyNoMoreInteractions(builder);
        verifyNoMoreInteractions(appendedBuilder);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLazyInitialization() throws Exception {
        AgentBuilder.InitializationStrategy.Dispatcher dispatcher = new AgentBuilder.InitializationStrategy.SelfInjection.Lazy.Dispatcher(nexusAccessor, IDENTIFIER);
        assertThat(dispatcher.apply(builder), is((DynamicType.Builder) appendedBuilder));
        verify(builder).initializer(matchesPrototype(new NexusAccessor.InitializationAppender(IDENTIFIER)));
        verifyNoMoreInteractions(builder);
        verifyNoMoreInteractions(appendedBuilder);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEagerInitialization() throws Exception {
        AgentBuilder.InitializationStrategy.Dispatcher dispatcher = new AgentBuilder.InitializationStrategy.SelfInjection.Eager.Dispatcher(nexusAccessor, IDENTIFIER);
        assertThat(dispatcher.apply(builder), is((DynamicType.Builder) appendedBuilder));
        verify(builder).initializer(matchesPrototype(new NexusAccessor.InitializationAppender(IDENTIFIER)));
        verifyNoMoreInteractions(builder);
        verifyNoMoreInteractions(appendedBuilder);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSplit() throws Exception {
        AgentBuilder.InitializationStrategy.Dispatcher dispatcher = new AgentBuilder.InitializationStrategy.SelfInjection.Split.Dispatcher(nexusAccessor, IDENTIFIER);
        dispatcher.register(dynamicType, Qux.class.getClassLoader(), Qux.class.getProtectionDomain(), injectionStrategy);
        verify(classInjector).inject(Collections.singletonMap(independent, BAR));
        verifyNoMoreInteractions(classInjector);
        verify(independentInitializer).onLoad(Bar.class);
        verifyNoMoreInteractions(independentInitializer);
        Nexus.initialize(Qux.class, IDENTIFIER);
        verify(classInjector).inject(Collections.singletonMap(dependent, FOO));
        verifyNoMoreInteractions(classInjector);
        verify(dependentInitializer).onLoad(Foo.class);
        verifyNoMoreInteractions(dependentInitializer);
        verify(instrumentedInitializer).onLoad(Qux.class);
        verifyNoMoreInteractions(instrumentedInitializer);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEager() throws Exception {
        AgentBuilder.InitializationStrategy.Dispatcher dispatcher = new AgentBuilder.InitializationStrategy.SelfInjection.Eager.Dispatcher(nexusAccessor, IDENTIFIER);
        dispatcher.register(dynamicType, Qux.class.getClassLoader(), Qux.class.getProtectionDomain(), injectionStrategy);
        Map<TypeDescription, byte[]> injected = new HashMap<TypeDescription, byte[]>();
        injected.put(independent, BAR);
        injected.put(dependent, FOO);
        verify(classInjector).inject(injected);
        verifyNoMoreInteractions(classInjector);
        verify(independentInitializer).onLoad(Bar.class);
        verifyNoMoreInteractions(independentInitializer);
        verify(dependentInitializer).onLoad(Foo.class);
        verifyNoMoreInteractions(dependentInitializer);
        Nexus.initialize(Qux.class, IDENTIFIER);
        verify(instrumentedInitializer).onLoad(Qux.class);
        verify(instrumentedInitializer).isAlive();
        verifyNoMoreInteractions(instrumentedInitializer);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLazy() throws Exception {
        AgentBuilder.InitializationStrategy.Dispatcher dispatcher = new AgentBuilder.InitializationStrategy.SelfInjection.Lazy.Dispatcher(nexusAccessor, IDENTIFIER);
        dispatcher.register(dynamicType, Qux.class.getClassLoader(), Qux.class.getProtectionDomain(), injectionStrategy);
        verifyNoMoreInteractions(classInjector, dependentInitializer, independentInitializer);
        Nexus.initialize(Qux.class, IDENTIFIER);
        Map<TypeDescription, byte[]> injected = new HashMap<TypeDescription, byte[]>();
        injected.put(independent, BAR);
        injected.put(dependent, FOO);
        verify(classInjector).inject(injected);
        verifyNoMoreInteractions(classInjector);
        verify(independentInitializer).onLoad(Bar.class);
        verifyNoMoreInteractions(independentInitializer);
        verify(dependentInitializer).onLoad(Foo.class);
        verifyNoMoreInteractions(dependentInitializer);
        verify(instrumentedInitializer).onLoad(Qux.class);
        verifyNoMoreInteractions(instrumentedInitializer);
    }

    @Test
    public void testDispatcherCreation() throws Exception {
        assertThat(new AgentBuilder.InitializationStrategy.SelfInjection.Split().dispatcher(),
                instanceOf(AgentBuilder.InitializationStrategy.SelfInjection.Split.Dispatcher.class));
        assertThat(new AgentBuilder.InitializationStrategy.SelfInjection.Eager().dispatcher(),
                instanceOf(AgentBuilder.InitializationStrategy.SelfInjection.Eager.Dispatcher.class));
        assertThat(new AgentBuilder.InitializationStrategy.SelfInjection.Lazy().dispatcher(),
                instanceOf(AgentBuilder.InitializationStrategy.SelfInjection.Lazy.Dispatcher.class));
    }

    private static class Foo {
        /* empty */
    }

    private static class Bar {
        /* empty */
    }

    private static class Qux {
        /* empty */
    }
}
