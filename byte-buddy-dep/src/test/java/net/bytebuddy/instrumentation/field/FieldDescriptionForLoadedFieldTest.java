package net.bytebuddy.instrumentation.field;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.packaging.PackagePrivateField;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class FieldDescriptionForLoadedFieldTest {

    private static final String FOO = "foo";
    private FieldDescription fieldDescription;
    private FieldDescription privateField;
    private FieldDescription packagePrivateField;
    private FieldDescription protectedField;

    @Before
    public void setUp() throws Exception {
        fieldDescription = new FieldDescription.ForLoadedField(Foo.class.getDeclaredField(FOO));
        protectedField = new FieldDescription.ForLoadedField(PackagePrivateField.class.getDeclaredField(PackagePrivateField.PROTECTED_FIELD_NAME));
        packagePrivateField = new FieldDescription.ForLoadedField(PackagePrivateField.class.getDeclaredField(PackagePrivateField.PACKAGE_PRIVATE_FIELD_NAME));
        privateField = new FieldDescription.ForLoadedField(PackagePrivateField.class.getDeclaredField(PackagePrivateField.PRIVATE_FIELD_NAME));
    }

    @Test
    public void testFieldName() throws Exception {
        assertThat(fieldDescription.getName(), is(FOO));
        assertThat(fieldDescription.getInternalName(), is(FOO));
    }

    @Test
    public void testIsVisibleTo() throws Exception {
        assertThat(fieldDescription.isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(true));
        assertThat(privateField.isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(false));
        assertThat(packagePrivateField.isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(false));
        assertThat(protectedField.isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(false));
    }

    @Test
    public void testFieldType() throws Exception {
        assertThat(fieldDescription.getFieldType().represents(Object.class), is(true));
    }

    @Test
    public void testDeclaringType() throws Exception {
        assertThat(fieldDescription.getDeclaringType().represents(Foo.class), is(true));
    }

    private static class Foo {

        public Object foo;
    }
}
