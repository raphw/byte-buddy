package net.bytebuddy.pool;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.AnnotationToken.Resolution.Simple.class).apply();
        ObjectPropertyAssertion.of(TypePool.LazyTypeDescription.AnnotationToken.Resolution.Illegal.class).apply();
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalResolutionThrowsException() throws Exception {
        new TypePool.LazyTypeDescription.AnnotationToken.Resolution.Illegal("foo").resolve();
    }

    @Test
    public void testIllegalResolutionIsNotResolved() throws Exception {
        assertThat(new TypePool.LazyTypeDescription.AnnotationToken.Resolution.Illegal("foo").isResolved(), is(false));
    }

    @Test
    @Ignore("Java 8 tests")
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
