package net.bytebuddy.description;

import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class ByteCodeElementTokenListTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ByteCodeElement.Token<?> original, transformed;

    @Mock
    private GenericTypeDescription.Visitor<? extends GenericTypeDescription> visitor;

    @Test
    @SuppressWarnings("unchecked")
    public void testTransformation() throws Exception {
        when(original.accept(visitor)).thenReturn((ByteCodeElement.Token) transformed);
        ByteCodeElement.Token.TokenList<?> tokenList = new ByteCodeElement.Token.TokenList(Collections.singletonList(original)).accept(visitor);
        assertThat(tokenList.size(), is(1));
        assertThat(tokenList.get(0), is((ByteCodeElement.Token) transformed));
    }
}
