package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.JavaConstantValue;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.JavaConstant;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.MatcherAssert.assertThat;

public class AdviceCustomAnnotationResolverTest {

    /**
     * A custom annotation
     */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Constant {
    }

    /**
     * Resolver for above annotation that binds a string value to target parameter on Advice.
     */
    public static class ConstantResolver implements Advice.OffsetMapping {
        private final String value;

        public ConstantResolver(String value) {
            this.value = value;
        }

        @Nonnull
        @Override
        public Target resolve(@Nonnull TypeDescription instrumentedType,
                              @Nonnull MethodDescription instrumentedMethod,
                              @Nonnull Assigner assigner,
                              @Nonnull Advice.ArgumentHandler argumentHandler,
                              @Nonnull Sort sort) {
            return new Target.ForStackManipulation(new JavaConstantValue(JavaConstant.Simple.ofLoaded(value)));
        }
    }

    public static class TargetClass {

        public static String enter(String value) {
            return value;
        }

        public static String exit() {
            return null;
        }
    }

    /**
     * Advice class that modifies the target method's input parameter by given value
     */
    public static class BeforeAdvice {
        @Advice.OnMethodEnter
        static void enter(@Constant String value,
                          @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args) {

            // Change the input arguments by given value
            args = new Object[]{value};
        }
    }

    /**
     * Advice class that modifies the target method's returning by given value
     */
    public static class AfterAdvice {
        @Advice.OnMethodExit
        static void exit(@Constant String value,
                         @Advice.Return(readOnly = false) String returned) {
            returned = value;
        }
    }

    /**
     * A demonstration of visitor that extracts binded value during instrumentation.
     */
    static class ConstExtractor implements AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper {
        private String value;

        @Override
        public MethodVisitor wrap(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  MethodVisitor methodVisitor,
                                  Implementation.Context implementationContext,
                                  TypePool typePool,
                                  int writerFlags,
                                  int readerFlags) {

            if (methodVisitor instanceof Advice.AdviceVisitor) {
                // It does not matter if both the Enter and Exit method define the Const annotation,
                // in such case, the Const annotations actually are binded to a same instance.
                extractConst(((Advice.AdviceVisitor) methodVisitor).getMethodEnter());
                extractConst(((Advice.AdviceVisitor) methodVisitor).getMethodExit());
            }
            return methodVisitor;
        }

        private void extractConst(Advice.Dispatcher.Bound bound) {
            if (bound instanceof Advice.Dispatcher.Inlining.Resolved.AdviceMethodInliner) {
                Map<Integer, Advice.OffsetMapping> mappings = ((Advice.Dispatcher.Inlining.Resolved.AdviceMethodInliner) bound).getOffsetMappings();

                for (Advice.OffsetMapping mapping : mappings.values()) {
                    if (mapping instanceof ConstantResolver) {
                        this.value = ((ConstantResolver) mapping).value;
                    }
                }
            }
        }
    }

    @Test
    public void testResolverOnEnter() throws Exception {
        String expected = "123";

        ConstExtractor constExtractor = new ConstExtractor();

        // Create an Advice object with a const value
        Advice beforeAdvice = Advice.withCustomMapping()
                              .bind(Constant.class, new ConstantResolver(expected))
                              .to(BeforeAdvice.class);

        // Apply above advice to target class and generates a new class
        Class<?> type = new ByteBuddy()
                .redefine(TargetClass.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().invokable(named("enter"), beforeAdvice, constExtractor))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        // Call 'enter' method to verify
        // Since 'enter' has been instrumented, it SHOULD return the modified value instead of passed value
        assertThat(type.getDeclaredMethod("enter", String.class).invoke(null, "original-argument"),
                   CoreMatchers.<Object>is(expected));

        // Check if the extractor correctly extracts
        assertThat(constExtractor.value, CoreMatchers.<Object>is(expected));
    }

    @Test
    public void testResolverOnExit() throws Exception {
        String expected = "123";

        ConstExtractor constExtractor = new ConstExtractor();

        // Create an Advice object with a const value
        Advice afterAdvice = Advice.withCustomMapping()
                              .bind(Constant.class, new ConstantResolver(expected))
                              .to(AfterAdvice.class);

        // Apply above advice to target class and generates a new class
        Class<?> type = new ByteBuddy()
                .redefine(TargetClass.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().invokable(named("exit"), afterAdvice, constExtractor))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        // Call 'exit' method to verify
        // Since 'exit' has been instrumented, it SHOULD return the modified value
        assertThat(type.getDeclaredMethod("exit").invoke(null), CoreMatchers.<Object>is(expected));

        // Check if the extractor correctly extracts
        assertThat(constExtractor.value, CoreMatchers.<Object>is(expected));
    }
}
