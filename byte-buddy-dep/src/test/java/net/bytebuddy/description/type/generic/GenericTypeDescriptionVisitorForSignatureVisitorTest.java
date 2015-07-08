package net.bytebuddy.description.type.generic;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class GenericTypeDescriptionVisitorForSignatureVisitorTest {

    /* The actual methods are implicitly tested when reading types via a type pool or while writing type signatures when creating types. */

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(GenericTypeDescription.Visitor.ForSignatureVisitor.class).apply();
        ObjectPropertyAssertion.of(GenericTypeDescription.Visitor.ForSignatureVisitor.OfParameter.class).apply();
    }
}
