package net.bytebuddy.dynamic;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeInitializer;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import static net.bytebuddy.test.utility.FieldByFieldComparison.matchesPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TypeResolutionStrategyTest {

    private static final byte[] FOO = new byte[]{1, 2, 3};

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeInitializer typeInitializer, otherTypeInitializer;

    @Mock
    private DynamicType dynamicType;

    @Mock
    private ClassLoader classLoader;

    @Mock
    private ClassLoadingStrategy<ClassLoader> classLoadingStrategy;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private LoadedTypeInitializer loadedTypeInitializer;

    @Before
    public void setUp() throws Exception {
        when(dynamicType.getTypeDescription()).thenReturn(typeDescription);
        when(dynamicType.getAllTypes()).thenReturn(Collections.singletonMap(typeDescription, FOO));
        when(dynamicType.getLoadedTypeInitializers()).thenReturn(Collections.singletonMap(typeDescription, loadedTypeInitializer));
        when(classLoadingStrategy.load(classLoader, Collections.singletonMap(typeDescription, FOO)))
                .thenReturn(Collections.<TypeDescription, Class<?>>singletonMap(typeDescription, Foo.class));
        when(loadedTypeInitializer.isAlive()).thenReturn(true);
        when(typeDescription.getName()).thenReturn(Foo.class.getName());
    }

    @Test
    public void testPassive() throws Exception {
        TypeResolutionStrategy.Resolved resolved = TypeResolutionStrategy.Passive.INSTANCE.resolve();
        assertThat(resolved.injectedInto(typeInitializer), is(typeInitializer));
        assertThat(resolved.initialize(dynamicType, classLoader, classLoadingStrategy),
                is(Collections.<TypeDescription, Class<?>>singletonMap(typeDescription, Foo.class)));
        verify(classLoadingStrategy).load(classLoader, Collections.singletonMap(typeDescription, FOO));
        verifyNoMoreInteractions(classLoadingStrategy);
        verify(loadedTypeInitializer).onLoad(Foo.class);
        verifyNoMoreInteractions(loadedTypeInitializer);
    }

    @Test
    public void testActive() throws Exception {
        TypeResolutionStrategy.Resolved resolved = new TypeResolutionStrategy.Active().resolve();
        Field field = TypeResolutionStrategy.Active.Resolved.class.getDeclaredField("identification");
        field.setAccessible(true);
        int identification = (Integer) field.get(resolved);
        when(typeInitializer.expandWith(matchesPrototype(new NexusAccessor.InitializationAppender(identification)))).thenReturn(otherTypeInitializer);
        assertThat(resolved.injectedInto(typeInitializer), is(otherTypeInitializer));
        assertThat(resolved.initialize(dynamicType, classLoader, classLoadingStrategy),
                is(Collections.<TypeDescription, Class<?>>singletonMap(typeDescription, Foo.class)));
        try {
            verify(classLoadingStrategy).load(classLoader, Collections.singletonMap(typeDescription, FOO));
            verifyNoMoreInteractions(classLoadingStrategy);
            verify(loadedTypeInitializer).isAlive();
            verifyNoMoreInteractions(loadedTypeInitializer);
        } finally {
            Field initializers = Nexus.class.getDeclaredField("TYPE_INITIALIZERS");
            initializers.setAccessible(true);
            Constructor<Nexus> constructor = Nexus.class.getDeclaredConstructor(String.class, ClassLoader.class, ReferenceQueue.class, int.class);
            constructor.setAccessible(true);
            Object value = ((Map<?, ?>) initializers.get(null)).remove(constructor.newInstance(Foo.class.getName(), Foo.class.getClassLoader(), null, identification));
            assertThat(value, CoreMatchers.is((Object) loadedTypeInitializer));
        }
    }

    @Test
    public void testLazy() throws Exception {
        TypeResolutionStrategy.Resolved resolved = TypeResolutionStrategy.Lazy.INSTANCE.resolve();
        assertThat(resolved.injectedInto(typeInitializer), is(typeInitializer));
        assertThat(resolved.initialize(dynamicType, classLoader, classLoadingStrategy),
                is(Collections.<TypeDescription, Class<?>>singletonMap(typeDescription, Foo.class)));
        verify(classLoadingStrategy).load(classLoader, Collections.singletonMap(typeDescription, FOO));
        verifyNoMoreInteractions(classLoadingStrategy);
        verifyNoMoreInteractions(loadedTypeInitializer);
    }

    @Test
    public void testDisabled() throws Exception {
        TypeResolutionStrategy.Resolved resolved = TypeResolutionStrategy.Disabled.INSTANCE.resolve();
        assertThat(resolved.injectedInto(typeInitializer), is(typeInitializer));
    }

    @Test(expected = IllegalStateException.class)
    public void testDisabledCannotBeApplied() throws Exception {
        TypeResolutionStrategy.Disabled.INSTANCE.resolve().initialize(dynamicType, classLoader, classLoadingStrategy);
    }

    private static class Foo {
        /* empty */
    }
}
