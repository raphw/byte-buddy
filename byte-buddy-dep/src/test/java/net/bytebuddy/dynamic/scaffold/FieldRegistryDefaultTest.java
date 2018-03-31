package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.dynamic.Transformer;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMatcher;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.FieldVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FieldRegistryDefaultTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private InstrumentedType instrumentedType;

    @Mock
    private FieldAttributeAppender.Factory distinctFactory;

    @Mock
    private FieldAttributeAppender distinct;

    @Mock
    private FieldDescription knownField, unknownField, instrumentedField;

    @Mock
    private LatentMatcher<FieldDescription> latentMatcher;

    @Mock
    private ElementMatcher<FieldDescription> matcher;

    @Mock
    private Object defaultValue, otherDefaultValue;

    @Mock
    private Transformer<FieldDescription> transformer;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(distinctFactory.make(instrumentedType)).thenReturn(distinct);
        when(latentMatcher.resolve(instrumentedType)).thenReturn((ElementMatcher) matcher);
        when(matcher.matches(knownField)).thenReturn(true);
        when(transformer.transform(instrumentedType, knownField)).thenReturn(instrumentedField);
    }

    @Test(expected = IllegalStateException.class)
    public void testImplicitFieldCannotResolveDefaultValue() throws Exception {
        new FieldRegistry.Default()
                .compile(instrumentedType)
                .target(unknownField)
                .resolveDefault(defaultValue);
    }

    @Test(expected = IllegalStateException.class)
    public void testImplicitFieldCannotReceiveAppender() throws Exception {
        new FieldRegistry.Default()
                .compile(instrumentedType)
                .target(unknownField)
                .getFieldAppender();
    }

    @Test(expected = IllegalStateException.class)
    public void testImplicitFieldCannotRApplyPartially() throws Exception {
        new FieldRegistry.Default()
                .compile(instrumentedType)
                .target(unknownField)
                .apply(mock(FieldVisitor.class), mock(AnnotationValueFilter.Factory.class));
    }

    @Test
    public void testKnownFieldRegistered() throws Exception {
        TypeWriter.FieldPool fieldPool = new FieldRegistry.Default()
                .prepend(latentMatcher, distinctFactory, defaultValue, transformer)
                .compile(instrumentedType);
        assertThat(fieldPool.target(knownField).isImplicit(), is(false));
        assertThat(fieldPool.target(knownField).getField(), is(instrumentedField));
        assertThat(fieldPool.target(knownField).getFieldAppender(), is(distinct));
        assertThat(fieldPool.target(knownField).resolveDefault(otherDefaultValue), is(defaultValue));
        assertThat(fieldPool.target(unknownField).isImplicit(), is(true));
        assertThat(fieldPool.target(unknownField).getField(), is(unknownField));
    }
}
