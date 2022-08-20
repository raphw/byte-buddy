package net.bytebuddy.asm;

import org.junit.Test;
import org.objectweb.asm.ClassVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassVisitorFactoryOtherTest {

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidInput() {
        ClassVisitorFactory.of(Object.class);
    }

    @Test
    public void testGetter() {
        assertThat(ClassVisitorFactory.of(ClassVisitor.class).getType(), is((Object) ClassVisitor.class));
    }
}
