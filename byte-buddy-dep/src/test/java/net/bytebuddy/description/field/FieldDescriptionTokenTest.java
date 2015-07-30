package net.bytebuddy.description.field;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class FieldDescriptionTokenTest {

    private static final String FOO = "foo";

    private static final String BAR = "bar";

    private static final int MODIFIERS = 42;

    @Test
    public void testFieldNameEqualityHashCode() throws Exception {
        assertThat(new FieldDescription.Token(FOO,
                        MODIFIERS,
                        mock(GenericTypeDescription.class),
                        Collections.singletonList(mock(AnnotationDescription.class))).hashCode(),
                is(new FieldDescription.Token(FOO,
                        MODIFIERS * 2,
                        mock(GenericTypeDescription.class),
                        Collections.singletonList(mock(AnnotationDescription.class))).hashCode()));
    }

    @Test
    public void testFieldNameInequalityHashCode() throws Exception {
        assertThat(new FieldDescription.Token(FOO,
                        MODIFIERS,
                        mock(GenericTypeDescription.class),
                        Collections.singletonList(mock(AnnotationDescription.class))).hashCode(),
                not(new FieldDescription.Token(BAR,
                        MODIFIERS * 2,
                        mock(GenericTypeDescription.class),
                        Collections.singletonList(mock(AnnotationDescription.class))).hashCode()));
    }

    @Test
    public void testFieldNameEquality() throws Exception {
        assertThat(new FieldDescription.Token(FOO,
                        MODIFIERS,
                        mock(GenericTypeDescription.class),
                        Collections.singletonList(mock(AnnotationDescription.class))),
                is(new FieldDescription.Token(FOO,
                        MODIFIERS * 2,
                        mock(GenericTypeDescription.class),
                        Collections.singletonList(mock(AnnotationDescription.class)))));
    }

    @Test
    public void testFieldNameInequality() throws Exception {
        assertThat(new FieldDescription.Token(FOO,
                        MODIFIERS,
                        mock(GenericTypeDescription.class),
                        Collections.singletonList(mock(AnnotationDescription.class))),
                not(new FieldDescription.Token(BAR,
                        MODIFIERS * 2,
                        mock(GenericTypeDescription.class),
                        Collections.singletonList(mock(AnnotationDescription.class)))));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldDescription.Token.class).applyBasic();
    }
}
