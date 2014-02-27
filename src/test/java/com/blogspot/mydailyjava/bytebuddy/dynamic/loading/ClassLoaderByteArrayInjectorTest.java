package com.blogspot.mydailyjava.bytebuddy.dynamic.loading;

import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.net.URL;
import java.net.URLClassLoader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ClassLoaderByteArrayInjectorTest {

    private static class Foo {
        /* Note: Foo is know to the system class loader but not to the bootstrap class loader */
    }

    private ClassLoader classLoader;

    private ClassLoaderByteArrayInjector classLoaderByteArrayInjector;

    @Before
    public void setUp() throws Exception {
        classLoader = new URLClassLoader(new URL[0], null /* null represents the bootstrap class loader */);
        classLoaderByteArrayInjector = new ClassLoaderByteArrayInjector(classLoader);
    }

    @Test
    public void testInjection() throws Exception {
        ClassReader classReader = new ClassReader(Foo.class.getName());
        ClassWriter classWriter = new ClassWriter(classReader, 0);
        classReader.accept(classWriter, 0);
        classLoaderByteArrayInjector.inject(Foo.class.getName(), classWriter.toByteArray());
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
    }
}
