package net.bytebuddy.dynamic;

import net.bytebuddy.dynamic.scaffold.ClassWriterStrategy;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ClassWriterStrategyDefaultTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypePool typePool;

    @Mock
    private ClassReader classReader;

    @Test
    public void testConstantPoolRetention() {
        ClassWriter withoutReader = ClassWriterStrategy.Default.CONSTANT_POOL_RETAINING.resolve(0, typePool);
        ClassWriter withReader = ClassWriterStrategy.Default.CONSTANT_POOL_RETAINING.resolve(0, typePool, classReader);
        assertThat(withReader.toByteArray().length > withoutReader.toByteArray().length, is(true));
    }

    @Test
    public void testConstantPoolDiscarding() {
        ClassWriter withoutReader = ClassWriterStrategy.Default.CONSTANT_POOL_DISCARDING.resolve(0, typePool);
        ClassWriter withReader = ClassWriterStrategy.Default.CONSTANT_POOL_DISCARDING.resolve(0, typePool, classReader);
        assertThat(withReader.toByteArray().length == withoutReader.toByteArray().length, is(true));
    }
}
