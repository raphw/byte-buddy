package net.bytebuddy.instrumentation.field;

import java.lang.reflect.Field;

public class FieldDescriptionForLoadedFieldTest extends AbstractFieldDescriptionTest {

    @Override
    protected FieldDescription describe(Field field) {
        return new FieldDescription.ForLoadedField(field);
    }
}
