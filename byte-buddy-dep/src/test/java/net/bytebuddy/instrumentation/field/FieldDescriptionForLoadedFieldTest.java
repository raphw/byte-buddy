package net.bytebuddy.instrumentation.field;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import org.junit.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FieldDescriptionForLoadedFieldTest extends AbstractFieldDescriptionTest {

    @Override
    protected FieldDescription describe(Field field) {
        return new FieldDescription.ForLoadedField(field);
    }
}
