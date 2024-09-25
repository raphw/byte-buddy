package net.bytebuddy.dynamic;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.jar.Manifest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassFileLocatorForFolderTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final int VALUE = 42;

    @Rule
    public JavaVersionRule javaVersionRule = new JavaVersionRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File folder;

    @Before
    public void setUp() throws Exception {
        folder = temporaryFolder.newFolder();
    }

    @Test
    public void testSuccessfulLocation() throws Exception {
        File packageFolder = new File(folder, FOO);
        assertThat(packageFolder.mkdir(), is(true));
        File file = new File(packageFolder, BAR + ClassFileLocator.CLASS_FILE_EXTENSION);
        assertThat(file.createNewFile(), is(true));
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        try {
            fileOutputStream.write(VALUE);
            fileOutputStream.write(VALUE * 2);
        } finally {
            fileOutputStream.close();
        }
        ClassFileLocator classFileLocator = new ClassFileLocator.ForFolder(folder);
        ClassFileLocator.Resolution resolution = classFileLocator.locate(FOO + "." + BAR);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is(new byte[]{VALUE, VALUE * 2}));
    }

    @Test
    public void testNonSuccessfulLocation() throws Exception {
        ClassFileLocator classFileLocator = new ClassFileLocator.ForFolder(folder);
        ClassFileLocator.Resolution resolution = classFileLocator.locate(FOO + "." + BAR);
        assertThat(resolution.isResolved(), is(false));
    }

    @Test
    public void testClose() throws Exception {
        new ClassFileLocator.ForFolder(folder).close();
    }

    @Test
    public void testMultiReleaseVersionLocation() throws Exception {
        File metaInf = new File(folder, "META-INF");
        assertThat(metaInf.mkdir(), is(true));
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Multi-Release", "true");
        OutputStream outputStream = new FileOutputStream(new File(metaInf, "MANIFEST.MF"));
        try {
            manifest.write(outputStream);
        } finally {
            outputStream.close();
        }
        File packageFolder = new File(metaInf, "versions/11/" + FOO);
        assertThat(packageFolder.mkdirs(), is(true));
        File file = new File(packageFolder, BAR + ClassFileLocator.CLASS_FILE_EXTENSION);
        assertThat(file.createNewFile(), is(true));
        outputStream = new FileOutputStream(file);
        try {
            outputStream.write(VALUE);
            outputStream.write(VALUE * 2);
        } finally {
            outputStream.close();
        }
        ClassFileLocator classFileLocator = ClassFileLocator.ForFolder.of(folder, ClassFileVersion.JAVA_V11);
        ClassFileLocator.Resolution resolution = classFileLocator.locate(FOO + "." + BAR);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is(new byte[]{VALUE, VALUE * 2}));
        ClassFileLocator unresolved = ClassFileLocator.ForFolder.of(folder, ClassFileVersion.JAVA_V9);
        assertThat(unresolved.locate(FOO + "." + BAR).isResolved(), is(false));
    }
}
