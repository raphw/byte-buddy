package net.bytebuddy.pool;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypePoolCacheProviderTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription;

    @Before
    public void setUp() throws Exception {
        when(typeDescription.getName()).thenReturn(FOO);
    }

    @Test
    public void testNoOp() throws Exception {
        assertThat(TypePool.CacheProvider.NoOp.INSTANCE.find(FOO), nullValue(TypeDescription.class));
        assertThat(TypePool.CacheProvider.NoOp.INSTANCE.register(typeDescription), sameInstance(typeDescription));
        assertThat(TypePool.CacheProvider.NoOp.INSTANCE.find(FOO), nullValue(TypeDescription.class));
        TypePool.CacheProvider.NoOp.INSTANCE.clear();
    }

    @Test
    public void testSimple() throws Exception {
        TypePool.CacheProvider simple = new TypePool.CacheProvider.Simple();
        assertThat(simple.find(FOO), nullValue(TypeDescription.class));
        assertThat(simple.register(typeDescription), sameInstance(typeDescription));
        assertThat(simple.find(FOO), sameInstance(typeDescription));
        TypeDescription typeDescription = mock(TypeDescription.class);
        when(typeDescription.getName()).thenReturn(FOO);
        assertThat(simple.register(typeDescription), sameInstance(this.typeDescription));
        assertThat(simple.find(FOO), sameInstance(this.typeDescription));
        simple.clear();
        assertThat(simple.find(FOO), nullValue(TypeDescription.class));
        assertThat(simple.register(typeDescription), sameInstance(typeDescription));
        assertThat(simple.find(FOO), sameInstance(typeDescription));
    }

    @Test
    public void testSimpleObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.CacheProvider.Simple.class).apply(new TypePool.CacheProvider.Simple());
    }
}
