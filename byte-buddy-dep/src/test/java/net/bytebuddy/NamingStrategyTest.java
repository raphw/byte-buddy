package net.bytebuddy;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.mockito.Mockito.*;

public class NamingStrategyTest {

    private static final String FOO = "foo", BAR = "bar", JAVA_QUX = "java.qux";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private NamingStrategy.Suffixing.BaseNameResolver baseNameResolver;

    @Mock
    private TypeDescription.Generic typeDescription;

    @Mock
    private TypeDescription rawTypeDescription;

    @Before
    public void setUp() throws Exception {
        when(typeDescription.asErasure()).thenReturn(rawTypeDescription);
    }

    @Test
    public void testSuffixingSubclassNonConflictingPackage() throws Exception {
        when(rawTypeDescription.getName()).thenReturn(FOO);
        NamingStrategy namingStrategy = new NamingStrategy.Suffixing(BAR);
        assertThat(namingStrategy.subclass(typeDescription), equalTo(FOO + "$" + BAR));
        verify(typeDescription, atLeast(1)).asErasure();
        verifyNoMoreInteractions(typeDescription);
        verify(rawTypeDescription).getName();
        verifyNoMoreInteractions(rawTypeDescription);
    }

    @Test
    public void testSuffixingSubclassConflictingPackage() throws Exception {
        when(baseNameResolver.resolve(rawTypeDescription)).thenReturn(JAVA_QUX);
        NamingStrategy namingStrategy = new NamingStrategy.Suffixing(FOO, baseNameResolver, BAR);
        assertThat(namingStrategy.subclass(typeDescription), equalTo(BAR + "." + JAVA_QUX + "$" + FOO));
        verify(typeDescription).asErasure();
        verifyNoMoreInteractions(typeDescription);
        verifyNoMoreInteractions(rawTypeDescription);
        verify(baseNameResolver).resolve(rawTypeDescription);
        verifyNoMoreInteractions(baseNameResolver);
    }

    @Test
    public void testSuffixingSubclassConflictingPackageDisabled() throws Exception {
        when(baseNameResolver.resolve(rawTypeDescription)).thenReturn(JAVA_QUX);
        NamingStrategy namingStrategy = new NamingStrategy.Suffixing(FOO, baseNameResolver, NamingStrategy.NO_PREFIX);
        assertThat(namingStrategy.subclass(typeDescription), equalTo(JAVA_QUX + "$" + FOO));
        verify(typeDescription).asErasure();
        verifyNoMoreInteractions(typeDescription);
        verifyNoMoreInteractions(rawTypeDescription);
        verify(baseNameResolver).resolve(rawTypeDescription);
        verifyNoMoreInteractions(baseNameResolver);
    }

    @Test
    public void testSuffixingRebase() throws Exception {
        when(rawTypeDescription.getName()).thenReturn(FOO);
        NamingStrategy namingStrategy = new NamingStrategy.Suffixing(BAR);
        assertThat(namingStrategy.rebase(rawTypeDescription), is(FOO));
        verify(rawTypeDescription).getName();
        verifyNoMoreInteractions(rawTypeDescription);
    }

    @Test
    public void testSuffixingRedefine() throws Exception {
        when(rawTypeDescription.getName()).thenReturn(FOO);
        NamingStrategy namingStrategy = new NamingStrategy.Suffixing(BAR);
        assertThat(namingStrategy.redefine(rawTypeDescription), is(FOO));
        verify(rawTypeDescription).getName();
        verifyNoMoreInteractions(rawTypeDescription);
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
        verifyNoMoreInteractions(rawTypeDescription);
        verify(baseNameResolver).resolve(rawTypeDescription);
        verifyNoMoreInteractions(baseNameResolver);
    }

    @Test
    public void testSuffixingRandomSubclassConflictingPackageDisabled() throws Exception {
        when(baseNameResolver.resolve(rawTypeDescription)).thenReturn(JAVA_QUX);
        NamingStrategy namingStrategy = new NamingStrategy.SuffixingRandom(FOO, baseNameResolver, NamingStrategy.NO_PREFIX);
        assertThat(namingStrategy.subclass(typeDescription), startsWith(JAVA_QUX + "$" + FOO + "$"));
        verify(typeDescription).asErasure();
        verifyNoMoreInteractions(typeDescription);
        verifyNoMoreInteractions(rawTypeDescription);
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
        assertThat(new NamingStrategy.Suffixing.BaseNameResolver.ForFixedValue(FOO).resolve(rawTypeDescription), is(FOO));
        when(rawTypeDescription.getName()).thenReturn(FOO);
        assertThat(new NamingStrategy.Suffixing.BaseNameResolver.ForGivenType(rawTypeDescription).resolve(rawTypeDescription), is(FOO));
        assertThat(NamingStrategy.Suffixing.BaseNameResolver.ForUnnamedType.INSTANCE.resolve(rawTypeDescription), is(FOO));
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

    @Test(expected = IllegalStateException.class)
    public void testCallerSen() {
        new NamingStrategy.Suffixing.BaseNameResolver.WithCallerSuffix(mock(NamingStrategy.Suffixing.BaseNameResolver.class))
                .resolve(mock(TypeDescription.class));
    }
}
