package net.bytebuddy.android;

import dalvik.system.DexClassLoader;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.FixedValue;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.DexCompilerRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class AndroidClassLoadingStrategyDexTest {

    private static final String FOO = "foo", TEMP = "tmp";

    @Rule
    public MethodRule dexCompilerRule = new DexCompilerRule();

    private File directory;

    private DynamicType dynamicType;

    @Before
    public void setUp() throws Exception {
        directory = File.createTempFile(FOO, TEMP);
        assertThat(directory.delete(), is(true));
        directory = new File(directory.getParentFile(), UUID.randomUUID().toString());
        assertThat(directory.mkdir(), is(true));
        dynamicType = new ByteBuddy().subclass(Object.class)
                .method(named("toString")).intercept(FixedValue.value("FOO"))
                .make();
    }

    @After
    public void tearDown() throws Exception {
        assertThat(directory.delete(), is(true));
    }

    @Test
    @DexCompilerRule.Enforce
    public void testStubbedClassLoading() throws Exception {
        ClassLoadingStrategy classLoadingStrategy = new AndroidClassLoadingStrategy(directory);
        Map<TypeDescription, Class<?>> map = classLoadingStrategy.load(getClass().getClassLoader(), dynamicType.getAllTypes());
        assertThat(map.size(), is(1));
        assertEquals(DexClassLoader.Target.class, map.get(dynamicType.getTypeDescription()));
    }
}
