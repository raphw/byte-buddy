package dalvik.system;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This is a simple stub of Android's DexClassLoader to allow for actual unit tests without running Android.
 */
public class DexClassLoader extends ClassLoader {

    public static class Target {

    }

    private final String dexPath;

    private final String optimizedDirectory;

    public DexClassLoader(String dexPath,
                          String optimizedDirectory,
                          String libraryPath,
                          ClassLoader parent) {
        super(parent);
        this.dexPath = dexPath;
        this.optimizedDirectory = optimizedDirectory;
        assertThat(libraryPath, is(""));
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        assertThat(new File(dexPath).isFile(), is(true));
        assertThat(new File(optimizedDirectory).isDirectory(), is(true));
        return Target.class;
    }
}
