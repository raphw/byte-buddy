package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodGraphCompilerForDeclaredMethodsTest {

    @Test
    public void testCompilationInvisible() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        MethodDescription.InDefinedShape methodDescription = mock(MethodDescription.InDefinedShape.class);
        when(typeDescription.getDeclaredMethods()).thenReturn(new MethodList.Explicit<MethodDescription.InDefinedShape>(methodDescription));
        when(methodDescription.isVirtual()).thenReturn(true);
        when(methodDescription.isBridge()).thenReturn(false);
        when(methodDescription.isVisibleTo(typeDescription)).thenReturn(false);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.ForDeclaredMethods.INSTANCE.compile(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(0));
    }

    @Test
    public void testCompilationNonVirtual() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        MethodDescription.InDefinedShape methodDescription = mock(MethodDescription.InDefinedShape.class);
        when(typeDescription.getDeclaredMethods()).thenReturn(new MethodList.Explicit<MethodDescription.InDefinedShape>(methodDescription));
        when(methodDescription.isVirtual()).thenReturn(false);
        when(methodDescription.isBridge()).thenReturn(false);
        when(methodDescription.isVisibleTo(typeDescription)).thenReturn(true);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.ForDeclaredMethods.INSTANCE.compile(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(0));
    }

    @Test
    public void testCompilationNonBridge() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        MethodDescription.InDefinedShape methodDescription = mock(MethodDescription.InDefinedShape.class);
        when(typeDescription.getDeclaredMethods()).thenReturn(new MethodList.Explicit<MethodDescription.InDefinedShape>(methodDescription));
        when(methodDescription.isVirtual()).thenReturn(true);
        when(methodDescription.getModifiers()).thenReturn(Opcodes.ACC_BRIDGE);
        when(methodDescription.isVisibleTo(typeDescription)).thenReturn(true);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.ForDeclaredMethods.INSTANCE.compile(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(0));
    }

    @Test
    public void testCompilation() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        MethodDescription.InDefinedShape methodDescription = mock(MethodDescription.InDefinedShape.class);
        MethodDescription.SignatureToken token = mock(MethodDescription.SignatureToken.class);
        when(methodDescription.asSignatureToken()).thenReturn(token);
        when(typeDescription.getDeclaredMethods()).thenReturn(new MethodList.Explicit<MethodDescription.InDefinedShape>(methodDescription));
        when(methodDescription.isVirtual()).thenReturn(true);
        when(methodDescription.isBridge()).thenReturn(false);
        when(methodDescription.isVisibleTo(typeDescription)).thenReturn(true);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.ForDeclaredMethods.INSTANCE.compile(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        assertThat(methodGraph.listNodes().getOnly().getRepresentative(), is((MethodDescription) methodDescription));
    }
}
