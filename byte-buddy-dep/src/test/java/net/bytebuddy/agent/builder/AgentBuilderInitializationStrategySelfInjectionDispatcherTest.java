package net.bytebuddy.agent.builder;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
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
import java.util.*;

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
    private DynamicType dynamicType, dependentAuxiliary, independentAuxiliary;

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
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setUp() throws Exception {
        when(builder.initializer((any(ByteCodeAppender.class)))).thenReturn((DynamicType.Builder) appendedBuilder);
        when(injectionStrategy.resolve(Qux.class.getClassLoader(), Qux.class.getProtectionDomain())).thenReturn(classInjector);
        when(dynamicType.getTypeDescription()).thenReturn(instrumented);
        when(dynamicType.getAuxiliaries()).thenReturn((List) Arrays.asList(dependentAuxiliary, independentAuxiliary));
        when(dynamicType.getAuxiliaryTypeDescriptions()).thenReturn(new HashSet<TypeDescription>(Arrays.asList(dependent, independent)));
        when(dependentAuxiliary.getAllTypeDescriptions()).thenReturn(Collections.singleton(dependent));
        when(independentAuxiliary.getAllTypeDescriptions()).thenReturn(Collections.singleton(independent));
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
        when(classInjector.inject(any(Set.class), any(ClassFileLocator.class))).then(new Answer<Map<TypeDescription, Class<?>>>() {
            public Map<TypeDescription, Class<?>> answer(InvocationOnMock invocationOnMock) throws Throwable {
                Map<TypeDescription, Class<?>> loaded = new HashMap<TypeDescription, Class<?>>();
                for (TypeDescription typeDescription : ((Set<TypeDescription>) invocationOnMock.getArguments()[0])) {
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
        verify(classInjector).inject(Collections.singleton(independent), dynamicType);
        verifyNoMoreInteractions(classInjector);
        verify(independentInitializer).onLoad(Bar.class);
        verifyNoMoreInteractions(independentInitializer);
        Nexus.initialize(Qux.class, IDENTIFIER);
        verify(classInjector).inject(Collections.singleton(dependent), dynamicType);
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
        Set<TypeDescription> injected = new HashSet<TypeDescription>();
        injected.add(independent);
        injected.add(dependent);
        verify(classInjector).inject(injected, dynamicType);
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
        Set<TypeDescription> injected = new HashSet<TypeDescription>();
        injected.add(independent);
        injected.add(dependent);
        verify(classInjector).inject(injected, dynamicType);
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
