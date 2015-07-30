package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.isVirtual;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodGraphCompilerDefaultTest {

    @Test
    public void testTrivial() throws Exception {
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(TypeDescription.OBJECT);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().size()));
    }

    @Test
    public void testSimpleClass() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(SimpleClass.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 1));
    }

    @Test
    public void testSimpleInterface() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(SimpleInterface.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(0));
    }

    @Test
    public void testClassInheritance() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(ClassInheritance.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 1));
    }

    @Test
    public void testInterfaceImplementation() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(InterfaceBase.InnerClass.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 2));
    }

    @Test
    public void testInterfaceExtension() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(InterfaceBase.InnerInterface.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(1));
    }

    @Test
    public void testGenericSingleEvolution() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(GenericBase.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 2));
    }

    @Test
    public void testGenericMultipleEvolution() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(GenericBase.Intermediate.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 2));
    }

    @Test
    public void testReturnTypeSingleEvolution() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(ReturnTypeBase.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 2));
    }

    @Test
    public void testReturnTypeMultipleEvolution() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(ReturnTypeBase.Intermediate.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 2));
    }

    @Test
    public void testVisibilityBridge() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(VisibilityBridgeTarget.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 2));
    }

    @Test
    public void testGenericVisibilityBridge() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(GenericVisibilityBridgeTarget.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 2));
    }

    @Test
    public void testMethodConvergence() throws Exception {
        TypeDescription typeDescription = new TypeDescription.ForLoadedType(MethodConvergence.Inner.class);
        MethodGraph.Linked methodGraph = MethodGraph.Compiler.Default.forJavaHierarchy().make(typeDescription);
        assertThat(methodGraph.listNodes().size(), is(TypeDescription.OBJECT.getDeclaredMethods().filter(isVirtual()).size() + 2));
    }

    public static class SimpleClass {
        /* empty */
    }

    public interface SimpleInterface {
        /* empty */
    }

    public static class ClassInheritance {

        @Override
        public String toString() {
            return null;
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

    public static class GenericBase<T> {

        void foo(T t) {
            /* empty */
        }

        public static class Inner extends GenericBase<Void> {

            @Override
            void foo(Void t) {
                /* empty */
            }
        }

        public static class Intermediate<T extends Number> extends GenericBase<T> {

            @Override
            void foo(T t) {
                /* empty */
            }

            public static class Inner extends Intermediate<Integer> {

                @Override
                void foo(Integer t) {
                    /* empty */
                }
            }
        }
    }

    public static class ReturnTypeBase {

        Object foo() {
            return null;
        }

        public static class Inner extends ReturnTypeBase {

            @Override
            Void foo() {
                return null;
            }
        }

        public static class Intermediate extends ReturnTypeBase {

            @Override
            Number foo() {
                return null;
            }

            public static class Inner extends Intermediate {

                @Override
                Integer foo() {
                    return null;
                }
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

    static class GenericVisibilityBridgeBase<T> {

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

    public static class MethodConvergence<T> {

        public T foo(T arg) {
            return null;
        }

        public Void foo(Void arg) {
            return null;
        }

        public static class Inner extends MethodConvergence<Void> {

            @Override
            public Void foo(Void arg) {
                return null;
            }
        }
    }
}