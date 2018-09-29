package net.bytebuddy.pool;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.AbstractTypeDescriptionTest;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypePoolDefaultWithLazyResolutionTypeDescriptionTest extends AbstractTypeDescriptionTest {

    protected TypeDescription describe(Class<?> type) {
        return describe(type, ClassFileLocator.ForClassLoader.of(type.getClassLoader()), TypePool.CacheProvider.NoOp.INSTANCE);
    }

    private static TypeDescription describe(Class<?> type, ClassFileLocator classFileLocator, TypePool.CacheProvider cacheProvider) {
        return new TypePool.Default.WithLazyResolution(cacheProvider,
                classFileLocator,
                TypePool.Default.ReaderMode.EXTENDED).describe(type.getName()).resolve();
    }

    protected TypeDescription.Generic describeType(Field field) {
        return describe(field.getDeclaringClass()).getDeclaredFields().filter(is(field)).getOnly().getType();
    }

    protected TypeDescription.Generic describeReturnType(Method method) {
        return describe(method.getDeclaringClass()).getDeclaredMethods().filter(is(method)).getOnly().getReturnType();
    }

    protected TypeDescription.Generic describeParameterType(Method method, int index) {
        return describe(method.getDeclaringClass()).getDeclaredMethods().filter(is(method)).getOnly().getParameters().get(index).getType();
    }

    protected TypeDescription.Generic describeExceptionType(Method method, int index) {
        return describe(method.getDeclaringClass()).getDeclaredMethods().filter(is(method)).getOnly().getExceptionTypes().get(index);
    }

    protected TypeDescription.Generic describeSuperClass(Class<?> type) {
        return describe(type).getSuperClass();
    }

    protected TypeDescription.Generic describeInterfaceType(Class<?> type, int index) {
        return describe(type).getInterfaces().get(index);
    }

    @Test
    public void testTypeIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        TypePool typePool = TypePool.Default.WithLazyResolution.of(classFileLocator);
        TypePool.Resolution resolution = typePool.describe(Object.class.getName());
        assertThat(resolution.resolve().getName(), CoreMatchers.is(TypeDescription.OBJECT.getName()));
        verifyZeroInteractions(classFileLocator);
    }

    @Test
    public void testReferencedTypeIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        TypePool typePool = TypePool.Default.WithLazyResolution.of(classFileLocator);
        TypePool.Resolution resolution = typePool.describe(String.class.getName());
        assertThat(resolution.resolve().getName(), CoreMatchers.is(TypeDescription.STRING.getName()));
        assertThat(resolution.resolve().getSuperClass().asErasure().getName(), CoreMatchers.is(TypeDescription.OBJECT.getName()));
        verify(classFileLocator).locate(String.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    public void testTypeIsCached() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
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
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
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
    public void testNonGenericResolutionIsLazyForSimpleCreationNonFrozen() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .with(MethodGraph.Empty.INSTANCE)
                .with(InstrumentedType.Factory.Default.MODIFIABLE)
                .redefine(describe(NonGenericType.class, classFileLocator, new TypePool.CacheProvider.Simple()), classFileLocator)
                .make();
        verify(classFileLocator, times(2)).locate(NonGenericType.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    public void testNonGenericResolutionIsLazyForSimpleCreation() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .with(MethodGraph.Empty.INSTANCE)
                .with(InstrumentedType.Factory.Default.FROZEN)
                .redefine(describe(NonGenericType.class, classFileLocator, new TypePool.CacheProvider.Simple()), classFileLocator)
                .make();
        verify(classFileLocator, times(2)).locate(NonGenericType.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    public void testGenericResolutionIsLazyForSimpleCreation() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .with(MethodGraph.Empty.INSTANCE)
                .with(InstrumentedType.Factory.Default.FROZEN)
                .redefine(describe(GenericType.class, classFileLocator, new TypePool.CacheProvider.Simple()), classFileLocator)
                .make();
        verify(classFileLocator, times(2)).locate(GenericType.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    public void testNonGenericSuperClassHierarchyResolutionIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        assertThat(describe(NonGenericType.class, classFileLocator, new TypePool.CacheProvider.Simple()).getSuperClass().asErasure(),
                CoreMatchers.is((TypeDescription) TypeDescription.ForLoadedType.of(SampleClass.class)));
        verify(classFileLocator).locate(NonGenericType.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    public void testNonGenericSuperClassNavigatedHierarchyResolutionIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        assertThat(describe(NonGenericType.class, classFileLocator, new TypePool.CacheProvider.Simple()).getSuperClass().getSuperClass().asErasure(),
                CoreMatchers.is((TypeDescription) TypeDescription.ForLoadedType.of(SuperClass.class)));
        verify(classFileLocator).locate(NonGenericType.class.getName());
        verify(classFileLocator).locate(SampleClass.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    public void testNonGenericSuperInterfaceHierarchyResolutionIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        assertThat(describe(NonGenericType.class, classFileLocator, new TypePool.CacheProvider.Simple()).getInterfaces().getOnly().asErasure(),
                CoreMatchers.is((TypeDescription) TypeDescription.ForLoadedType.of(SampleInterface.class)));
        verify(classFileLocator).locate(NonGenericType.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    public void testNonGenericSuperInterfaceNavigatedHierarchyResolutionIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        assertThat(describe(NonGenericType.class, classFileLocator, new TypePool.CacheProvider.Simple()).getInterfaces().getOnly()
                .getInterfaces().getOnly().asErasure(), CoreMatchers.is((TypeDescription) TypeDescription.ForLoadedType.of(SuperInterface.class)));
        verify(classFileLocator).locate(NonGenericType.class.getName());
        verify(classFileLocator).locate(SampleInterface.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    public void testGenericSuperClassHierarchyResolutionIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        assertThat(describe(GenericType.class, classFileLocator, new TypePool.CacheProvider.Simple()).getSuperClass().asErasure(),
                CoreMatchers.is((TypeDescription) TypeDescription.ForLoadedType.of(SampleGenericClass.class)));
        verify(classFileLocator).locate(GenericType.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    public void testGenericSuperClassNavigatedHierarchyResolutionIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        assertThat(describe(GenericType.class, classFileLocator, new TypePool.CacheProvider.Simple()).getSuperClass().getSuperClass().asErasure(),
                CoreMatchers.is((TypeDescription) TypeDescription.ForLoadedType.of(SuperClass.class)));
        verify(classFileLocator).locate(GenericType.class.getName());
        verify(classFileLocator).locate(SampleGenericClass.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    public void testGenericSuperInterfaceHierarchyResolutionIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        assertThat(describe(GenericType.class, classFileLocator, new TypePool.CacheProvider.Simple()).getInterfaces().getOnly().asErasure(),
                CoreMatchers.is((TypeDescription) TypeDescription.ForLoadedType.of(SampleGenericInterface.class)));
        verify(classFileLocator).locate(GenericType.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    public void testGenericSuperInterfaceNavigatedHierarchyResolutionIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        assertThat(describe(GenericType.class, classFileLocator, new TypePool.CacheProvider.Simple()).getInterfaces().getOnly()
                .getInterfaces().getOnly().asErasure(), CoreMatchers.is((TypeDescription) TypeDescription.ForLoadedType.of(SuperInterface.class)));
        verify(classFileLocator).locate(GenericType.class.getName());
        verify(classFileLocator).locate(SampleGenericInterface.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    private static class SuperClass {
        /* empty */
    }

    private interface SuperInterface {
        /* empty */
    }

    private static class SampleClass extends SuperClass {
        /* empty */
    }

    private interface SampleInterface extends SuperInterface {
        /* empty */
    }

    private static class NonGenericType extends SampleClass implements SampleInterface {

        Object foo;

        Object foo(Object argument) throws Exception {
            return argument;
        }
    }

    private static class SampleGenericClass<T> extends SuperClass {
        /* empty */
    }

    private interface SampleGenericInterface<T> extends SuperInterface {
        /* empty */
    }

    private static class GenericType<T extends Exception> extends SampleGenericClass<T> implements SampleGenericInterface<T> {

        T foo;

        T foo(T argument) throws T {
            return argument;
        }
    }
}
