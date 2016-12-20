package net.bytebuddy.agent.builder;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderTransformerTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private DynamicType.Builder<?> builder;

    @Mock
    private AgentBuilder.Transformer first, second;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private ClassLoader classLoader;

    @Test
    @SuppressWarnings("unchecked")
    public void testNoOp() throws Exception {
        assertThat(AgentBuilder.Transformer.NoOp.INSTANCE.transform(builder, typeDescription, classLoader), sameInstance((DynamicType.Builder) builder));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCompound() throws Exception {
        when(first.transform(builder, typeDescription, classLoader)).thenReturn((DynamicType.Builder) builder);
        when(second.transform(builder, typeDescription, classLoader)).thenReturn((DynamicType.Builder) builder);
        assertThat(new AgentBuilder.Transformer.Compound(first, second).transform(builder, typeDescription, classLoader), sameInstance((DynamicType.Builder) builder));
        verify(first).transform(builder, typeDescription, classLoader);
        verifyNoMoreInteractions(first);
        verify(second).transform(builder, typeDescription, classLoader);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.Transformer.NoOp.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Transformer.Compound.class).create(new ObjectPropertyAssertion.Creator<List<?>>() {
            @Override
            public List<?> create() {
                return Collections.singletonList(mock(AgentBuilder.Transformer.class));
            }
        }).apply();
    }
}
