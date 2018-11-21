package net.bytebuddy.dynamic.scaffold.subclass;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.packaging.NoPackageTypeHolder;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaType;
import org.hamcrest.CoreMatchers;

public class NoPackageSubclassDynamicTypeBuilderTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    /*
     * Executes MethodHandles.privateLookupIn(clazz, MethodHandles.lookup()) using reflection
     * to be able to compile with Java 8 and below.
     */
    private static Object privateLookupIn(Class<?> clazz)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
        return JavaType.METHOD_HANDLES.load()
                .getMethod( "privateLookupIn", Class.class, JavaType.METHOD_HANDLES_LOOKUP.load() )
                .invoke( null, clazz, JavaType.METHOD_HANDLES.load().getMethod( "lookup" ).invoke( null  ) );
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testSubclassUsingLookup() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(NoPackageTypeHolder.TYPE)
                .make()
                .load(
                        NoPackageTypeHolder.TYPE.getClassLoader(),
                        ClassLoadingStrategy.UsingLookup.of(privateLookupIn(NoPackageTypeHolder.TYPE))
                )
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(0));
        assertThat(type.getDeclaredConstructors().length, is(1));
        assertThat(type.getDeclaredConstructor(), notNullValue( Constructor.class));
        assertThat(NoPackageTypeHolder.TYPE.isAssignableFrom(type), is(true));
        assertThat(type, not(CoreMatchers.<Class<?>>is(NoPackageTypeHolder.TYPE)));
        assertThat(type.getDeclaredConstructor().newInstance(), CoreMatchers.instanceOf(NoPackageTypeHolder.TYPE));
        assertTrue(NoPackageTypeHolder.TYPE.isInstance(type.getDeclaredConstructor().newInstance()));
        assertThat(type.isInterface(), is(false));
        assertThat(type.isAnnotation(), is(false));
    }
}
