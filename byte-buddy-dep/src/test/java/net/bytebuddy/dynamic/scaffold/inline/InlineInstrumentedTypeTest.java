package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.instrumentation.field.FieldList;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.type.AbstractInstrumentedTypeTest;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.util.Collections;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.isConstructor;
import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.isMethod;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class InlineInstrumentedTypeTest extends AbstractInstrumentedTypeTest {

    private static final String FOO = "foo", BAR = "bar", FOOBAR = FOO + "." + BAR;

    @Mock
    private TypeDescription targetType;

    @Before
    public void setUp() throws Exception {
        when(targetType.getDeclaredMethods()).thenReturn(new MethodList.Empty());
        when(targetType.getDeclaredFields()).thenReturn(new FieldList.Empty());
        when(targetType.getInterfaces()).thenReturn(new TypeList.Empty());
        when(targetType.getSupertype()).thenReturn(new TypeDescription.ForLoadedType(Object.class));
    }

    @Override
    protected InstrumentedType makePlainInstrumentedType() {
        return new InlineInstrumentedType(
                ClassFileVersion.forCurrentJavaVersion(),
                targetType,
                new TypeList.ForLoadedType(Collections.<Class<?>>singletonList(Serializable.class)),
                Opcodes.ACC_PUBLIC,
                new NamingStrategy.Fixed(FOOBAR));
    }

    @Test
    public void testTargetTypeMemberInheritance() throws Exception {
        TypeDescription typeDescription = new InlineInstrumentedType(
                ClassFileVersion.forCurrentJavaVersion(),
                new TypeDescription.ForLoadedType(Foo.class),
                new TypeList.ForLoadedType(Collections.<Class<?>>singletonList(Serializable.class)),
                Opcodes.ACC_PUBLIC,
                new NamingStrategy.Fixed(FOOBAR));
        assertThat(typeDescription.getDeclaredMethods().size(), is(2));
        assertThat(typeDescription.getDeclaredMethods().filter(isConstructor()).size(), is(1));
        assertThat(typeDescription.getDeclaredMethods().filter(isMethod()).size(), is(1));
        assertThat(typeDescription.getDeclaredFields().size(), is(1));
    }

    public static class Foo {

        private Void foo;

        public void foo() {
            /* empty */
        }
    }
}
