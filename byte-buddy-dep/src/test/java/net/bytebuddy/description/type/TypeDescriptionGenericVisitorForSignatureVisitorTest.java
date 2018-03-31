package net.bytebuddy.description.type;

import org.junit.Test;
import org.objectweb.asm.signature.SignatureVisitor;

import static org.mockito.Mockito.mock;

public class TypeDescriptionGenericVisitorForSignatureVisitorTest {

    @Test(expected = IllegalStateException.class)
    public void testSignatureVisitorTypeVariableThrowsException() throws Exception {
        new TypeDescription.Generic.Visitor.ForSignatureVisitor(mock(SignatureVisitor.class)).onWildcard(mock(TypeDescription.Generic.class));
    }
}
