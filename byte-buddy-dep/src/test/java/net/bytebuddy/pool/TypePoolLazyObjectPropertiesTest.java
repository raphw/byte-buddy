package net.bytebuddy.pool;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Iterator;

import static org.mockito.Mockito.when;

public class TypePoolLazyObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.FieldToken.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.MethodToken.class).apply();
        final Iterator<Integer> iterator = Arrays.asList(1, 2).iterator();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.MethodToken.ParameterToken.class).create(new ObjectPropertyAssertion.Creator<Integer>() {
            @Override
            public Integer create() {
                return iterator.next();
            }
        }).apply();
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
    public void testInvocationHandlerObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.AnnotationInvocationHandler.class).create(new ObjectPropertyAssertion.Creator<Class<?>>() {
            @Override
            public Class<?> create() {
                return Object.class;
            }
        }).applyMutable();
    }

    @Test
    public void testDeclarationContextObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.DeclarationContext.DeclaredInType.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.DeclarationContext.DeclaredInMethod.class).apply();
    }
}
