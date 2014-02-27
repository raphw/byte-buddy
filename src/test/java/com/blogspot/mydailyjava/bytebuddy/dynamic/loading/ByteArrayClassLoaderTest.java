package com.blogspot.mydailyjava.bytebuddy.dynamic.loading;

import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ByteArrayClassLoaderTest {

    private static final String BAR = "bar";

    private static class Foo {
        /* Note: Foo is know to the system class loader but not to the bootstrap class loader */
    }

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);
    @Mock
    private Map<String, byte[]> values;

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        ClassReader classReader = new ClassReader(Foo.class.getName());
        ClassWriter classWriter = new ClassWriter(classReader, 0);
        classReader.accept(classWriter, 0);
        when(values.get(Foo.class.getName())).thenReturn(classWriter.toByteArray());
        classLoader = new ByteArrayClassLoader(null /* null represents the bootstrap class loader */, values);
    }

    @Test
    public void testSuccessfulHit() throws Exception {
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
        verify(values).get(Foo.class.getName());
    }

    @Test(expected = ClassNotFoundException.class)
    public void testNonSuccessfulHit() throws Exception {
        // Note: Will throw a class format error instead targeting not found exception targeting loader attempts.
        classLoader.loadClass(BAR);
    }
}
