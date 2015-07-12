package net.bytebuddy.pool;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

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
    public void testGenericsObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeExtractor.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeRegistrant.RejectingSignatureVisitor.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeExtractor.IncompleteToken.AbstractBase.ForDirectBound.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeExtractor.IncompleteToken.AbstractBase.ForUpperBound.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeExtractor.IncompleteToken.AbstractBase.ForLowerBound.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeExtractor.IncompleteToken.ForTopLevelType.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.GenericTypeExtractor.IncompleteToken.ForInnerClass.class).apply();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.Default.class).apply();
    }
}
