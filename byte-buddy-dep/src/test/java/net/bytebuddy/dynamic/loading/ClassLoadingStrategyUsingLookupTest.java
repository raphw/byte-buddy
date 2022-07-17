package net.bytebuddy.dynamic.loading;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class ClassLoadingStrategyUsingLookupTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    private TypeDescription typeDescription;

    private byte[] binaryRepresentation;

    @Mock
    private ClassInjector classInjector;

    @Before
    public void setUp() throws Exception {
        typeDescription = TypeDescription.ForLoadedType.of(Foo.class);
        binaryRepresentation = new byte[]{1, 2, 3};
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInjection() throws Exception {
        when(classInjector.inject(Collections.singletonMap(typeDescription, binaryRepresentation)))
                .thenReturn((Map) Collections.singletonMap(typeDescription, Foo.class));
        Map<TypeDescription, Class<?>> loaded = new ClassLoadingStrategy.UsingLookup(classInjector)
                .load(Foo.class.getClassLoader(), Collections.singletonMap(typeDescription, binaryRepresentation));
        assertThat(loaded.size(), is(1));
        Class<?> type = loaded.get(typeDescription);
        assertThat(type.getName(), is(Foo.class.getName()));
    }

    @Test
    public void testFallback() {
        assertThat(ClassLoadingStrategy.UsingLookup.withFallback(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return JavaType.METHOD_HANDLES.load().getMethod("lookup").invoke(null);
            }
        }), notNullValue(ClassLoadingStrategy.class));
    }

    private static class Foo {
        /* empty */
    }
}
