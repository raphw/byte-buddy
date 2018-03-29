package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.FieldVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class FieldRegistryCompiledNoOpTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private FieldDescription fieldDescription;


    @Test(expected = IllegalStateException.class)
    public void testCannotResolveDefault() throws Exception {
        FieldRegistry.Compiled.NoOp.INSTANCE.target(fieldDescription).resolveDefault(FieldDescription.NO_DEFAULT_VALUE);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotResolveFieldAppender() throws Exception {
        FieldRegistry.Compiled.NoOp.INSTANCE.target(fieldDescription).getFieldAppender();
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotApplyPartially() throws Exception {
        FieldRegistry.Compiled.NoOp.INSTANCE.target(fieldDescription).apply(mock(FieldVisitor.class), mock(AnnotationValueFilter.Factory.class));
    }

    @Test
    public void testReturnsFieldAttributeAppender() throws Exception {
        TypeWriter.FieldPool.Record record = FieldRegistry.Compiled.NoOp.INSTANCE.target(fieldDescription);
        assertThat(record.isImplicit(), is(true));
    }
}
