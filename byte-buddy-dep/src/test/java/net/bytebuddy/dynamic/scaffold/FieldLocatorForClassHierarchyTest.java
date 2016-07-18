package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FieldLocatorForClassHierarchyTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription;

    @Test
    public void testClassHierarchyTypeFound() throws Exception {
        FieldLocator.Resolution resolution = new FieldLocator.ForClassHierarchy(new TypeDescription.ForLoadedType(Foo.class)).locate(FOO);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.getField(), is((FieldDescription) new FieldDescription.ForLoadedField(Foo.class.getDeclaredField(FOO))));
    }

    @Test
    public void testClassHierarchyFoundWithType() throws Exception {
        FieldLocator.Resolution resolution = new FieldLocator.ForClassHierarchy(new TypeDescription.ForLoadedType(Foo.class)).locate(FOO, new TypeDescription.ForLoadedType(Void.class));
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.getField(), is((FieldDescription) new FieldDescription.ForLoadedField(Foo.class.getDeclaredField(FOO))));
    }

    @Test
    public void testClassHierarchyFoundInherited() throws Exception {
        FieldLocator.Resolution resolution = new FieldLocator.ForClassHierarchy(new TypeDescription.ForLoadedType(Qux.class)).locate(BAR);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.getField(), is((FieldDescription) new FieldDescription.ForLoadedField(Foo.class.getDeclaredField(BAR))));
    }

    @Test
    public void testClassHierarchyFoundInheritedShadowed() throws Exception {
        FieldLocator.Resolution resolution = new FieldLocator.ForClassHierarchy(new TypeDescription.ForLoadedType(Bar.class)).locate(BAR);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.getField(), is((FieldDescription) new FieldDescription.ForLoadedField(Bar.class.getDeclaredField(BAR))));
    }

    @Test
    public void testClassHierarchyNotFoundInherited() throws Exception {
        assertThat(new FieldLocator.ForClassHierarchy(new TypeDescription.ForLoadedType(Bar.class)).locate(FOO).isResolved(), is(false));
    }

    @Test
    public void testClassHierarchyNotFoundNotExistent() throws Exception {
        assertThat(new FieldLocator.ForClassHierarchy(new TypeDescription.ForLoadedType(Foo.class)).locate(QUX).isResolved(), is(false));
    }

    @Test
    public void testClassHierarchyNotFoundInvisible() throws Exception {
        assertThat(new FieldLocator.ForClassHierarchy(new TypeDescription.ForLoadedType(Foo.class), new TypeDescription.ForLoadedType(Object.class)).locate(FOO).isResolved(), is(false));
    }

    @Test
    public void testClassHierarchyNotFoundWrongType() throws Exception {
        assertThat(new FieldLocator.ForClassHierarchy(new TypeDescription.ForLoadedType(Foo.class)).locate(FOO, new TypeDescription.ForLoadedType(Object.class)).isResolved(), is(false));
    }

    @Test
    public void testFactory() throws Exception {
        assertThat(FieldLocator.ForClassHierarchy.Factory.INSTANCE.make(typeDescription), is((FieldLocator) new FieldLocator.ForClassHierarchy(typeDescription)));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldLocator.ForClassHierarchy.class).apply();
        ObjectPropertyAssertion.of(FieldLocator.ForClassHierarchy.Factory.class).apply();
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

    @SuppressWarnings("unused")
    private static class Qux extends Foo {

        private Void baz;
    }
}
