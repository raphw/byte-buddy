package net.bytebuddy.pool;

import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import static org.mockito.Mockito.when;

public class TypePoolLazyObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.FieldToken.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.MethodToken.class).apply();
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
        final Iterator<Class<?>> typeIterator = Arrays.<Class<?>>asList(FirstSample.class, SecondSample.class).iterator();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.AnnotationInvocationHandler.class).create(new ObjectPropertyAssertion.Creator<Class<?>>() {
            @Override
            public Class<?> create() {
                return typeIterator.next();
            }
        }).refine(new ObjectPropertyAssertion.Refinement<Map<?, ?>>() {
            @Override
            public void apply(Map<?, ?> mock) {
                when(mock.get(Mockito.any(String.class))).thenReturn(Mockito.mock(TypePool.LazyTypeDescription.AnnotationValue.class));
            }
        }).apply();
        final Iterator<Method> methodIterator = Arrays.asList(Object.class.getDeclaredMethods()).iterator();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.AnnotationInvocationHandler.ResolvedAnnotationValue.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return methodIterator.next();
            }
        }).apply();
    }

    @Test
    public void testDeclarationContextObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.DeclarationContext.DeclaredInType.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.DeclarationContext.DeclaredInMethod.class).apply();
    }

    private static @interface FirstSample {
        String value();
    }

    private static @interface SecondSample {
        String value();
    }
}
