package net.bytebuddy.pool;

import net.bytebuddy.description.type.AbstractTypeDescriptionTest;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import org.junit.After;
import org.junit.Before;

import java.lang.reflect.Field;

import static net.bytebuddy.matcher.ElementMatchers.named;

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

    @Override
    protected GenericTypeDescription describe(Field field) {
        return typePool.describe(field.getDeclaringClass().getName()).resolve()
                .getDeclaredFields()
                .filter(named(field.getName()))
                .getOnly()
                .getFieldTypeGen();
    }
}
