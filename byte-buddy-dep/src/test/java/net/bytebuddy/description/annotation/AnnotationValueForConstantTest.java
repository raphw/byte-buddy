package net.bytebuddy.description.annotation;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class AnnotationValueForConstantTest {

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgument() throws Exception {
        AnnotationValue.ForConstant.of(new Object());
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AnnotationValue.ForConstant.PropertyDelegate.ForArrayType.class).apply();
        ObjectPropertyAssertion.of(AnnotationValue.ForConstant.PropertyDelegate.ForNonArrayType.class).apply();
    }
}
