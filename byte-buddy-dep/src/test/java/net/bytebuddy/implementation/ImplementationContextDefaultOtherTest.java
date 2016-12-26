package net.bytebuddy.implementation;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.TypeInitializer;
import net.bytebuddy.dynamic.scaffold.TypeWriter;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImplementationContextDefaultOtherTest {

    @Test
    public void testFactory() throws Exception {
        assertThat(Implementation.Context.Default.Factory.INSTANCE.make(mock(TypeDescription.class),
                mock(AuxiliaryType.NamingStrategy.class),
                mock(TypeInitializer.class),
                mock(ClassFileVersion.class),
                mock(ClassFileVersion.class)), instanceOf(Implementation.Context.Default.class));
    }

    @Test
    public void testInstrumentationGetter() throws Exception {
        TypeDescription instrumentedType = mock(TypeDescription.class);
        assertThat(new Implementation.Context.Default(instrumentedType,
                mock(ClassFileVersion.class),
                mock(AuxiliaryType.NamingStrategy.class),
                mock(TypeInitializer.class),
                mock(ClassFileVersion.class)).getInstrumentedType(), is(instrumentedType));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDefaultContext() throws Exception {
        new Implementation.Context.Default.AbstractDelegationRecord(mock(MethodDescription.class)) {
            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                throw new AssertionError();
            }
        }.prepend(mock(ByteCodeAppender.class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Implementation.Context.Default.class).applyBasic();
        ObjectPropertyAssertion.of(Implementation.Context.Default.FieldCacheEntry.class).apply();
        ObjectPropertyAssertion.of(Implementation.Context.Default.AccessorMethodDelegation.class).apply();
        ObjectPropertyAssertion.of(Implementation.Context.Default.FieldSetterDelegation.class).apply();
        ObjectPropertyAssertion.of(Implementation.Context.Default.FieldGetterDelegation.class).apply();
        ObjectPropertyAssertion.of(Implementation.Context.Default.Factory.class).apply();
    }
}
