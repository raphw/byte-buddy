package net.bytebuddy.pool;

import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypePoolDefaultLazyTypeDescriptionTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypePool.Default.LazyTypeDescription.GenericTypeToken genericTypeToken;

    @Mock
    private TypePool typePool;

    @Test(expected = IllegalStateException.class)
    public void testIllegalResolutionThrowsException() throws Exception {
        new TypePool.Default.LazyTypeDescription.AnnotationToken.Resolution.Illegal("foo").resolve();
    }

    @Test
    public void testIllegalResolutionIsNotResolved() throws Exception {
        assertThat(new TypePool.Default.LazyTypeDescription.AnnotationToken.Resolution.Illegal("foo").isResolved(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotResolveTypePathPrefixForPrimitiveType() throws Exception {
        TypePool.Default.LazyTypeDescription.GenericTypeToken.ForPrimitiveType.VOID.getTypePathPrefix();
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotResolveTypePathPrefixForRawType() throws Exception {
        new TypePool.Default.LazyTypeDescription.GenericTypeToken.ForPrimitiveType.ForRawType(FOO).getTypePathPrefix();
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotResolveTypePathPrefixForTypeVariable() throws Exception {
        new TypePool.Default.LazyTypeDescription.GenericTypeToken.ForPrimitiveType.ForTypeVariable(FOO).getTypePathPrefix();
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotResolveTypePathPrefixForGenericArray() throws Exception {
        new TypePool.Default.LazyTypeDescription.GenericTypeToken.ForPrimitiveType.ForGenericArray(genericTypeToken).getTypePathPrefix();
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotResolveTypePathPrefixForUnboundWildcard() throws Exception {
        TypePool.Default.LazyTypeDescription.GenericTypeToken.ForPrimitiveType.ForUnboundWildcard.INSTANCE.getTypePathPrefix();
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotResolveTypePathPrefixForLowerBoundWildcard() throws Exception {
        new TypePool.Default.LazyTypeDescription.GenericTypeToken.ForPrimitiveType.ForLowerBoundWildcard(genericTypeToken).getTypePathPrefix();
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotResolveTypePathPrefixForUpperBoundWildcard() throws Exception {
        new TypePool.Default.LazyTypeDescription.GenericTypeToken.ForPrimitiveType.ForUpperBoundWildcard(genericTypeToken).getTypePathPrefix();
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotResolvePrimaryBoundPropertyForPrimitiveType() throws Exception {
        TypePool.Default.LazyTypeDescription.GenericTypeToken.ForPrimitiveType.VOID.isPrimaryBound(typePool);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotResolvePrimaryBoundPropertyForGenericArray() throws Exception {
        new TypePool.Default.LazyTypeDescription.GenericTypeToken.ForPrimitiveType.ForGenericArray(genericTypeToken).isPrimaryBound(typePool);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotResolvePrimaryBoundPropertyForUnboundWildcard() throws Exception {
        TypePool.Default.LazyTypeDescription.GenericTypeToken.ForPrimitiveType.ForUnboundWildcard.INSTANCE.isPrimaryBound(typePool);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotResolvePrimaryBoundPropertyForLowerBoundWildcard() throws Exception {
        new TypePool.Default.LazyTypeDescription.GenericTypeToken.ForPrimitiveType.ForLowerBoundWildcard(genericTypeToken).isPrimaryBound(typePool);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotResolvePrimaryBoundPropertyForUpperBoundWildcard() throws Exception {
        new TypePool.Default.LazyTypeDescription.GenericTypeToken.ForPrimitiveType.ForUpperBoundWildcard(genericTypeToken).isPrimaryBound(typePool);
    }
}
