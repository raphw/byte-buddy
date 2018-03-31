package net.bytebuddy.description.type;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.scaffold.TypeInitializer;
import net.bytebuddy.dynamic.scaffold.TypeWriter;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeInitializerTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeWriter.MethodPool.Record record, expanded;

    @Mock
    private ByteCodeAppender byteCodeAppender;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Mock
    private MethodDescription methodDescription;

    @Test
    public void testNoneExpansion() throws Exception {
        assertThat(TypeInitializer.None.INSTANCE.expandWith(byteCodeAppender), hasPrototype((TypeInitializer) new TypeInitializer.Simple(byteCodeAppender)));
    }

    @Test
    public void testNoneDefined() throws Exception {
        assertThat(TypeInitializer.None.INSTANCE.isDefined(), is(false));
    }

    @Test
    public void testNoneThrowsExceptionOnApplication() throws Exception {
        ByteCodeAppender.Size size = TypeInitializer.None.INSTANCE.apply(methodVisitor, implementationContext, methodDescription);
        assertThat(size.getOperandStackSize(), is(0));
        assertThat(size.getLocalVariableSize(), is(0));
        verifyZeroInteractions(methodDescription);
    }

    @Test
    public void testNoneWrap() throws Exception {
        assertThat(TypeInitializer.None.INSTANCE.wrap(record), is(record));
    }

    @Test
    public void testSimpleExpansion() throws Exception {
        assertThat(new TypeInitializer.Simple(byteCodeAppender).expandWith(byteCodeAppender),
                hasPrototype((TypeInitializer) new TypeInitializer.Simple(new ByteCodeAppender.Compound(byteCodeAppender, byteCodeAppender))));
    }

    @Test
    public void testSimpleApplication() throws Exception {
        TypeInitializer typeInitializer = new TypeInitializer.Simple(byteCodeAppender);
        assertThat(typeInitializer.isDefined(), is(true));
        typeInitializer.apply(methodVisitor, implementationContext, methodDescription);
        verify(byteCodeAppender).apply(methodVisitor, implementationContext, methodDescription);
        verifyZeroInteractions(byteCodeAppender);
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testSimpleWrap() throws Exception {
        when(record.prepend(byteCodeAppender)).thenReturn(expanded);
        assertThat(new TypeInitializer.Simple(byteCodeAppender).wrap(record), is(expanded));
    }
}
