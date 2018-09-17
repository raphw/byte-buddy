package net.bytebuddy.description.field;

import java.lang.reflect.Field;
import java.util.List;

public class FieldListForLoadedFieldsTest extends AbstractFieldListTest<Field, FieldDescription.InDefinedShape> {

    protected Field getFirst() throws Exception {
        return Foo.class.getDeclaredField("foo");
    }

    protected Field getSecond() throws Exception {
        return Foo.class.getDeclaredField("bar");
    }

    protected FieldList<FieldDescription.InDefinedShape> asList(List<Field> elements) {
        return new FieldList.ForLoadedFields(elements);
    }

    protected FieldDescription.InDefinedShape asElement(Field element) {
        return new FieldDescription.ForLoadedField(element);
    }
}
