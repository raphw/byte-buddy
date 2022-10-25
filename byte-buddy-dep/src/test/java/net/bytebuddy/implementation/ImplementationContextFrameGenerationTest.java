package net.bytebuddy.implementation;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ImplementationContextFrameGenerationTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodVisitor methodVisitor;

    @Test
    public void testActive() {
        assertThat(Implementation.Context.FrameGeneration.GENERATE.isActive(), is(true));
        assertThat(Implementation.Context.FrameGeneration.EXPAND.isActive(), is(true));
        assertThat(Implementation.Context.FrameGeneration.DISABLED.isActive(), is(false));
    }

    @Test
    public void testGenerateSame() {
        Implementation.Context.FrameGeneration.GENERATE.same(methodVisitor, Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)));
        verify(methodVisitor).visitFrame(Opcodes.F_SAME, 0, new Object[0], 0, new Object[0]);
    }

    @Test
    public void testExpandSame() {
        Implementation.Context.FrameGeneration.EXPAND.same(methodVisitor, Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)));
        verify(methodVisitor).visitFrame(Opcodes.F_NEW, 1, new Object[]{Type.getInternalName(Object.class)}, 0, new Object[0]);
    }

    @Test
    public void testIgnoreSame() {
        Implementation.Context.FrameGeneration.DISABLED.same(methodVisitor, Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)));
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testGenerateSame1() {
        Implementation.Context.FrameGeneration.GENERATE.same1(methodVisitor, TypeDescription.ForLoadedType.of(Object.class), Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)));
        verify(methodVisitor).visitFrame(Opcodes.F_SAME1, 0, new Object[0], 1, new Object[]{Type.getInternalName(Object.class)});
    }

    @Test
    public void testExpandSame1() {
        Implementation.Context.FrameGeneration.EXPAND.same1(methodVisitor, TypeDescription.ForLoadedType.of(Object.class), Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)));
        verify(methodVisitor).visitFrame(Opcodes.F_NEW, 1, new Object[]{Type.getInternalName(Object.class)}, 1, new Object[]{Type.getInternalName(Object.class)});
    }

    @Test
    public void testIgnoreSame1() {
        Implementation.Context.FrameGeneration.DISABLED.same1(methodVisitor, TypeDescription.ForLoadedType.of(Object.class), Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)));
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testGenerateAppend() {
        Implementation.Context.FrameGeneration.GENERATE.append(methodVisitor, Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)), Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)));
        verify(methodVisitor).visitFrame(Opcodes.F_APPEND, 1, new Object[]{Type.getInternalName(Object.class)}, 0, new Object[0]);
    }

    @Test
    public void testExpandAppend() {
        Implementation.Context.FrameGeneration.EXPAND.append(methodVisitor, Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)), Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)));
        verify(methodVisitor).visitFrame(Opcodes.F_NEW, 2, new Object[]{Type.getInternalName(Object.class), Type.getInternalName(Object.class)}, 0, new Object[0]);
    }

    @Test
    public void testIgnoreAppend() {
        Implementation.Context.FrameGeneration.DISABLED.append(methodVisitor, Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)), Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)));
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testGenerateChop() {
        Implementation.Context.FrameGeneration.GENERATE.chop(methodVisitor, 1, Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)));
        verify(methodVisitor).visitFrame(Opcodes.F_CHOP, 1, new Object[0], 0, new Object[0]);
    }

    @Test
    public void testExpandChop() {
        Implementation.Context.FrameGeneration.EXPAND.chop(methodVisitor, 1, Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)));
        verify(methodVisitor).visitFrame(Opcodes.F_NEW, 1, new Object[]{Type.getInternalName(Object.class)}, 0, new Object[0]);
    }

    @Test
    public void testIgnoreChop() {
        Implementation.Context.FrameGeneration.DISABLED.chop(methodVisitor, 1, Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)));
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testGenerateFull() {
        Implementation.Context.FrameGeneration.GENERATE.full(methodVisitor, Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)), Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)));
        verify(methodVisitor).visitFrame(Opcodes.F_FULL, 1, new Object[]{Type.getInternalName(Object.class)}, 1, new Object[]{Type.getInternalName(Object.class)});
    }

    @Test
    public void testExpandFull() {
        Implementation.Context.FrameGeneration.EXPAND.full(methodVisitor, Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)), Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)));
        verify(methodVisitor).visitFrame(Opcodes.F_NEW, 1, new Object[]{Type.getInternalName(Object.class)}, 1, new Object[]{Type.getInternalName(Object.class)});
    }

    @Test
    public void testIgnoreFull() {
        Implementation.Context.FrameGeneration.DISABLED.full(methodVisitor, Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)), Collections.singletonList(TypeDescription.ForLoadedType.of(Object.class)));
        verifyNoMoreInteractions(methodVisitor);
    }
}
