package net.bytebuddy.utility;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class FileSystemTest {

    private static final String FOO = "foo";

    private File source, target;

    @Before
    public void setUp() throws Exception {
        source = File.createTempFile("source", ".tmp");
        OutputStream outputStream = new FileOutputStream(source);
        try {
            outputStream.write(FOO.getBytes("UTF-8"));
        } finally {
            outputStream.close();
        }
        target = File.createTempFile("target", ".tmp");
        assertThat(target.delete(), is(true));
    }

    @After
    public void tearDown() throws Exception {
        assertThat(!source.exists() || source.delete(), is(true));
        assertThat(!target.exists() || target.delete(), is(true));
    }

    @Test
    public void testMove() throws Exception {
        FileSystem.INSTANCE.move(source, target);
        InputStream inputStream = new FileInputStream(target);
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            assertThat(outputStream.toString("UTF-8"), is(FOO));
        } finally {
            inputStream.close();
        }
        assertThat(source.exists(), is(false));
    }

    @Test
    public void testCopy() throws Exception {
        FileSystem.INSTANCE.copy(source, target);
        InputStream inputStream = new FileInputStream(target);
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            assertThat(outputStream.toString("UTF-8"), is(FOO));
        } finally {
            inputStream.close();
        }
        assertThat(source.exists(), is(true));
    }
}
