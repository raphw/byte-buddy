package net.bytebuddy.description.field;

import net.bytebuddy.description.method.MethodDescription;

import java.lang.reflect.Field;
import java.util.List;

public class FieldListForLoadedFieldTest extends AbstractFieldListTest<Field, FieldDescription.InDeclaredForm> {

    @Override
    protected Field getFirst() throws Exception {
        return Foo.class.getDeclaredField("foo");
    }

    @Override
    protected Field getSecond() throws Exception {
        return Foo.class.getDeclaredField("bar");
    }

    @Override
    protected FieldList<FieldDescription.InDeclaredForm> asList(List<Field> elements) {
        return new FieldList.ForLoadedField(elements);
    }

    @Override
    protected FieldDescription.InDeclaredForm asElement(Field element) {
        return new FieldDescription.ForLoadedField(element);
    }
}
