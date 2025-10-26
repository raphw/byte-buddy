package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.attribute.AnnotationRetention;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.packaging.EmptyType;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.visibility.PackageAnnotation;
import net.bytebuddy.test.visibility.Sample;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class RebaseDynamicTypeBuilderTest extends AbstractDynamicTypeBuilderForInliningTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final String DEFAULT_METHOD_INTERFACE = "net.bytebuddy.test.precompiled.v8.SingleDefaultMethodInterface";

    protected DynamicType.Builder<?> createPlain() {
        return create(Foo.class);
    }

    protected DynamicType.Builder<?> createPlainEmpty() {
        return create(EmptyType.class);
    }

    protected DynamicType.Builder<?> createDisabledContext() {
        return new ByteBuddy().with(Implementation.Context.Disabled.Factory.INSTANCE).rebase(Foo.class);
    }

    protected DynamicType.Builder<?> createDisabledRetention(Class<?> annotatedClass) {
        return new ByteBuddy().with(AnnotationRetention.DISABLED).rebase(annotatedClass);
    }

    protected DynamicType.Builder<?> create(Class<?> type) {
        return new ByteBuddy().rebase(type);
    }

    protected DynamicType.Builder<?> create(TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        return new ByteBuddy().rebase(typeDescription, classFileLocator);
    }

    protected DynamicType.Builder<?> createPlainWithoutValidation() {
        return new ByteBuddy().with(TypeValidation.DISABLED).redefine(Foo.class);
    }

    @Test
    public void testConstructorRetentionNoAuxiliaryType() throws Exception {
        DynamicType.Unloaded<?> dynamicType = new ByteBuddy()
                .rebase(Bar.class)
                .make();
        assertThat(dynamicType.getAuxiliaryTypes().size(), is(0));
        Class<?> type = dynamicType.load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredMethods().length, is(0));
        Field field = type.getDeclaredField(BAR);
        assertThat(field.get(type.getDeclaredConstructor(String.class).newInstance(FOO)), is((Object) FOO));
    }

    @Test
    public void testConstructorRebaseSingleAuxiliaryType() throws Exception {
        DynamicType.Unloaded<?> dynamicType = new ByteBuddy()
                .rebase(Bar.class)
                .constructor(any()).intercept(SuperMethodCall.INSTANCE)
                .make();
        assertThat(dynamicType.getAuxiliaryTypes().size(), is(1));
        Class<?> type = dynamicType.load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        assertThat(type.getDeclaredConstructors().length, is(2));
        assertThat(type.getDeclaredMethods().length, is(0));
        Field field = type.getDeclaredField(BAR);
        assertThat(field.get(type.getDeclaredConstructor(String.class).newInstance(FOO)), is((Object) FOO));
    }

    @Test
    public void testConstructorRebaseSingleAuxiliaryTypeStackMapAdjustment() throws Exception {
        DynamicType.Unloaded<?> dynamicType = new ByteBuddy()
                .rebase(Foobar.class)
                .constructor(any()).intercept(SuperMethodCall.INSTANCE)
                .make();
        assertThat(dynamicType.getAuxiliaryTypes().size(), is(1));
        Class<?> type = dynamicType.load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        assertThat(type.getDeclaredConstructors().length, is(2));
        assertThat(type.getDeclaredMethods().length, is(0));
        Field field = type.getDeclaredField(BAR);
        assertThat(field.get(type.getDeclaredConstructor(String.class).newInstance(FOO)), is((Object) BAR));
    }

    @Test
    public void testConstructorRebaseSingleAuxiliaryTypeStackMapAdjustmentExpanded() throws Exception {
        DynamicType.Unloaded<?> dynamicType = new ByteBuddy()
                .rebase(Foobar.class)
                .constructor(any()).intercept(SuperMethodCall.INSTANCE)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().readerFlags(ClassReader.EXPAND_FRAMES))
                .make();
        assertThat(dynamicType.getAuxiliaryTypes().size(), is(1));
        Class<?> type = dynamicType.load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        assertThat(type.getDeclaredConstructors().length, is(2));
        assertThat(type.getDeclaredMethods().length, is(0));
        Field field = type.getDeclaredField(BAR);
        assertThat(field.get(type.getDeclaredConstructor(String.class).newInstance(FOO)), is((Object) BAR));
    }

    @Test
    public void testMethodRebase() throws Exception {
        DynamicType.Unloaded<?> dynamicType = new ByteBuddy()
                .rebase(Qux.class)
                .method(named(BAR)).intercept(StubMethod.INSTANCE)
                .make();
        assertThat(dynamicType.getAuxiliaryTypes().size(), is(0));
        Class<?> type = dynamicType.load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredMethods().length, is(3));
        assertThat(type.getDeclaredMethod(FOO).invoke(null), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
        assertThat(type.getDeclaredMethod(BAR).invoke(null), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(null), is((Object) FOO));
    }

    @Test
    public void testPackageRebasement() throws Exception {
        Class<?> packageType = new ByteBuddy()
                .rebase(Sample.class.getPackage(), ClassFileLocator.ForClassLoader.of(getClass().getClassLoader()))
                .annotateType(AnnotationDescription.Builder.ofType(Baz.class).build())
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(packageType.getSimpleName(), CoreMatchers.is(PackageDescription.PACKAGE_CLASS_NAME));
        assertThat(packageType.getName(), CoreMatchers.is(Sample.class.getPackage().getName() + "." + PackageDescription.PACKAGE_CLASS_NAME));
        assertThat(packageType.getModifiers(), CoreMatchers.is(PackageDescription.PACKAGE_MODIFIERS));
        assertThat(packageType.getDeclaredFields().length, CoreMatchers.is(0));
        assertThat(packageType.getDeclaredMethods().length, CoreMatchers.is(0));
        assertThat(packageType.getDeclaredAnnotations().length, CoreMatchers.is(2));
        assertThat(packageType.getAnnotation(PackageAnnotation.class), notNullValue(PackageAnnotation.class));
        assertThat(packageType.getAnnotation(Baz.class), notNullValue(Baz.class));
    }

    @Test
    public void testRebaseOfRenamedType() throws Exception {
        Class<?> rebased = new ByteBuddy()
                .rebase(Sample.class)
                .name(Sample.class.getName() + FOO)
                .constructor(ElementMatchers.any())
                .intercept(SuperMethodCall.INSTANCE)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(rebased.getName(), is(Sample.class.getName() + FOO));
        assertThat(rebased.getDeclaredConstructors().length, is(2));
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotRebaseDefinedMethod() throws Exception {
        new ByteBuddy()
                .rebase(Foo.class)
                .defineMethod(FOO, void.class).intercept(SuperMethodCall.INSTANCE)
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testDefaultInterfaceSubInterface() throws Exception {
        Class<?> interfaceType = Class.forName(DEFAULT_METHOD_INTERFACE);
        Class<?> dynamicInterfaceType = new ByteBuddy()
                .rebase(interfaceType)
                .method(named(FOO)).intercept(MethodDelegation.to(InterfaceOverrideInterceptor.class))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        Class<?> dynamicClassType = new ByteBuddy()
                .subclass(dynamicInterfaceType)
                .make()
                .load(dynamicInterfaceType.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicClassType.getMethod(FOO).invoke(dynamicClassType.getDeclaredConstructor().newInstance()), is((Object) (FOO + BAR)));
        assertThat(dynamicInterfaceType.getDeclaredMethods().length, is(3));
        assertThat(dynamicClassType.getDeclaredMethods().length, is(0));
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Baz {
        /* empty */
    }

    public static class Bar {

        public final String bar;

        public Bar(String bar) {
            this.bar = bar;
        }
    }

    public static class Qux {

        public static String foo;

        public static String foo() {
            try {
                return foo;
            } finally {
                foo = FOO;
            }
        }

        public static String bar() {
            try {
                return foo;
            } finally {
                foo = FOO;
            }
        }
    }

    public static class Foobar {

        public final String bar;

        public Foobar(String foo) {
            if (foo == null) {
                throw new AssertionError();
            }
            String value = FOO;
            if (!value.equals(BAR)) {
                value = BAR;
            }
            bar = value;
        }
    }
}
