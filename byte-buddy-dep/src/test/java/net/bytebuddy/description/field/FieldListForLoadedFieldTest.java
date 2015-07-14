package net.bytebuddy.description.field;

import java.lang.reflect.Field;
import java.util.List;

public class FieldListForLoadedFieldTest extends AbstractFieldListTest<Field> {

    @Override
    protected Field getFirst() throws Exception {
        return Foo.class.getDeclaredField("foo");
    }

    @Override
    protected Field getSecond() throws Exception {
        return Foo.class.getDeclaredField("bar");
    }

    @Override
    protected FieldList asList(List<Field> elements) {
        return new FieldList.ForLoadedField(elements);
    }

    @Override
    protected FieldDescription asElement(Field element) {
        return new FieldDescription.ForLoadedField(element);
    }
}
