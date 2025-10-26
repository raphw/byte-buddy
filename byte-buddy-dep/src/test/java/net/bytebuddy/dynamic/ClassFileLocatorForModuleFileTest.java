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
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ClassFileLocatorForModuleFileTest {

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
    public void testSuccessfulLocation() throws Exception {
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file));
        try {
            ZipEntry zipEntry = new ZipEntry("classes/" + FOO + "/" + BAR + ClassFileLocator.CLASS_FILE_EXTENSION);
            zipOutputStream.putNextEntry(zipEntry);
            zipOutputStream.write(VALUE);
            zipOutputStream.write(VALUE * 2);
            zipOutputStream.closeEntry();
        } finally {
            zipOutputStream.close();
        }
        ZipFile zipFile = new ZipFile(file);
        try {
            ClassFileLocator classFileLocator = new ClassFileLocator.ForModuleFile(zipFile);
            ClassFileLocator.Resolution resolution = classFileLocator.locate(FOO + "." + BAR);
            assertThat(resolution.isResolved(), is(true));
            assertThat(resolution.resolve(), is(new byte[]{VALUE, VALUE * 2}));
        } finally {
            zipFile.close();
        }
    }

    @Test
    public void testZipFileClosable() throws Exception {
        ZipFile zipFile = mock(ZipFile.class);
        Closeable classFileLocator = new ClassFileLocator.ForModuleFile(zipFile);
        classFileLocator.close();
        verify(zipFile).close();
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testBootJar() throws Exception {
        ClassFileLocator classFileLocator;
        try {
            classFileLocator = ClassFileLocator.ForModuleFile.ofBootPath();
        } catch (IllegalStateException exception) {
            Logger.getLogger("net.bytebuddy").info("Module boot path not available as jmod files");
            return;
        }
        try {
            assertThat(classFileLocator.locate(Object.class.getName()).isResolved(), is(true));
        } finally {
            classFileLocator.close();
        }
    }

    @Test
    public void testNonSuccessfulLocation() throws Exception {
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file));
        try {
            ZipEntry zipEntry = new ZipEntry("noop.class");
            zipOutputStream.putNextEntry(zipEntry);
            zipOutputStream.write(VALUE);
            zipOutputStream.closeEntry();
        } finally {
            zipOutputStream.close();
        }
        ZipFile zipFile = new ZipFile(file);
        try {
            ClassFileLocator classFileLocator = new ClassFileLocator.ForModuleFile(zipFile);
            ClassFileLocator.Resolution resolution = classFileLocator.locate(FOO + "." + BAR);
            assertThat(resolution.isResolved(), is(false));
        } finally {
            zipFile.close();
        }
    }

    @Test
    public void testClose() throws Exception {
        ZipFile zipFile = mock(ZipFile.class);
        new ClassFileLocator.ForModuleFile(zipFile).close();
        verify(zipFile).close();
        verifyNoMoreInteractions(zipFile);
    }
}
