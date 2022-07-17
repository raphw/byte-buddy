package net.bytebuddy.agent.builder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderTransformerDecoratorTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private ResettableClassFileTransformer classFileTransformer;

    @Mock
    private AgentBuilder.TransformerDecorator first, second;

    @Test
    public void testNoOp() {
        assertThat(AgentBuilder.TransformerDecorator.NoOp.INSTANCE.decorate(classFileTransformer), is(classFileTransformer));
    }

    @Test
    public void testCompound() {
        when(first.decorate(classFileTransformer)).thenReturn(classFileTransformer);
        when(second.decorate(classFileTransformer)).thenReturn(classFileTransformer);
        assertThat(new AgentBuilder.TransformerDecorator.Compound(first, second).decorate(classFileTransformer), is(classFileTransformer));
        verify(first).decorate(classFileTransformer);
        verifyNoMoreInteractions(first);
        verify(second).decorate(classFileTransformer);
        verifyNoMoreInteractions(second);
    }
}
