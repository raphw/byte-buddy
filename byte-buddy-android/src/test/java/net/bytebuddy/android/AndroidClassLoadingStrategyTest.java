package net.bytebuddy.android;

import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.test.utility.MockitoRule;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

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
        AndroidClassLoadingStrategy.DexProcessor.Conversion conversion = mock(AndroidClassLoadingStrategy.DexProcessor.Conversion.class);
        when(dexProcessor.create()).thenReturn(conversion);
        AndroidClassLoadingStrategy classLoadingStrategy = spy(new StubbedClassLoadingStrategy(folder, dexProcessor));
        Map<TypeDescription, byte[]> unloaded = new HashMap<TypeDescription, byte[]>();
        unloaded.put(firstType, QUX);
        unloaded.put(secondType, BAZ);
        Map<TypeDescription, Class<?>> loaded = new HashMap<TypeDescription, Class<?>>();
        loaded.put(firstType, Foo.class);
        loaded.put(secondType, Bar.class);
        doReturn(loaded).when(classLoadingStrategy).doLoad(eq(getClass().getClassLoader()), eq(unloaded.keySet()), any(File.class));
        Map<TypeDescription, Class<?>> result = classLoadingStrategy.load(getClass().getClassLoader(), unloaded);
        assertThat(result.size(), is(2));
        assertThat(result.get(firstType), CoreMatchers.<Class<?>>is(Foo.class));
        assertThat(result.get(secondType), CoreMatchers.<Class<?>>is(Bar.class));
        verify(dexProcessor).create();
        verifyNoMoreInteractions(dexProcessor);
        verify(conversion).register(Foo.class.getName(), QUX);
        verify(conversion).register(Bar.class.getName(), BAZ);
        verify(conversion).drainTo(any(OutputStream.class));
        verifyNoMoreInteractions(conversion);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAndroidClassLoaderRequiresDirectory() throws Exception {
        new StubbedClassLoadingStrategy(mock(File.class), mock(AndroidClassLoadingStrategy.DexProcessor.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInjectBootstrapLoader() throws Exception {
        File file = mock(File.class);
        when(file.isDirectory()).thenReturn(true);
        new StubbedClassLoadingStrategy.Injecting(file, mock(AndroidClassLoadingStrategy.DexProcessor.class))
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, Collections.<TypeDescription, byte[]>emptyMap());
    }

    @Test
    public void testStubbedClassLoading() throws Exception {
        final DynamicType.Unloaded<?> dynamicType = new ByteBuddy(ClassFileVersion.JAVA_V6)
                .subclass(Object.class)
                .method(named(TO_STRING)).intercept(FixedValue.value(FOO))
                .make();
        AndroidClassLoadingStrategy classLoadingStrategy = spy(new StubbedClassLoadingStrategy(folder, new StubbedClassLoaderDexCompilation()));
        doReturn(Collections.singletonMap(dynamicType.getTypeDescription(), Foo.class)).when(classLoadingStrategy).doLoad(eq(getClass().getClassLoader()),
                eq(Collections.singleton(dynamicType.getTypeDescription())),
                any(File.class));
        Map<TypeDescription, Class<?>> map = classLoadingStrategy.load(getClass().getClassLoader(), dynamicType.getAllTypes());
        assertThat(map.size(), is(1));
    }

    private static class StubbedClassLoadingStrategy extends AndroidClassLoadingStrategy {

        public StubbedClassLoadingStrategy(File privateDirectory, DexProcessor dexProcessor) {
            super(privateDirectory, dexProcessor);
        }

        @Override
        protected Map<TypeDescription, Class<?>> doLoad(ClassLoader classLoader, Set<TypeDescription> typeDescriptions, File jar) throws IOException {
            throw new AssertionError();
        }
    }

    private static class StubbedClassLoaderDexCompilation implements AndroidClassLoadingStrategy.DexProcessor {

        @Override
        public Conversion create() {
            return new AndroidClassLoadingStrategy.DexProcessor.ForSdkCompiler(new DexOptions(), new CfOptions()).create();
        }
    }

    private static class Foo {
        /* empty */
    }

    private static class Bar {
        /* empty */
    }
}
