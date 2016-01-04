package net.bytebuddy;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.mockito.Mockito.*;

public class NamingStrategyTest {

    private static final String FOO = "foo", BAR = "bar", JAVA_QUX = "java.qux";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private NamingStrategy.SuffixingRandom.BaseNameResolver baseNameResolver;

    @Mock
    private TypeDescription.Generic typeDescription;

    @Mock
    private TypeDescription rawTypeDescription;

    @Before
    public void setUp() throws Exception {
        when(typeDescription.asErasure()).thenReturn(rawTypeDescription);
    }

    @Test
    public void testSuffixingRandomSubclassNonConflictingPackage() throws Exception {
        when(rawTypeDescription.getName()).thenReturn(FOO);
        NamingStrategy namingStrategy = new NamingStrategy.SuffixingRandom(BAR);
        assertThat(namingStrategy.subclass(typeDescription), startsWith(FOO + "$" + BAR + "$"));
        verify(typeDescription, atLeast(1)).asErasure();
        verifyNoMoreInteractions(typeDescription);
        verify(rawTypeDescription).getName();
        verifyNoMoreInteractions(rawTypeDescription);
    }

    @Test
    public void testSuffixingRandomSubclassConflictingPackage() throws Exception {
        when(baseNameResolver.resolve(rawTypeDescription)).thenReturn(JAVA_QUX);
        NamingStrategy namingStrategy = new NamingStrategy.SuffixingRandom(FOO, baseNameResolver, BAR);
        assertThat(namingStrategy.subclass(typeDescription), startsWith(BAR + "." + JAVA_QUX + "$" + FOO + "$"));
        verify(typeDescription).asErasure();
        verifyNoMoreInteractions(typeDescription);
        verifyZeroInteractions(rawTypeDescription);
        verify(baseNameResolver).resolve(rawTypeDescription);
        verifyNoMoreInteractions(baseNameResolver);
    }

    @Test
    public void testSuffixingRandomSubclassConflictingPackageDisabled() throws Exception {
        when(baseNameResolver.resolve(rawTypeDescription)).thenReturn(JAVA_QUX);
        NamingStrategy namingStrategy = new NamingStrategy.SuffixingRandom(FOO, baseNameResolver, NamingStrategy.SuffixingRandom.NO_PREFIX);
        assertThat(namingStrategy.subclass(typeDescription), startsWith(JAVA_QUX + "$" + FOO + "$"));
        verify(typeDescription).asErasure();
        verifyNoMoreInteractions(typeDescription);
        verifyZeroInteractions(rawTypeDescription);
        verify(baseNameResolver).resolve(rawTypeDescription);
        verifyNoMoreInteractions(baseNameResolver);
    }

    @Test
    public void testSuffixingRandomRebase() throws Exception {
        when(rawTypeDescription.getName()).thenReturn(FOO);
        NamingStrategy namingStrategy = new NamingStrategy.SuffixingRandom(BAR);
        assertThat(namingStrategy.rebase(rawTypeDescription), is(FOO));
        verify(rawTypeDescription).getName();
        verifyNoMoreInteractions(rawTypeDescription);
    }

    @Test
    public void testSuffixingRandomRedefine() throws Exception {
        when(rawTypeDescription.getName()).thenReturn(FOO);
        NamingStrategy namingStrategy = new NamingStrategy.SuffixingRandom(BAR);
        assertThat(namingStrategy.redefine(rawTypeDescription), is(FOO));
        verify(rawTypeDescription).getName();
        verifyNoMoreInteractions(rawTypeDescription);
    }

    @Test
    public void testBaseNameResolvers() throws Exception {
        assertThat(new NamingStrategy.SuffixingRandom.BaseNameResolver.ForFixedValue(FOO).resolve(rawTypeDescription), is(FOO));
        when(rawTypeDescription.getName()).thenReturn(FOO);
        assertThat(new NamingStrategy.SuffixingRandom.BaseNameResolver.ForGivenType(rawTypeDescription).resolve(rawTypeDescription), is(FOO));
        assertThat(NamingStrategy.SuffixingRandom.BaseNameResolver.ForUnnamedType.INSTANCE.resolve(rawTypeDescription), is(FOO));
    }

    @Test
    public void testSuffixingRandomObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(NamingStrategy.SuffixingRandom.class).apply();
        ObjectPropertyAssertion.of(NamingStrategy.SuffixingRandom.BaseNameResolver.ForGivenType.class).apply();
        ObjectPropertyAssertion.of(NamingStrategy.SuffixingRandom.BaseNameResolver.ForUnnamedType.class).apply();
        ObjectPropertyAssertion.of(NamingStrategy.SuffixingRandom.BaseNameResolver.ForFixedValue.class).apply();
    }

    @Test
    public void testPrefixingRandom() throws Exception {
        when(rawTypeDescription.getName()).thenReturn(BAR);
        NamingStrategy namingStrategy = new NamingStrategy.PrefixingRandom(FOO);
        assertThat(namingStrategy.subclass(typeDescription), startsWith(FOO + "." + BAR));
        verify(typeDescription).asErasure();
        verifyNoMoreInteractions(typeDescription);
        verify(rawTypeDescription).getName();
        verifyNoMoreInteractions(rawTypeDescription);
    }

    @Test
    public void testPrefixingRandomRebase() throws Exception {
        when(rawTypeDescription.getName()).thenReturn(FOO);
        NamingStrategy namingStrategy = new NamingStrategy.PrefixingRandom(BAR);
        assertThat(namingStrategy.rebase(rawTypeDescription), is(FOO));
        verify(rawTypeDescription).getName();
        verifyNoMoreInteractions(rawTypeDescription);
    }

    @Test
    public void testPrefixingRandomRedefine() throws Exception {
        when(rawTypeDescription.getName()).thenReturn(FOO);
        NamingStrategy namingStrategy = new NamingStrategy.PrefixingRandom(BAR);
        assertThat(namingStrategy.redefine(rawTypeDescription), is(FOO));
        verify(rawTypeDescription).getName();
        verifyNoMoreInteractions(rawTypeDescription);
    }

    @Test
    public void testPrefixingRandomEqualsHashCode() throws Exception {
        ObjectPropertyAssertion.of(NamingStrategy.PrefixingRandom.class).apply();
    }
}
