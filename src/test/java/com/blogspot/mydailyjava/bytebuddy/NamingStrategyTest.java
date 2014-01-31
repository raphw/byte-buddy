package com.blogspot.mydailyjava.bytebuddy;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;

import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;
public class NamingStrategyTest {

    private static final String FOO = "foo", BAR = "bar";

    private NamingStrategy.UnnamedType unnamedType;

    @Before
    public void setUp() throws Exception {
        unnamedType = mock(NamingStrategy.UnnamedType.class);
    }

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
