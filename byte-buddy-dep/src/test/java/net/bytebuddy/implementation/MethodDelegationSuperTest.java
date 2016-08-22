package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.TypeManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.implementation.bind.annotation.Super;
import net.bytebuddy.test.utility.ClassFileExtraction;
import org.junit.Test;

import java.io.Serializable;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationSuperTest extends AbstractImplementationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Test
    public void testSuperInstance() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(Baz.class));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.qux(), is((Object) (FOO + QUX)));
    }

    @Test
    public void testSuperInterface() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(FooBar.class));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.qux(), is((Object) (FOO + QUX)));
    }

    @Test
    public void testSuperInstanceUnsafe() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(QuxBaz.class));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.qux(), is((Object) (FOO + QUX)));
    }

    @Test
    public void testBridgeMethodResolution() throws Exception {
        DynamicType.Loaded<Bar> loaded = implement(Bar.class, MethodDelegation.to(GenericBaz.class));
        Bar instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.qux(BAR), is(BAR + QUX));
    }

    @Test(expected = AbstractMethodError.class)
    public void testSuperCallOnAbstractMethod() throws Exception {
        DynamicType.Loaded<FooBarQuxBaz> loaded = implement(FooBarQuxBaz.class, MethodDelegation.to(FooBar.class));
        loaded.getLoaded().getDeclaredConstructor().newInstance().qux();
    }

    @Test
    public void testSerializableProxy() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(SerializationCheck.class));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.qux(), is((Object) (FOO + QUX)));
    }

    @Test
    public void testTargetTypeProxy() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(TargetTypeTest.class));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.qux(), is((Object) (FOO + QUX)));
    }

    @Test
    public void testExplicitTypeProxy() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(ExplicitTypeTest.class));
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.qux(), is((Object) (FOO + QUX)));
    }

    @Test
    public void testFinalType() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader(null,
                ClassFileExtraction.of(SimpleInterceptor.class),
                null,
                ByteArrayClassLoader.PersistenceHandler.LATENT,
                PackageDefinitionStrategy.NoOp.INSTANCE);
        Class<?> type = new ByteBuddy()
                .rebase(FinalType.class)
                .modifiers(TypeManifestation.PLAIN, Visibility.PUBLIC)
                .method(named(FOO)).intercept(ExceptionMethod.throwing(RuntimeException.class))
                .method(named(BAR)).intercept(MethodDelegation.to(SimpleInterceptor.class))
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    public interface Qux {

        Object qux();
    }

    public static class Foo implements Qux {

        @Override
        public Object qux() {
            return FOO;
        }
    }

    public static class Baz {

        public static String baz(@Super Foo foo) {
            assertThat(foo, not(instanceOf(Serializable.class)));
            return foo.qux() + QUX;
        }
    }

    public static class FooBar {

        public static String baz(@Super Qux foo) {
            assertThat(foo, not(instanceOf(Serializable.class)));
            return foo.qux() + QUX;
        }
    }

    public static class QuxBaz {

        public static String baz(@Super(strategy = Super.Instantiation.UNSAFE) Foo foo) {
            assertThat(foo, not(instanceOf(Serializable.class)));
            return foo.qux() + QUX;
        }
    }

    public abstract static class FooBarQuxBaz implements Qux {

        @Override
        public abstract Object qux();
    }

    public static class GenericBase<T> {

        public T qux(T value) {
            return value;
        }
    }

    public static class Bar extends GenericBase<String> {

        @Override
        public String qux(String value) {
            return super.qux(value);
        }
    }

    public static class GenericBaz {

        public static String baz(@Super GenericBase<String> foo, String value) {
            assertThat(foo, not(instanceOf(Serializable.class)));
            return foo.qux(value) + QUX;
        }
    }

    public static class SerializationCheck {

        public static String baz(@Super(serializableProxy = true) Foo foo) {
            assertThat(foo, instanceOf(Serializable.class));
            return foo.qux() + QUX;
        }
    }

    public static class TargetTypeTest {

        public static String baz(@Super(proxyType = TargetType.class) Object proxy) throws Exception {
            assertThat(proxy, instanceOf(Foo.class));
            return Foo.class.getDeclaredMethod(QUX).invoke(proxy) + QUX;
        }
    }

    public static class ExplicitTypeTest {

        public static String baz(@Super(proxyType = Qux.class) Object proxy) throws Exception {
            assertThat(proxy, instanceOf(Qux.class));
            assertThat(proxy, not(instanceOf(Foo.class)));
            return Qux.class.getDeclaredMethod(QUX).invoke(proxy) + QUX;
        }
    }

    public static final class FinalType {

        public Object foo() {
            return FOO;
        }

        public Object bar() {
            return null;
        }
    }

    public static class SimpleInterceptor {

        public static Object intercept(@Super FinalType finalType) {
            return finalType.foo();
        }
    }
}
