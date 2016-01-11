package net.bytebuddy;

import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.precompiled.TypeAnnotation;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class ByteBuddyTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteBuddy.class).apply();
        ObjectPropertyAssertion.of(ByteBuddy.EnumerationImplementation.class).apply();
    }

    @Test
    public void testName() throws Exception {
        TypePool.Default.ofClassPath().describe(Foo.class.getName()).resolve();
    }

    static class Foo<@TypeAnnotation(0) T extends @TypeAnnotation(1) Runnable, @TypeAnnotation(0) S extends @TypeAnnotation(1) Object> {

    }
}
