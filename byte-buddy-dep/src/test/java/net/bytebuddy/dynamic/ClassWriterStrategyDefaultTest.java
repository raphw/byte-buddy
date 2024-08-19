package net.bytebuddy.dynamic;

import net.bytebuddy.pool.TypePool;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassWriterStrategyDefaultTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypePool typePool;

    @Mock
    private ClassReader classReader;

    @Test
    @SuppressWarnings("deprecation")
    public void testConstantPoolRetention() {
        ClassWriter withoutReader = net.bytebuddy.dynamic.scaffold.ClassWriterStrategy.Default.CONSTANT_POOL_RETAINING.resolve(0, typePool);
        ClassWriter withReader = net.bytebuddy.dynamic.scaffold.ClassWriterStrategy.Default.CONSTANT_POOL_RETAINING.resolve(0, typePool, classReader);
        assertThat(withReader.toByteArray().length > withoutReader.toByteArray().length, is(true));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testConstantPoolDiscarding() {
        ClassWriter withoutReader = net.bytebuddy.dynamic.scaffold.ClassWriterStrategy.Default.CONSTANT_POOL_DISCARDING.resolve(0, typePool);
        ClassWriter withReader = net.bytebuddy.dynamic.scaffold.ClassWriterStrategy.Default.CONSTANT_POOL_DISCARDING.resolve(0, typePool, classReader);
        assertThat(withReader.toByteArray().length == withoutReader.toByteArray().length, is(true));
    }
}
