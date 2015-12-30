package net.bytebuddy.description.method;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodDescriptionTokenTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodDescription.Token.class).create(new ObjectPropertyAssertion.Creator<List<?>>() {
            @Override
            public List<?> create() {
                TypeDescription.Generic typeDescription = mock(TypeDescription.Generic.class);
                when(typeDescription.asGenericType()).thenReturn(typeDescription);
                return Collections.singletonList(typeDescription);
            }
        }).apply();
        ObjectPropertyAssertion.of(MethodDescription.SignatureToken.class).apply();
        ObjectPropertyAssertion.of(MethodDescription.TypeToken.class).apply();
    }
}
