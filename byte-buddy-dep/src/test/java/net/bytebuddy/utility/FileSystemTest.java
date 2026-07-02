package net.bytebuddy.utility;

import net.bytebuddy.ClassFileVersion;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

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

    @Test
    public void testValidatedReturnsContainedName() {
        assertThat(FileSystem.validated("foo/Bar.class"), is("foo/Bar.class"));
    }

    @Test
    public void testValidatedReturnsMultiReleaseName() {
        assertThat(FileSystem.validated("META-INF/versions/11/foo/Bar.class"), is("META-INF/versions/11/foo/Bar.class"));
    }

    @Test
    public void testValidatedAllowsContainedTraversal() {
        assertThat(FileSystem.validated("foo/../bar/Qux.class"), is("foo/../bar/Qux.class"));
    }

    @Test
    public void testValidatedAllowsCurrentDirectory() {
        assertThat(FileSystem.validated("foo/./Bar.class"), is("foo/./Bar.class"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidatedRejectsLeadingTraversal() {
        FileSystem.validated("../foo/Bar.class");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidatedRejectsEscapingTraversal() {
        FileSystem.validated("foo/../../bar/Qux.class");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidatedRejectsDeepTraversal() {
        FileSystem.validated("../../../etc/cron.d/evil.class");
    }

    @Test
    public void testValidatedFolderReturnsContainedTarget() throws Exception {
        File folder = temporaryFolder.getRoot();
        File target = new File(folder, "foo/Bar.class");
        assertThat(FileSystem.validated(folder, target), is(target));
    }

    @Test
    public void testValidatedFolderAllowsFolderItself() throws Exception {
        File folder = temporaryFolder.getRoot();
        assertThat(FileSystem.validated(folder, folder), is(folder));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidatedFolderRejectsEscapingTarget() throws Exception {
        File folder = temporaryFolder.getRoot();
        FileSystem.validated(folder, new File(folder, "../evil.class"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidatedFolderRejectsSiblingWithSharedPrefix() throws Exception {
        File folder = temporaryFolder.getRoot();
        FileSystem.validated(folder, new File(folder.getParentFile(), folder.getName() + "-evil"));
    }
}
