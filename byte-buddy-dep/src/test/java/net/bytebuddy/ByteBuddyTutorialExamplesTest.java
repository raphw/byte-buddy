package net.bytebuddy;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.instrumentation.*;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.*;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.PrimitiveTypeAwareAssigner;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.IntegerConstant;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.TextConstant;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.JavaVersionRule;
import net.bytebuddy.utility.PrecompiledTypeClassLoader;
import net.bytebuddy.utility.ToolsJarRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.not;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteBuddyTutorialExamplesTest {

    private static final String DEFAULT_METHOD_INTERFACE = "net.bytebuddy.test.precompiled.SingleDefaultMethodInterface";
    private static final String CONFLICTING_DEFAULT_METHOD_INTERFACE = "net.bytebuddy.test.precompiled.SingleDefaultMethodConflictingInterface";

    @Rule
    public MethodRule java8Rule = new JavaVersionRule(8);

    @Rule
    public MethodRule toolsJarRule = new ToolsJarRule();

    @SuppressWarnings("unused")
    private static void println(String s) {
        /* do nothing */
    }

    @Test
    public void testHelloWorld() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .subclass(Object.class)
                .method(named("toString")).intercept(FixedValue.value("Hello World!"))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.newInstance().toString(), is("Hello World!"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExtensiveExample() throws Exception {
        Class<? extends Comparator> dynamicType = new ByteBuddy()
                .subclass(Comparator.class)
                .method(named("compare")).intercept(MethodDelegation.to(new ComparisonInterceptor()))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.newInstance().compare(3, 1), is(2));
    }

    @Test
    public void testTutorialGettingStartedUnnamed() throws Exception {
        DynamicType.Unloaded<?> dynamicType = new ByteBuddy()
                .subclass(Object.class)
                .make();
        assertThat(dynamicType, notNullValue());
    }

    @Test
    public void testTutorialGettingStartedNamed() throws Exception {
        DynamicType.Unloaded<?> dynamicType = new ByteBuddy()
                .subclass(Object.class)
                .name("example.Type")
                .make();
        assertThat(dynamicType, notNullValue());
    }

    @Test
    public void testTutorialGettingStartedNamingStrategy() throws Exception {
        DynamicType.Unloaded<?> dynamicType = new ByteBuddy()
                .withNamingStrategy(new GettingStartedNamingStrategy())
                .subclass(Object.class)
                .make();
        assertThat(dynamicType, notNullValue());
        Class<?> type = dynamicType.load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        assertThat(type.getName(), is("i.heart.ByteBuddy.Object"));
    }

    @Test
    public void testTutorialGettingStartedClassLoading() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .subclass(Object.class)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType, notNullValue());
    }

    @Test
    @ToolsJarRule.Enforce
    public void testTutorialGettingStartedClassReloading() throws Exception {
        ByteBuddyAgent.installOnOpenJDK();
        FooReloading foo = new FooReloading();
        new ByteBuddy()
                .redefine(BarReloading.class)
                .name(FooReloading.class.getName())
                .make()
                .load(FooReloading.class.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
        assertThat(foo.m(), is("bar"));
        ClassReloadingStrategy.fromInstalledAgent().reset(FooReloading.class);
        assertThat(foo.m(), is("foo"));
    }

    @Test
    public void testFieldsAndMethodsToString() throws Exception {
        String toString = new ByteBuddy()
                .subclass(Object.class)
                .name("example.Type")
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance()
                .toString();
        assertThat(toString, startsWith("example.Type"));
    }

    @Test
    public void testFieldsAndMethodsDetailedMatcher() throws Exception {
        assertThat(new TypeDescription.ForLoadedType(Object.class)
                .getDeclaredMethods()
                .filter(named("toString").and(returns(String.class)).and(takesArguments(0))).size(), is(1));
    }

    @Test
    public void testFieldsAndMethodsMatcherStack() throws Exception {
        Foo foo = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class)).intercept(FixedValue.value("Hello World!"))
                .method(named("foo")).intercept(FixedValue.value("Hello Foo!"))
                .method(named("foo").and(takesArguments(1))).intercept(FixedValue.value("..."))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance();
        assertThat(foo.bar(), is("Hello World!"));
        assertThat(foo.foo(), is("Hello Foo!"));
        assertThat(foo.foo(null), is("..."));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFieldsAndMethodsIllegalAssignment() throws Exception {
        new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class)).intercept(FixedValue.value(0))
                .make();
    }

    @Test
    public void testFieldsAndMethodsMethodDelegation() throws Exception {
        String helloWorld = new ByteBuddy()
                .subclass(Source.class)
                .method(named("hello")).intercept(MethodDelegation.to(Target.class))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance()
                .hello("World");
        assertThat(helloWorld, is("Hello World!"));
    }

    @Test
    public void testFieldsAndMethodsMethodDelegationAlternatives() throws Exception {
        String helloWorld = new ByteBuddy()
                .subclass(Source.class)
                .method(named("hello")).intercept(MethodDelegation.to(Target2.class))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance()
                .hello("World");
        assertThat(helloWorld, is("Hello World!"));
    }

    @Test
    public void testFieldsAndMethodsMethodSuperCall() throws Exception {
        MemoryDatabase loggingDatabase = new ByteBuddy()
                .subclass(MemoryDatabase.class)
                .method(named("load")).intercept(MethodDelegation.to(LoggerInterceptor.class))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance();
        assertThat(loggingDatabase.load("qux"), is(Arrays.asList("qux: foo", "qux: bar")));
    }

    @Test
    public void testFieldsAndMethodsMethodSuperCallExplicit() throws Exception {
        assertThat(new LoggingMemoryDatabase().load("qux"), is(Arrays.asList("qux: foo", "qux: bar")));
    }

    @Test
    @JavaVersionRule.Enforce
    public void testFieldsAndMethodMethodDefaultCall() throws Exception {
        // This test differs from the tutorial by only conditionally expressing the Java 8 types.
        ClassLoader classLoader = new PrecompiledTypeClassLoader(getClass().getClassLoader());
        Object instance = new ByteBuddy(ClassFileVersion.JAVA_V8)
                .subclass(Object.class)
                .implement(classLoader.loadClass(DEFAULT_METHOD_INTERFACE))
                .implement(classLoader.loadClass(CONFLICTING_DEFAULT_METHOD_INTERFACE))
                .method(named("foo")).intercept(DefaultMethodCall.prioritize(classLoader.loadClass(DEFAULT_METHOD_INTERFACE)))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance();
        Method method = instance.getClass().getMethod("foo");
        assertThat(method.invoke(instance), is((Object) "foo"));
    }

    @Test
    public void testFieldsAndMethodsSuper() throws Exception {
        MemoryDatabase loggingDatabase = new ByteBuddy()
                .subclass(MemoryDatabase.class)
                .method(named("load")).intercept(MethodDelegation.to(ChangingLoggerInterceptor.class))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance();
        assertThat(loggingDatabase.load("qux"), is(Arrays.asList("qux (logged access): foo", "qux (logged access): bar")));
    }

    @Test
    public void testFieldsAndMethodsRuntimeType() throws Exception {
        Loop trivialGetterBean = new ByteBuddy()
                .subclass(Loop.class)
                .method(isDeclaredBy(Loop.class)).intercept(MethodDelegation.to(Interceptor.class))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance();
        assertThat(trivialGetterBean.loop(42), is(42));
        assertThat(trivialGetterBean.loop("foo"), is("foo"));
    }

    @Test
    public void testFieldsAndMethodsForwarding() throws Exception {
        MemoryDatabase memoryDatabase = new MemoryDatabase();
        MemoryDatabase loggingDatabase = new ByteBuddy()
                .subclass(MemoryDatabase.class)
                .method(named("load")).intercept(MethodDelegation
                        .to(new ForwardingLoggerInterceptor(memoryDatabase))
                        .appendParameterBinder(Pipe.Binder.install(Forwarder.class)))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance();
        assertThat(loggingDatabase.load("qux"), is(Arrays.asList("qux: foo", "qux: bar")));
    }

    @Test
    public void testFieldsAndMethodsFieldAccess() throws Exception {
        ByteBuddy byteBuddy = new ByteBuddy();
        Class<? extends UserType> dynamicUserType = byteBuddy
                .subclass(UserType.class)
                .method(not(isDeclaredBy(Object.class))).intercept(MethodDelegation.toInstanceField(Interceptor2.class, "interceptor"))
                .implement(InterceptionAccessor.class).intercept(FieldAccessor.ofBeanProperty())
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        InstanceCreator factory = byteBuddy
                .subclass(InstanceCreator.class)
                .method(not(isDeclaredBy(Object.class))).intercept(MethodDelegation.toConstructor(dynamicUserType))
                .make()
                .load(dynamicUserType.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded().newInstance();
        UserType userType = (UserType) factory.makeInstance();
        ((InterceptionAccessor) userType).setInterceptor(new HelloWorldInterceptor());
        assertThat(userType.doSomething(), is("Hello World!"));
    }

    @Test
    public void testAttributesAndAnnotationForClass() throws Exception {
        assertThat(new ByteBuddy()
                .subclass(Object.class)
                .annotateType(new RuntimeDefinitionImpl())
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .isAnnotationPresent(RuntimeDefinition.class), is(true));
    }

    @Test
    public void testAttributesAndAnnotationForMethodAndField() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .subclass(Object.class)
                .method(named("toString"))
                .intercept(SuperMethodCall.INSTANCE)
                .annotateMethod(new RuntimeDefinitionImpl())
                .defineField("foo", Object.class)
                .annotateField(new RuntimeDefinitionImpl())
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredMethod("toString").isAnnotationPresent(RuntimeDefinition.class), is(true));
        assertThat(dynamicType.getDeclaredField("foo").isAnnotationPresent(RuntimeDefinition.class), is(true));
    }

    @Test
    public void testCustomInstrumentationMethodImplementation() throws Exception {
        assertThat(new ByteBuddy()
                .subclass(SumExample.class)
                .method(named("calculate")).intercept(SumInstrumentation.INSTANCE)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance()
                .calculate(), is(60));
    }

    @Test
    public void testCustomInstrumentationAssigner() throws Exception {
        assertThat(new ByteBuddy()
                .subclass(Object.class)
                .method(named("toString"))
                .intercept(FixedValue.value(42)
                        .withAssigner(new PrimitiveTypeAwareAssigner(ToStringAssigner.INSTANCE), false))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance()
                .toString(), is("42"));
    }

    @Test
    public void testCustomInstrumentationDelegationAnnotation() throws Exception {
        assertThat(new ByteBuddy()
                .subclass(Object.class)
                .method(named("toString"))
                .intercept(MethodDelegation.to(ToStringInterceptor.class)
                        .defineParameterBinder(StringValueBinder.INSTANCE))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance()
                .toString(), is("Hello!"));
    }

    public static enum IntegerSum implements StackManipulation {
        INSTANCE;

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            methodVisitor.visitInsn(Opcodes.IADD);
            return new Size(-1, 0);
        }
    }

    public static enum SumMethod implements ByteCodeAppender {
        INSTANCE;

        @Override
        public boolean appendsCode() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor,
                          Instrumentation.Context instrumentationContext,
                          MethodDescription instrumentedMethod) {
            if (!instrumentedMethod.getReturnType().represents(int.class)) {
                throw new IllegalArgumentException(instrumentedMethod + " must return int");
            }
            StackManipulation.Size operandStackSize = new StackManipulation.Compound(
                    IntegerConstant.forValue(10),
                    IntegerConstant.forValue(50),
                    IntegerSum.INSTANCE,
                    MethodReturn.INTEGER
            ).apply(methodVisitor, instrumentationContext);
            return new Size(operandStackSize.getMaximalSize(), instrumentedMethod.getStackSize());
        }
    }

    public static enum SumInstrumentation implements Instrumentation {
        INSTANCE;

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(Target instrumentationTarget) {
            return SumMethod.INSTANCE;
        }
    }

    public static enum ToStringAssigner implements Assigner {
        INSTANCE;

        @Override
        public StackManipulation assign(TypeDescription sourceType,
                                        TypeDescription targetType,
                                        boolean considerRuntimeType) {
            if (!sourceType.isPrimitive() && targetType.represents(String.class)) {
                MethodDescription toStringMethod = new TypeDescription.ForLoadedType(Object.class)
                        .getDeclaredMethods()
                        .filter(named("toString"))
                        .getOnly();
                return MethodInvocation.invoke(toStringMethod).virtual(sourceType);
            } else {
                return StackManipulation.Illegal.INSTANCE;
            }
        }
    }

    public static enum StringValueBinder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<StringValue> {

        INSTANCE;

        @Override
        public Class<StringValue> getHandledType() {
            return StringValue.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<StringValue> annotation,
                                                               int targetParameterIndex,
                                                               MethodDescription source,
                                                               MethodDescription target,
                                                               Instrumentation.Target instrumentationTarget,
                                                               Assigner assigner) {
            if (!target.getParameterTypes().get(targetParameterIndex).represents(String.class)) {
                throw new IllegalStateException(target + " makes wrong use of StringValue");
            }
            StackManipulation constant = new TextConstant(annotation.loadSilent().value()); // TODO: Update documentation.
            return new MethodDelegationBinder.ParameterBinding.Anonymous(constant);
        }
    }

    public interface Forwarder<T, S> {

        T to(S target);
    }

    @Retention(RetentionPolicy.RUNTIME)
    private static @interface Unsafe {
        /* empty */
    }

    @Retention(RetentionPolicy.RUNTIME)
    private static @interface Secured {
        /* empty */
    }

    @SuppressWarnings("unused")
    public static interface Interceptor2 {

        String doSomethingElse();
    }

    @SuppressWarnings("unused")
    public static interface InterceptionAccessor {

        Interceptor2 getInterceptor();

        void setInterceptor(Interceptor2 interceptor);
    }

    @SuppressWarnings("unused")
    public static interface InstanceCreator {
        Object makeInstance();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public static @interface RuntimeDefinition {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public static @interface StringValue {

        String value();
    }

    public static class ComparisonInterceptor {

        public int intercept(Object first, Object second) {
            return first.hashCode() - second.hashCode();
        }
    }

    @SuppressWarnings("unchecked")
    public static class FooReloading {

        public String m() {
            return "foo";
        }
    }

    @SuppressWarnings("unchecked")
    public static class BarReloading {

        public String m() {
            return "bar";
        }
    }

    @SuppressWarnings("unused")
    public static class Account {

        private int amount = 100;

        @Unsafe
        public String transfer(int amount, String recipient) {
            this.amount -= amount;
            return "transferred $" + amount + " to " + recipient;
        }
    }

    @SuppressWarnings("unused")
    public static class Bank {

        public static String obfuscate(@Argument(1) String recipient,
                                       @Argument(0) Integer amount,
                                       @Super Account zuper) {
            //System.out.println("Transfer " + amount + " to " + recipient);
            return zuper.transfer(amount, recipient.substring(0, 3) + "XXX") + " (obfuscated)";
        }
    }

    private static class GettingStartedNamingStrategy implements NamingStrategy {

        @Override
        public String name(UnnamedType unnamedType) {
            return "i.heart.ByteBuddy." + unnamedType.getSuperClass().getSimpleName();
        }
    }

    @SuppressWarnings("unused")
    public static class Foo {

        public String foo() {
            return null;
        }

        public String foo(Object o) {
            return null;
        }

        public String bar() {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public static class Source {
        public String hello(String name) {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public static class Target {
        public static String hello(String name) {
            return "Hello " + name + "!";
        }
    }

    @SuppressWarnings("unused")
    public static class Target2 {
        public static String intercept(String name) {
            return "Hello " + name + "!";
        }

        public static String intercept(int i) {
            return Integer.toString(i);
        }

        public static String intercept(Object o) {
            return o.toString();
        }
    }

    public static class MemoryDatabase {
        public List<String> load(String info) {
            return Arrays.asList(info + ": foo", info + ": bar");
        }
    }

    public static class LoggerInterceptor {
        public static List<String> log(@SuperCall Callable<List<String>> zuper) throws Exception {
            println("Calling database");
            try {
                return zuper.call();
            } finally {
                println("Returned from database");
            }
        }
    }

    @SuppressWarnings("unused")
    public static class ChangingLoggerInterceptor {
        public static List<String> log(@Super MemoryDatabase zuper, String info) {
            println("Calling database");
            try {
                return zuper.load(info + " (logged access)");
            } finally {
                println("Returned from database");
            }
        }
    }

    @SuppressWarnings("unused")
    public static class Loop {

        public String loop(String value) {
            return value;
        }

        public int loop(int value) {
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static class Interceptor {

        @RuntimeType
        public static Object intercept(@RuntimeType Object value) {
            println("Invoked method with: " + value);
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static class UserType {

        public String doSomething() {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public static class HelloWorldInterceptor implements Interceptor2 {

        @Override
        public String doSomethingElse() {
            return "Hello World!";
        }
    }

    private static class RuntimeDefinitionImpl implements RuntimeDefinition {

        @Override
        public Class<? extends Annotation> annotationType() {
            return RuntimeDefinition.class;
        }
    }

    public static abstract class SumExample {

        public abstract int calculate();
    }

    public static class ToStringInterceptor {

        public static String makeString(@StringValue("Hello!") String value) {
            return value;
        }
    }

    public class ForwardingLoggerInterceptor {

        private final MemoryDatabase memoryDatabase;

        public ForwardingLoggerInterceptor(MemoryDatabase memoryDatabase) {
            this.memoryDatabase = memoryDatabase;
        }

        public List<String> log(@Pipe Forwarder<List<String>, MemoryDatabase> pipe) {
            println("Calling database");
            try {
                return pipe.to(memoryDatabase);
            } finally {
                println("Returned from database");
            }
        }
    }

    @SuppressWarnings("unchecked")
    class LoggingMemoryDatabase extends MemoryDatabase {

        @Override
        public List<String> load(String info) {
            try {
                return LoggerInterceptor.log(new LoadMethodSuperCall(info));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private class LoadMethodSuperCall implements Callable {

            private final String info;

            private LoadMethodSuperCall(String info) {
                this.info = info;
            }

            @Override
            public Object call() throws Exception {
                return LoggingMemoryDatabase.super.load(info);
            }
        }
    }
}
