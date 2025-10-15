package net.bytebuddy.pool;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.AbstractTypeDescriptionTest;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.utility.AsmClassReader;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.ClassVisitor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class TypePoolDefaultWithLazyResolutionTypeDescriptionTest extends AbstractTypeDescriptionTest {

    private final TypePool.Default.WithLazyResolution.LazinessMode lazinessMode;

    public TypePoolDefaultWithLazyResolutionTypeDescriptionTest(TypePool.Default.WithLazyResolution.LazinessMode lazinessMode) {
        this.lazinessMode = lazinessMode;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {TypePool.Default.WithLazyResolution.LazinessMode.NAME},
                {TypePool.Default.WithLazyResolution.LazinessMode.EXTENDED}
        });
    }

    protected TypeDescription describe(Class<?> type) {
        return describe(type, ClassFileLocator.ForClassLoader.of(type.getClassLoader()), TypePool.CacheProvider.NoOp.INSTANCE);
    }

    private TypeDescription describe(Class<?> type, ClassFileLocator classFileLocator, TypePool.CacheProvider cacheProvider) {
        return new TypePool.Default.WithLazyResolution(cacheProvider,
                classFileLocator,
                TypePool.Default.ReaderMode.EXTENDED,
                lazinessMode).describe(type.getName()).resolve();
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
    public void testIllegalResolutionDoesNotPersist() throws Exception {
        TypePool typePool = new TypePool.Default.WithLazyResolution(TypePool.CacheProvider.WithIllegalResolutionReattempt.of(new TypePool.CacheProvider.Simple()),
                ClassFileLocator.NoOp.INSTANCE,
                TypePool.Default.ReaderMode.FAST,
                lazinessMode);
        String name = "foo.Bar";
        TypePool.Resolution resolution = typePool.describe(name);
        assertThat(resolution.resolve().getName(), CoreMatchers.is(name));
        assertThat(resolution.isResolved(), CoreMatchers.is(false));
        assertThat(typePool.describe(name).resolve().getName(), CoreMatchers.is(name));
    }

    @Test
    public void testTypeIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        TypePool typePool = TypePool.Default.WithLazyResolution.of(classFileLocator);
        TypePool.Resolution resolution = typePool.describe(Object.class.getName());
        assertThat(resolution.resolve().getName(), CoreMatchers.is(TypeDescription.ForLoadedType.of(Object.class).getName()));
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    public void testReferencedTypeIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        TypePool typePool = TypePool.Default.WithLazyResolution.of(classFileLocator);
        TypePool.Resolution resolution = typePool.describe(String.class.getName());
        assertThat(resolution.resolve().getName(), CoreMatchers.is(TypeDescription.ForLoadedType.of(String.class).getName()));
        assertThat(resolution.resolve().getSuperClass().asErasure().getName(), CoreMatchers.is(TypeDescription.ForLoadedType.of(Object.class).getName()));
        verify(classFileLocator).locate(String.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    public void testTypeIsCached() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        TypePool typePool = TypePool.Default.WithLazyResolution.of(classFileLocator);
        TypePool.Resolution resolution = typePool.describe(Object.class.getName());
        assertThat(resolution.resolve().getModifiers(), CoreMatchers.is(TypeDescription.ForLoadedType.of(Object.class).getModifiers()));
        assertThat(resolution.resolve().getInterfaces(), CoreMatchers.is(TypeDescription.ForLoadedType.of(Object.class).getInterfaces()));
        assertThat(typePool.describe(Object.class.getName()).resolve(), CoreMatchers.is(resolution.resolve()));
        verify(classFileLocator).locate(Object.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    public void testReferencedTypeIsCached() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        TypePool typePool = TypePool.Default.WithLazyResolution.of(classFileLocator);
        TypePool.Resolution resolution = typePool.describe(String.class.getName());
        assertThat(resolution.resolve().getModifiers(), CoreMatchers.is(TypeDescription.ForLoadedType.of(String.class).getModifiers()));
        TypeDescription superClass = resolution.resolve().getSuperClass().asErasure();
        assertThat(superClass, CoreMatchers.is(TypeDescription.ForLoadedType.of(Object.class)));
        assertThat(superClass.getModifiers(), CoreMatchers.is(TypeDescription.ForLoadedType.of(Object.class).getModifiers()));
        assertThat(superClass.getInterfaces(), CoreMatchers.is(TypeDescription.ForLoadedType.of(Object.class).getInterfaces()));
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
    @SuppressWarnings("cast")
    public void testNonGenericSuperClassHierarchyResolutionIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        assertThat(describe(NonGenericType.class, classFileLocator, new TypePool.CacheProvider.Simple()).getSuperClass().asErasure(),
                CoreMatchers.is((TypeDescription) TypeDescription.ForLoadedType.of(SampleClass.class)));
        verify(classFileLocator).locate(NonGenericType.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    @SuppressWarnings("cast")
    public void testNonGenericSuperClassNavigatedHierarchyResolutionIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        assertThat(describe(NonGenericType.class, classFileLocator, new TypePool.CacheProvider.Simple()).getSuperClass().getSuperClass().asErasure(),
                CoreMatchers.is((TypeDescription) TypeDescription.ForLoadedType.of(SuperClass.class)));
        verify(classFileLocator).locate(NonGenericType.class.getName());
        verify(classFileLocator).locate(SampleClass.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    @SuppressWarnings("cast")
    public void testNonGenericSuperInterfaceHierarchyResolutionIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        assertThat(describe(NonGenericType.class, classFileLocator, new TypePool.CacheProvider.Simple()).getInterfaces().getOnly().asErasure(),
                CoreMatchers.is((TypeDescription) TypeDescription.ForLoadedType.of(SampleInterface.class)));
        verify(classFileLocator).locate(NonGenericType.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    @SuppressWarnings("cast")
    public void testNonGenericSuperInterfaceNavigatedHierarchyResolutionIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        assertThat(describe(NonGenericType.class, classFileLocator, new TypePool.CacheProvider.Simple()).getInterfaces().getOnly()
                .getInterfaces().getOnly().asErasure(), CoreMatchers.is((TypeDescription) TypeDescription.ForLoadedType.of(SuperInterface.class)));
        verify(classFileLocator).locate(NonGenericType.class.getName());
        verify(classFileLocator).locate(SampleInterface.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    @SuppressWarnings("cast")
    public void testGenericSuperClassHierarchyResolutionIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        assertThat(describe(GenericType.class, classFileLocator, new TypePool.CacheProvider.Simple()).getSuperClass().asErasure(),
                CoreMatchers.is((TypeDescription) TypeDescription.ForLoadedType.of(SampleGenericClass.class)));
        verify(classFileLocator).locate(GenericType.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    @SuppressWarnings("cast")
    public void testGenericSuperClassNavigatedHierarchyResolutionIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        assertThat(describe(GenericType.class, classFileLocator, new TypePool.CacheProvider.Simple()).getSuperClass().getSuperClass().asErasure(),
                CoreMatchers.is((TypeDescription) TypeDescription.ForLoadedType.of(SuperClass.class)));
        verify(classFileLocator).locate(GenericType.class.getName());
        verify(classFileLocator).locate(SampleGenericClass.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    @SuppressWarnings("cast")
    public void testGenericSuperInterfaceHierarchyResolutionIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        assertThat(describe(GenericType.class, classFileLocator, new TypePool.CacheProvider.Simple()).getInterfaces().getOnly().asErasure(),
                CoreMatchers.is((TypeDescription) TypeDescription.ForLoadedType.of(SampleGenericInterface.class)));
        verify(classFileLocator).locate(GenericType.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    @SuppressWarnings("cast")
    public void testGenericSuperInterfaceNavigatedHierarchyResolutionIsLazy() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofSystemLoader());
        assertThat(describe(GenericType.class, classFileLocator, new TypePool.CacheProvider.Simple()).getInterfaces().getOnly()
                .getInterfaces().getOnly().asErasure(), CoreMatchers.is((TypeDescription) TypeDescription.ForLoadedType.of(SuperInterface.class)));
        verify(classFileLocator).locate(GenericType.class.getName());
        verify(classFileLocator).locate(SampleGenericInterface.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    @Override
    public void testSimpleName() throws Exception {
        super.testSimpleName();
        assertThat(describe($DollarInName.class).getSimpleName(), CoreMatchers.is($DollarInName.class.getSimpleName()));
    }

    @Test
    public void testClassFileIsNotParsedForExtendedProperties() throws Exception {
        if (lazinessMode == TypePool.Default.WithLazyResolution.LazinessMode.NAME) {
            return;
        }
        TypeDescription typeDescription = new TypePool.Default.WithLazyResolution(new TypePool.CacheProvider.Simple(),
                ClassFileLocator.ForClassLoader.of(NonGenericType.class.getClassLoader()),
                TypePool.Default.ReaderMode.EXTENDED,
                new AsmClassReader.Factory() {
                    public AsmClassReader make(byte[] binaryRepresentation) {
                        return make(Default.IMPLICIT.make(binaryRepresentation));
                    }

                    public AsmClassReader make(byte[] binaryRepresentation, boolean experimental) {
                        return make(Default.IMPLICIT.make(binaryRepresentation, experimental));
                    }

                    private AsmClassReader make(final AsmClassReader delegate) {
                        return new AsmClassReader() {
                            public <T> T unwrap(Class<T> type) {
                                return delegate.unwrap(type);
                            }

                            public int getModifiers() {
                                return delegate.getModifiers();
                            }

                            public String getInternalName() {
                                return delegate.getInternalName();
                            }

                            public String getSuperClassInternalName() {
                                return delegate.getSuperClassInternalName();
                            }

                            public List<String> getInterfaceInternalNames() {
                                return delegate.getInterfaceInternalNames();
                            }

                            public void accept(ClassVisitor classVisitor, int flags) {
                                throw new AssertionError();
                            }
                        };
                    }
                },
                lazinessMode).describe(NonGenericType.class.getName()).resolve();
        assertThat(typeDescription.getSuperClass().asErasure().getName(), CoreMatchers.is(NonGenericType.class.getSuperclass().getName()));
        assertThat(typeDescription.getInterfaces().get(0).asErasure().getName(), CoreMatchers.is(NonGenericType.class.getInterfaces()[0].getName()));
        assertThat(typeDescription.isAbstract(), CoreMatchers.is(Modifier.isAbstract(NonGenericType.class.getModifiers())));
        assertThat(typeDescription.isInterface(), CoreMatchers.is(Modifier.isInterface(NonGenericType.class.getModifiers())));
        assertThat(typeDescription.isAnnotation(), CoreMatchers.is(NonGenericType.class.isAnnotation()));
        assertThat(typeDescription.isEnum(), CoreMatchers.is(NonGenericType.class.isEnum()));
        assertThat(typeDescription.isAssignableTo(NonGenericType.class.getSuperclass()), CoreMatchers.is(true));
        assertThat(typeDescription.isAssignableTo(NonGenericType.class.getInterfaces()[0]), CoreMatchers.is(true));
        assertThat(typeDescription.isAssignableTo(Object.class), CoreMatchers.is(true));
        assertThat(typeDescription.isAssignableTo(Void.class), CoreMatchers.is(false));
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

    private static class $DollarInName {
        /* empty */
    }
}
