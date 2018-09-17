package net.bytebuddy.pool;

import net.bytebuddy.description.field.AbstractFieldListTest;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import org.junit.After;
import org.junit.Before;

import java.lang.reflect.Field;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.anyOf;

public class TypePoolDefaultLazyFieldListTest extends AbstractFieldListTest<Field, FieldDescription.InDefinedShape> {

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofClassPath();
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    protected Field getFirst() throws Exception {
        return Foo.class.getDeclaredField("foo");
    }

    protected Field getSecond() throws Exception {
        return Foo.class.getDeclaredField("bar");
    }

    protected FieldList<FieldDescription.InDefinedShape> asList(List<Field> elements) {
        return typePool.describe(Foo.class.getName()).resolve().getDeclaredFields().filter(anyOf(elements.toArray(new Field[elements.size()])));
    }

    protected FieldDescription.InDefinedShape asElement(Field element) {
        return new FieldDescription.ForLoadedField(element);
    }
}
