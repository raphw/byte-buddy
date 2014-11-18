package net.bytebuddy.pool;

import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

    @Test(expected = IllegalArgumentException.class)
    public void testCannotFindClass() throws Exception {
        typePool.describe("foo");
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.Default.class).apply();
    }
}
