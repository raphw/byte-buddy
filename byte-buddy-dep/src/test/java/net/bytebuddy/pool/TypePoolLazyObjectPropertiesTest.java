package net.bytebuddy.pool;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;

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
    }

    @Test
    public void testGenericTypeTokenObjectPropertiesTest() throws Exception {
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.GenericTypeToken.ForPrimitiveType.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.GenericTypeToken.ForRawType.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.GenericTypeToken.ForTypeVariable.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.GenericTypeToken.ForTypeVariable.Formal.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.GenericTypeToken.ForGenericArray.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.GenericTypeToken.ForUnboundWildcard.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.GenericTypeToken.ForLowerBoundWildcard.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.GenericTypeToken.ForUpperBoundWildcard.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.GenericTypeToken.ForParameterizedType.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.GenericTypeToken.ForParameterizedType.Nested.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.GenericTypeToken.Resolution.ForType.Tokenized.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.GenericTypeToken.Resolution.ForMethod.Tokenized.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.GenericTypeToken.Resolution.ForField.Tokenized.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.GenericTypeToken.Resolution.Raw.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.GenericTypeToken.Resolution.Malformed.class).apply();
    }

    @Test
    public void testDeclarationContextObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.DeclarationContext.DeclaredInType.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.DeclarationContext.DeclaredInMethod.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.DeclarationContext.SelfDeclared.class).apply();
    }
}
