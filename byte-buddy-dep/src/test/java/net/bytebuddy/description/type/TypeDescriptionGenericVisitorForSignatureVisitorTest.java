package net.bytebuddy.description.type;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class TypeDescriptionGenericVisitorForSignatureVisitorTest {

    /* The actual methods are implicitly tested when reading types via a type pool or while writing type signatures when creating types. */

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.ForSignatureVisitor.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.ForSignatureVisitor.OfParameter.class).apply();
    }
}
