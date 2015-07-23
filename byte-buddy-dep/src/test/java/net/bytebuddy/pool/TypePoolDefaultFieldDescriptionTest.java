package net.bytebuddy.pool;

import net.bytebuddy.description.field.AbstractFieldDescriptionTest;
import net.bytebuddy.description.field.FieldDescription;
import org.junit.After;
import org.junit.Before;

import java.lang.reflect.Field;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class TypePoolDefaultFieldDescriptionTest extends AbstractFieldDescriptionTest {

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        typePool = TypePool.Default.ofClassPath();
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    @Override
    protected FieldDescription.InDefinedShape describe(Field field) {
        return typePool.describe(field.getDeclaringClass().getName())
                .resolve()
                .getDeclaredFields().filter(named(field.getName())).getOnly();
    }
}
