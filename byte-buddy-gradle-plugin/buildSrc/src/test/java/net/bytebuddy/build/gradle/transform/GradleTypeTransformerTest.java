package net.bytebuddy.build.gradle.transform;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class GradleTypeTransformerTest {

    private File file;

    @Before
    public void setUp() throws Exception {
        file = File.createTempFile("gradle-test", ".jar");
    }

    @After
    public void tearDown() throws Exception {
        assertTrue(file.delete());
    }

    @Test
    public void testTransform() throws Exception {
        // TODO: add pseudo api into test package.
        new GradleTypeTransformer().transform(file);
    }
}
