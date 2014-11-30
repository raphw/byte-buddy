package dalvik.system;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class DexClassLoader extends ClassLoader {

    public static class Target {

    }

    public DexClassLoader(String dexPath, String optimizedDirectory, String libraryPath, ClassLoader parent) {
        super(parent);
        assertThat(new File(dexPath).isFile(), is(true));
        assertThat(new File(optimizedDirectory).isFile(), is(true));
        assertThat(libraryPath, notNullValue());
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return Target.class;
    }
}
