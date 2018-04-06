package net.bytebuddy.test.utility;

import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

public class InjectionStrategyResolver {

    public static ClassLoadingStrategy<ClassLoader> resolve(Class<?> base) throws Exception {
        if (ClassInjector.UsingReflection.isAvailable()) {
            return ClassLoadingStrategy.Default.INJECTION;
        } else if (ClassInjector.UsingLookup.isAvailable()) {
            return ClassLoadingStrategy.UsingLookup.of(
                    Class.forName("java.lang.invoke.MethodHandles")
                            .getMethod("privateLookupIn", Class.class, Class.forName("java.lang.invoke.MethodHandles$Lookup"))
                            .invoke(null, base, Class.forName("java.lang.invoke.MethodHandles").getMethod("lookup").invoke(null)));
        } else {
            throw new AssertionError("No injection strategy available");
        }
    }

    private InjectionStrategyResolver() {
        throw new UnsupportedOperationException();
    }
}
