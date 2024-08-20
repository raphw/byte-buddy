package net.bytebuddy.asm;

import net.bytebuddy.utility.OpenedClassReader;
import org.junit.Test;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassVisitorFactoryOtherTest {

    private static final String FOO = "foo";

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidInput() {
        ClassVisitorFactory.of(Object.class);
    }

    @Test
    public void testGetter() {
        assertThat(ClassVisitorFactory.of(ClassVisitor.class).getType(), is((Object) ClassVisitor.class));
    }

    @Test
    public void testAttributeWrapped() {
        final Attribute sample = new Attribute(FOO) {

            @Override
            public boolean isUnknown() {
                return false;
            }

            @Override
            public boolean isCodeAttribute() {
                return true;
            }
        };
        ClassVisitorFactory.of(ClassVisitor.class).wrap(new ClassVisitor(OpenedClassReader.ASM_API) {

            @Override
            public void visitAttribute(Attribute attribute) {
                assertThat(attribute, not(sameInstance(sample)));
                assertThat(attribute.type, is(FOO));
                assertThat(attribute.isUnknown(), is(false));
                assertThat(attribute.isCodeAttribute(), is(true));
                try {
                    assertThat(attribute.getClass().getField("delegate").get(attribute), sameInstance((Object) sample));
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }
        }).visitAttribute(sample);
    }
}
