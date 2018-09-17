package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class MethodGraphSimpleTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private MethodDescription.SignatureToken token;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.asSignatureToken()).thenReturn(token);
    }

    @Test
    public void testNodeList() throws Exception {
        assertThat(MethodGraph.Simple.of(Collections.singletonList(methodDescription)).listNodes().getOnly(),
                hasPrototype((MethodGraph.Node) new MethodGraph.Node.Simple(methodDescription)));
    }

    @Test
    public void testNodeLocation() throws Exception {
        assertThat(MethodGraph.Simple.of(Collections.singletonList(methodDescription)).locate(token),
                hasPrototype((MethodGraph.Node) new MethodGraph.Node.Simple(methodDescription)));
    }
}
