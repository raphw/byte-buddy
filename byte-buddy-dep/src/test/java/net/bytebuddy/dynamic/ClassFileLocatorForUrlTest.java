package net.bytebuddy.dynamic;

import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TemporaryFolder;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassFileLocatorForUrlTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final int VALUE = 42;

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File file;

    @Before
    public void setUp() throws Exception {
        file = temporaryFolder.newFile();
    }

    @Test
    @JavaVersionRule.Enforce(7) // Avoid leak since class loader cannot be closed
    public void testSuccessfulLocation() throws Exception {
        OutputStream outputStream = new FileOutputStream(file);
        try {
            JarOutputStream jarOutputStream = new JarOutputStream(outputStream);
            JarEntry jarEntry = new JarEntry(FOO + "/" + BAR + ClassFileLocator.CLASS_FILE_EXTENSION);
            jarOutputStream.putNextEntry(jarEntry);
            jarOutputStream.write(VALUE);
            jarOutputStream.write(VALUE * 2);
            jarOutputStream.closeEntry();
            jarOutputStream.close();
        } finally {
            outputStream.close();
        }
        URL url = file.toURI().toURL();
        ClassFileLocator classFileLocator = new ClassFileLocator.ForUrl(Collections.singleton(url));
        try {
            ClassFileLocator.Resolution resolution = classFileLocator.locate(FOO + "." + BAR);
            assertThat(resolution.isResolved(), is(true));
            assertThat(resolution.resolve(), is(new byte[]{VALUE, VALUE * 2}));
        } finally {
            classFileLocator.close();
        }
    }

    @Test
    @JavaVersionRule.Enforce(7) // Avoid leak since class loader cannot be closed
    public void testJarFileClosable() throws Exception {
        URL url = URI.create("http://localhost:123").toURL();
        Closeable classFileLocator = new ClassFileLocator.ForUrl(url);
        classFileLocator.close();
    }

    @Test
    @JavaVersionRule.Enforce(7) // Avoid leak since class loader cannot be closed
    public void testNonSuccessfulLocation() throws Exception {
        OutputStream outputStream = new FileOutputStream(file);
        try {
            JarOutputStream jarOutputStream = new JarOutputStream(outputStream);
            JarEntry jarEntry = new JarEntry("noop.class");
            jarOutputStream.putNextEntry(jarEntry);
            jarOutputStream.write(VALUE);
            jarOutputStream.closeEntry();
            jarOutputStream.close();
        } finally {
            outputStream.close();
        }
        URL url = file.toURI().toURL();
        ClassFileLocator classFileLocator = new ClassFileLocator.ForUrl(url);
        try {
            ClassFileLocator.Resolution resolution = classFileLocator.locate(FOO + "." + BAR);
            assertThat(resolution.isResolved(), is(false));
        } finally {
            classFileLocator.close();
        }
    }
}
