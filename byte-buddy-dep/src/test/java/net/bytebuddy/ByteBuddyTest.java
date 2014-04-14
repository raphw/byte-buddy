package net.bytebuddy;

import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.FixedValue;
import net.bytebuddy.instrumentation.MethodDelegation;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Super;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.SuperCall;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Test;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteBuddyTest {

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

    @Retention(RetentionPolicy.RUNTIME)
    private static @interface Unsafe {
        /* empty */
    }

    @Retention(RetentionPolicy.RUNTIME)
    private static @interface Secured {
        /* empty */
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

    @Test
    public void testExtensiveExample() throws Exception {
        Class<? extends Account> dynamicType = new ByteBuddy()
                .subclass(Account.class)
                .implement(Serializable.class)
                .name("BankAccount")
                .annotateType(new Secured() {
                    @Override
                    public Class<? extends Annotation> annotationType() {
                        return Secured.class;
                    }
                })
                .method(isAnnotatedBy(Unsafe.class)).intercept(MethodDelegation.to(Bank.class))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getName(), is("BankAccount"));
        assertThat(Serializable.class.isAssignableFrom(dynamicType), is(true));
        assertThat(dynamicType.isAnnotationPresent(Secured.class), is(true));
        assertThat(dynamicType.newInstance().transfer(26, "123456"), is("transferred $26 to 123XXX (obfuscated)"));
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

    private static class GettingStartedNamingStrategy implements NamingStrategy {

        @Override
        public String getName(UnnamedType unnamedType) {
            return "i.heart.ByteBuddy." + unnamedType.getSuperClass().getSimpleName();
        }
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

    @SuppressWarnings("unchecked")
    class LoggingMemoryDatabase extends MemoryDatabase {

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

        @Override
        public List<String> load(String info) {
            try {
                return LoggerInterceptor.log(new LoadMethodSuperCall(info));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void testFieldsAndMethodsMethodSuperCallExplicit() throws Exception {
        assertThat(new LoggingMemoryDatabase().load("qux"), is(Arrays.asList("qux: foo", "qux: bar")));
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

    @Test
    public void testFieldsAndMethodsMethodSuper() throws Exception {
        MemoryDatabase loggingDatabase = new ByteBuddy()
                .subclass(MemoryDatabase.class)
                .method(named("load")).intercept(MethodDelegation.to(ChangingLoggerInterceptor.class))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .newInstance();
        assertThat(loggingDatabase.load("qux"), is(Arrays.asList("qux (logged access): foo", "qux (logged access): bar")));
    }

    @SuppressWarnings("unused")
    private static void println(String s) {
        /* do nothing */
    }
}
