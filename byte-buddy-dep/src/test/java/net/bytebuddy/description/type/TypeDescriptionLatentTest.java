package net.bytebuddy.description.type;

import net.bytebuddy.description.type.generic.GenericTypeDescription;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TypeDescriptionLatentTest {

    private static final String FOO = "foo";

    private static final int MODIFIERS = 42;

    @Mock
    private GenericTypeDescription superType, interfaceType;

    @Test
    public void testName() throws Exception {
        assertThat(new TypeDescription.Latent(FOO, MODIFIERS, superType, Collections.singletonList(interfaceType)).getName(), is(FOO));
    }

    @Test
    public void testModifiers() throws Exception {
        assertThat(new TypeDescription.Latent(FOO, MODIFIERS, superType, Collections.singletonList(interfaceType)).getModifiers(), is(MODIFIERS));
    }

    @Test
    public void testSuperType() throws Exception {
        assertThat(new TypeDescription.Latent(FOO, MODIFIERS, superType, Collections.singletonList(interfaceType)).getSuperType(), is(superType));
    }

    @Test
    public void testInterfaceTypes() throws Exception {
        assertThat(new TypeDescription.Latent(FOO, MODIFIERS, superType, Collections.singletonList(interfaceType)).getInterfaces().size(), is(1));
        assertThat(new TypeDescription.Latent(FOO, MODIFIERS, superType, Collections.singletonList(interfaceType)).getInterfaces().getOnly(), is(interfaceType));
    }

    @Test
    public void testFields() throws Exception {
        assertThat(new TypeDescription.Latent(FOO, MODIFIERS, superType, Collections.singletonList(interfaceType)).getDeclaredFields().size(), is(0));
    }

    @Test
    public void testMethods() throws Exception {
        assertThat(new TypeDescription.Latent(FOO, MODIFIERS, superType, Collections.singletonList(interfaceType)).getDeclaredMethods().size(), is(0));
    }

    @Test
    public void testAnnotations() throws Exception {
        assertThat(new TypeDescription.Latent(FOO, MODIFIERS, superType, Collections.singletonList(interfaceType)).getDeclaredAnnotations().size(), is(0));
    }

    @Test
    public void testTypeVariables() throws Exception {
        assertThat(new TypeDescription.Latent(FOO, MODIFIERS, superType, Collections.singletonList(interfaceType)).getTypeVariables().size(), is(0));
    }
}