package net.bytebuddy.pool;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class TypePoolDefaultTest {

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofClassPath();
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNameCannotContainSlash() throws Exception {
        typePool.describe("/");
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotFindClass() throws Exception {
        TypePool.Resolution resolution = typePool.describe("foo");
        assertThat(resolution.isResolved(), is(false));
        resolution.resolve();
        fail();
    }

    @Test
    public void testNoSuperFlag() throws Exception {
        assertThat(typePool.describe(Object.class.getName()).resolve().getModifiers() & Opcodes.ACC_SUPER, is(0));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testNoDeprecationFlag() throws Exception {
        assertThat(typePool.describe(DeprecationSample.class.getName()).resolve().getModifiers() & Opcodes.ACC_DEPRECATED, is(0));
        assertThat(typePool.describe(DeprecationSample.class.getName()).resolve().getDeclaredFields().filter(named("foo")).getOnly().getModifiers(), is(0));
        assertThat(typePool.describe(DeprecationSample.class.getName()).resolve().getDeclaredMethods().filter(named("foo")).getOnly().getModifiers(), is(0));
    }

    @Test
    public void testGenericsObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeExtractor.class).applyBasic();
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeRegistrant.RejectingSignatureVisitor.class).applyBasic();
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeExtractor.IncompleteToken.AbstractBase.ForDirectBound.class).applyBasic();
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeExtractor.IncompleteToken.AbstractBase.ForUpperBound.class).applyBasic();
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeExtractor.IncompleteToken.AbstractBase.ForLowerBound.class).applyBasic();
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeExtractor.IncompleteToken.ForTopLevelType.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeExtractor.IncompleteToken.ForInnerClass.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeExtractor.ForSignature.OfType.class).applyBasic();
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeExtractor.ForSignature.OfType.SuperClassRegistrant.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeExtractor.ForSignature.OfType.InterfaceTypeRegistrant.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeExtractor.ForSignature.OfMethod.class).applyBasic();
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeExtractor.ForSignature.OfMethod.ReturnTypeTypeRegistrant.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeExtractor.ForSignature.OfMethod.ParameterTypeRegistrant.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeExtractor.ForSignature.OfMethod.ExceptionTypeRegistrant.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeExtractor.ForSignature.OfField.class).applyBasic();
    }

    @Test
    public void testAnnotationRegistrantObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.Default.AnnotationRegistrant.ForByteCodeElement.class).applyBasic();
        ObjectPropertyAssertion.of(TypePool.Default.AnnotationRegistrant.ForByteCodeElement.WithIndex.class).applyBasic();
        ObjectPropertyAssertion.of(TypePool.Default.AnnotationRegistrant.ForTypeVariable.class).applyBasic();
        ObjectPropertyAssertion.of(TypePool.Default.AnnotationRegistrant.ForTypeVariable.WithIndex.class).applyBasic();
        ObjectPropertyAssertion.of(TypePool.Default.AnnotationRegistrant.ForTypeVariable.WithIndex.DoubleIndexed.class).applyBasic();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.Default.class).apply();
    }

    @Test
    public void testTypeIsCached() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofClassPath());
        TypePool typePool = TypePool.Default.of(classFileLocator);
        TypePool.Resolution resolution = typePool.describe(Object.class.getName());
        assertThat(typePool.describe(Object.class.getName()).resolve(), CoreMatchers.is(resolution.resolve()));
        verify(classFileLocator).locate(Object.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Test
    public void testReferencedTypeIsCached() throws Exception {
        ClassFileLocator classFileLocator = spy(ClassFileLocator.ForClassLoader.ofClassPath());
        TypePool typePool = TypePool.Default.of(classFileLocator);
        TypePool.Resolution resolution = typePool.describe(String.class.getName());
        assertThat(typePool.describe(String.class.getName()).resolve(), CoreMatchers.is(resolution.resolve()));
        assertThat(typePool.describe(String.class.getName()).resolve().getSuperClass().asErasure(), CoreMatchers.is(TypeDescription.OBJECT));
        verify(classFileLocator).locate(String.class.getName());
        verify(classFileLocator).locate(Object.class.getName());
        verifyNoMoreInteractions(classFileLocator);
    }

    @Deprecated
    private static class DeprecationSample {

        @Deprecated
        Void foo;

        @Deprecated
        void foo() {
            /* empty */
        }
    }
}
