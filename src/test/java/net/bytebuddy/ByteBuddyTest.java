package net.bytebuddy;

import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.instrumentation.FixedValue;
import net.bytebuddy.instrumentation.MethodDelegation;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Super;
import org.junit.Test;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.isAnnotatedBy;
import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteBuddyTest {

    @Test
    public void testHelloWorld() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .subclass(Object.class)
                .method(named("toString"))
                .intercept(FixedValue.value("Hello World!"))
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
}
