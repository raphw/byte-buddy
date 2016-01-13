package net.bytebuddy.description.type;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypeDescriptionForPackageDescriptionTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    private TypeDescription typeDescription;

    @Mock
    private PackageDescription packageDescription;

    @Before
    public void setUp() throws Exception {
        typeDescription = new TypeDescription.ForPackageDescription(packageDescription);
    }

    @Test
    public void testName() throws Exception {
        when(packageDescription.getName()).thenReturn(FOO);
        assertThat(typeDescription.getName(), is(FOO + "." + PackageDescription.PACKAGE_CLASS_NAME));
    }

    @Test
    public void testModifiers() throws Exception {
        assertThat(typeDescription.getModifiers(), is(PackageDescription.PACKAGE_MODIFIERS));
    }

    @Test
    public void testInterfaces() throws Exception {
        assertThat(typeDescription.getInterfaces().size(), is(0));
    }

    @Test
    public void testAnnotations() throws Exception {
        AnnotationList annotationList = mock(AnnotationList.class);
        when(packageDescription.getDeclaredAnnotations()).thenReturn(annotationList);
        assertThat(typeDescription.getDeclaredAnnotations(), is(annotationList));
    }

    @Test
    public void testTypeVariables() throws Exception {
        assertThat(typeDescription.getTypeVariables().size(), is(0));
    }

    @Test
    public void testFields() throws Exception {
        assertThat(typeDescription.getDeclaredFields().size(), is(0));
    }

    @Test
    public void testMethods() throws Exception {
        assertThat(typeDescription.getDeclaredMethods().size(), is(0));
    }

    @Test
    public void testPackage() throws Exception {
        assertThat(typeDescription.getPackage(), is(packageDescription));
    }

    @Test
    public void testSuperClass() throws Exception {
        assertThat(typeDescription.getSuperClass(), is(TypeDescription.Generic.OBJECT));
    }
}
