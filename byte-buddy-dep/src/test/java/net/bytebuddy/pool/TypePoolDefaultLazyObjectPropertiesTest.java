package net.bytebuddy.pool;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.Arrays;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypePoolDefaultLazyObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.FieldToken.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.MethodToken.class).apply();
        final Iterator<Integer> iterator = Arrays.asList(1, 2).iterator();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.MethodToken.ParameterToken.class).create(new ObjectPropertyAssertion.Creator<Integer>() {
            @Override
            public Integer create() {
                return iterator.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.AnnotationToken.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.AnnotationToken.Resolution.Simple.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.AnnotationToken.Resolution.Illegal.class).apply();
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalResolutionThrowsException() throws Exception {
        new TypePool.Default.LazyTypeDescription.AnnotationToken.Resolution.Illegal("foo").resolve();
    }

    @Test
    public void testIllegalResolutionIsNotResolved() throws Exception {
        assertThat(new TypePool.Default.LazyTypeDescription.AnnotationToken.Resolution.Illegal("foo").isResolved(), is(false));
    }

    @Test
    public void testGenericTypeTokenObjectPropertiesTest() throws Exception {
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.GenericTypeToken.ForPrimitiveType.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.GenericTypeToken.ForRawType.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.GenericTypeToken.ForTypeVariable.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.GenericTypeToken.ForTypeVariable.Formal.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.GenericTypeToken.ForGenericArray.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.GenericTypeToken.ForUnboundWildcard.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.GenericTypeToken.ForLowerBoundWildcard.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.GenericTypeToken.ForUpperBoundWildcard.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.GenericTypeToken.ForParameterizedType.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.GenericTypeToken.ForParameterizedType.Nested.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.GenericTypeToken.Resolution.ForType.Tokenized.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.GenericTypeToken.Resolution.ForMethod.Tokenized.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.GenericTypeToken.Resolution.ForField.Tokenized.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.GenericTypeToken.Resolution.Raw.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.GenericTypeToken.Resolution.Malformed.class).apply();
    }

    @Test
    public void testDeclarationContextObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.DeclarationContext.DeclaredInType.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.DeclarationContext.DeclaredInMethod.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.LazyTypeDescription.DeclarationContext.SelfDeclared.class).apply();
    }
}
