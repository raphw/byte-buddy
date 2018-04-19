package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.ClassReader;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClassWriterStrategyFrameComputingClassWriterTest {

    private static final String FOO = "pkg/foo", BAR = "pkg/bar", QUX = "pkg/qux", BAZ = "pkg/baz", FOOBAR = "pkg/foobar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypePool typePool;

    @Mock
    private TypeDescription leftType, rightType, superClass;

    @Mock
    private TypeDescription.Generic genericSuperClass;

    private ClassWriterStrategy.FrameComputingClassWriter frameComputingClassWriter;

    @Before
    public void setUp() throws Exception {
        frameComputingClassWriter = new ClassWriterStrategy.FrameComputingClassWriter(mock(ClassReader.class), 0, typePool);
        when(typePool.describe(FOO.replace('/', '.'))).thenReturn(new TypePool.Resolution.Simple(leftType));
        when(typePool.describe(BAR.replace('/', '.'))).thenReturn(new TypePool.Resolution.Simple(rightType));
        when(leftType.getInternalName()).thenReturn(QUX);
        when(rightType.getInternalName()).thenReturn(BAZ);
        when(leftType.getSuperClass()).thenReturn(genericSuperClass);
        when(genericSuperClass.asErasure()).thenReturn(superClass);
        when(superClass.getInternalName()).thenReturn(FOOBAR);
    }

    @Test
    public void testLeftIsAssignable() throws Exception {
        when(leftType.isAssignableFrom(rightType)).thenReturn(true);
        assertThat(frameComputingClassWriter.getCommonSuperClass(FOO, BAR), is(QUX));
    }

    @Test
    public void testRightIsAssignable() throws Exception {
        when(leftType.isAssignableTo(rightType)).thenReturn(true);
        assertThat(frameComputingClassWriter.getCommonSuperClass(FOO, BAR), is(BAZ));
    }

    @Test
    public void testLeftIsInterface() throws Exception {
        when(leftType.isInterface()).thenReturn(true);
        assertThat(frameComputingClassWriter.getCommonSuperClass(FOO, BAR), is(TypeDescription.OBJECT.getInternalName()));
    }

    @Test
    public void testRightIsInterface() throws Exception {
        when(rightType.isInterface()).thenReturn(true);
        assertThat(frameComputingClassWriter.getCommonSuperClass(FOO, BAR), is(TypeDescription.OBJECT.getInternalName()));
    }

    @Test
    public void testSuperClassIteration() throws Exception {
        when(superClass.isAssignableFrom(rightType)).thenReturn(true);
        assertThat(frameComputingClassWriter.getCommonSuperClass(FOO, BAR), is(FOOBAR));
    }
}
