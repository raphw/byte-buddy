package net.bytebuddy.description.type;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.objectweb.asm.signature.SignatureVisitor;

import static org.mockito.Mockito.mock;

public class TypeDescriptionGenericVisitorForSignatureVisitorTest {

    @Test(expected = IllegalStateException.class)
    public void testSignatureVisitorTypeVariableThrowsException() throws Exception {
        new TypeDescription.Generic.Visitor.ForSignatureVisitor(mock(SignatureVisitor.class)).onWildcard(mock(TypeDescription.Generic.class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.ForSignatureVisitor.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.ForSignatureVisitor.OfTypeArgument.class).apply();
    }
}
