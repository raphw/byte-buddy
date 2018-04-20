package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FieldLocatorForTopLevelTypeTest {

    private static final String FOO = "foo", QUX = "qux";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription;

    @Test
    public void testExactTypeFound() throws Exception {
        FieldLocator.Resolution resolution = new FieldLocator.ForTopLevelType(TypeDescription.ForLoadedType.of(Foo.class)).locate(FOO);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.getField(), is((FieldDescription) new FieldDescription.ForLoadedField(Foo.class.getDeclaredField(FOO))));
    }

    @Test
    public void testExactTypeFoundWithType() throws Exception {
        FieldLocator.Resolution resolution = new FieldLocator.ForTopLevelType(TypeDescription.ForLoadedType.of(Foo.class)).locate(FOO, TypeDescription.ForLoadedType.of(Void.class));
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.getField(), is((FieldDescription) new FieldDescription.ForLoadedField(Foo.class.getDeclaredField(FOO))));
    }

    @Test
    public void testExactTypeNotFoundInherited() throws Exception {
        assertThat(new FieldLocator.ForTopLevelType(TypeDescription.ForLoadedType.of(Bar.class)).locate(FOO).isResolved(), is(false));
    }

    @Test
    public void testExactTypeNotFoundNotExistent() throws Exception {
        assertThat(new FieldLocator.ForTopLevelType(TypeDescription.ForLoadedType.of(Foo.class)).locate(QUX).isResolved(), is(false));
    }

    @Test
    public void testExactTypeNotFoundWrongType() throws Exception {
        assertThat(new FieldLocator.ForTopLevelType(TypeDescription.ForLoadedType.of(Foo.class)).locate(FOO, TypeDescription.ForLoadedType.of(Object.class)).isResolved(), is(false));
    }

    @Test
    public void testFactory() throws Exception {
        assertThat(FieldLocator.ForTopLevelType.Factory.INSTANCE.make(typeDescription), hasPrototype((FieldLocator) new FieldLocator.ForTopLevelType(typeDescription)));
    }

    @SuppressWarnings("unused")
    private static class Foo {

        private Void foo;

        protected Void bar;
    }

    @SuppressWarnings("unused")
    private static class Bar extends Foo {

        protected Void bar;
    }
}
