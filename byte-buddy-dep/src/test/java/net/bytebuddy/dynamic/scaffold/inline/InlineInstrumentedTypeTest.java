package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.dynamic.scaffold.AbstractInstrumentedTypeTest;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class InlineInstrumentedTypeTest extends AbstractInstrumentedTypeTest {

    private static final String FOO = "foo", BAR = "bar", FOOBAR = FOO + "." + BAR;

    @Mock
    private TypeDescription levelType;

    @Mock
    private PackageDescription packageDescription;

    @Before
    public void setUp() throws Exception {
        when(levelType.getSourceCodeName()).thenReturn(FOO);
        when(levelType.getDeclaredMethods()).thenReturn(new MethodList.Empty());
        when(levelType.getDeclaredFields()).thenReturn(new FieldList.Empty());
        when(levelType.getInterfacesGen()).thenReturn(new GenericTypeList.Empty());
        when(levelType.getTypeVariables()).thenReturn(new GenericTypeList.Empty());
        when(levelType.getSuperTypeGen()).thenReturn(new TypeDescription.ForLoadedType(Object.class));
        when(levelType.getPackage()).thenReturn(packageDescription);
        when(packageDescription.getName()).thenReturn(FOO);
    }

    @Override
    protected InstrumentedType makePlainInstrumentedType() {
        return new InlineInstrumentedType(
                ClassFileVersion.forCurrentJavaVersion(),
                levelType,
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
