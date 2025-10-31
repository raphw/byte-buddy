package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.module.ModuleDescription;
import net.bytebuddy.description.type.RecordComponentDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeVariableToken;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.objectweb.asm.Opcodes;

import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.definedMethod;
import static net.bytebuddy.matcher.ElementMatchers.erasure;
import static net.bytebuddy.matcher.ElementMatchers.isBridge;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isVirtual;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class MethodGraphCompilerDefaultTest {

    private static final String TYPE_VARIABLE_INTERFACE_BRIDGE = "net.bytebuddy.test.precompiled.v8.TypeVariableInterfaceBridge";

    private static final String RETURN_TYPE_INTERFACE_BRIDGE = "net.bytebuddy.test.precompiled.v8.ReturnTypeInterfaceBridge";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    public void testTrivialJavaHierarchy() throws Exception {
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) TypeDescription.ForLoadedType.of(Object.class));
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size()));
        assertThat(methodGraph.getSuperClassGraph().listNodes().size(), is(0));
        assertThat(methodGraph.getInterfaceGraph(mock(TypeDescription.class)).listNodes().size(), is(0));
        for (MethodDescription methodDescription : TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual())) {
            MethodGraph.Node node = methodGraph.locate(methodDescription.asSignatureToken());
            assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
            assertThat(node.getMethodTypes().size(), is(1));
            assertThat(node.getMethodTypes().contains(methodDescription.asTypeToken()), is(true));
            assertThat(node.getRepresentative(), is(methodDescription));
            assertThat(node.getVisibility(), is(methodDescription.getVisibility()));
            assertThat(methodGraph.listNodes().contains(node), is(true));
        }
    }

    @Test
    public void testTrivialJVMHierarchy() throws Exception {
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJVMHierarchy().compile((TypeDefinition) TypeDescription.ForLoadedType.of(Object.class));
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size()));
        assertThat(methodGraph.getSuperClassGraph().listNodes().size(), is(0));
        assertThat(methodGraph.getInterfaceGraph(mock(TypeDescription.class)).listNodes().size(), is(0));
        for (MethodDescription methodDescription : TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual())) {
            MethodGraph.Node node = methodGraph.locate(methodDescription.asSignatureToken());
            assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
            assertThat(node.getMethodTypes().size(), is(1));
            assertThat(node.getMethodTypes().contains(methodDescription.asTypeToken()), is(true));
            assertThat(node.getRepresentative(), is(methodDescription));
            assertThat(node.getVisibility(), is(methodDescription.getVisibility()));
            assertThat(methodGraph.listNodes().contains(node), is(true));
        }
    }

    @Test
    public void testSimpleClass() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(SimpleClass.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size()));
        assertThat(methodGraph.getSuperClassGraph().listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size()));
        assertThat(methodGraph.getInterfaceGraph(mock(TypeDescription.class)).listNodes().size(), is(0));
        for (MethodDescription methodDescription : TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual())) {
            MethodGraph.Node node = methodGraph.locate(methodDescription.asSignatureToken());
            assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
            assertThat(node.getMethodTypes().size(), is(1));
            assertThat(node.getMethodTypes().contains(methodDescription.asTypeToken()), is(true));
            assertThat(node.getRepresentative(), is(methodDescription));
            assertThat(node.getVisibility(), is(methodDescription.getVisibility()));
            assertThat(methodGraph.listNodes().contains(node), is(true));
        }
    }

    @Test
    public void testSimpleInterface() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(SimpleInterface.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(0));
    }

    @Test
    public void testClassInheritance() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(ClassBase.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription method = typeDescription.getDeclaredMethods().filter(isMethod()).getOnly();
        MethodGraph.Node node = methodGraph.locate(method.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(1));
        assertThat(node.getMethodTypes().contains(method.asTypeToken()), is(true));
        assertThat(node.getRepresentative(), is(method));
        assertThat(node.getVisibility(), is(method.getVisibility()));
        assertThat(methodGraph.listNodes().contains(node), is(true));
        MethodGraph.Node baseNode = methodGraph.getSuperClassGraph().locate(method.asSignatureToken());
        assertThat(node, not(baseNode));
        assertThat(typeDescription.getSuperClass().getDeclaredMethods().filter(ElementMatchers.is(baseNode.getRepresentative())).getOnly(),
                is(baseNode.getRepresentative()));
    }

    @Test
    public void testInterfaceImplementation() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(InterfaceBase.InnerClass.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription method = typeDescription.getInterfaces().getOnly().getDeclaredMethods().getOnly();
        MethodGraph.Node node = methodGraph.locate(method.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(1));
        assertThat(node.getMethodTypes().contains(method.asTypeToken()), is(true));
        assertThat(node.getRepresentative(), is(method));
        assertThat(node.getVisibility(), is(method.getVisibility()));
        assertThat(methodGraph.listNodes().contains(node), is(true));
    }

    @Test
    public void testInterfaceExtension() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(InterfaceBase.InnerInterface.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodDescription method = typeDescription.getInterfaces().getOnly().getDeclaredMethods().getOnly();
        MethodGraph.Node node = methodGraph.locate(method.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(1));
        assertThat(node.getMethodTypes().contains(method.asTypeToken()), is(true));
        assertThat(node.getRepresentative(), is(method));
        assertThat(node.getVisibility(), is(method.getVisibility()));
        assertThat(methodGraph.listNodes().contains(node), is(true));
    }

    @Test
    public void testInterfaceDuplicateInHierarchyImplementation() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(InterfaceBase.InterfaceDuplicate.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodDescription method = typeDescription.getInterfaces().filter(ElementMatchers.is(InterfaceBase.class)).getOnly().getDeclaredMethods().getOnly();
        MethodGraph.Node node = methodGraph.locate(method.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(1));
        assertThat(node.getMethodTypes().contains(method.asTypeToken()), is(true));
        assertThat(node.getRepresentative(), is(method));
        assertThat(node.getVisibility(), is(method.getVisibility()));
        assertThat(methodGraph.listNodes().contains(node), is(true));
    }

    @Test
    public void testClassAndInterfaceDominantInheritance() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(ClassAndInterfaceInheritance.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription method = typeDescription.getSuperClass().getDeclaredMethods().filter(isMethod()).getOnly();
        MethodGraph.Node node = methodGraph.locate(method.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(1));
        assertThat(node.getMethodTypes().contains(method.asTypeToken()), is(true));
        assertThat(node.getRepresentative(), is(method));
        assertThat(node.getVisibility(), is(method.getVisibility()));
        MethodGraph.Node baseNode = methodGraph.getInterfaceGraph(TypeDescription.ForLoadedType.of(InterfaceBase.class)).locate(method.asSignatureToken());
        assertThat(node, not(baseNode));
        assertThat(baseNode.getRepresentative(),
                is((MethodDescription) typeDescription.getInterfaces().getOnly().getDeclaredMethods().getOnly()));
    }

    @Test
    public void testMultipleAmbiguousClassInheritance() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(AmbiguousInterfaceBase.ClassTarget.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription first = typeDescription.getInterfaces().filter(erasure(InterfaceBase.class)).getOnly()
                .getDeclaredMethods().filter(isMethod()).getOnly();
        MethodDescription second = typeDescription.getInterfaces().filter(erasure(AmbiguousInterfaceBase.class)).getOnly()
                .getDeclaredMethods().filter(isMethod()).getOnly();
        MethodGraph.Node node = methodGraph.locate(first.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.AMBIGUOUS));
        assertThat(node.getMethodTypes().size(), is(1));
        assertThat(node.getMethodTypes().contains(first.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(second.asTypeToken()), is(true));
        assertThat(node.getRepresentative(), is(first));
        assertThat(node.getRepresentative(), not(second));
        assertThat(node.getVisibility(), is(first.getVisibility()));
        assertThat(node, is(methodGraph.locate(second.asSignatureToken())));
        MethodGraph.Node firstBaseNode = methodGraph.getInterfaceGraph(TypeDescription.ForLoadedType.of(InterfaceBase.class)).locate(first.asSignatureToken());
        assertThat(node, not(firstBaseNode));
        assertThat(firstBaseNode.getRepresentative(), is(first));
        MethodGraph.Node secondBaseNode = methodGraph.getInterfaceGraph(TypeDescription.ForLoadedType.of(InterfaceBase.class)).locate(second.asSignatureToken());
        assertThat(node, not(secondBaseNode));
        assertThat(secondBaseNode.getRepresentative(), is(first));
    }

    @Test
    public void testMultipleAmbiguousInterfaceInheritance() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(AmbiguousInterfaceBase.InterfaceTarget.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodDescription first = typeDescription.getInterfaces().filter(erasure(InterfaceBase.class)).getOnly()
                .getDeclaredMethods().filter(isMethod()).getOnly();
        MethodDescription second = typeDescription.getInterfaces().filter(erasure(AmbiguousInterfaceBase.class)).getOnly()
                .getDeclaredMethods().filter(isMethod()).getOnly();
        MethodGraph.Node node = methodGraph.locate(first.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.AMBIGUOUS));
        assertThat(node.getMethodTypes().size(), is(1));
        assertThat(node.getMethodTypes().contains(first.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(second.asTypeToken()), is(true));
        assertThat(node.getRepresentative(), is(first));
        assertThat(node.getRepresentative(), not(second));
        assertThat(node.getVisibility(), is(first.getVisibility()));
        assertThat(node, is(methodGraph.locate(second.asSignatureToken())));
        MethodGraph.Node firstBaseNode = methodGraph.getInterfaceGraph(TypeDescription.ForLoadedType.of(InterfaceBase.class)).locate(first.asSignatureToken());
        assertThat(node, not(firstBaseNode));
        assertThat(firstBaseNode.getRepresentative(), is(first));
        MethodGraph.Node secondBaseNode = methodGraph.getInterfaceGraph(TypeDescription.ForLoadedType.of(InterfaceBase.class)).locate(second.asSignatureToken());
        assertThat(node, not(secondBaseNode));
        assertThat(secondBaseNode.getRepresentative(), is(first));
    }

    @Test
    public void testDominantClassInheritance() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(AmbiguousInterfaceBase.DominantClassTarget.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription methodDescription = TypeDescription.ForLoadedType.of(AmbiguousInterfaceBase.DominantIntermediate.class)
                .getDeclaredMethods().getOnly();
        MethodGraph.Node node = methodGraph.locate(methodDescription.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(1));
        assertThat(node.getMethodTypes().contains(methodDescription.asTypeToken()), is(true));
        assertThat(node.getRepresentative(), is(methodDescription));
        assertThat(node.getVisibility(), is(methodDescription.getVisibility()));
    }

    @Test
    public void testDominantInterfaceInheritanceLeft() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(AmbiguousInterfaceBase.DominantInterfaceTargetLeft.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodDescription methodDescription = TypeDescription.ForLoadedType.of(AmbiguousInterfaceBase.DominantIntermediate.class)
                .getDeclaredMethods().getOnly();
        MethodGraph.Node node = methodGraph.locate(methodDescription.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(1));
        assertThat(node.getMethodTypes().contains(methodDescription.asTypeToken()), is(true));
        assertThat(node.getRepresentative(), is(methodDescription));
        assertThat(node.getVisibility(), is(methodDescription.getVisibility()));
    }

    @Test
    public void testDominantInterfaceInheritanceRight() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(AmbiguousInterfaceBase.DominantInterfaceTargetRight.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodDescription methodDescription = TypeDescription.ForLoadedType.of(AmbiguousInterfaceBase.DominantIntermediate.class)
                .getDeclaredMethods().getOnly();
        MethodGraph.Node node = methodGraph.locate(methodDescription.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(1));
        assertThat(node.getMethodTypes().contains(methodDescription.asTypeToken()), is(true));
        assertThat(node.getRepresentative(), is(methodDescription));
        assertThat(node.getVisibility(), is(methodDescription.getVisibility()));
    }

    @Test
    public void testNonDominantInterfaceInheritanceLeft() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(AmbiguousInterfaceBase.NonDominantTargetLeft.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodDescription methodDescription = TypeDescription.ForLoadedType.of(AmbiguousInterfaceBase.DominantIntermediate.class)
                .getDeclaredMethods().getOnly();
        MethodGraph.Node node = methodGraph.locate(methodDescription.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.AMBIGUOUS));
        assertThat(node.getMethodTypes().size(), is(1));
        assertThat(node.getMethodTypes().contains(methodDescription.asTypeToken()), is(true));
        assertThat(node.getRepresentative(), is(methodDescription));
        assertThat(node.getVisibility(), is(methodDescription.getVisibility()));
    }

    @Test
    public void testNonDominantInterfaceInheritanceRight() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(AmbiguousInterfaceBase.NonDominantTargetRight.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodDescription methodDescription = TypeDescription.ForLoadedType.of(AmbiguousInterfaceBase.DominantIntermediate.class)
                .getDeclaredMethods().getOnly();
        MethodGraph.Node node = methodGraph.locate(methodDescription.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.AMBIGUOUS));
        assertThat(node.getMethodTypes().size(), is(1));
        assertThat(node.getMethodTypes().contains(methodDescription.asTypeToken()), is(true));
        assertThat(node.getRepresentative(), is(methodDescription));
        assertThat(node.getVisibility(), is(methodDescription.getVisibility()));
    }

    @Test
    public void testGenericClassSingleEvolution() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(GenericClassBase.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription.SignatureToken token = typeDescription.getDeclaredMethods().filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asSignatureToken();
        MethodGraph.Node node = methodGraph.locate(token);
        MethodDescription.SignatureToken bridgeToken = typeDescription.getSuperClass().getDeclaredMethods().filter(isMethod()).getOnly().asDefined().asSignatureToken();
        assertThat(node, is(methodGraph.locate(bridgeToken)));
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(2));
        assertThat(node.getMethodTypes().contains(token.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(bridgeToken.asTypeToken()), is(true));
        assertThat(node.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    public void testGenericClassMultipleEvolution() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(GenericClassBase.Intermediate.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription.SignatureToken token = typeDescription.getDeclaredMethods().filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asSignatureToken();
        MethodGraph.Node node = methodGraph.locate(token);
        MethodDescription.SignatureToken firstBridgeToken = typeDescription.getSuperClass().getDeclaredMethods()
                .filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asDefined().asSignatureToken();
        MethodDescription.SignatureToken secondBridgeToken = typeDescription.getSuperClass().getSuperClass().getDeclaredMethods()
                .filter(isMethod()).getOnly().asDefined().asSignatureToken();
        assertThat(node, is(methodGraph.locate(firstBridgeToken)));
        assertThat(node, is(methodGraph.locate(secondBridgeToken)));
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(3));
        assertThat(node.getMethodTypes().contains(token.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(firstBridgeToken.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(secondBridgeToken.asTypeToken()), is(true));
        assertThat(node.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    public void testReturnTypeClassSingleEvolution() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(ReturnTypeClassBase.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription.SignatureToken token = typeDescription.getDeclaredMethods().filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asSignatureToken();
        MethodGraph.Node node = methodGraph.locate(token);
        MethodDescription.SignatureToken bridgeToken = typeDescription.getSuperClass().getDeclaredMethods().filter(isMethod()).getOnly().asSignatureToken();
        assertThat(node, is(methodGraph.locate(bridgeToken)));
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(2));
        assertThat(node.getMethodTypes().contains(token.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(bridgeToken.asTypeToken()), is(true));
        assertThat(node.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    public void testReturnTypeClassMultipleEvolution() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(ReturnTypeClassBase.Intermediate.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription.SignatureToken token = typeDescription.getDeclaredMethods().filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asSignatureToken();
        MethodGraph.Node node = methodGraph.locate(token);
        MethodDescription.SignatureToken firstBridgeToken = typeDescription.getSuperClass().getDeclaredMethods()
                .filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asDefined().asSignatureToken();
        MethodDescription.SignatureToken secondBridgeToken = typeDescription.getSuperClass().getSuperClass().getDeclaredMethods()
                .filter(isMethod()).getOnly().asDefined().asSignatureToken();
        assertThat(node, is(methodGraph.locate(firstBridgeToken)));
        assertThat(node, is(methodGraph.locate(secondBridgeToken)));
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(3));
        assertThat(node.getMethodTypes().contains(token.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(firstBridgeToken.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(secondBridgeToken.asTypeToken()), is(true));
        assertThat(node.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    public void testGenericReturnTypeClassSingleEvolution() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(GenericReturnClassBase.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription.SignatureToken token = typeDescription.getDeclaredMethods().filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asSignatureToken();
        MethodGraph.Node node = methodGraph.locate(token);
        MethodDescription.SignatureToken bridgeToken = typeDescription.getSuperClass().getDeclaredMethods().filter(isMethod()).getOnly().asSignatureToken();
        assertThat(node, is(methodGraph.locate(bridgeToken)));
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(2));
        assertThat(node.getMethodTypes().contains(token.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(bridgeToken.asTypeToken()), is(true));
        assertThat(node.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    public void testGenericReturnTypeClassMultipleEvolution() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(GenericReturnClassBase.Intermediate.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription.SignatureToken token = typeDescription.getDeclaredMethods().filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asSignatureToken();
        MethodGraph.Node node = methodGraph.locate(token);
        MethodDescription.SignatureToken firstBridgeToken = typeDescription.getSuperClass().getDeclaredMethods()
                .filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asDefined().asSignatureToken();
        MethodDescription.SignatureToken secondBridgeToken = typeDescription.getSuperClass().getSuperClass().getDeclaredMethods()
                .filter(isMethod()).getOnly().asDefined().asSignatureToken();
        assertThat(node, is(methodGraph.locate(firstBridgeToken)));
        assertThat(node, is(methodGraph.locate(secondBridgeToken)));
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(3));
        assertThat(node.getMethodTypes().contains(token.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(firstBridgeToken.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(secondBridgeToken.asTypeToken()), is(true));
        assertThat(node.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    public void testGenericInterfaceSingleEvolution() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(GenericInterfaceBase.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodDescription.SignatureToken token = typeDescription.getDeclaredMethods().filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asSignatureToken();
        MethodGraph.Node node = methodGraph.locate(token);
        MethodDescription.SignatureToken bridgeToken = typeDescription.getInterfaces().getOnly()
                .getDeclaredMethods().filter(isMethod()).getOnly().asDefined().asSignatureToken();
        assertThat(node, is(methodGraph.locate(bridgeToken)));
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(2));
        assertThat(node.getMethodTypes().contains(token.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(bridgeToken.asTypeToken()), is(true));
        assertThat(node.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    public void testGenericInterfaceMultipleEvolution() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(GenericInterfaceBase.Intermediate.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodDescription.SignatureToken token = typeDescription.getDeclaredMethods().filter(ElementMatchers.not(isBridge())).getOnly().asSignatureToken();
        MethodGraph.Node node = methodGraph.locate(token);
        MethodDescription.SignatureToken firstBridgeToken = typeDescription.getInterfaces().getOnly()
                .getDeclaredMethods().filter(ElementMatchers.not(isBridge())).getOnly().asDefined().asSignatureToken();
        MethodDescription.SignatureToken secondBridgeToken = typeDescription.getInterfaces().getOnly().getInterfaces().getOnly()
                .getDeclaredMethods().getOnly().asDefined().asSignatureToken();
        assertThat(node, is(methodGraph.locate(firstBridgeToken)));
        assertThat(node, is(methodGraph.locate(secondBridgeToken)));
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(3));
        assertThat(node.getMethodTypes().contains(token.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(firstBridgeToken.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(secondBridgeToken.asTypeToken()), is(true));
        assertThat(node.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    public void testReturnTypeInterfaceSingleEvolution() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(ReturnTypeInterfaceBase.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodDescription.SignatureToken token = typeDescription.getDeclaredMethods().filter(ElementMatchers.not(isBridge())).getOnly().asSignatureToken();
        MethodGraph.Node node = methodGraph.locate(token);
        MethodDescription.SignatureToken bridgeToken = typeDescription.getInterfaces().getOnly().getDeclaredMethods().getOnly().asSignatureToken();
        assertThat(node, is(methodGraph.locate(bridgeToken)));
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(2));
        assertThat(node.getMethodTypes().contains(token.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(bridgeToken.asTypeToken()), is(true));
        assertThat(node.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    public void testReturnTypeInterfaceMultipleEvolution() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(ReturnTypeInterfaceBase.Intermediate.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodDescription.SignatureToken token = typeDescription.getDeclaredMethods().filter(ElementMatchers.not(isBridge())).getOnly().asSignatureToken();
        MethodGraph.Node node = methodGraph.locate(token);
        MethodDescription.SignatureToken firstBridgeToken = typeDescription.getInterfaces().getOnly()
                .getDeclaredMethods().filter(ElementMatchers.not(isBridge())).getOnly().asSignatureToken();
        MethodDescription.SignatureToken secondBridgeToken = typeDescription.getInterfaces().getOnly().getInterfaces().getOnly()
                .getDeclaredMethods().getOnly().asSignatureToken();
        assertThat(node, is(methodGraph.locate(firstBridgeToken)));
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(3));
        assertThat(node.getMethodTypes().contains(token.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(firstBridgeToken.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(secondBridgeToken.asTypeToken()), is(true));
        assertThat(node.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    public void testGenericWithReturnTypeClassSingleEvolution() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(GenericWithReturnTypeClassBase.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription.SignatureToken token = typeDescription.getDeclaredMethods().filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asSignatureToken();
        MethodGraph.Node node = methodGraph.locate(token);
        MethodDescription.SignatureToken bridgeToken = typeDescription.getSuperClass().getDeclaredMethods().filter(isMethod()).getOnly().asDefined().asSignatureToken();
        assertThat(node, is(methodGraph.locate(bridgeToken)));
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(2));
        assertThat(node.getMethodTypes().contains(token.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(bridgeToken.asTypeToken()), is(true));
        assertThat(node.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    public void testGenericWithReturnTypeClassMultipleEvolution() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(GenericWithReturnTypeClassBase.Intermediate.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription.SignatureToken token = typeDescription.getDeclaredMethods().filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asSignatureToken();
        MethodGraph.Node node = methodGraph.locate(token);
        MethodDescription.SignatureToken firstBridgeToken = typeDescription.getSuperClass().getDeclaredMethods()
                .filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asDefined().asSignatureToken();
        MethodDescription.SignatureToken secondBridgeToken = typeDescription.getSuperClass().getSuperClass().getDeclaredMethods()
                .filter(isMethod()).getOnly().asDefined().asSignatureToken();
        assertThat(node, is(methodGraph.locate(firstBridgeToken)));
        assertThat(node, is(methodGraph.locate(secondBridgeToken)));
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(3));
        assertThat(node.getMethodTypes().contains(token.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(firstBridgeToken.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(secondBridgeToken.asTypeToken()), is(true));
        assertThat(node.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    public void testGenericWithReturnTypeInterfaceSingleEvolution() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(GenericWithReturnTypeInterfaceBase.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodDescription.SignatureToken token = typeDescription.getDeclaredMethods().filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asSignatureToken();
        MethodGraph.Node node = methodGraph.locate(token);
        MethodDescription.SignatureToken bridgeToken = typeDescription.getInterfaces().getOnly()
                .getDeclaredMethods().filter(isMethod()).getOnly().asDefined().asSignatureToken();
        assertThat(node, is(methodGraph.locate(bridgeToken)));
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(2));
        assertThat(node.getMethodTypes().contains(token.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(bridgeToken.asTypeToken()), is(true));
        assertThat(node.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    public void testGenericWithReturnTypeInterfaceMultipleEvolution() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(GenericWithReturnTypeInterfaceBase.Intermediate.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodDescription.SignatureToken token = typeDescription.getDeclaredMethods().filter(ElementMatchers.not(isBridge())).getOnly().asSignatureToken();
        MethodGraph.Node node = methodGraph.locate(token);
        MethodDescription.SignatureToken firstBridgeToken = typeDescription.getInterfaces().getOnly()
                .getDeclaredMethods().filter(ElementMatchers.not(isBridge())).getOnly().asDefined().asSignatureToken();
        MethodDescription.SignatureToken secondBridgeToken = typeDescription.getInterfaces().getOnly().getInterfaces().getOnly()
                .getDeclaredMethods().getOnly().asDefined().asSignatureToken();
        assertThat(node, is(methodGraph.locate(firstBridgeToken)));
        assertThat(node, is(methodGraph.locate(secondBridgeToken)));
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(3));
        assertThat(node.getMethodTypes().contains(token.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(firstBridgeToken.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(secondBridgeToken.asTypeToken()), is(true));
        assertThat(node.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    public void testGenericNonOverriddenClassExtension() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(GenericNonOverriddenClassBase.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription methodDescription = typeDescription.getSuperClass().getDeclaredMethods().filter(isMethod()).getOnly();
        MethodGraph.Node node = methodGraph.locate(methodDescription.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getRepresentative(), is(methodDescription));
        assertThat(node.getMethodTypes().size(), is(2));
        assertThat(node.getMethodTypes().contains(methodDescription.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(methodDescription.asDefined().asTypeToken()), is(true));
        assertThat(node, hasPrototype(methodGraph.getSuperClassGraph().locate(methodDescription.asSignatureToken())));
        assertThat(node.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    public void testGenericNonOverriddenInterfaceExtension() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(GenericNonOverriddenInterfaceBase.InnerClass.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription methodDescription = typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(isMethod()).getOnly();
        MethodGraph.Node node = methodGraph.locate(methodDescription.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getRepresentative(), is(methodDescription));
        assertThat(node.getMethodTypes().size(), is(2));
        assertThat(node.getMethodTypes().contains(methodDescription.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(methodDescription.asDefined().asTypeToken()), is(true));
        assertThat(node, hasPrototype(methodGraph.getInterfaceGraph(TypeDescription.ForLoadedType.of(GenericNonOverriddenInterfaceBase.class))
                .locate(methodDescription.asSignatureToken())));
        assertThat(node.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    public void testGenericNonOverriddenInterfaceImplementation() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(GenericNonOverriddenInterfaceBase.InnerInterface.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodDescription methodDescription = typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(isMethod()).getOnly();
        MethodGraph.Node node = methodGraph.locate(methodDescription.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getRepresentative(), is(methodDescription));
        assertThat(node.getMethodTypes().size(), is(2));
        assertThat(node.getMethodTypes().contains(methodDescription.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(methodDescription.asDefined().asTypeToken()), is(true));
        assertThat(node, hasPrototype(methodGraph.getInterfaceGraph(TypeDescription.ForLoadedType.of(GenericNonOverriddenInterfaceBase.class))
                .locate(methodDescription.asSignatureToken())));
        assertThat(node.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testTypeVariableInterfaceBridge() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(Class.forName(TYPE_VARIABLE_INTERFACE_BRIDGE));
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodDescription methodDescription = typeDescription.getDeclaredMethods().filter(takesArguments(String.class)).getOnly();
        MethodGraph.Node node = methodGraph.locate(methodDescription.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getRepresentative(), is(methodDescription));
        assertThat(node.getMethodTypes().size(), is(2));
        assertThat(node.getMethodTypes().contains(methodDescription.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(typeDescription.getDeclaredMethods().filter(takesArguments(Object.class)).getOnly().asTypeToken()), is(true));
        assertThat(node.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testReturnTypeInterfaceBridge() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(Class.forName(RETURN_TYPE_INTERFACE_BRIDGE));
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodDescription methodDescription = typeDescription.getDeclaredMethods().filter(returns(String.class)).getOnly();
        MethodGraph.Node node = methodGraph.locate(methodDescription.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getRepresentative(), is(methodDescription));
        assertThat(node.getMethodTypes().size(), is(2));
        assertThat(node.getMethodTypes().contains(methodDescription.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(typeDescription.getDeclaredMethods().filter(returns(Object.class)).getOnly().asTypeToken()), is(true));
        assertThat(node.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    public void testDuplicateNameClass() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(DuplicateNameClass.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 2));
        MethodDescription objectMethod = typeDescription.getDeclaredMethods().filter(takesArguments(Object.class)).getOnly();
        MethodGraph.Node objectNode = methodGraph.locate(objectMethod.asSignatureToken());
        assertThat(objectNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(objectNode.getRepresentative(), is(objectMethod));
        assertThat(objectNode.getMethodTypes().size(), is(1));
        assertThat(objectNode.getMethodTypes().contains(objectMethod.asTypeToken()), is(true));
        assertThat(objectNode.getVisibility(), is(objectMethod.getVisibility()));
        MethodDescription voidMethod = typeDescription.getDeclaredMethods().filter(takesArguments(Integer.class)).getOnly();
        MethodGraph.Node voidNode = methodGraph.locate(voidMethod.asSignatureToken());
        assertThat(voidNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(voidNode.getRepresentative(), is(voidMethod));
        assertThat(voidNode.getMethodTypes().size(), is(1));
        assertThat(voidNode.getMethodTypes().contains(voidMethod.asTypeToken()), is(true));
        assertThat(voidNode.getVisibility(), is(voidMethod.getVisibility()));
    }

    @Test
    public void testDuplicateNameClassExtension() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(DuplicateNameClass.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 3));
        MethodDescription objectMethod = typeDescription.getSuperClass().getDeclaredMethods().filter(takesArguments(Object.class)).getOnly();
        MethodGraph.Node objectNode = methodGraph.locate(objectMethod.asSignatureToken());
        assertThat(objectNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(objectNode.getRepresentative(), is(objectMethod));
        assertThat(objectNode.getMethodTypes().size(), is(1));
        assertThat(objectNode.getMethodTypes().contains(objectMethod.asTypeToken()), is(true));
        assertThat(objectNode.getVisibility(), is(objectMethod.getVisibility()));
        MethodDescription integerMethod = typeDescription.getSuperClass().getDeclaredMethods().filter(takesArguments(Integer.class)).getOnly();
        MethodGraph.Node integerNode = methodGraph.locate(integerMethod.asSignatureToken());
        assertThat(integerNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(integerNode.getRepresentative(), is(integerMethod));
        assertThat(integerNode.getMethodTypes().size(), is(1));
        assertThat(integerNode.getMethodTypes().contains(integerMethod.asTypeToken()), is(true));
        assertThat(integerNode.getVisibility(), is(integerMethod.getVisibility()));
        MethodDescription voidMethod = typeDescription.getDeclaredMethods().filter(takesArguments(Void.class)).getOnly();
        MethodGraph.Node voidNode = methodGraph.locate(voidMethod.asSignatureToken());
        assertThat(voidNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(voidNode.getRepresentative(), is(voidMethod));
        assertThat(voidNode.getMethodTypes().size(), is(1));
        assertThat(voidNode.getMethodTypes().contains(voidMethod.asTypeToken()), is(true));
        assertThat(voidNode.getVisibility(), is(voidMethod.getVisibility()));
    }

    @Test
    public void testDuplicateNameInterface() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(DuplicateNameInterface.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(2));
        MethodDescription objectMethod = typeDescription.getDeclaredMethods().filter(takesArguments(Object.class)).getOnly();
        MethodGraph.Node objectNode = methodGraph.locate(objectMethod.asSignatureToken());
        assertThat(objectNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(objectNode.getRepresentative(), is(objectMethod));
        assertThat(objectNode.getMethodTypes().size(), is(1));
        assertThat(objectNode.getMethodTypes().contains(objectMethod.asTypeToken()), is(true));
        assertThat(objectNode.getVisibility(), is(objectMethod.getVisibility()));
        MethodDescription voidMethod = typeDescription.getDeclaredMethods().filter(takesArguments(Integer.class)).getOnly();
        MethodGraph.Node voidNode = methodGraph.locate(voidMethod.asSignatureToken());
        assertThat(voidNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(voidNode.getRepresentative(), is(voidMethod));
        assertThat(voidNode.getMethodTypes().size(), is(1));
        assertThat(voidNode.getMethodTypes().contains(voidMethod.asTypeToken()), is(true));
        assertThat(voidNode.getVisibility(), is(voidMethod.getVisibility()));
    }

    @Test
    public void testDuplicateNameInterfaceImplementation() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(DuplicateNameInterface.InnerClass.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 3));
        MethodDescription objectMethod = typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(takesArguments(Object.class)).getOnly();
        MethodGraph.Node objectNode = methodGraph.locate(objectMethod.asSignatureToken());
        assertThat(objectNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(objectNode.getRepresentative(), is(objectMethod));
        assertThat(objectNode.getMethodTypes().size(), is(1));
        assertThat(objectNode.getMethodTypes().contains(objectMethod.asTypeToken()), is(true));
        assertThat(objectMethod.getVisibility(), is(objectMethod.getVisibility()));
        MethodDescription integerMethod = typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(takesArguments(Integer.class)).getOnly();
        MethodGraph.Node integerNode = methodGraph.locate(integerMethod.asSignatureToken());
        assertThat(integerNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(integerNode.getRepresentative(), is(integerMethod));
        assertThat(integerNode.getMethodTypes().size(), is(1));
        assertThat(integerNode.getMethodTypes().contains(integerMethod.asTypeToken()), is(true));
        assertThat(integerNode.getVisibility(), is(integerMethod.getVisibility()));
        MethodDescription voidMethod = typeDescription.getDeclaredMethods().filter(takesArguments(Void.class)).getOnly();
        MethodGraph.Node voidNode = methodGraph.locate(voidMethod.asSignatureToken());
        assertThat(voidNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(voidNode.getRepresentative(), is(voidMethod));
        assertThat(voidNode.getMethodTypes().size(), is(1));
        assertThat(voidNode.getMethodTypes().contains(voidMethod.asTypeToken()), is(true));
        assertThat(voidNode.getVisibility(), is(voidMethod.getVisibility()));
    }

    @Test
    public void testDuplicateNameInterfaceExtension() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(DuplicateNameInterface.InnerInterface.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(3));
        MethodDescription objectMethod = typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(takesArguments(Object.class)).getOnly();
        MethodGraph.Node objectNode = methodGraph.locate(objectMethod.asSignatureToken());
        assertThat(objectNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(objectNode.getRepresentative(), is(objectMethod));
        assertThat(objectNode.getMethodTypes().size(), is(1));
        assertThat(objectNode.getMethodTypes().contains(objectMethod.asTypeToken()), is(true));
        assertThat(objectNode.getVisibility(), is(objectMethod.getVisibility()));
        MethodDescription integerMethod = typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(takesArguments(Integer.class)).getOnly();
        MethodGraph.Node integerNode = methodGraph.locate(integerMethod.asSignatureToken());
        assertThat(integerNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(integerNode.getRepresentative(), is(integerMethod));
        assertThat(integerNode.getMethodTypes().size(), is(1));
        assertThat(integerNode.getMethodTypes().contains(integerMethod.asTypeToken()), is(true));
        assertThat(integerMethod.getVisibility(), is(integerNode.getVisibility()));
        MethodDescription voidMethod = typeDescription.getDeclaredMethods().filter(takesArguments(Void.class)).getOnly();
        MethodGraph.Node voidNode = methodGraph.locate(voidMethod.asSignatureToken());
        assertThat(voidNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(voidNode.getRepresentative(), is(voidMethod));
        assertThat(voidNode.getMethodTypes().size(), is(1));
        assertThat(voidNode.getMethodTypes().contains(voidMethod.asTypeToken()), is(true));
        assertThat(voidNode.getVisibility(), is(voidMethod.getVisibility()));
    }

    @Test
    public void testDuplicateNameGenericClass() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(DuplicateNameGenericClass.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 2));
        MethodDescription objectMethod = typeDescription.getDeclaredMethods().filter(takesArguments(Object.class)).getOnly();
        MethodGraph.Node objectNode = methodGraph.locate(objectMethod.asSignatureToken());
        assertThat(objectNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(objectNode.getRepresentative(), is(objectMethod));
        assertThat(objectNode.getMethodTypes().size(), is(1));
        assertThat(objectNode.getMethodTypes().contains(objectMethod.asTypeToken()), is(true));
        assertThat(objectNode.getVisibility(), is(objectMethod.getVisibility()));
        MethodDescription voidMethod = typeDescription.getDeclaredMethods().filter(takesArguments(Integer.class)).getOnly();
        MethodGraph.Node voidNode = methodGraph.locate(voidMethod.asSignatureToken());
        assertThat(voidNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(voidNode.getRepresentative(), is(voidMethod));
        assertThat(voidNode.getMethodTypes().size(), is(1));
        assertThat(voidNode.getMethodTypes().contains(voidMethod.asTypeToken()), is(true));
        assertThat(voidNode.getVisibility(), is(voidMethod.getVisibility()));
    }

    @Test
    public void testDuplicateNameGenericClassExtension() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(DuplicateNameGenericClass.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 3));
        MethodDescription objectMethod = typeDescription.getSuperClass().getDeclaredMethods().filter(takesArguments(String.class)).getOnly();
        MethodGraph.Node objectNode = methodGraph.locate(objectMethod.asSignatureToken());
        assertThat(objectNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(objectNode.getRepresentative(), is(objectMethod));
        assertThat(objectNode.getMethodTypes().size(), is(2));
        assertThat(objectNode.getMethodTypes().contains(objectMethod.asTypeToken()), is(true));
        assertThat(objectNode.getMethodTypes().contains(objectMethod.asDefined().asTypeToken()), is(true));
        assertThat(objectNode.getVisibility(), is(objectMethod.getVisibility()));
        MethodDescription integerMethod = typeDescription.getSuperClass().getDeclaredMethods().filter(takesArguments(Integer.class)).getOnly();
        MethodGraph.Node integerNode = methodGraph.locate(integerMethod.asSignatureToken());
        assertThat(integerNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(integerNode.getRepresentative(), is(integerMethod));
        assertThat(integerNode.getMethodTypes().size(), is(1));
        assertThat(integerNode.getMethodTypes().contains(integerMethod.asTypeToken()), is(true));
        MethodDescription voidMethod = typeDescription.getDeclaredMethods().filter(takesArguments(Void.class)).getOnly();
        MethodGraph.Node voidNode = methodGraph.locate(voidMethod.asSignatureToken());
        assertThat(voidNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(voidNode.getRepresentative(), is(voidMethod));
        assertThat(voidNode.getMethodTypes().size(), is(1));
        assertThat(voidNode.getMethodTypes().contains(voidMethod.asTypeToken()), is(true));
        assertThat(voidNode.getVisibility(), is(voidMethod.getVisibility()));
    }

    @Test
    public void testDuplicateNameGenericInterface() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(DuplicateNameGenericInterface.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(2));
        MethodDescription objectMethod = typeDescription.getDeclaredMethods().filter(takesArguments(Object.class)).getOnly();
        MethodGraph.Node objectNode = methodGraph.locate(objectMethod.asSignatureToken());
        assertThat(objectNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(objectNode.getRepresentative(), is(objectMethod));
        assertThat(objectNode.getMethodTypes().size(), is(1));
        assertThat(objectNode.getMethodTypes().contains(objectMethod.asTypeToken()), is(true));
        assertThat(objectNode.getVisibility(), is(objectMethod.getVisibility()));
        MethodDescription voidMethod = typeDescription.getDeclaredMethods().filter(takesArguments(Integer.class)).getOnly();
        MethodGraph.Node voidNode = methodGraph.locate(voidMethod.asSignatureToken());
        assertThat(voidNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(voidNode.getRepresentative(), is(voidMethod));
        assertThat(voidNode.getMethodTypes().size(), is(1));
        assertThat(voidNode.getMethodTypes().contains(voidMethod.asTypeToken()), is(true));
        assertThat(voidNode.getVisibility(), is(voidMethod.getVisibility()));
    }

    @Test
    public void testDuplicateNameGenericInterfaceImplementation() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(DuplicateNameGenericInterface.InnerClass.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 3));
        MethodDescription objectMethod = typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(takesArguments(String.class)).getOnly();
        MethodGraph.Node objectNode = methodGraph.locate(objectMethod.asSignatureToken());
        assertThat(objectNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(objectNode.getRepresentative(), is(objectMethod));
        assertThat(objectNode.getMethodTypes().size(), is(2));
        assertThat(objectNode.getMethodTypes().contains(objectMethod.asTypeToken()), is(true));
        assertThat(objectNode.getMethodTypes().contains(objectMethod.asDefined().asTypeToken()), is(true));
        assertThat(objectNode.getVisibility(), is(objectMethod.getVisibility()));
        MethodDescription integerMethod = typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(takesArguments(Integer.class)).getOnly();
        MethodGraph.Node integerNode = methodGraph.locate(integerMethod.asSignatureToken());
        assertThat(integerNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(integerNode.getRepresentative(), is(integerMethod));
        assertThat(integerNode.getMethodTypes().size(), is(1));
        assertThat(integerNode.getMethodTypes().contains(integerMethod.asTypeToken()), is(true));
        assertThat(integerNode.getMethodTypes().contains(integerMethod.asDefined().asTypeToken()), is(true));
        assertThat(integerNode.getVisibility(), is(integerMethod.getVisibility()));
        MethodDescription voidMethod = typeDescription.getDeclaredMethods().filter(takesArguments(Void.class)).getOnly();
        MethodGraph.Node voidNode = methodGraph.locate(voidMethod.asSignatureToken());
        assertThat(voidNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(voidNode.getRepresentative(), is(voidMethod));
        assertThat(voidNode.getMethodTypes().size(), is(1));
        assertThat(voidNode.getMethodTypes().contains(voidMethod.asTypeToken()), is(true));
        assertThat(voidNode.getVisibility(), is(voidMethod.getVisibility()));
    }

    @Test
    public void testDuplicateNameGenericInterfaceExtension() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(DuplicateNameGenericInterface.InnerInterface.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(3));
        MethodDescription objectMethod = typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(takesArguments(String.class)).getOnly();
        MethodGraph.Node objectNode = methodGraph.locate(objectMethod.asSignatureToken());
        assertThat(objectNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(objectNode.getRepresentative(), is(objectMethod));
        assertThat(objectNode.getMethodTypes().size(), is(2));
        assertThat(objectNode.getMethodTypes().contains(objectMethod.asTypeToken()), is(true));
        assertThat(objectNode.getMethodTypes().contains(objectMethod.asDefined().asTypeToken()), is(true));
        assertThat(objectNode.getVisibility(), is(objectMethod.getVisibility()));
        MethodDescription integerMethod = typeDescription.getInterfaces().getOnly().getDeclaredMethods().filter(takesArguments(Integer.class)).getOnly();
        MethodGraph.Node integerNode = methodGraph.locate(integerMethod.asSignatureToken());
        assertThat(integerNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(integerNode.getRepresentative(), is(integerMethod));
        assertThat(integerNode.getMethodTypes().size(), is(1));
        assertThat(integerNode.getMethodTypes().contains(integerMethod.asTypeToken()), is(true));
        assertThat(integerNode.getMethodTypes().contains(integerMethod.asDefined().asTypeToken()), is(true));
        assertThat(integerNode.getVisibility(), is(integerMethod.getVisibility()));
        MethodDescription voidMethod = typeDescription.getDeclaredMethods().filter(takesArguments(Void.class)).getOnly();
        MethodGraph.Node voidNode = methodGraph.locate(voidMethod.asSignatureToken());
        assertThat(voidNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(voidNode.getRepresentative(), is(voidMethod));
        assertThat(voidNode.getMethodTypes().size(), is(1));
        assertThat(voidNode.getMethodTypes().contains(voidMethod.asTypeToken()), is(true));
        assertThat(voidNode.getVisibility(), is(voidMethod.getVisibility()));
    }

    @Test
    public void testVisibilityBridge() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(VisibilityBridgeTarget.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription methodDescription = typeDescription.getSuperClass().getDeclaredMethods().filter(isMethod()).getOnly();
        MethodGraph.Node node = methodGraph.locate(methodDescription.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.VISIBLE));
        assertThat(node.getMethodTypes().size(), is(1));
        assertThat(node.getMethodTypes().contains(methodDescription.asTypeToken()), is(true));
        assertThat(node.getRepresentative(), is(methodDescription));
        assertThat(node.getVisibility(), is(methodDescription.getVisibility()));
    }

    @Test
    public void testGenericVisibilityBridge() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(GenericVisibilityBridgeTarget.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription methodDescription = typeDescription.getSuperClass()
                .getDeclaredMethods().filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly();
        MethodDescription.SignatureToken bridgeToken = typeDescription.getSuperClass().getSuperClass()
                .getDeclaredMethods().filter(isMethod()).getOnly().asSignatureToken();
        MethodGraph.Node node = methodGraph.locate(methodDescription.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.VISIBLE));
        assertThat(node, is(methodGraph.locate(bridgeToken)));
        assertThat(node.getMethodTypes().size(), is(2));
        assertThat(node.getMethodTypes().contains(methodDescription.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(methodDescription.asDefined().asTypeToken()), is(true));
        assertThat(node.getRepresentative(), is(methodDescription));
        assertThat(node.getVisibility(), is(methodDescription.getVisibility()));
    }

    @Test
    public void testMethodClassConvergence() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(MethodClassConvergence.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription methodDescription = typeDescription.getDeclaredMethods().filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly();
        MethodDescription genericMethod = typeDescription.getSuperClass().getDeclaredMethods()
                .filter(isMethod().and(definedMethod(takesArguments(Object.class)))).getOnly();
        MethodDescription nonGenericMethod = typeDescription.getSuperClass().getDeclaredMethods()
                .filter(isMethod().and(definedMethod(takesArguments(Void.class)))).getOnly();
        MethodGraph.Node node = methodGraph.locate(methodDescription.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node, is(methodGraph.locate(genericMethod.asDefined().asSignatureToken())));
        assertThat(node, is(methodGraph.locate(nonGenericMethod.asDefined().asSignatureToken())));
        assertThat(node.getMethodTypes().size(), is(2));
        assertThat(node.getMethodTypes().contains(methodDescription.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(methodDescription.asDefined().asTypeToken()), is(true));
        assertThat(node.getRepresentative(), is(methodDescription));
        assertThat(node.getVisibility(), is(methodDescription.getVisibility()));
        MethodGraph superGraph = methodGraph.getSuperClassGraph();
        MethodGraph.Node superNode = superGraph.locate(methodDescription.asSignatureToken());
        assertThat(superNode.getSort(), is(MethodGraph.Node.Sort.AMBIGUOUS));
        assertThat(superNode.getMethodTypes().size(), is(2));
        assertThat(superNode.getMethodTypes().contains(methodDescription.asTypeToken()), is(true));
        assertThat(superNode.getMethodTypes().contains(methodDescription.asDefined().asTypeToken()), is(true));
        assertThat(superNode.getRepresentative(), is(nonGenericMethod));
        assertThat(superNode.getRepresentative(), is(genericMethod));
        assertThat(superNode.getVisibility(), is(methodDescription.getVisibility()));
    }

    @Test
    public void testMethodInterfaceConvergence() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(MethodInterfaceConvergenceTarget.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodDescription genericMethod = typeDescription.getInterfaces().filter(erasure(MethodInterfaceConvergenceFirstBase.class)).getOnly()
                .getDeclaredMethods().filter(isMethod()).getOnly();
        MethodDescription nonGenericMethod = typeDescription.getInterfaces().filter(erasure(MethodInterfaceConvergenceSecondBase.class)).getOnly()
                .getDeclaredMethods().filter(isMethod()).getOnly();
        assertThat(methodGraph.getSuperClassGraph().locate(genericMethod.asSignatureToken()).getSort(), is(MethodGraph.Node.Sort.UNRESOLVED));
        assertThat(methodGraph.getSuperClassGraph().locate(nonGenericMethod.asSignatureToken()).getSort(), is(MethodGraph.Node.Sort.UNRESOLVED));
        MethodGraph.Node node = methodGraph.locate(genericMethod.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.AMBIGUOUS));
        assertThat(node, is(methodGraph.locate(genericMethod.asDefined().asSignatureToken())));
        assertThat(node, is(methodGraph.locate(nonGenericMethod.asDefined().asSignatureToken())));
        assertThat(node.getMethodTypes().size(), is(2));
        assertThat(node.getMethodTypes().contains(genericMethod.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(genericMethod.asDefined().asTypeToken()), is(true));
        assertThat(node.getRepresentative(), is(genericMethod));
        assertThat(node.getRepresentative(), not(nonGenericMethod));
        assertThat(node.getVisibility(), is(genericMethod.getVisibility()));
    }

    @Test
    public void testMethodConvergenceVisibilityTarget() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(MethodConvergenceVisibilityBridgeTarget.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription genericMethod = typeDescription.getSuperClass().getSuperClass()
                .getDeclaredMethods().filter(isMethod().and(definedMethod(takesArguments(Object.class)))).getOnly();
        MethodDescription nonGenericMethod = typeDescription.getSuperClass().getSuperClass()
                .getDeclaredMethods().filter(isMethod().and(definedMethod(takesArguments(Void.class)))).getOnly();
        MethodGraph.Node node = methodGraph.locate(genericMethod.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.VISIBLE));
        assertThat(node, is(methodGraph.locate(nonGenericMethod.asSignatureToken())));
        assertThat(node.getMethodTypes().size(), is(2));
        assertThat(node.getMethodTypes().contains(genericMethod.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(genericMethod.asDefined().asTypeToken()), is(true));
        assertThat(node.getRepresentative(),
                is((MethodDescription) typeDescription.getSuperClass().getDeclaredMethods().filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly()));
        assertThat(node.getVisibility(), is(genericMethod.getVisibility()));
    }

    @Test
    public void testDiamondInheritanceClass() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(GenericDiamondClassBase.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription diamondOverride = typeDescription.getInterfaces().getOnly().getDeclaredMethods().getOnly();
        MethodDescription explicitOverride = typeDescription.getSuperClass().getDeclaredMethods().filter(isVirtual()).getOnly();
        MethodGraph.Node node = methodGraph.locate(diamondOverride.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(methodGraph.locate(explicitOverride.asDefined().asSignatureToken()), is(node));
        assertThat(node.getMethodTypes().size(), is(2));
        assertThat(node.getMethodTypes().contains(diamondOverride.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(explicitOverride.asDefined().asTypeToken()), is(true));
        assertThat(node.getVisibility(), is(explicitOverride.getVisibility()));
    }

    @Test
    public void testDiamondInheritanceInterface() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(GenericDiamondInterfaceBase.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodDescription diamondOverride = typeDescription.getInterfaces().get(0).getDeclaredMethods().getOnly();
        MethodDescription explicitOverride = typeDescription.getInterfaces().get(1).getDeclaredMethods().getOnly();
        MethodGraph.Node node = methodGraph.locate(diamondOverride.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.AMBIGUOUS));
        assertThat(methodGraph.locate(explicitOverride.asDefined().asSignatureToken()), is(node));
        assertThat(node.getMethodTypes().size(), is(2));
        assertThat(node.getMethodTypes().contains(diamondOverride.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(explicitOverride.asDefined().asTypeToken()), is(true));
        assertThat(node.getVisibility(), is(explicitOverride.getVisibility()));
    }

    @Test
    public void testDominantInterfaceMethod() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(BaseInterface.ExtensionType.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(12));
        MethodDescription method = typeDescription.getInterfaces().get(0).getDeclaredMethods().getOnly();
        MethodGraph.Node node = methodGraph.locate(method.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(1));
        assertThat(node.getRepresentative(), is(method));
    }

    @Test
    public void testDominantInterfaceMethodTriangle() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(AmbiguousInterface.TopType.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(12));
        MethodDescription method = typeDescription.getInterfaces().get(0).getDeclaredMethods().getOnly();
        MethodGraph.Node node = methodGraph.locate(method.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(1));
        assertThat(node.getRepresentative(), is(method));
    }

    @Test
    public void testDominantInterfaceMethodTriangleDuplicate() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(AmbiguousInterface.TopTypeWithDuplication.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(12));
        MethodDescription method = typeDescription.getInterfaces().get(1).getDeclaredMethods().getOnly();
        MethodGraph.Node node = methodGraph.locate(method.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(1));
        assertThat(node.getRepresentative(), is(method));
    }

    @Test
    public void testImplicitImplementation() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(ImplicitImplementation.ExtendedClass.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(12));
        MethodDescription method = typeDescription.getSuperClass().getDeclaredMethods().filter(isMethod()).getOnly();
        MethodGraph.Node node = methodGraph.locate(method.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getMethodTypes().size(), is(1));
        assertThat(node.getRepresentative(), is(method));
    }

    @Test
    public void testVisibilityExtension() throws Exception {
        TypeDescription typeDescription = new InstrumentedType.Default("foo",
                Opcodes.ACC_PUBLIC,
                ModuleDescription.UNDEFINED,
                Collections.<TypeVariableToken>emptyList(),
                TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(VisibilityExtension.Base.class),
                Collections.<TypeDescription.Generic>singletonList(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(VisibilityExtension.class)),
                Collections.<FieldDescription.Token>emptyList(),
                Collections.<String, Object>emptyMap(),
                Collections.<MethodDescription.Token>emptyList(),
                Collections.<RecordComponentDescription.Token>emptyList(),
                Collections.<AnnotationDescription>emptyList(),
                TypeInitializer.None.INSTANCE,
                LoadedTypeInitializer.NoOp.INSTANCE,
                TypeDescription.UNDEFINED,
                MethodDescription.UNDEFINED,
                TypeDescription.UNDEFINED,
                Collections.<TypeDescription>emptyList(),
                Collections.<TypeDescription>emptyList(),
                false,
                false,
                false,
                TargetType.DESCRIPTION,
                Collections.<TypeDescription>emptyList());
        MethodDescription.SignatureToken signatureToken = new MethodDescription.SignatureToken("foo",
                TypeDescription.ForLoadedType.of(void.class),
                Collections.<TypeDescription>emptyList());
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1 + TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(ElementMatchers.isVirtual()).size()));
        MethodGraph.Node node = methodGraph.locate(signatureToken);
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getRepresentative().asSignatureToken(), is(signatureToken));
        assertThat(node.getMethodTypes().size(), is(1));
        assertThat(node.getMethodTypes(), hasItem(signatureToken.asTypeToken()));
        assertThat(node.getVisibility(), is(Visibility.PUBLIC));
    }

    @Test
    public void testOrphanedBridge() throws Exception {
        MethodDescription.SignatureToken bridgeMethod = new MethodDescription.SignatureToken("foo",
                TypeDescription.ForLoadedType.of(void.class),
                Collections.<TypeDescription>emptyList());
        TypeDescription typeDescription = new InstrumentedType.Default("foo",
                Opcodes.ACC_PUBLIC,
                ModuleDescription.UNDEFINED,
                Collections.<TypeVariableToken>emptyList(),
                TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class),
                Collections.<TypeDescription.Generic>emptyList(),
                Collections.<FieldDescription.Token>emptyList(),
                Collections.<String, Object>emptyMap(),
                Collections.singletonList(new MethodDescription.Token("foo",
                        Opcodes.ACC_BRIDGE,
                        TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(void.class),
                        Collections.<TypeDescription.Generic>emptyList())),
                Collections.<RecordComponentDescription.Token>emptyList(),
                Collections.<AnnotationDescription>emptyList(),
                TypeInitializer.None.INSTANCE,
                LoadedTypeInitializer.NoOp.INSTANCE,
                TypeDescription.UNDEFINED,
                MethodDescription.UNDEFINED,
                TypeDescription.UNDEFINED,
                Collections.<TypeDescription>emptyList(),
                Collections.<TypeDescription>emptyList(),
                false,
                false,
                false,
                TargetType.DESCRIPTION,
                Collections.<TypeDescription>emptyList());
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1 + TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(ElementMatchers.isVirtual()).size()));
        MethodGraph.Node node = methodGraph.locate(bridgeMethod);
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(node.getRepresentative().asSignatureToken(), is(bridgeMethod));
        assertThat(node.getMethodTypes().size(), is(1));
        assertThat(node.getMethodTypes(), hasItem(bridgeMethod.asTypeToken()));
        assertThat(node.getVisibility(), is(Visibility.PACKAGE_PRIVATE));
    }

    @Test
    public void testRawType() throws Exception {
        TypeDescription typeDescription = TypeDescription.ForLoadedType.of(RawType.Raw.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().compile((TypeDefinition) typeDescription);
        assertThat(methodGraph.getSuperClassGraph().listNodes().size(), is(TypeDescription.ForLoadedType.of(Object.class).getDeclaredMethods().filter(isVirtual()).size() + 1));
        MethodDescription method = typeDescription.getSuperClass().getDeclaredMethods().filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly();
        MethodGraph.Node node = methodGraph.locate(method.asSignatureToken());
        assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(methodGraph.locate(method.asDefined().asSignatureToken()), is(node));
        assertThat(node.getMethodTypes().size(), is(2));
        assertThat(node.getMethodTypes().contains(method.asTypeToken()), is(true));
        assertThat(node.getMethodTypes().contains(method.asDefined().asTypeToken()), is(true));
        assertThat(node.getVisibility(), is(method.getVisibility()));
    }

    public interface SimpleInterface {
        /* empty */
    }

    public interface InterfaceBase {

        void foo();

        interface InnerInterface extends InterfaceBase {
            /* empty */
        }

        abstract class InnerClass implements InterfaceBase {
            /* empty */
        }

        interface InterfaceDuplicate extends InnerInterface, InterfaceBase {
            /* empty */
        }
    }

    public interface AmbiguousInterfaceBase {

        void foo();

        interface InterfaceTarget extends InterfaceBase, AmbiguousInterfaceBase {
            /* empty */
        }

        interface DominantIntermediate extends InterfaceBase, AmbiguousInterfaceBase {

            void foo();
        }

        interface DominantInterfaceTargetLeft extends InterfaceBase, DominantIntermediate {
            /* empty */
        }

        interface DominantInterfaceTargetRight extends DominantIntermediate, InterfaceBase {
            /* empty */
        }

        interface NonDominantAmbiguous {

            void foo();
        }

        interface NonDominantIntermediateLeft extends InterfaceBase, NonDominantAmbiguous {
            /* empty */
        }

        interface NonDominantIntermediateRight extends NonDominantAmbiguous, InterfaceBase {
            /* empty */
        }

        interface NonDominantTargetLeft extends DominantIntermediate, NonDominantIntermediateLeft {
            /* empty */
        }

        interface NonDominantTargetRight extends DominantIntermediate, NonDominantIntermediateRight {
            /* empty */
        }

        abstract class ClassTarget implements InterfaceBase, AmbiguousInterfaceBase {
            /* empty */
        }

        abstract class DominantClassBase implements DominantIntermediate {
            /* empty */
        }

        abstract class DominantClassTarget extends DominantClassBase implements InterfaceBase {
            /* empty */
        }
    }

    public interface GenericInterfaceBase<T> {

        void foo(T t);

        interface Inner extends GenericInterfaceBase<Void> {

            void foo(Void t);
        }

        interface Intermediate<T extends Number> extends GenericInterfaceBase<T> {

            void foo(T t);

            interface Inner extends Intermediate<Integer> {

                void foo(Integer t);
            }
        }
    }

    public interface ReturnTypeInterfaceBase {

        Object foo();

        interface Inner extends ReturnTypeInterfaceBase {

            Void foo();
        }

        interface Intermediate extends ReturnTypeInterfaceBase {

            Number foo();

            interface Inner extends Intermediate {

                Integer foo();
            }
        }
    }

    public interface GenericWithReturnTypeInterfaceBase<T> {

        Object foo(T t);

        interface Inner extends GenericWithReturnTypeInterfaceBase<Void> {

            Void foo(Void t);
        }

        interface Intermediate<T extends Number> extends GenericWithReturnTypeInterfaceBase<T> {

            Number foo(T t);

            interface Inner extends Intermediate<Integer> {

                Integer foo(Integer t);
            }
        }
    }

    public interface GenericNonOverriddenInterfaceBase<T> {

        T foo(T t);

        interface InnerInterface extends GenericNonOverriddenInterfaceBase<Void> {
            /* empty */
        }

        abstract class InnerClass implements GenericNonOverriddenInterfaceBase<Void> {
            /* empty */
        }
    }

    public interface DuplicateNameInterface {

        void foo(Object o);

        void foo(Integer o);

        interface InnerInterface extends DuplicateNameInterface {

            void foo(Void o);
        }

        abstract class InnerClass implements DuplicateNameInterface {

            public abstract void foo(Void o);
        }
    }

    public interface DuplicateNameGenericInterface<T> {

        void foo(T o);

        void foo(Integer o);

        interface InnerInterface extends DuplicateNameGenericInterface<String> {

            void foo(Void o);
        }

        @SuppressWarnings("unused")
        abstract class InnerClass implements DuplicateNameGenericInterface<String> {

            public abstract void foo(Void o);
        }
    }

    public interface MethodInterfaceConvergenceFirstBase<T> {

        T foo();
    }

    public interface MethodInterfaceConvergenceSecondBase {

        Void foo();
    }

    public interface MethodInterfaceConvergenceTarget extends MethodInterfaceConvergenceFirstBase<Void>, MethodInterfaceConvergenceSecondBase {
        /* empty */
    }

    public static class SimpleClass {
        /* empty */
    }

    public static class ClassBase {

        public void foo() {
            /* empty */
        }

        static class Inner extends ClassBase {

            public void foo() {
                /* empty */
            }
        }
    }

    public static class ClassAndInterfaceInheritance extends ClassBase implements InterfaceBase {
        /* empty */
    }

    public static class GenericClassBase<T> {

        public void foo(T t) {
            /* empty */
        }

        public static class Inner extends GenericClassBase<Void> {

            public void foo(Void t) {
                /* empty */
            }
        }

        public static class Intermediate<T extends Number> extends GenericClassBase<T> {

            public void foo(T t) {
                /* empty */
            }

            public static class Inner extends Intermediate<Integer> {

                public void foo(Integer t) {
                    /* empty */
                }
            }
        }
    }

    public static class GenericReturnClassBase<T> {

        public T foo() {
            return null;
        }

        public static class Inner extends GenericReturnClassBase<Void> {

            public Void foo() {
                return null;
            }
        }

        public static class Intermediate<T extends Number> extends GenericReturnClassBase<T> {

            public T foo() {
                return null;
            }

            public static class Inner extends Intermediate<Integer> {

                public Integer foo() {
                    return null;
                }
            }
        }
    }

    public static class ReturnTypeClassBase {

        public Object foo() {
            return null;
        }

        public static class Inner extends ReturnTypeClassBase {

            public Void foo() {
                return null;
            }
        }

        public static class Intermediate extends ReturnTypeClassBase {

            public Number foo() {
                return null;
            }

            public static class Inner extends Intermediate {

                public Integer foo() {
                    return null;
                }
            }
        }
    }

    public static class GenericWithReturnTypeClassBase<T> {

        public Object foo(T t) {
            return null;
        }

        public static class Inner extends GenericWithReturnTypeClassBase<Void> {

            public Void foo(Void t) {
                return null;
            }
        }

        public static class Intermediate<T extends Number> extends GenericWithReturnTypeClassBase<T> {

            public Number foo(T t) {
                return null;
            }

            public static class Inner extends Intermediate<Integer> {

                public Integer foo(Integer t) {
                    return null;
                }
            }
        }
    }

    public static class GenericNonOverriddenClassBase<T> {

        public T foo(T t) {
            return null;
        }

        public class Inner extends GenericNonOverriddenClassBase<Void> {
            /* empty */
        }
    }

    public static class GenericDiamondClassBase<T> {

        public T foo(T t) {
            return null;
        }

        public interface DiamondInterface {

            Void foo(Void t);
        }

        public class Inner extends GenericNonOverriddenClassBase<Void> implements DiamondInterface {
            /* empty */
        }
    }

    public interface GenericDiamondInterfaceBase<T> {

        T foo(T t);

        interface DiamondInterface {

            Void foo(Void s);
        }

        interface Inner extends GenericDiamondInterfaceBase<Void>, DiamondInterface {
            /* empty */
        }
    }

    public interface VisibilityExtension {

        void foo();

        class Base {

            protected void foo() {
                /* do nothing */
            }
        }
    }

    public static class DuplicateNameClass {

        public void foo(Object o) {
            /* empty */
        }

        public void foo(Integer o) {
            /* empty */
        }

        public static class Inner extends DuplicateNameClass {

            public void foo(Void o) {
                /* empty */
            }
        }
    }

    public static class DuplicateNameGenericClass<T> {

        public void foo(T o) {
            /* empty */
        }

        public void foo(Integer o) {
            /* empty */
        }

        public static class Inner extends DuplicateNameGenericClass<String> {

            public void foo(Void o) {
                /* empty */
            }
        }
    }

    static class VisibilityBridgeBase {

        public void foo() {
            /* empty */
        }
    }

    public static class VisibilityBridgeTarget extends VisibilityBridgeBase {
        /* empty */
    }

    public static class GenericVisibilityBridgeBase<T> {

        public void foo(T t) {
            /* empty */
        }
    }

    static class GenericVisibilityBridge extends GenericVisibilityBridgeBase<Void> {

        public void foo(Void aVoid) {
            /* empty */
        }
    }

    public static class GenericVisibilityBridgeTarget extends GenericVisibilityBridge {
        /* empty */
    }

    public static class MethodClassConvergence<T> {

        public T foo(T arg) {
            return null;
        }

        public Void foo(Void arg) {
            return null;
        }

        public static class Inner extends MethodClassConvergence<Void> {

            public Void foo(Void arg) {
                return null;
            }
        }
    }

    static class MethodConvergenceVisibilityBridgeBase<T> {

        public T foo(T arg) {
            return null;
        }

        public Void foo(Void arg) {
            return null;
        }
    }

    static class MethodConvergenceVisibilityBridgeIntermediate extends MethodConvergenceVisibilityBridgeBase<Void> {

        public Void foo(Void arg) {
            return null;
        }
    }

    public static class MethodConvergenceVisibilityBridgeTarget extends MethodConvergenceVisibilityBridgeIntermediate {
        /* empty */
    }

    public static class RawType<T> {

        public void foo(T t) {
            /* empty */
        }

        public static class Intermediate<T extends Number> extends RawType<T> {

            public void foo(T t) {
                /* empty */
            }
        }

        @SuppressWarnings("rawtypes")
        public static class Raw extends Intermediate {

            public void foo(Number t) {
                /* empty */
            }
        }
    }

    public interface BaseInterface {

        void foo();

        abstract class BaseType implements BaseInterface {
            /* empty */
        }

        interface ExtensionInterface extends BaseInterface {

            void foo();
        }

        abstract class ExtensionType extends BaseType implements ExtensionInterface {
            /* empty */
        }
    }

    public interface AmbiguousInterface {

        void foo();

        interface Left {

            void foo();
        }

        interface Right {

            void foo();
        }

        interface Top extends Left, Right, AmbiguousInterface {

            void foo();
        }

        abstract class BaseType implements Left {
            /* empty */
        }

        abstract class ExtensionType extends BaseType implements Right {
            /* empty */
        }

        abstract class TopType extends ExtensionType implements Top {
            /* empty */
        }

        abstract class TopTypeWithDuplication extends ExtensionType implements AmbiguousInterface, Top {
            /* empty */
        }
    }

    interface ImplicitImplementation {

        void foo();

        abstract class BaseClass {

            public abstract void foo();
        }

        abstract class ExtendedClass extends BaseClass implements ImplicitImplementation {
            /* empty */
        }
    }
}
