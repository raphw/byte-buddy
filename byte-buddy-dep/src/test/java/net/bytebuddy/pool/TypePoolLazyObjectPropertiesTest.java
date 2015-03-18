package net.bytebuddy.pool;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypePoolLazyObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.FieldToken.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.MethodToken.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.MethodToken.ParameterToken.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.AnnotationToken.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.AnnotationValue.Trivial.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.AnnotationValue.ForAnnotation.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.AnnotationValue.ForEnumeration.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.AnnotationValue.ForComplexArray.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.AnnotationValue.ForType.class)
                .refine(new ObjectPropertyAssertion.Refinement<Type>() {
                    @Override
                    public void apply(Type mock) {
                        when(mock.getClassName()).thenReturn("" + System.identityHashCode(mock));
                    }
                }).apply();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInvocationHandlerObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.AnnotationInvocationHandler.class)
                .apply(new TypePool.LazyTypeDescription.AnnotationInvocationHandler(mock(ClassLoader.class), Annotation.class, mock(Map.class)));
    }

    @Test
    public void testDeclarationContextObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.DeclarationContext.DeclaredInType.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.DeclarationContext.DeclaredInMethod.class).apply();
    }
}
