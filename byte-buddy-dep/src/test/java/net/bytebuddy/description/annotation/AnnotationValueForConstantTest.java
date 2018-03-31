package net.bytebuddy.description.annotation;

import org.junit.Test;

public class AnnotationValueForConstantTest {

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgument() throws Exception {
        AnnotationValue.ForConstant.of(new Object());
    }
}
