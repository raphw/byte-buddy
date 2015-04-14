package net.bytebuddy.pool;

import net.bytebuddy.description.type.AbstractTypeDescriptionTest;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.After;
import org.junit.Before;

public class TypePoolDefaultTypeDescriptionTest extends AbstractTypeDescriptionTest {

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofClassPath();
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    @Override
    protected TypeDescription describe(Class<?> type) {
        return typePool.describe(type.getName()).resolve();
    }
}
