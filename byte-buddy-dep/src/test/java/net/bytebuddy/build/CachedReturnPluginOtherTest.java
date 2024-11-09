package net.bytebuddy.build;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

public class CachedReturnPluginOtherTest {

    @Test
    @SuppressWarnings("rawtypes")
    public void testIgnoreExistingField() {
        DynamicType.Builder<ExistingField> builder = new ByteBuddy().redefine(ExistingField.class);
        assertThat(new CachedReturnPlugin(true).apply(builder,
                TypeDescription.ForLoadedType.of(ExistingField.class),
                ClassFileLocator.ForClassLoader.of(ExistingField.class.getClassLoader())), sameInstance((DynamicType.Builder) builder));
    }

    @Test(expected = IllegalStateException.class)
    public void testCacheVoid() {
        new CachedReturnPlugin().apply(new ByteBuddy().redefine(VoidCache.class),
                TypeDescription.ForLoadedType.of(VoidCache.class),
                ClassFileLocator.ForClassLoader.of(VoidCache.class.getClassLoader()));
    }

    @Test(expected = IllegalStateException.class)
    public void testAbstractMethod() {
        new CachedReturnPlugin().apply(new ByteBuddy().redefine(AbstractCache.class),
                TypeDescription.ForLoadedType.of(AbstractCache.class),
                ClassFileLocator.ForClassLoader.of(AbstractCache.class.getClassLoader()));
    }

    @Test(expected = IllegalStateException.class)
    public void testParameterMethod() {
        new CachedReturnPlugin().apply(new ByteBuddy().redefine(ParameterCache.class),
                TypeDescription.ForLoadedType.of(ParameterCache.class),
                ClassFileLocator.ForClassLoader.of(ParameterCache.class.getClassLoader()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAdviceResolverVoid() {
        CachedReturnPlugin.AdviceResolver.of(TypeDescription.ForLoadedType.of(void.class));
    }

    private static class VoidCache {

        @CachedReturnPlugin.Enhance
        private void foo() {
            /* do nothing */
        }
    }

    private abstract static class AbstractCache {

        @CachedReturnPlugin.Enhance
        protected abstract void foo();
    }

    private static class ParameterCache {

        @CachedReturnPlugin.Enhance
        private void foo(Void argument) {
            /* do nothing */
        }
    }

    private static class ExistingField {

        private String foo;

        @CachedReturnPlugin.Enhance("foo")
        private String foo() {
            return null;
        }
    }
}