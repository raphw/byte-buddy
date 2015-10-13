package net.bytebuddy.description.type;

import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class TypeDescriptionAbstractBaseRawTypeWrapperTest {

    @Test
    public void testRawType() throws Exception {
        GenericTypeDescription rawType = new TypeDescription.ForLoadedType(Foo.class).accept(TypeDescription.AbstractBase.RawTypeWrapper.INSTANCE);
        assertThat(rawType.getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(rawType.getDeclaredMethods().filter(isMethod()).getOnly().getReturnType().getSort(), is(GenericTypeDescription.Sort.NON_GENERIC));
        assertThat(rawType.getDeclaredMethods().filter(isMethod()).getOnly().getReturnType(), is((GenericTypeDescription) TypeDescription.OBJECT));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.AbstractBase.RawTypeWrapper.class).apply();
    }

    @SuppressWarnings("unused")
    private static class Foo<T> {

        T foo() {
            return null;
        }
    }
}
