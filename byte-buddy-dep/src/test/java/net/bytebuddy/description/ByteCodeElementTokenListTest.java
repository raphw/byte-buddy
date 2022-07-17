package net.bytebuddy.description;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class ByteCodeElementTokenListTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private ByteCodeElement.Token<?> original, transformed;

    @Mock
    private TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor;

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testTransformation() throws Exception {
        when(original.accept(visitor)).thenReturn((ByteCodeElement.Token) transformed);
        ByteCodeElement.Token.TokenList<?> tokenList = new ByteCodeElement.Token.TokenList(original).accept(visitor);
        assertThat(tokenList.size(), is(1));
        assertThat(tokenList.get(0), is((ByteCodeElement.Token) transformed));
    }
}
