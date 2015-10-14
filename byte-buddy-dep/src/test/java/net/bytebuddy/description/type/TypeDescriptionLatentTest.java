package net.bytebuddy.description.type;

import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class TypeDescriptionLatentTest {

    private static final String FOO = "foo";

    private static final int MODIFIERS = 42;

    @Rule
    public MockitoRule mockitoRule = new MockitoRule(this);

    @Mock
    private GenericTypeDescription superType, interfaceType;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(superType.accept(any(GenericTypeDescription.Visitor.class))).thenReturn(superType);
        when(interfaceType.accept(any(GenericTypeDescription.Visitor.class))).thenReturn(interfaceType);
    }

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

    @Test(expected = IllegalStateException.class)
    public void testFields() throws Exception {
        new TypeDescription.Latent(FOO, MODIFIERS, superType, Collections.singletonList(interfaceType)).getDeclaredFields();
    }

    @Test(expected = IllegalStateException.class)
    public void testMethods() throws Exception {
        new TypeDescription.Latent(FOO, MODIFIERS, superType, Collections.singletonList(interfaceType)).getDeclaredMethods();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnnotations() throws Exception {
        new TypeDescription.Latent(FOO, MODIFIERS, superType, Collections.singletonList(interfaceType)).getDeclaredAnnotations();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeVariables() throws Exception {
        new TypeDescription.Latent(FOO, MODIFIERS, superType, Collections.singletonList(interfaceType)).getTypeVariables();
    }

    @Test(expected = IllegalStateException.class)
    public void testMemberClass() throws Exception {
        new TypeDescription.Latent(FOO, MODIFIERS, superType, Collections.singletonList(interfaceType)).isMemberClass();
    }

    @Test(expected = IllegalStateException.class)
    public void testAnoynmousClass() throws Exception {
        new TypeDescription.Latent(FOO, MODIFIERS, superType, Collections.singletonList(interfaceType)).isAnonymousClass();
    }

    @Test(expected = IllegalStateException.class)
    public void testLocalClass() throws Exception {
        new TypeDescription.Latent(FOO, MODIFIERS, superType, Collections.singletonList(interfaceType)).isLocalClass();
    }

    @Test(expected = IllegalStateException.class)
    public void testEnclosingMethod() throws Exception {
        new TypeDescription.Latent(FOO, MODIFIERS, superType, Collections.singletonList(interfaceType)).getEnclosingMethod();
    }

    @Test(expected = IllegalStateException.class)
    public void testEnclosingType() throws Exception {
        new TypeDescription.Latent(FOO, MODIFIERS, superType, Collections.singletonList(interfaceType)).getEnclosingType();
    }
}