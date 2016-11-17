package net.bytebuddy.description.type;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class TypeDescriptionGenericLazyProjectionWithLazyNavigationTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription, rawSuperType;

    @Mock
    private TypeDescription.Generic superType;

    @Before
    public void setUp() throws Exception {
        when(typeDescription.asErasure()).thenReturn(typeDescription);
        when(superType.asErasure()).thenReturn(rawSuperType);
        when(superType.asGenericType()).thenReturn(superType);
    }

    @Test
    public void testLazySuperClass() throws Exception {
        when(typeDescription.getSuperClass()).thenReturn(superType);
        assertThat(new AssertingLazyType(typeDescription).getSuperClass().asErasure(), is(rawSuperType));
    }

    @Test
    public void testUndefinedLazySuperClass() throws Exception {
        assertThat(new AssertingLazyType(typeDescription).getSuperClass(), nullValue(TypeDescription.Generic.class));
    }

    @Test
    public void testInterfaceType() throws Exception {
        when(typeDescription.getInterfaces()).thenReturn(new TypeList.Generic.Explicit(superType));
        assertThat(new AssertingLazyType(typeDescription).getInterfaces().getOnly().asErasure(), is(rawSuperType));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testInterfaceTypeOutOfBounds() throws Exception {
        when(typeDescription.getInterfaces()).thenReturn(new TypeList.Generic.Explicit(superType));
        new AssertingLazyType(typeDescription).getInterfaces().get(1);
    }

    private static class AssertingLazyType extends TypeDescription.Generic.LazyProjection.WithLazyNavigation {

        private final TypeDescription typeDescription;

        private AssertingLazyType(TypeDescription typeDescription) {
            this.typeDescription = typeDescription;
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            throw new AssertionError();
        }

        @Override
        public TypeDescription asErasure() {
            return typeDescription;
        }

        @Override
        protected TypeDescription.Generic resolve() {
            throw new AssertionError();
        }
    }
}