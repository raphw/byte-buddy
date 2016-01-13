package net.bytebuddy.description.type;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class TypeDescriptionGenericVisitorAnnotationStripperTest {

    // TODO:


    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.AnnotationStripper.class).apply();
    }
}