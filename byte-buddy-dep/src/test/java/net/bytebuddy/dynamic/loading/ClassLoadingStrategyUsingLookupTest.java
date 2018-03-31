package net.bytebuddy.dynamic.loading;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class ClassLoadingStrategyUsingLookupTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    private TypeDescription typeDescription;

    private byte[] binaryRepresentation;

    @Mock
    private ClassInjector classInjector;

    @Before
    public void setUp() throws Exception {
        typeDescription = new TypeDescription.ForLoadedType(Foo.class);
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

    private static class Foo {
        /* empty */
    }
}
