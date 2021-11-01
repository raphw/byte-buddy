package net.bytebuddy.dynamic.loading;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassLoadingStrategyForPreloadedTypesTest {

    @Test
    public void testClassExists() {
        Map<TypeDescription, Class<?>> types = ClassLoadingStrategy.ForPreloadedTypes.INSTANCE.load(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                Collections.<TypeDescription, byte[]>singletonMap(TypeDescription.OBJECT, new byte[0]));
        assertThat(types.size(), is(1));
    }

    @Test(expected = IllegalStateException.class)
    public void testClassNotExists() {
        ClassLoadingStrategy.ForPreloadedTypes.INSTANCE.load(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                Collections.<TypeDescription, byte[]>singletonMap(TypeDescription.ForLoadedType.of(Foo.class), new byte[0]));
    }

    private static class Foo {
        /* empty */
    }
}
