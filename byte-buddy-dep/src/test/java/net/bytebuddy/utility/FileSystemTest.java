package net.bytebuddy.utility;

import net.bytebuddy.ClassFileVersion;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FileSystemTest {

    private static final String FOO = "foo";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File source, target;

    @Before
    public void setUp() throws Exception {
        source = temporaryFolder.newFile();
        OutputStream outputStream = new FileOutputStream(source);
        try {
            outputStream.write(FOO.getBytes("UTF-8"));
        } finally {
            outputStream.close();
        }
        target = temporaryFolder.newFile();
    }

    @After
    public void tearDown() throws Exception {
        assertThat(!source.exists() || source.delete(), is(true));
        assertThat(!target.exists() || target.delete(), is(true));
    }

    @Test
    public void testMove() throws Exception {
        FileSystem.getInstance().move(source, target);
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
        FileSystem.getInstance().copy(source, target);
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

    @Test
    public void testFileSystemType() {
        assertThat(FileSystem.getInstance().getClass(), is((Object) (ClassFileVersion.ofThisVm().isAtLeast(ClassFileVersion.JAVA_V7)
                ? FileSystem.ForNio2CapableVm.class
                : FileSystem.ForLegacyVm.class)));
    }
}
