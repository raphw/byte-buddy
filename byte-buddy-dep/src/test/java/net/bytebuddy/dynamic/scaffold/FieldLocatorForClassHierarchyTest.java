package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.packaging.FieldLocatorTestHelper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FieldLocatorForClassHierarchyTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription typeDescription;

    @Test
    public void testClassHierarchyTypeFound() throws Exception {
        FieldLocator.Resolution resolution = new FieldLocator.ForClassHierarchy(TypeDescription.ForLoadedType.of(Sample.class)).locate(FOO);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.getField(), is((FieldDescription) new FieldDescription.ForLoadedField(Sample.class.getDeclaredField(FOO))));
    }

    @Test
    public void testClassHierarchyFoundWithType() throws Exception {
        FieldLocator.Resolution resolution = new FieldLocator.ForClassHierarchy(TypeDescription.ForLoadedType.of(Sample.class)).locate(FOO, TypeDescription.ForLoadedType.of(Void.class));
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.getField(), is((FieldDescription) new FieldDescription.ForLoadedField(Sample.class.getDeclaredField(FOO))));
    }

    @Test
    public void testClassHierarchyFoundInherited() throws Exception {
        FieldLocator.Resolution resolution = new FieldLocator.ForClassHierarchy(TypeDescription.ForLoadedType.of(Qux.class)).locate(BAR);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.getField(), is((FieldDescription) new FieldDescription.ForLoadedField(Sample.class.getDeclaredField(BAR))));
    }

    @Test
    public void testClassHierarchyFoundInheritedShadowed() throws Exception {
        FieldLocator.Resolution resolution = new FieldLocator.ForClassHierarchy(TypeDescription.ForLoadedType.of(Bar.class)).locate(BAR);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.getField(), is((FieldDescription) new FieldDescription.ForLoadedField(Bar.class.getDeclaredField(BAR))));
    }

    @Test
    public void testClassHierarchyNotFoundInherited() throws Exception {
        assertThat(new FieldLocator.ForClassHierarchy(TypeDescription.ForLoadedType.of(Bar.class)).locate(FOO).isResolved(),
                is(ClassFileVersion.of(Bar.class).isAtLeast(ClassFileVersion.JAVA_V11)));
    }

    @Test
    public void testClassHierarchyNotFoundInheritedNoNestMates() throws Exception {
        assertThat(new FieldLocator.ForClassHierarchy(TypeDescription.ForLoadedType.of(FieldLocatorTestHelper.class)).locate(FOO).isResolved(), is(false));
    }

    @Test
    public void testClassHierarchyNotFoundNotExistent() throws Exception {
        assertThat(new FieldLocator.ForClassHierarchy(TypeDescription.ForLoadedType.of(Sample.class)).locate(QUX).isResolved(), is(false));
    }

    @Test
    public void testClassHierarchyNotFoundInvisible() throws Exception {
        assertThat(new FieldLocator.ForClassHierarchy(TypeDescription.ForLoadedType.of(Sample.class), TypeDescription.ForLoadedType.of(Object.class)).locate(FOO).isResolved(), is(false));
    }

    @Test
    public void testClassHierarchyNotFoundWrongType() throws Exception {
        assertThat(new FieldLocator.ForClassHierarchy(TypeDescription.ForLoadedType.of(Sample.class)).locate(FOO, TypeDescription.ForLoadedType.of(Object.class)).isResolved(), is(false));
    }

    @Test
    public void testFactory() throws Exception {
        assertThat(FieldLocator.ForClassHierarchy.Factory.INSTANCE.make(typeDescription), hasPrototype((FieldLocator) new FieldLocator.ForClassHierarchy(typeDescription)));
    }

    @SuppressWarnings("unused")
    public static class Sample {

        private Void foo;

        protected Void bar;
    }

    @SuppressWarnings("unused")
    private static class Bar extends Sample {

        protected Void bar;
    }

    @SuppressWarnings("unused")
    private static class Qux extends Sample {

        private Void baz;
    }
}
