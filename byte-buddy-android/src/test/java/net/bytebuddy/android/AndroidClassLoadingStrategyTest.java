package net.bytebuddy.android;

import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.file.DexFile;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.File;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AndroidClassLoadingStrategyTest {

    private static final String FOO = "foo", TEMP = "tmp", TO_STRING = "toString";

    private static final byte[] QUX = new byte[]{1, 2, 3}, BAZ = new byte[]{4, 5, 6};

    @Rule
    public TestRule dexCompilerRule = new MockitoRule(this);

    private File folder;

    @Mock
    private TypeDescription firstType, secondType;

    @Before
    public void setUp() throws Exception {
        folder = File.createTempFile(FOO, TEMP);
        assertThat(folder.delete(), is(true));
        folder = new File(folder.getParentFile(), UUID.randomUUID().toString());
        assertThat(folder.mkdir(), is(true));
        when(firstType.getName()).thenReturn(Foo.class.getName());
        when(secondType.getName()).thenReturn(Bar.class.getName());
    }

    @After
    public void tearDown() throws Exception {
        assertThat(folder.delete(), is(true));
    }

    @Test
    public void testProcessing() throws Exception {
        AndroidClassLoadingStrategy.DexProcessor dexProcessor = mock(AndroidClassLoadingStrategy.DexProcessor.class);
        Map<String, Class<?>> types = new HashMap<String, Class<?>>();
        types.put(Foo.class.getName(), Foo.class);
        types.put(Bar.class.getName(), Bar.class);
        ClassLoader classLoader = new MapClassLoader(getClass().getClassLoader(), types);
        when(dexProcessor.makeClassLoader(any(File.class), eq(folder), any(ClassLoader.class))).thenReturn(classLoader);
        AndroidClassLoadingStrategy.DexProcessor.Conversion conversion = mock(AndroidClassLoadingStrategy.DexProcessor.Conversion.class);
        when(dexProcessor.create()).thenReturn(conversion);
        ClassLoadingStrategy classLoadingStrategy = new AndroidClassLoadingStrategy(folder, dexProcessor);
        Map<TypeDescription, byte[]> unloaded = new HashMap<TypeDescription, byte[]>();
        unloaded.put(firstType, QUX);
        unloaded.put(secondType, BAZ);
        Map<TypeDescription, Class<?>> loaded = classLoadingStrategy.load(getClass().getClassLoader(), unloaded);
        assertThat(loaded.size(), is(2));
        assertThat(loaded.get(firstType), CoreMatchers.<Class<?>>is(Foo.class));
        assertThat(loaded.get(secondType), CoreMatchers.<Class<?>>is(Bar.class));
        verify(dexProcessor).create();
        verify(dexProcessor).makeClassLoader(any(File.class), eq(folder), eq(getClass().getClassLoader()));
        verifyNoMoreInteractions(dexProcessor);
        verify(conversion).register(Foo.class.getName(), QUX);
        verify(conversion).register(Bar.class.getName(), BAZ);
        verify(conversion).drainTo(any(OutputStream.class));
        verifyNoMoreInteractions(conversion);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAndroidClassLoaderRequiresDirectory() throws Exception {
        new AndroidClassLoadingStrategy(mock(File.class), mock(AndroidClassLoadingStrategy.DexProcessor.class));
    }

    @Test
    public void testStubbedClassLoading() throws Exception {
        final DynamicType.Unloaded<?> dynamicType = new ByteBuddy(ClassFileVersion.JAVA_V6)
                .subclass(Object.class)
                .method(named(TO_STRING)).intercept(FixedValue.value(FOO))
                .make();
        StubClassLoader stubClassLoader = new StubClassLoader(dynamicType);
        ClassLoadingStrategy classLoadingStrategy = new AndroidClassLoadingStrategy(folder, new StubbedClassLoaderDexCompilation(stubClassLoader));
        Map<TypeDescription, Class<?>> map = classLoadingStrategy.load(getClass().getClassLoader(), dynamicType.getAllTypes());
        assertThat(map.size(), is(1));
        assertThat(map.get(dynamicType.getTypeDescription()), CoreMatchers.<Class<?>>is(stubClassLoader.getLoaded()));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AndroidClassLoadingStrategy.class).create(new ObjectPropertyAssertion.Creator<File>() {
            @Override
            public File create() {
                return folder;
            }
        }).applyBasic();
        ObjectPropertyAssertion.of(AndroidClassLoadingStrategy.DexProcessor.ForSdkCompiler.class).apply();
        ObjectPropertyAssertion.of(AndroidClassLoadingStrategy.DexProcessor.ForSdkCompiler.Conversion.class).create(new ObjectPropertyAssertion.Creator<DexFile>() {
            @Override
            public DexFile create() {
                return new DexFile(new DexOptions());
            }
        }).apply();
    }

    private static class StubbedClassLoaderDexCompilation implements AndroidClassLoadingStrategy.DexProcessor {

        private final ClassLoader classLoader;

        private StubbedClassLoaderDexCompilation(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        public Conversion create() {
            return new AndroidClassLoadingStrategy.DexProcessor.ForSdkCompiler(new DexOptions(), new CfOptions()).create();
        }

        @Override
        public ClassLoader makeClassLoader(File zipFile, File privateDirectory, ClassLoader parentClassLoader) {
            return classLoader;
        }
    }

    private static class StubClassLoader extends ClassLoader {

        private Class<?> loaded;

        private final DynamicType.Unloaded<?> dynamicType;

        public StubClassLoader(DynamicType.Unloaded<?> dynamicType) {
            super(new URLClassLoader(new URL[0], null));
            this.dynamicType = dynamicType;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (loaded != null) {
                throw new AssertionError("Already loaded: " + loaded);
            } else if (resolve) {
                throw new AssertionError("Did not intend to resolve: " + name);
            }
            loaded = dynamicType.load(getParent()).getLoaded();
            return loaded;
        }

        public Class<?> getLoaded() {
            return loaded;
        }
    }

    private static class MapClassLoader extends ClassLoader {

        private final Map<String, Class<?>> types;

        public MapClassLoader(ClassLoader parent, Map<String, Class<?>> types) {
            super(parent);
            this.types = types;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> type = types.get(name);
            if (type == null) {
                throw new AssertionError("Unexpected type: " + name);
            } else if (resolve) {
                throw new AssertionError("Did not intend to resolve: " + name);
            }
            return type;
        }
    }

    private static class Foo {
        /* empty */
    }

    private static class Bar {
        /* empty */
    }
}
