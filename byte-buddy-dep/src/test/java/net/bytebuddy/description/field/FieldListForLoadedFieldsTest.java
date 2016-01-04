package net.bytebuddy.description.field;

import java.lang.reflect.Field;
import java.util.List;

public class FieldListForLoadedFieldsTest extends AbstractFieldListTest<Field, FieldDescription.InDefinedShape> {

    @Override
    protected Field getFirst() throws Exception {
        return Foo.class.getDeclaredField("foo");
    }

    @Override
    protected Field getSecond() throws Exception {
        return Foo.class.getDeclaredField("bar");
    }

    @Override
    protected FieldList<FieldDescription.InDefinedShape> asList(List<Field> elements) {
        return new FieldList.ForLoadedFields(elements);
    }

    @Override
    protected FieldDescription.InDefinedShape asElement(Field element) {
        return new FieldDescription.ForLoadedField(element);
    }
}
