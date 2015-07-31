package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class MethodGraphCompilerDefaultTest {

    @Test
    public void testObjectType() throws Exception {
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(TypeDescription.OBJECT);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().size()));
        assertThat(methodGraph.getSuperGraph().listNodes().size(), is(0));
        assertThat(methodGraph.getInterfaceGraph(mock(TypeDescription.class)).listNodes().size(), is(0));
        for (MethodDescription methodDescription : TypeDescription.OBJECT.getDeclaredMethods()) {
            MethodGraph.Node node = methodGraph.locate(methodDescription.asToken());
            assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
            assertThat(node.isMadeVisible(), is(false));
            assertThat(node.getBridges().size(), is(0));
            assertThat(node.getRepresentative(), is(methodDescription));
            assertThat(methodGraph.listNodes().contains(node), is(true));
        }
    }

    @Test
    public void testSimpleClass() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(SimpleClass.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 1));
        assertThat(methodGraph.getSuperGraph().listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size()));
        assertThat(methodGraph.getInterfaceGraph(mock(TypeDescription.class)).listNodes().size(), is(0));
        for (MethodDescription methodDescription : TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual())) {
            MethodGraph.Node node = methodGraph.locate(methodDescription.asToken());
            assertThat(node.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
            assertThat(node.isMadeVisible(), is(false));
            assertThat(node.getBridges().size(), is(0));
            assertThat(node.getRepresentative(), is(methodDescription));
            assertThat(methodGraph.listNodes().contains(node), is(true));
        }
        MethodDescription constructor = typeDescription.getDeclaredMethods().filter(isConstructor()).getOnly();
        MethodGraph.Node constructorNode = methodGraph.locate(constructor.asToken());
        assertThat(constructorNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(constructorNode.isMadeVisible(), is(false));
        assertThat(constructorNode.getBridges().size(), is(0));
        assertThat(constructorNode.getRepresentative(), is(constructor));
        assertThat(methodGraph.listNodes().contains(constructorNode), is(true));
    }

    @Test
    public void testSimpleInterface() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(SimpleInterface.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(0));
    }

    @Test
    public void testClassInheritance() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(ClassBase.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 2));
        MethodDescription method = typeDescription.getDeclaredMethods().filter(isMethod()).getOnly();
        MethodGraph.Node methodNode = methodGraph.locate(method.asToken());
        assertThat(methodNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(methodNode.isMadeVisible(), is(false));
        assertThat(methodNode.getBridges().size(), is(0));
        assertThat(methodNode.getRepresentative(), is(method));
        assertThat(methodGraph.listNodes().contains(methodNode), is(true));
        MethodGraph.Node baseNode = methodGraph.getSuperGraph().locate(method.asToken());
        assertThat(methodNode, not(is(baseNode)));
        assertThat(baseNode.getRepresentative(), is(typeDescription.getSuperType().getDeclaredMethods().filter(representedBy(method.asToken())).getOnly()));
    }

    @Test
    public void testInterfaceImplementation() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(InterfaceBase.InnerClass.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 2));
        MethodDescription method = typeDescription.getInterfaces().getOnly().getDeclaredMethods().getOnly();
        MethodGraph.Node methodNode = methodGraph.locate(method.asToken());
        assertThat(methodNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(methodNode.isMadeVisible(), is(false));
        assertThat(methodNode.getBridges().size(), is(0));
        assertThat(methodNode.getRepresentative(), is(method));
        assertThat(methodGraph.listNodes().contains(methodNode), is(true));
    }

    @Test
    public void testInterfaceExtension() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(InterfaceBase.InnerInterface.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodDescription method = typeDescription.getInterfaces().getOnly().getDeclaredMethods().getOnly();
        MethodGraph.Node methodNode = methodGraph.locate(method.asToken());
        assertThat(methodNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(methodNode.isMadeVisible(), is(false));
        assertThat(methodNode.getBridges().size(), is(0));
        assertThat(methodNode.getRepresentative(), is(method));
        assertThat(methodGraph.listNodes().contains(methodNode), is(true));
    }

    @Test
    public void testInterfaceClassMultipleInheritance() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(MultipleInheritance.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 2));
        MethodDescription method = typeDescription.getSuperType().getDeclaredMethods().filter(isMethod()).getOnly();
        MethodGraph.Node methodNode = methodGraph.locate(method.asToken());
        assertThat(methodNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(methodNode.isMadeVisible(), is(false));
        assertThat(methodNode.getBridges().size(), is(0));
        assertThat(methodNode.getRepresentative(), is(method));
        assertThat(methodGraph.listNodes().contains(methodNode), is(true));
        MethodGraph.Node baseNode = methodGraph.getInterfaceGraph(new TypeDescription.ForLoadedType(InterfaceBase.class)).locate(method.asToken());
        assertThat(methodNode, not(is(baseNode)));
        assertThat(baseNode.getRepresentative(), is(typeDescription.getInterfaces().getOnly().getDeclaredMethods().getOnly()));
    }

    @Test
    public void testGenericClassSingleEvolution() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(GenericClassBase.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 2));
        MethodGraph.Node methodNode = methodGraph.locate(typeDescription.getDeclaredMethods()
                .filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asToken());
        MethodDescription.Token bridgeToken = typeDescription.getSuperType().getDeclaredMethods().filter(isMethod()).getOnly().asDefined().asToken();
        assertThat(methodNode, is(methodGraph.locate(bridgeToken)));
        assertThat(methodNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(methodNode.isMadeVisible(), is(false));
        assertThat(methodNode.getBridges().size(), is(1));
        assertThat(methodNode.getBridges().contains(bridgeToken), is(true));
    }

    @Test
    public void testGenericClassMultipleEvolution() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(GenericClassBase.Intermediate.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 2));
        MethodGraph.Node methodNode = methodGraph.locate(typeDescription.getDeclaredMethods()
                .filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asToken());
        MethodDescription.Token firstBridgeToken = typeDescription.getSuperType().getDeclaredMethods()
                .filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asDefined().asToken();
        MethodDescription.Token secondBridgeToken = typeDescription.getSuperType().getSuperType().getDeclaredMethods()
                .filter(isMethod()).getOnly().asDefined().asToken();
        assertThat(methodNode, is(methodGraph.locate(firstBridgeToken)));
        assertThat(methodNode, is(methodGraph.locate(secondBridgeToken)));
        assertThat(methodNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(methodNode.isMadeVisible(), is(false));
        assertThat(methodNode.getBridges().size(), is(2));
        assertThat(methodNode.getBridges().contains(firstBridgeToken), is(true));
        assertThat(methodNode.getBridges().contains(secondBridgeToken), is(true));
    }

    @Test
    public void testReturnTypeClassSingleEvolution() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(ReturnTypeClassBase.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 2));
        MethodGraph.Node methodNode = methodGraph.locate(typeDescription.getDeclaredMethods()
                .filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asToken());
        MethodDescription.Token bridgeToken = typeDescription.getSuperType().getDeclaredMethods().filter(isMethod()).getOnly().asToken();
        assertThat(methodNode, is(methodGraph.locate(bridgeToken)));
        assertThat(methodNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(methodNode.isMadeVisible(), is(false));
        assertThat(methodNode.getBridges().size(), is(1));
        assertThat(methodNode.getBridges().contains(bridgeToken), is(true));
    }

    @Test
    public void testReturnTypeClassMultipleEvolution() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(ReturnTypeClassBase.Intermediate.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 2));
        MethodGraph.Node methodNode = methodGraph.locate(typeDescription.getDeclaredMethods()
                .filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asToken());
        MethodDescription.Token firstBridgeToken = typeDescription.getSuperType().getDeclaredMethods()
                .filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asDefined().asToken();
        MethodDescription.Token secondBridgeToken = typeDescription.getSuperType().getSuperType().getDeclaredMethods()
                .filter(isMethod()).getOnly().asDefined().asToken();
        assertThat(methodNode, is(methodGraph.locate(firstBridgeToken)));
        assertThat(methodNode, is(methodGraph.locate(secondBridgeToken)));
        assertThat(methodNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(methodNode.isMadeVisible(), is(false));
        assertThat(methodNode.getBridges().size(), is(2));
        assertThat(methodNode.getBridges().contains(firstBridgeToken), is(true));
        assertThat(methodNode.getBridges().contains(secondBridgeToken), is(true));
    }

    @Test
    public void testGenericInterfaceSingleEvolution() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(GenericInterfaceBase.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodGraph.Node methodNode = methodGraph.locate(typeDescription.getDeclaredMethods()
                .filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly().asToken());
        MethodDescription.Token bridgeToken = typeDescription.getInterfaces().getOnly()
                .getDeclaredMethods().filter(isMethod()).getOnly().asDefined().asToken();
        assertThat(methodNode, is(methodGraph.locate(bridgeToken)));
        assertThat(methodNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(methodNode.isMadeVisible(), is(false));
        assertThat(methodNode.getBridges().size(), is(1));
        assertThat(methodNode.getBridges().contains(bridgeToken), is(true));
    }

    @Test
    public void testGenericInterfaceMultipleEvolution() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(GenericInterfaceBase.Intermediate.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodGraph.Node methodNode = methodGraph.locate(typeDescription.getDeclaredMethods().getOnly().asToken());
        MethodDescription.Token firstBridgeToken = typeDescription.getInterfaces().getOnly()
                .getDeclaredMethods().getOnly().asDefined().asToken();
        MethodDescription.Token secondBridgeToken = typeDescription.getInterfaces().getOnly().getInterfaces().getOnly()
                .getDeclaredMethods().getOnly().asDefined().asToken();
        assertThat(methodNode, is(methodGraph.locate(firstBridgeToken)));
        assertThat(methodNode, is(methodGraph.locate(secondBridgeToken)));
        assertThat(methodNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(methodNode.isMadeVisible(), is(false));
        assertThat(methodNode.getBridges().size(), is(2));
        assertThat(methodNode.getBridges().contains(firstBridgeToken), is(true));
        assertThat(methodNode.getBridges().contains(secondBridgeToken), is(true));
    }

    @Test
    public void testReturnTypeInterfaceSingleEvolution() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(ReturnTypeInterfaceBase.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodGraph.Node methodNode = methodGraph.locate(typeDescription.getDeclaredMethods().getOnly().asToken());
        MethodDescription.Token bridgeToken = typeDescription.getInterfaces().getOnly().getDeclaredMethods().getOnly().asToken();
        assertThat(methodNode, is(methodGraph.locate(bridgeToken)));
        assertThat(methodNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(methodNode.isMadeVisible(), is(false));
        assertThat(methodNode.getBridges().size(), is(1));
        assertThat(methodNode.getBridges().contains(bridgeToken), is(true));
    }

    @Test
    public void testReturnTypeInterfaceMultipleEvolution() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(ReturnTypeInterfaceBase.Intermediate.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
        MethodGraph.Node methodNode = methodGraph.locate(typeDescription.getDeclaredMethods().getOnly().asToken());
        MethodDescription.Token firstBridgeToken = typeDescription.getInterfaces().getOnly()
                .getDeclaredMethods().getOnly().asToken();
        MethodDescription.Token secondBridgeToken = typeDescription.getInterfaces().getOnly().getInterfaces().getOnly()
                .getDeclaredMethods().getOnly().asToken();
        assertThat(methodNode, is(methodGraph.locate(firstBridgeToken)));
        assertThat(methodNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(methodNode.isMadeVisible(), is(false));
        assertThat(methodNode.getBridges().size(), is(2));
        assertThat(methodNode.getBridges().contains(firstBridgeToken), is(true));
        assertThat(methodNode.getBridges().contains(secondBridgeToken), is(true));
    }

    @Test
    public void testVisibilityBridge() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(VisibilityBridgeTarget.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 2));
        MethodDescription methodDescription = typeDescription.getDeclaredMethods().filter(isMethod()).getOnly();
        MethodGraph.Node methodNode = methodGraph.locate(methodDescription.asToken());
        assertThat(methodNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(methodNode.isMadeVisible(), is(true));
        assertThat(methodNode.getBridges().size(), is(0));
        assertThat(methodNode.getRepresentative(), is(methodDescription));
    }

    @Test
    public void testGenericVisibilityBridge() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(GenericVisibilityBridgeTarget.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 2));
        MethodDescription methodDescription = typeDescription.getSuperType()
                .getDeclaredMethods().filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly();
        MethodDescription.Token bridgeToken = typeDescription.getSuperType().getSuperType()
                .getDeclaredMethods().filter(isMethod()).getOnly().asToken();
        MethodGraph.Node methodNode = methodGraph.locate(methodDescription.asToken());
        assertThat(methodNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(methodNode, is(methodGraph.locate(bridgeToken)));
        assertThat(methodNode.isMadeVisible(), is(true));
        assertThat(methodNode.getBridges().size(), is(1));
        assertThat(methodNode.getRepresentative(), is(methodDescription));
    }

    @Test
    public void testMethodClassConvergence() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(MethodClassConvergence.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 2));
        MethodDescription methodDescription = typeDescription.getDeclaredMethods().filter(isMethod().and(ElementMatchers.not(isBridge()))).getOnly();
        MethodDescription originalMethod = typeDescription.getSuperType().getDeclaredMethods()
                .filter(isMethod().and(definedMethod(takesArguments(Object.class)))).getOnly();
        MethodGraph.Node methodNode = methodGraph.locate(methodDescription.asToken());
        assertThat(methodNode.getSort(), is(MethodGraph.Node.Sort.RESOLVED));
        assertThat(methodNode, is(methodGraph.locate(originalMethod.asDefined().asToken())));
        assertThat(methodNode.isMadeVisible(), is(false));
        assertThat(methodNode.getBridges().size(), is(1));
        assertThat(methodNode.getRepresentative(), is(methodDescription));
        MethodGraph superGraph = methodGraph.getSuperGraph();
        MethodGraph.Node superNode = superGraph.locate(methodDescription.asToken());
        assertThat(superNode.getSort(), is(MethodGraph.Node.Sort.AMBIGUOUS));
        assertThat(superNode.isMadeVisible(), is(false));
        assertThat(superNode.getBridges().size(), is(1));
        assertThat(superNode.getRepresentative(), is(originalMethod)); // TODO
    }

    // TODO: method convergance, generic

    @Test
    public void testAmbiguousInterface() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(MethodInterfaceConvergenceTarget.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
    }

    // TODO: ambigous interfaces are not properly merged

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodGraph.Compiler.Default.class).apply();
    }

    public static class SimpleClass {
        /* empty */
    }

    public interface SimpleInterface {
        /* empty */
    }

    public static class ClassBase {

        public void foo() {
            /* empty */
        }

        static class Inner extends ClassBase {

            @Override
            public void foo() {
                /* empty */
            }
        }
    }

    public interface InterfaceBase {

        void foo();

        abstract class InnerClass implements InterfaceBase {
            /* empty */
        }

        interface InnerInterface extends InterfaceBase {
            /* empty */
        }
    }

    public static class MultipleInheritance extends ClassBase implements InterfaceBase {
        /* empty */
    }

    public static class GenericClassBase<T> {

        public void foo(T t) {
            /* empty */
        }

        public static class Inner extends GenericClassBase<Void> {

            @Override
            public void foo(Void t) {
                /* empty */
            }
        }

        public static class Intermediate<T extends Number> extends GenericClassBase<T> {

            @Override
            public void foo(T t) {
                /* empty */
            }

            public static class Inner extends Intermediate<Integer> {

                @Override
                public void foo(Integer t) {
                    /* empty */
                }
            }
        }
    }

    public static class ReturnTypeClassBase {

        public Object foo() {
            return null;
        }

        public static class Inner extends ReturnTypeClassBase {

            @Override
            public Void foo() {
                return null;
            }
        }

        public static class Intermediate extends ReturnTypeClassBase {

            @Override
            public Number foo() {
                return null;
            }

            public static class Inner extends Intermediate {

                @Override
                public Integer foo() {
                    return null;
                }
            }
        }
    }

    public interface GenericInterfaceBase<T> {

        void foo(T t);

        interface Inner extends GenericInterfaceBase<Void> {

            @Override
            void foo(Void t);
        }

        interface Intermediate<T extends Number> extends GenericInterfaceBase<T> {

            @Override
            void foo(T t);

            interface Inner extends Intermediate<Integer> {

                @Override
                void foo(Integer t);
            }
        }
    }

    public interface ReturnTypeInterfaceBase {

        Object foo();

        interface Inner extends ReturnTypeInterfaceBase {

            @Override
            Void foo();
        }

        interface Intermediate extends ReturnTypeInterfaceBase {

            @Override
            Number foo();

            interface Inner extends Intermediate {

                @Override
                Integer foo();
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

        @Override
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

            @Override
            public Void foo(Void arg) {
                return null;
            }
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

    static class MethodConvergenceVisibilityBridgeBase<T> {

        public T foo(T arg) {
            return null;
        }

        public Void foo(Void arg) {
            return null;
        }
    }

    public static class MethodConvergenceVisibilityBridgeTarget extends MethodConvergenceVisibilityBridgeBase<Void> {

        @Override
        public Void foo(Void arg) {
            return null;
        }
    }
}