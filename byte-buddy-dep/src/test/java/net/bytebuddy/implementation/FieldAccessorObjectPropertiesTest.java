package net.bytebuddy.implementation;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class FieldAccessorObjectPropertiesTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final Class<?> TYPE = Void.class, OTHER_TYPE = Object.class;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription first, second;

    @Test
    public void testBeanPropertyFieldAccessor() throws Exception {
        assertThat(FieldAccessor.ofBeanProperty().hashCode(), is(FieldAccessor.ofBeanProperty().hashCode()));
        assertThat(FieldAccessor.ofBeanProperty(), is(FieldAccessor.ofBeanProperty()));
        assertThat(FieldAccessor.ofBeanProperty().in(first).hashCode(), is(FieldAccessor.ofBeanProperty().in(first).hashCode()));
        assertThat(FieldAccessor.ofBeanProperty().in(first), is(FieldAccessor.ofBeanProperty().in(first)));
        assertThat(FieldAccessor.ofBeanProperty().in(first).hashCode(), not(is(FieldAccessor.ofBeanProperty().hashCode())));
        assertThat((FieldAccessor) FieldAccessor.ofBeanProperty().in(first), not(is((FieldAccessor) FieldAccessor.ofBeanProperty())));
        assertThat(FieldAccessor.ofBeanProperty().in(first).hashCode(), not(is(FieldAccessor.ofBeanProperty().in(second).hashCode())));
        assertThat(FieldAccessor.ofBeanProperty().in(first), not(is(FieldAccessor.ofBeanProperty().in(second))));
    }

    @Test
    public void testExplicitFieldAccessor() throws Exception {
        assertThat(FieldAccessor.ofField(FOO).hashCode(), is(FieldAccessor.ofField(FOO).hashCode()));
        assertThat(FieldAccessor.ofField(FOO), is(FieldAccessor.ofField(FOO)));
        assertThat(FieldAccessor.ofField(FOO).hashCode(), not(is(FieldAccessor.ofField(BAR).hashCode())));
        assertThat(FieldAccessor.ofField(FOO), not(is(FieldAccessor.ofField(BAR))));
        assertThat(FieldAccessor.ofField(FOO).in(first).hashCode(), is(FieldAccessor.ofField(FOO).in(first).hashCode()));
        assertThat(FieldAccessor.ofField(FOO).in(first), is(FieldAccessor.ofField(FOO).in(first)));
        assertThat(FieldAccessor.ofField(FOO).in(first).hashCode(), not(is(FieldAccessor.ofField(FOO).hashCode())));
        assertThat((FieldAccessor) FieldAccessor.ofField(FOO).in(first), not(is((FieldAccessor) FieldAccessor.ofField(FOO))));
        assertThat(FieldAccessor.ofField(FOO).in(first).hashCode(), not(is(FieldAccessor.ofField(FOO).in(second).hashCode())));
        assertThat(FieldAccessor.ofField(FOO).in(first), not(is(FieldAccessor.ofField(FOO).in(second))));
        assertThat(FieldAccessor.ofField(FOO).defineAs(TYPE).hashCode(), is(FieldAccessor.ofField(FOO).defineAs(TYPE).hashCode()));
        assertThat(FieldAccessor.ofField(FOO).defineAs(TYPE), is(FieldAccessor.ofField(FOO).defineAs(TYPE)));
        assertThat(FieldAccessor.ofField(FOO).defineAs(TYPE).hashCode(), not(is(FieldAccessor.ofField(FOO).defineAs(OTHER_TYPE).hashCode())));
        assertThat(FieldAccessor.ofField(FOO).defineAs(TYPE), not(is(FieldAccessor.ofField(FOO).defineAs(OTHER_TYPE))));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldAccessor.Appender.class).apply();
        ObjectPropertyAssertion.of(FieldAccessor.ForNamedField.class).apply();
        ObjectPropertyAssertion.of(FieldAccessor.ForUnnamedField.class).apply();
        ObjectPropertyAssertion.of(FieldAccessor.FieldLocator.ForGivenType.class).apply();
        ObjectPropertyAssertion.of(FieldAccessor.FieldLocator.ForGivenType.Factory.class).apply();
        ObjectPropertyAssertion.of(FieldAccessor.FieldLocator.ForInstrumentedType.class).apply();
        ObjectPropertyAssertion.of(FieldAccessor.FieldLocator.ForInstrumentedTypeHierarchy.class).apply();
        ObjectPropertyAssertion.of(FieldAccessor.FieldLocator.ForInstrumentedTypeHierarchy.Factory.class).apply();
        ObjectPropertyAssertion.of(FieldAccessor.FieldNameExtractor.ForBeanProperty.class).apply();
        ObjectPropertyAssertion.of(FieldAccessor.ForNamedField.PreparationHandler.FieldDefiner.class).apply();
        ObjectPropertyAssertion.of(FieldAccessor.ForNamedField.PreparationHandler.NoOp.class).apply();
    }
}
