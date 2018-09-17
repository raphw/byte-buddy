package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class AdviceDeadCodeTest {

    private static final String FOO = "foo";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {ClassFileVersion.JAVA_V5},
                {ClassFileVersion.JAVA_V6}
        });
    }

    private final ClassFileVersion classFileVersion;

    public AdviceDeadCodeTest(ClassFileVersion classFileVersion) {
        this.classFileVersion = classFileVersion;
    }

    @Test
    public void testAdviceProcessesDeadCode() throws Exception {
        Class<?> type = new ByteBuddy(classFileVersion)
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Visibility.PUBLIC)
                .intercept(new DeadStringAppender())
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        Class<?> redefined = new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(ExitAdvice.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(redefined.getDeclaredMethod(FOO).invoke(redefined.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testAdviceContainsDeadCode() throws Exception {
        Class<?> advice = new ByteBuddy(classFileVersion)
                .subclass(Object.class)
                .defineMethod(FOO, void.class, Ownership.STATIC)
                .intercept(new DeadVoidAppender())
                .annotateMethod(AnnotationDescription.Builder.ofType(Advice.OnMethodEnter.class).define("suppress", RuntimeException.class).build())
                .annotateMethod(AnnotationDescription.Builder.ofType(Advice.OnMethodExit.class).define("suppress", RuntimeException.class).build())
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        Class<?> foo = new ByteBuddy(classFileVersion)
                .subclass(Object.class)
                .defineMethod("foo", String.class, Visibility.PUBLIC)
                .intercept(FixedValue.value(FOO))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        Class<?> redefined = new ByteBuddy()
                .redefine(foo)
                .visit(Advice.to(advice).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(redefined, not(sameInstance((Object) foo)));
        assertThat(redefined.getDeclaredMethod(FOO).invoke(redefined.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testAdviceWithExchangeDuplicationDeadCode() throws Exception {
        Class<?> type = new ByteBuddy(classFileVersion)
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Visibility.PUBLIC)
                .intercept(new DeadExchangeAppender())
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        Class<?> redefined = new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(ExitAdvice.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(redefined.getDeclaredMethod(FOO).invoke(redefined.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @SuppressWarnings("all")
    private static class ExitAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Return(readOnly = false) String value) {
            value = FOO;
        }
    }

    private static class DeadStringAppender implements Implementation, ByteCodeAppender {

        public ByteCodeAppender appender(Target implementationTarget) {
            return this;
        }

        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            methodVisitor.visitInsn(Opcodes.POP); // dead code
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            return new Size(1, instrumentedMethod.getStackSize());
        }
    }

    private static class DeadVoidAppender implements Implementation, ByteCodeAppender {

        public ByteCodeAppender appender(Target implementationTarget) {
            return this;
        }

        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            methodVisitor.visitInsn(Opcodes.RETURN);
            methodVisitor.visitInsn(Opcodes.POP); // dead code
            methodVisitor.visitInsn(Opcodes.RETURN);
            return new Size(1, instrumentedMethod.getStackSize());
        }
    }

    private static class DeadExchangeAppender implements Implementation, ByteCodeAppender {

        public ByteCodeAppender appender(Target implementationTarget) {
            return this;
        }

        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            methodVisitor.visitInsn(Opcodes.DUP_X1); // dead code
            methodVisitor.visitInsn(Opcodes.ARETURN);
            return new Size(1, instrumentedMethod.getStackSize());
        }
    }
}
