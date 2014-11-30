package net.bytebuddy.android;

import dalvik.system.DexClassLoader;
import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.File;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class AndroidClassLoadingStrategyTest {

    private static final String FOO = "bar.foo", BAR = "foo.bar", TEMP = "tmp";

    private static final byte[] QUX = new byte[]{1, 2, 3}, BAZ = new byte[]{4, 5, 6};

    @Rule
    public TestRule dexCompilerRule = new MockitoRule(this);

    private File directory;

    @Mock
    private TypeDescription first, second;

    @Before
    public void setUp() throws Exception {
        directory = File.createTempFile(FOO, TEMP);
        assertThat(directory.delete(), is(true));
        directory = new File(directory.getParentFile(), UUID.randomUUID().toString());
        assertThat(directory.mkdir(), is(true));
        when(first.getName()).thenReturn(FOO);
        when(second.getName()).thenReturn(BAR);
    }

    @After
    public void tearDown() throws Exception {
        assertThat(directory.delete(), is(true));
    }

    @Test
    public void testProcessing() throws Exception {
        AndroidClassLoadingStrategy.DexProcessor dexProcessor = mock(AndroidClassLoadingStrategy.DexProcessor.class);
        AndroidClassLoadingStrategy.DexProcessor.Process process = mock(AndroidClassLoadingStrategy.DexProcessor.Process.class);
        when(dexProcessor.createNewDexFile()).thenReturn(process);
        ClassLoadingStrategy classLoadingStrategy = new AndroidClassLoadingStrategy(directory, dexProcessor);
        Map<TypeDescription, byte[]> unloaded = new HashMap<TypeDescription, byte[]>();
        unloaded.put(first, QUX);
        unloaded.put(second, BAZ);
        Map<TypeDescription, Class<?>> loaded = classLoadingStrategy.load(mock(ClassLoader.class), unloaded);
        assertThat(loaded.size(), is(2));
        assertEquals(DexClassLoader.Target.class, loaded.get(first));
        assertEquals(DexClassLoader.Target.class, loaded.get(second));
        verify(dexProcessor).createNewDexFile();
        verifyNoMoreInteractions(dexProcessor);
        verify(process).register(FOO, QUX);
        verify(process).register(BAR, BAZ);
        verify(process).store(any(Writer.class));
        verifyNoMoreInteractions(process);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAndroidClassLoaderRequiresDirectory() throws Exception {
        ObjectPropertyAssertion.of(AndroidClassLoadingStrategy.class)
                .apply(new AndroidClassLoadingStrategy(mock(File.class), mock(AndroidClassLoadingStrategy.DexProcessor.class)));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AndroidClassLoadingStrategy.class)
                .apply(new AndroidClassLoadingStrategy(directory, mock(AndroidClassLoadingStrategy.DexProcessor.class)));
        ObjectPropertyAssertion.of(AndroidClassLoadingStrategy.DexProcessor.class).apply();
        ObjectPropertyAssertion.of(AndroidClassLoadingStrategy.DexProcessor.ForSdkCompiler.Process.class).apply();
        ObjectPropertyAssertion.of(AndroidClassLoadingStrategy.DexProcessor.ForSdkCompiler.Handler.Default.class).apply();
        ObjectPropertyAssertion.of(AndroidClassLoadingStrategy.DexProcessor.ForSdkCompiler.Handler.Exceptional.class).apply();
        ObjectPropertyAssertion.of(AndroidClassLoadingStrategy.DexCreator.class).apply();
        ObjectPropertyAssertion.of(AndroidClassLoadingStrategy.DexCreator.Creation.class).apply();
    }
}
