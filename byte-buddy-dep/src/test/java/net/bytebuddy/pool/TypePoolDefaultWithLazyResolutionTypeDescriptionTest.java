package net.bytebuddy.pool;

import net.bytebuddy.description.type.AbstractTypeDescriptionTest;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypePoolDefaultWithLazyResolutionTypeDescriptionTest extends AbstractTypeDescriptionTest {

    @Override
    protected TypeDescription describe(Class<?> type) {
        TypePool typePool = new TypePool.Default.WithLazyResolution(TypePool.CacheProvider.NoOp.INSTANCE,
                ClassFileLocator.ForClassLoader.of(type.getClassLoader()),
                TypePool.Default.ReaderMode.EXTENDED);
        try {
            return typePool.describe(type.getName()).resolve();
        } finally {
            typePool.clear();
        }
    }

    @Override
    protected TypeDescription.Generic describeType(Field field) {
        return describe(field.getDeclaringClass()).getDeclaredFields().filter(is(field)).getOnly().getType();
    }

    @Override
    protected TypeDescription.Generic describeReturnType(Method method) {
        return describe(method.getDeclaringClass()).getDeclaredMethods().filter(is(method)).getOnly().getReturnType();
    }

    @Override
    protected TypeDescription.Generic describeParameterType(Method method, int index) {
        return describe(method.getDeclaringClass()).getDeclaredMethods().filter(is(method)).getOnly().getParameters().get(index).getType();
    }

    @Override
    protected TypeDescription.Generic describeExceptionType(Method method, int index) {
        return describe(method.getDeclaringClass()).getDeclaredMethods().filter(is(method)).getOnly().getExceptionTypes().get(index);
    }

    @Override
    protected TypeDescription.Generic describeSuperClass(Class<?> type) {
        return describe(type).getSuperClass();
    }

    @Override
    protected TypeDescription.Generic describeInterfaceType(Class<?> type, int index) {
        return describe(type).getInterfaces().get(index);
    }

    @Test
    public void testTypeIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofClassPath());
        TypePool typePool = TypePool.Default.WithLazyResolution.of(classFileLocator);
        TypePool.Resolution resolution = typePool.describe(Object.class.getName());
        assertThat(resolution.resolve().getName(), CoreMatchers.is(TypeDescription.OBJECT.getName()));
        verifyZeroInteractions(classFileLocator);
    }

    @Test
    public void testReferencedTypeIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofClassPath());
        TypePool typePool = TypePool.Default.WithLazyResolution.of(classFileLocator);
        TypePool.Resolution resolution = typePool.describe(String.class.getName());
        assertThat(resolution.resolve().getName(), CoreMatchers.is(TypeDescription.STRING.getName()));
        assertThat(resolution.resolve().getSuperClass().asErasure().getName(), CoreMatchers.is(TypeDescription.OBJECT.getName()));
        verify(classFileLocator).locate(String.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    public void testTypeIsCached() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofClassPath());
        TypePool typePool = TypePool.Default.WithLazyResolution.of(classFileLocator);
        TypePool.Resolution resolution = typePool.describe(Object.class.getName());
        assertThat(resolution.resolve().getModifiers(), CoreMatchers.is(TypeDescription.OBJECT.getModifiers()));
        assertThat(resolution.resolve().getInterfaces(), CoreMatchers.is(TypeDescription.OBJECT.getInterfaces()));
        assertThat(typePool.describe(Object.class.getName()).resolve(), CoreMatchers.is(resolution.resolve()));
        verify(classFileLocator).locate(Object.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    public void testReferencedTypeIsCached() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofClassPath());
        TypePool typePool = TypePool.Default.WithLazyResolution.of(classFileLocator);
        TypePool.Resolution resolution = typePool.describe(String.class.getName());
        assertThat(resolution.resolve().getModifiers(), CoreMatchers.is(TypeDescription.STRING.getModifiers()));
        TypeDescription superClass = resolution.resolve().getSuperClass().asErasure();
        assertThat(superClass, CoreMatchers.is(TypeDescription.OBJECT));
        assertThat(superClass.getModifiers(), CoreMatchers.is(TypeDescription.OBJECT.getModifiers()));
        assertThat(superClass.getInterfaces(), CoreMatchers.is(TypeDescription.OBJECT.getInterfaces()));
        assertThat(typePool.describe(String.class.getName()).resolve(), CoreMatchers.is(resolution.resolve()));
        verify(classFileLocator).locate(String.class.getName());
        verify(classFileLocator).locate(Object.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.Default.WithLazyResolution.LazyResolution.class).apply();
    }
}
