package net.bytebuddy.build.gradle;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.test.utility.MockitoRule;
import org.gradle.api.GradleException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class InitializationTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassLoaderResolver classLoaderResolver;

    @Mock
    private File file, explicit, other;

    @Test
    public void testRebase() throws Exception {
        Initialization initialization = new Initialization();
        initialization.setEntryPoint(EntryPoint.Default.REBASE.name());
        assertThat(initialization.getEntryPoint(classLoaderResolver, explicit, Collections.singleton(other)), is((EntryPoint) EntryPoint.Default.REBASE));
        verifyZeroInteractions(classLoaderResolver);
    }

    @Test
    public void testRedefine() throws Exception {
        Initialization initialization = new Initialization();
        initialization.setEntryPoint(EntryPoint.Default.REDEFINE.name());
        assertThat(initialization.getEntryPoint(classLoaderResolver, explicit, Collections.singleton(other)), is((EntryPoint) EntryPoint.Default.REDEFINE));
        verifyZeroInteractions(classLoaderResolver);
    }

    @Test
    public void testRedefineLocal() throws Exception {
        Initialization initialization = new Initialization();
        initialization.setEntryPoint(EntryPoint.Default.REDEFINE_LOCAL.name());
        assertThat(initialization.getEntryPoint(classLoaderResolver, explicit, Collections.singleton(other)), is((EntryPoint) EntryPoint.Default.REDEFINE_LOCAL));
        verifyZeroInteractions(classLoaderResolver);
    }

    @Test
    public void testExplicitClassPath() throws Exception {
        Initialization initialization = new Initialization();
        initialization.setClassPath(Collections.singleton(file));
        Iterator<? extends File> iterator = initialization.getClassPath(explicit, Collections.singleton(other)).iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(file));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void testCustom() throws Exception {
        Initialization initialization = new Initialization();
        initialization.setEntryPoint(Foo.class.getName());
        initialization.setClassPath(Collections.singleton(file));
        when(classLoaderResolver.resolve(Collections.singleton(file))).thenReturn(Foo.class.getClassLoader());
        assertThat(initialization.getEntryPoint(classLoaderResolver, explicit, Collections.singleton(other)), instanceOf(Foo.class));
        verify(classLoaderResolver).resolve(Collections.singleton(file));
        verifyNoMoreInteractions(classLoaderResolver);
    }

    @Test(expected = GradleException.class)
    public void testCustomFailed() throws Exception {
        Initialization initialization = new Initialization();
        initialization.setClassPath(Collections.singleton(file));
        initialization.setEntryPoint(FOO);
        when(classLoaderResolver.resolve(Collections.singleton(file)));
        initialization.getEntryPoint(classLoaderResolver, explicit, Collections.singleton(other));
    }

    @Test(expected = GradleException.class)
    public void testEmpty() throws Exception {
        Initialization initialization = new Initialization();
        initialization.setEntryPoint("");
        initialization.getEntryPoint(classLoaderResolver, explicit, Collections.singleton(other));
    }

    @Test(expected = GradleException.class)
    public void testNull() throws Exception {
        new Initialization().getEntryPoint(classLoaderResolver, explicit, Collections.singleton(other));
    }

    @Test
    public void testImplicitClassPath() throws Exception {
        Initialization initialization = new Initialization();
        Iterator<? extends File> iterator = initialization.getClassPath(explicit, Collections.singleton(other)).iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(explicit));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(other));
        assertThat(iterator.hasNext(), is(false));
    }

    public static class Foo implements EntryPoint {

        public ByteBuddy byteBuddy(ClassFileVersion classFileVersion) {
            throw new AssertionError();
        }

        public DynamicType.Builder<?> transform(TypeDescription typeDescription, ByteBuddy byteBuddy,
                                                ClassFileLocator classFileLocator,
                                                MethodNameTransformer methodNameTransformer) {
            throw new AssertionError();
        }
    }
}
