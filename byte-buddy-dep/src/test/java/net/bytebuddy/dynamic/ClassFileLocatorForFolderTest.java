package net.bytebuddy.dynamic;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassFileLocatorForFolderTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final int VALUE = 42;

    private File folder;

    @Before
    public void setUp() throws Exception {
        File file = File.createTempFile(FOO, BAR);
        assertThat(file.delete(), is(true));
        folder = new File(file.getParentFile(), FOO + new Random().nextInt());
        assertThat(folder.mkdir(), is(true));
    }

    @After
    public void tearDown() throws Exception {
        assertThat(folder.delete(), is(true));
    }

    @Test
    public void testSuccessfulLocation() throws Exception {
        File packageFolder = new File(folder, FOO);
        assertThat(packageFolder.mkdir(), is(true));
        File file = new File(packageFolder, BAR + ".class");
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
        assertThat(file.delete(), is(true));
        assertThat(packageFolder.delete(), is(true));
    }

    @Test
    public void testNonSuccessfulLocation() throws Exception {
        ClassFileLocator classFileLocator = new ClassFileLocator.ForFolder(folder);
        ClassFileLocator.Resolution resolution = classFileLocator.locate(FOO + "." + BAR);
        assertThat(resolution.isResolved(), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassFileLocator.ForFolder.class).apply();
    }
}
