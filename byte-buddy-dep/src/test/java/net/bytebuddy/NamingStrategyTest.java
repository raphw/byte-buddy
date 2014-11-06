package net.bytebuddy;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

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
        ObjectPropertyAssertion.of(NamingStrategy.SuffixingRandom.class).apply();
    }

    @Test
    public void testFixed() throws Exception {
        NamingStrategy namingStrategy = new NamingStrategy.Fixed(FOO);
        assertThat(namingStrategy.name(unnamedType), is(FOO));
        verifyZeroInteractions(unnamedType);
    }

    @Test
    public void testFixedEqualsHashCode() throws Exception {
        ObjectPropertyAssertion.of(NamingStrategy.Fixed.class).apply();
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
        ObjectPropertyAssertion.of(NamingStrategy.PrefixingRandom.class).apply();
    }
}
