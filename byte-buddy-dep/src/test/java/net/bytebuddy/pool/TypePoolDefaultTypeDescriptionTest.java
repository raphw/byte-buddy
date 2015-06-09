package net.bytebuddy.pool;

import net.bytebuddy.description.type.AbstractTypeDescriptionTest;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import org.junit.After;
import org.junit.Before;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.is;

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
                .filter(is(field))
                .getOnly()
                .getType();
    }

    @Override
    protected GenericTypeDescription describe(Method method) {
        return typePool.describe(method.getDeclaringClass().getName()).resolve()
                .getDeclaredMethods()
                .filter(is(method))
                .getOnly()
                .getReturnTypeGen();
    }
}
