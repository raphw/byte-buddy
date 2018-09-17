package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.attribute.AnnotationRetention;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.matcher.LatentMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.OpenedClassReader;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class DecoratingDynamicTypeBuilderTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testDecoration() throws Exception {
        Object instance = new ByteBuddy()
                .decorate(Foo.class)
                .annotateType(AnnotationDescription.Builder.ofType(Qux.class).build())
                .ignoreAlso(new LatentMatcher.Resolved<MethodDescription>(none()))
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().method(named(FOO), new AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper() {
                    public MethodVisitor wrap(TypeDescription instrumentedType,
                                              MethodDescription instrumentedMethod,
                                              MethodVisitor methodVisitor,
                                              Implementation.Context implementationContext,
                                              TypePool typePool,
                                              int writerFlags,
                                              int readerFlags) {
                        return new MethodVisitor(OpenedClassReader.ASM_API, methodVisitor) {
                            public void visitLdcInsn(Object value) {
                                if (FOO.equals(value)) {
                                    value = BAR;
                                }
                                super.visitLdcInsn(value);
                            }
                        };
                    }
                }))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getConstructor()
                .newInstance();
        assertThat(instance.getClass().getMethod(FOO).invoke(instance), is((Object) BAR));
        assertThat(instance.getClass().isAnnotationPresent(Bar.class), is(true));
        assertThat(instance.getClass().isAnnotationPresent(Qux.class), is(true));
    }

    @Test
    public void testDecorationNonVirtualMember() throws Exception {
        Object instance = new ByteBuddy()
                .decorate(Foo.class)
                .annotateType(AnnotationDescription.Builder.ofType(Qux.class).build())
                .ignoreAlso(new LatentMatcher.Resolved<MethodDescription>(none()))
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().method(named(BAR), new AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper() {
                    public MethodVisitor wrap(TypeDescription instrumentedType,
                                              MethodDescription instrumentedMethod,
                                              MethodVisitor methodVisitor,
                                              Implementation.Context implementationContext,
                                              TypePool typePool,
                                              int writerFlags,
                                              int readerFlags) {
                        return new MethodVisitor(OpenedClassReader.ASM_API, methodVisitor) {
                            @Override
                            public void visitLdcInsn(Object value) {
                                if (FOO.equals(value)) {
                                    value = BAR;
                                }
                                super.visitLdcInsn(value);
                            }
                        };
                    }
                }))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getConstructor()
                .newInstance();
        assertThat(instance.getClass().getMethod(BAR).invoke(null), is((Object) BAR));
        assertThat(instance.getClass().isAnnotationPresent(Bar.class), is(true));
        assertThat(instance.getClass().isAnnotationPresent(Qux.class), is(true));
    }

    @Test
    public void testDecorationWithoutAnnotationRetention() throws Exception {
        Object instance = new ByteBuddy()
                .with(AnnotationRetention.DISABLED)
                .decorate(Foo.class)
                .annotateType(AnnotationDescription.Builder.ofType(Qux.class).build())
                .ignoreAlso(new LatentMatcher.Resolved<MethodDescription>(none()))
                .visit(new AsmVisitorWrapper.ForDeclaredMethods()
                        .method(named(FOO), new AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper() {
                            public MethodVisitor wrap(TypeDescription instrumentedType,
                                                      MethodDescription instrumentedMethod,
                                                      MethodVisitor methodVisitor,
                                                      Implementation.Context implementationContext,
                                                      TypePool typePool,
                                                      int writerFlags,
                                                      int readerFlags) {
                                return new MethodVisitor(OpenedClassReader.ASM_API, methodVisitor) {
                                    @Override
                                    public void visitLdcInsn(Object value) {
                                        if (FOO.equals(value)) {
                                            value = BAR;
                                        }
                                        super.visitLdcInsn(value);
                                    }
                                };
                            }
                        }))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getConstructor()
                .newInstance();
        assertThat(instance.getClass().getMethod(FOO).invoke(instance), is((Object) BAR));
        assertThat(instance.getClass().isAnnotationPresent(Bar.class), is(true));
        assertThat(instance.getClass().isAnnotationPresent(Qux.class), is(true));
    }

    @Test
    public void testAuxiliaryTypes() throws Exception {
        Map<TypeDescription, byte[]> auxiliaryTypes = new ByteBuddy()
                .decorate(Foo.class)
                .require(TypeDescription.VOID, new byte[]{1, 2, 3})
                .make()
                .getAuxiliaryTypes();
        assertThat(auxiliaryTypes.size(), is(1));
        assertThat(auxiliaryTypes.get(TypeDescription.VOID).length, is(3));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDecorationChangeName() throws Exception {
        new ByteBuddy().decorate(Foo.class).name(FOO);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDecorationChangeModifiers() throws Exception {
        new ByteBuddy().decorate(Foo.class).modifiers(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDecorationChangeModifiersMerge() throws Exception {
        new ByteBuddy().decorate(Foo.class).merge();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDecorationChangeInterface() throws Exception {
        new ByteBuddy().decorate(Foo.class).implement(Runnable.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDecorationChange() throws Exception {
        new ByteBuddy().decorate(Foo.class).implement(Runnable.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testInnerClassChangeForTopLevel() throws Exception {
        new ByteBuddy().decorate(Foo.class).topLevelType();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testInnerClassChangeForType() throws Exception {
        new ByteBuddy().decorate(Foo.class).innerTypeOf(Object.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testInnerClassChangeForMethod() throws Exception {
        new ByteBuddy().decorate(Foo.class).innerTypeOf(Object.class.getMethod("toString"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testInnerClassChangeForConstructor() throws Exception {
        new ByteBuddy().decorate(Foo.class).innerTypeOf(Object.class.getConstructor());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testNestHost() throws Exception {
        new ByteBuddy().decorate(Foo.class).nestHost(Object.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testNestMember() throws Exception {
        new ByteBuddy().decorate(Foo.class).nestMembers(Object.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDefineField() throws Exception {
        new ByteBuddy().decorate(Foo.class).defineField(FOO, Void.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testInterceptField() throws Exception {
        new ByteBuddy().decorate(Foo.class).field(any());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDefineMethod() throws Exception {
        new ByteBuddy().decorate(Foo.class).defineMethod(FOO, void.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDefineConstructor() throws Exception {
        new ByteBuddy().decorate(Foo.class).defineConstructor();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testInterceptInvokable() throws Exception {
        new ByteBuddy().decorate(Foo.class).invokable(any());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testTypeVariable() throws Exception {
        new ByteBuddy().decorate(Foo.class).typeVariable(FOO);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testInitializer() throws Exception {
        new ByteBuddy().decorate(Foo.class).initializer(mock(ByteCodeAppender.class));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testLoadedInitializer() throws Exception {
        new ByteBuddy().decorate(Foo.class).initializer(mock(LoadedTypeInitializer.class));
    }

    @Bar
    public static class Foo {

        public String foo() {
            return FOO;
        }

        public static String bar() {
            return FOO;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Bar {
        /* empty */
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Qux {
        /* empty */
    }
}
