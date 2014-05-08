package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypeWriterMethodPoolEntryTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ByteCodeAppender first, second;

    @Mock
    private MethodAttributeAppender methodAttributeAppender;

    @Test
    public void testSimpleEntry() throws Exception {
        TypeWriter.MethodPool.Entry entry = new TypeWriter.MethodPool.Entry.Simple(first, methodAttributeAppender);
        assertThat(entry.isDefineMethod(), is(true));
        assertThat(entry.getByteCodeAppender(), is(first));
        assertThat(entry.getAttributeAppender(), is(methodAttributeAppender));
    }

    @Test
    public void testSimpleEntryHashCodeEquals() throws Exception {
        assertThat(new TypeWriter.MethodPool.Entry.Simple(first, methodAttributeAppender).hashCode(),
                is(new TypeWriter.MethodPool.Entry.Simple(first, methodAttributeAppender).hashCode()));
        assertThat(new TypeWriter.MethodPool.Entry.Simple(first, methodAttributeAppender),
                is(new TypeWriter.MethodPool.Entry.Simple(first, methodAttributeAppender)));
        assertThat(new TypeWriter.MethodPool.Entry.Simple(first, methodAttributeAppender).hashCode(),
                not(is(new TypeWriter.MethodPool.Entry.Simple(second, methodAttributeAppender).hashCode())));
        assertThat(new TypeWriter.MethodPool.Entry.Simple(first, methodAttributeAppender),
                not(is(new TypeWriter.MethodPool.Entry.Simple(second, methodAttributeAppender))));
    }

    @Test
    public void testSkipEntry() throws Exception {
        assertThat(TypeWriter.MethodPool.Entry.Skip.INSTANCE.isDefineMethod(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testSkipEntryWithoutByteCodeAppender() throws Exception {
        TypeWriter.MethodPool.Entry.Skip.INSTANCE.getByteCodeAppender();
    }

    @Test(expected = IllegalStateException.class)
    public void testSkipEntryWithoutAtributeAppender() throws Exception {
        TypeWriter.MethodPool.Entry.Skip.INSTANCE.getAttributeAppender();
    }
}
