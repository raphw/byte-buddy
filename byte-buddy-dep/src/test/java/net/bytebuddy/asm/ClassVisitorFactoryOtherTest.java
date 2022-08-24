package net.bytebuddy.asm;

import net.bytebuddy.utility.OpenedClassReader;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

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
            /* empty */
        };
        ClassVisitorFactory.of(ClassVisitor.class).wrap(new ClassVisitor(OpenedClassReader.ASM_API) {

            @Override
            public void visitAttribute(Attribute attribute) {
                assertThat(attribute, not(sameInstance(sample)));
                try {
                    assertThat(attribute.getClass().getField("delegate").get(attribute), sameInstance((Object) sample));
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }
        }).visitAttribute(sample);
    }
}
