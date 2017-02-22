package net.bytebuddy.dynamic.loading;

import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteArrayClassLoaderPackageLookupStrategy {

    @Test
    public void testGetPackage() throws Exception {
        ByteArrayClassLoader byteArrayClassLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassFileExtraction.of(Foo.class));
        byteArrayClassLoader.loadClass(Foo.class.getName());
        assertThat(ByteArrayClassLoader.PackageLookupStrategy.ForLegacyVm.INSTANCE.apply(byteArrayClassLoader, Foo.class.getPackage().getName()).getName(),
                is(Foo.class.getPackage().getName()));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteArrayClassLoader.PackageLookupStrategy.CreationAction.class).apply();
        ObjectPropertyAssertion.of(ByteArrayClassLoader.PackageLookupStrategy.ForLegacyVm.class).apply();
        final Iterator<Method> iterator = Arrays.asList(Object.class.getDeclaredMethods()).iterator();
        ObjectPropertyAssertion.of(ByteArrayClassLoader.PackageLookupStrategy.ForJava9CapableVm.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return iterator.next();
            }
        }).apply();
    }

    private static class Foo {
        /* empty */
    }
}
