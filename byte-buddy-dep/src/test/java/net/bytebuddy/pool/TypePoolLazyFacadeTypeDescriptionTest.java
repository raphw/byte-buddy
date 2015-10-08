package net.bytebuddy.pool;

import net.bytebuddy.description.type.AbstractTypeDescriptionTest;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.is;

public class TypePoolLazyFacadeTypeDescriptionTest  extends AbstractTypeDescriptionTest {

    @Override
    protected TypeDescription describe(Class<?> type) {
        TypePool typePool = new TypePool.LazyFacade(new TypePool.Default(TypePool.CacheProvider.NoOp.INSTANCE,
                ClassFileLocator.ForClassLoader.of(type.getClassLoader())));
        try {
            return typePool.describe(type.getName()).resolve();
        } finally {
            typePool.clear();
        }
    }

    @Override
    protected GenericTypeDescription describe(Field field) {
        return describe(field.getDeclaringClass()).getDeclaredFields().filter(is(field)).getOnly().getType();
    }

    @Override
    protected GenericTypeDescription describe(Method method) {
        return describe(method.getDeclaringClass()).getDeclaredMethods().filter(is(method)).getOnly().getReturnType();
    }
}
