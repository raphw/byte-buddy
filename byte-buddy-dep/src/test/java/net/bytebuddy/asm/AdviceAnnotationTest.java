package net.bytebuddy.asm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.annotation.*;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class AdviceAnnotationTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Advice.Argument.class, ElementType.PARAMETER},
                {Advice.AllArguments.class, ElementType.PARAMETER},
                {Advice.FieldValue.class, ElementType.PARAMETER},
                {Advice.Enter.class, ElementType.PARAMETER},
                {Advice.Unused.class, ElementType.PARAMETER},
                {Advice.Origin.class, ElementType.PARAMETER},
                {Advice.Return.class, ElementType.PARAMETER},
                {Advice.This.class, ElementType.PARAMETER},
                {Advice.Thrown.class, ElementType.PARAMETER},
                {Advice.StubValue.class, ElementType.PARAMETER},
                {Advice.OnMethodEnter.class, ElementType.METHOD},
                {Advice.OnMethodExit.class, ElementType.METHOD}
        });
    }

    private final Class<? extends Annotation> type;

    private final ElementType elementType;

    public AdviceAnnotationTest(Class<? extends Annotation> type, ElementType elementType) {
        this.type = type;
        this.elementType = elementType;
    }

    @Test
    public void testDocumented() throws Exception {
        assertThat(type.isAnnotationPresent(Documented.class), is(true));
    }

    @Test
    public void testVisible() throws Exception {
        assertThat(type.getAnnotation(Retention.class).value(), is(RetentionPolicy.RUNTIME));
    }

    @Test
    public void testTarget() throws Exception {
        assertThat(type.getAnnotation(Target.class).value().length, is(1));
        assertThat(type.getAnnotation(Target.class).value()[0], is(elementType));
    }
}
