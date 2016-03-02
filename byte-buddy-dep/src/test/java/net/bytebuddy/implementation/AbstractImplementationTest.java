package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.matcher.ElementMatcher;
import org.hamcrest.CoreMatchers;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractImplementationTest {

    protected <T> DynamicType.Loaded<T> implement(Class<T> target, Implementation implementation) {
        return implement(target, implementation, target.getClassLoader(), isDeclaredBy(target));
    }

    protected <T> DynamicType.Loaded<T> implement(Class<T> target,
                                                  Implementation implementation,
                                                  ClassLoader classLoader,
                                                  ElementMatcher<? super MethodDescription> targetMethods,
                                                  Class<?>... interfaces) {
        assertThat(target.isInterface(), CoreMatchers.is(false));
        for (Class<?> anInterface : interfaces) {
            assertThat(anInterface.isInterface(), CoreMatchers.is(true));
        }
        return new ByteBuddy()
                .subclass(target)
                .implement(interfaces)
                .invokable(targetMethods).intercept(implementation)
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.WRAPPER);
    }
}
