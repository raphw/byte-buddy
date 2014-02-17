package com.blogspot.mydailyjava.bytebuddy;

import com.blogspot.mydailyjava.bytebuddy.test.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;
public class NamingStrategyTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private NamingStrategy.UnnamedType unnamedType;

    @Test
    public void testPrefixingRandomNonConflictingPackage() throws Exception {
        doReturn(MethodVisitor.class).when(unnamedType).getSuperClass();
        NamingStrategy namingStrategy = new NamingStrategy.PrefixingRandom(FOO);
        assertThat(namingStrategy.getName(unnamedType), startsWith(MethodVisitor.class.getName() + "$$" + FOO + "$$"));
        verify(unnamedType, atLeast(1)).getSuperClass();
        verifyNoMoreInteractions(unnamedType);
    }

    @Test
    public void testPrefixingRandomConflictingPackage() throws Exception {
        doReturn(Object.class).when(unnamedType).getSuperClass();
        NamingStrategy namingStrategy = new NamingStrategy.PrefixingRandom(FOO, BAR);
        assertThat(namingStrategy.getName(unnamedType), startsWith(BAR + "." + Object.class.getName() + "$$" + FOO + "$$"));
        verify(unnamedType, atLeast(1)).getSuperClass();
        verifyNoMoreInteractions(unnamedType);
    }

    @Test
    public void testFixed() throws Exception {
        NamingStrategy namingStrategy = new NamingStrategy.Fixed(FOO);
        assertThat(namingStrategy.getName(unnamedType), is(FOO));
        verifyZeroInteractions(unnamedType);
    }
}
