package net.bytebuddy;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.*;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.assign.primitive.PrimitiveTypeAwareAssigner;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.utility.AgentAttachmentRule;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class ByteBuddyTutorialExamplesTest {

    private static final String DEFAULT_METHOD_INTERFACE = "net.bytebuddy.test.precompiled.SingleDefaultMethodInterface";

    private static final String CONFLICTING_DEFAULT_METHOD_INTERFACE = "net.bytebuddy.test.precompiled.SingleDefaultMethodConflictingInterface";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Rule
    public MethodRule agentAttachmentRule = new AgentAttachmentRule();

    // Other than in the tutorial, the tests use a wrapper strategy for class loading to retain the test's repeatability.

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
        assertThat(dynamicType.getDeclaredConstructor().newInstance().toString(), is("Hello World!"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExtensiveExample() throws Exception {
        Class<? extends Function> dynamicType = new ByteBuddy()
                .subclass(Function.class)
                .method(named("apply"))
                .intercept(MethodDelegation.to(new GreetingInterceptor()))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredConstructor().newInstance().apply("Byte Buddy"), is((Object) "Hello from Byte Buddy"));
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
                .with(new GettingStartedNamingStrategy())
                .subclass(Object.class)
                .make();
        assertThat(dynamicType, notNullValue());
        Class<?> type = dynamicType.load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        assertThat(type.getName(), is("i.love.ByteBuddy.Object"));
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
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    public void testTutorialGettingStartedClassReloading() throws Exception {
        ByteBuddyAgent.install();
        FooReloading foo = new FooReloading();
        try {
            new ByteBuddy()
                    .redefine(BarReloading.class)
                    .name(FooReloading.class.getName())
                    .make()
                    .load(FooReloading.class.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
            assertThat(foo.m(), is("bar"));
        } finally {
            ClassReloadingStrategy.fromInstalledAgent().reset(FooReloading.class); // Assure repeatability.
        }
        assertThat(foo.m(), is("foo"));
    }

    @Test
    public void testTutorialGettingStartedTypePool() throws Exception {
        TypePool typePool = TypePool.Default.ofClassPath();
        ClassLoader classLoader = new URLClassLoader(new URL[0], null); // Assure repeatability.
        new ByteBuddy().redefine(typePool.describe(UnloadedBar.class.getName()).resolve(),
                ClassFileLocator.ForClassLoader.ofClassPath())
                .defineField("qux", String.class)
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.INJECTION);
        assertThat(classLoader.loadClass(UnloadedBar.class.getName()).getDeclaredField("qux"), notNullValue(java.lang.reflect.Field.class));
    }

    @Test
    public void testTutorialGettingStartedJavaAgent() throws Exception {
        new AgentBuilder.Default().type(isAnnotatedWith(Rebase.class)).transform(new AgentBuilder.Transformer() {
            @Override
            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
                return builder.method(named("toString")).intercept(FixedValue.value("transformed"));
            }
        }).installOn(mock(Instrumentation.class));
    }

    @Test
    public void testFieldsAndMethodsToString() throws Exception {
        String toString = new ByteBuddy()
                .subclass(Object.class)
                .name("example.Type")
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()
                .toString();
        assertThat(toString, startsWith("example.Type"));
    }

    @Test
    public void testFieldsAndMethodsDetailedMatcher() throws Exception {
        assertThat(TypeDescription.OBJECT
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
                .getDeclaredConstructor()
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
                .getDeclaredConstructor()
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
                .getDeclaredConstructor()
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
                .getDeclaredConstructor()
                .newInstance();
        assertThat(loggingDatabase.load("qux"), is(Arrays.asList("qux: foo", "qux: bar")));
    }

    @Test
    public void testFieldsAndMethodsMethodSuperCallExplicit() throws Exception {
        assertThat(new LoggingMemoryDatabase().load("qux"), is(Arrays.asList("qux: foo", "qux: bar")));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testFieldsAndMethodsMethodDefaultCall() throws Exception {
        // This test differs from the tutorial by only conditionally expressing the Java 8 types.
        Object instance = new ByteBuddy(ClassFileVersion.JAVA_V8)
                .subclass(Object.class)
                .implement(Class.forName(DEFAULT_METHOD_INTERFACE))
                .implement(Class.forName(CONFLICTING_DEFAULT_METHOD_INTERFACE))
                .method(named("foo")).intercept(DefaultMethodCall.prioritize(Class.forName(DEFAULT_METHOD_INTERFACE)))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
        Method method = instance.getClass().getMethod("foo");
        assertThat(method.invoke(instance), is((Object) "foo"));
    }

    @Test
    public void testFieldsAndMethodsExplicitMethodCall() throws Exception {
        Object object = new ByteBuddy()
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .defineConstructor(Visibility.PUBLIC).withParameters(int.class)
                .intercept(MethodCall.invoke(Object.class.getDeclaredConstructor()))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredConstructor(int.class)
                .newInstance(42);
        assertThat(object.getClass(), CoreMatchers.not(CoreMatchers.<Class<?>>is(Object.class)));
    }

    @Test
    public void testFieldsAndMethodsSuper() throws Exception {
        MemoryDatabase loggingDatabase = new ByteBuddy()
                .subclass(MemoryDatabase.class)
                .method(named("load")).intercept(MethodDelegation.to(ChangingLoggerInterceptor.class))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredConstructor()
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
                .getDeclaredConstructor()
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
                .getDeclaredConstructor()
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
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
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
    public void testCustomImplementationMethodImplementation() throws Exception {
        assertThat(new ByteBuddy()
                .subclass(SumExample.class)
                .method(named("calculate")).intercept(SumImplementation.INSTANCE)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()
                .calculate(), is(60));
    }

    @Test
    public void testCustomImplementationAssigner() throws Exception {
        assertThat(new ByteBuddy()
                .subclass(Object.class)
                .method(named("toString"))
                .intercept(FixedValue.value(42).withAssigner(new PrimitiveTypeAwareAssigner(ToStringAssigner.INSTANCE), Assigner.Typing.STATIC))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()
                .toString(), is("42"));
    }

    @Test
    public void testCustomImplementationDelegationAnnotation() throws Exception {
        assertThat(new ByteBuddy()
                .subclass(Object.class)
                .method(named("toString"))
                .intercept(MethodDelegation.to(ToStringInterceptor.class)
                        .defineParameterBinder(StringValueBinder.INSTANCE))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()
                .toString(), is("Hello!"));
    }

    public enum IntegerSum implements StackManipulation {

        INSTANCE;

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            methodVisitor.visitInsn(Opcodes.IADD);
            return new Size(-1, 0);
        }
    }

    public enum SumMethod implements ByteCodeAppender {

        INSTANCE;

        @Override
        public Size apply(MethodVisitor methodVisitor,
                          Implementation.Context implementationContext,
                          MethodDescription instrumentedMethod) {
            if (!instrumentedMethod.getReturnType().asErasure().represents(int.class)) {
                throw new IllegalArgumentException(instrumentedMethod + " must return int");
            }
            StackManipulation.Size operandStackSize = new StackManipulation.Compound(
                    IntegerConstant.forValue(10),
                    IntegerConstant.forValue(50),
                    IntegerSum.INSTANCE,
                    MethodReturn.INTEGER
            ).apply(methodVisitor, implementationContext);
            return new Size(operandStackSize.getMaximalSize(), instrumentedMethod.getStackSize());
        }
    }

    public enum SumImplementation implements Implementation {
        INSTANCE;

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return SumMethod.INSTANCE;
        }
    }

    public enum ToStringAssigner implements Assigner {
        INSTANCE;

        @Override
        public StackManipulation assign(TypeDescription.Generic source, TypeDescription.Generic target, Typing typing) {
            if (!source.isPrimitive() && target.represents(String.class)) {
                MethodDescription toStringMethod = TypeDescription.OBJECT.getDeclaredMethods()
                        .filter(named("toString"))
                        .getOnly();
                return MethodInvocation.invoke(toStringMethod).virtual(source.asErasure());
            } else {
                return StackManipulation.Illegal.INSTANCE;
            }
        }
    }

    public enum StringValueBinder implements TargetMethodAnnotationDrivenBinder.ParameterBinder<StringValue> {

        INSTANCE;

        @Override
        public Class<StringValue> getHandledType() {
            return StringValue.class;
        }

        @Override
        public MethodDelegationBinder.ParameterBinding<?> bind(AnnotationDescription.Loadable<StringValue> annotation,
                                                               MethodDescription source,
                                                               ParameterDescription target,
                                                               Implementation.Target implementationTarget,
                                                               Assigner assigner) {
            if (!target.getType().asErasure().represents(String.class)) {
                throw new IllegalStateException(target + " makes wrong use of StringValue");
            }
            StackManipulation constant = new TextConstant(annotation.loadSilent().value());
            return new MethodDelegationBinder.ParameterBinding.Anonymous(constant);
        }
    }

    public interface Forwarder<T, S> {

        T to(S target);
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface Unsafe {
        /* empty */
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface Secured {
        /* empty */
    }

    @SuppressWarnings("unused")
    public interface Interceptor2 {

        String doSomethingElse();
    }

    @SuppressWarnings("unused")
    public interface InterceptionAccessor {

        Interceptor2 getInterceptor();

        void setInterceptor(Interceptor2 interceptor);
    }

    @SuppressWarnings("unused")
    public interface InstanceCreator {

        Object makeInstance();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface RuntimeDefinition {

    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface StringValue {

        String value();
    }

    private @interface Rebase {

    }

    public interface Function {

        Object apply(Object arg);
    }

    public static class GreetingInterceptor {

        public Object greet(Object argument) {
            return "Hello from " + argument;
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

    private static class GettingStartedNamingStrategy extends NamingStrategy.AbstractBase {

        @Override
        protected String name(TypeDescription superClass) {
            return "i.love.ByteBuddy." + superClass.getSimpleName();
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

    public abstract static class SumExample {

        public abstract int calculate();
    }

    public static class ToStringInterceptor {

        public static String makeString(@StringValue("Hello!") String value) {
            return value;
        }
    }

    private static class UnloadedBar {

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
            } catch (Exception exception) {
                throw new RuntimeException(exception);
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
