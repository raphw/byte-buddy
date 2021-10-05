package net.bytebuddy.description.annotation;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class AnnotationValueForTypeDescriptionTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {boolean.class},
                {byte.class},
                {short.class},
                {char.class},
                {int.class},
                {long.class},
                {float.class},
                {double.class},
                {void.class},
                {Object.class},
                {boolean[].class},
                {byte[].class},
                {short[].class},
                {char[].class},
                {int[].class},
                {long[].class},
                {float[].class},
                {double[].class},
                {Object[].class}
        });
    }

    private final Class<?> type;

    public AnnotationValueForTypeDescriptionTest(Class<?> type) {
        this.type = type;
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testCanLoadType() {
        assertThat(new AnnotationValue.ForTypeDescription(TypeDescription.ForLoadedType.of(type))
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER)
                .resolve(), is((Object) type));
    }
}
