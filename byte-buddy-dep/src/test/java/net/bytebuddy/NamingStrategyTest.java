package net.bytebuddy;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.mockito.Mockito.*;

public class NamingStrategyTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private NamingStrategy.UnnamedType unnamedType;

    @Test
    public void testSuffixingRandomNonConflictingPackage() throws Exception {
        when(unnamedType.getSuperClass()).thenReturn(new TypeDescription.ForLoadedType(MethodVisitor.class));
        NamingStrategy namingStrategy = new NamingStrategy.SuffixingRandom(FOO);
        assertThat(namingStrategy.name(unnamedType), startsWith(MethodVisitor.class.getName() + "$" + FOO + "$"));
        verify(unnamedType, atLeast(1)).getSuperClass();
        verifyNoMoreInteractions(unnamedType);
    }

    @Test
    public void testSuffixingRandomConflictingPackage() throws Exception {
        when(unnamedType.getSuperClass()).thenReturn(new TypeDescription.ForLoadedType(Object.class));
        NamingStrategy namingStrategy = new NamingStrategy.SuffixingRandom(FOO, BAR);
        assertThat(namingStrategy.name(unnamedType), startsWith(BAR + "." + Object.class.getName() + "$" + FOO + "$"));
        verify(unnamedType, atLeast(1)).getSuperClass();
        verifyNoMoreInteractions(unnamedType);
    }

    @Test
    public void testSuffixingRandomEqualsHashCode() throws Exception {
        assertThat(new NamingStrategy.SuffixingRandom(FOO, BAR).hashCode(), is(new NamingStrategy.SuffixingRandom(FOO, BAR).hashCode()));
        assertThat(new NamingStrategy.SuffixingRandom(FOO, BAR), equalTo(new NamingStrategy.SuffixingRandom(FOO, BAR)));
        assertThat(new NamingStrategy.SuffixingRandom(FOO, BAR).hashCode(), not(is(new NamingStrategy.SuffixingRandom(BAR, FOO).hashCode())));
        assertThat(new NamingStrategy.SuffixingRandom(FOO, BAR), not(equalTo(new NamingStrategy.SuffixingRandom(BAR, FOO))));
    }

    @Test
    public void testFixed() throws Exception {
        NamingStrategy namingStrategy = new NamingStrategy.Fixed(FOO);
        assertThat(namingStrategy.name(unnamedType), is(FOO));
        verifyZeroInteractions(unnamedType);
    }

    @Test
    public void testFixedEqualsHashCode() throws Exception {
        assertThat(new NamingStrategy.Fixed(FOO).hashCode(), is(new NamingStrategy.Fixed(FOO).hashCode()));
        assertThat(new NamingStrategy.Fixed(FOO), equalTo(new NamingStrategy.Fixed(FOO)));
        assertThat(new NamingStrategy.Fixed(FOO).hashCode(), not(is(new NamingStrategy.Fixed(BAR).hashCode())));
        assertThat(new NamingStrategy.Fixed(FOO), not(equalTo(new NamingStrategy.Fixed(BAR))));
    }

    @Test
    public void testPrefixingRandom() throws Exception {
        when(unnamedType.getSuperClass()).thenReturn(new TypeDescription.ForLoadedType(Object.class));
        NamingStrategy namingStrategy = new NamingStrategy.PrefixingRandom(FOO);
        assertThat(namingStrategy.name(unnamedType), startsWith(FOO + "." + Object.class.getName()));
        verify(unnamedType).getSuperClass();
        verifyNoMoreInteractions(unnamedType);
    }

    @Test
    public void testPrefixingRandomEqualsHashCode() throws Exception {
        assertThat(new NamingStrategy.PrefixingRandom(FOO).hashCode(), is(new NamingStrategy.PrefixingRandom(FOO).hashCode()));
        assertThat(new NamingStrategy.PrefixingRandom(FOO), equalTo(new NamingStrategy.PrefixingRandom(FOO)));
        assertThat(new NamingStrategy.PrefixingRandom(FOO).hashCode(), not(is(new NamingStrategy.PrefixingRandom(BAR).hashCode())));
        assertThat(new NamingStrategy.PrefixingRandom(FOO), not(equalTo(new NamingStrategy.PrefixingRandom(BAR))));
    }
}
