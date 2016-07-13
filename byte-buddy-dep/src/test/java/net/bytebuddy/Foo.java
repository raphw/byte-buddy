package net.bytebuddy;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.test.utility.DebuggingWrapper;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class Foo {

    @Test
    public void testCglib() throws Exception {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(Object.class);
        enhancer.setCallback(new MethodInterceptor() {
            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                return methodProxy.invokeSuper(o, objects);
            }
        });
        Class<?> type = enhancer.create().getClass();

//        System.out.println(type.isSynthetic());

        new ByteBuddy()
                .redefine(type, ClassFileLocator.AgentBased.of(ByteBuddyAgent.install(), type))
                .name("XYZ")
//                .visit(DebuggingWrapper.makeDefault(false))
                .visit(Advice.to(Foo.class).on(named("toString")))
                .make()
                .load(type.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance();
    }

    @Test
    public void testMethods() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod("foo", String.class)
                .intercept(new Implementation() {
                    @Override
                    public ByteCodeAppender appender(Target implementationTarget) {
                        return new ByteCodeAppender() {
                            @Override
                            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                                methodVisitor.visitLdcInsn("foo");
                                methodVisitor.visitLdcInsn("bar");
                                methodVisitor.visitInsn(Opcodes.ARETURN);
                                methodVisitor.visitFrame(Opcodes.F_SAME, 0, new Object[0], 0, new Object[0]);
                                methodVisitor.visitLdcInsn("foo");
                                methodVisitor.visitInsn(Opcodes.ARETURN);
                                return new Size(2, 1);
                            }
                        };
                    }

                    @Override
                    public InstrumentedType prepare(InstrumentedType instrumentedType) {
                        return instrumentedType;
                    }
                }).make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        new ByteBuddy()
                .redefine(type, ClassFileLocator.AgentBased.of(ByteBuddyAgent.install(), type))
                .name("XYZ")
                .visit(DebuggingWrapper.makeDefault(true))
                .visit(Advice.to(Foo.class).on(named("foo")))
                .make()
                .load(type.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance();
    }

    @Test
    public void testPop() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod("foo", void.class)
                .intercept(new Implementation() {
                    @Override
                    public ByteCodeAppender appender(Target implementationTarget) {
                        return new ByteCodeAppender() {
                            @Override
                            public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
                                methodVisitor.visitLdcInsn(1L);
                                methodVisitor.visitInsn(Opcodes.POP);
                                methodVisitor.visitInsn(Opcodes.RETURN);
                                return new Size(10, 1);
                            }
                        };
                    }

                    @Override
                    public InstrumentedType prepare(InstrumentedType instrumentedType) {
                        return instrumentedType;
                    }
                }).make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        type.newInstance();
    }

    @Advice.OnMethodExit
    private static void exit() {
        // empty
    }
}
