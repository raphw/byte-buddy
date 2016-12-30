package net.bytebuddy.implementation;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.scaffold.TypeInitializer;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.RandomString;
import org.junit.Test;
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
        new Implementation.Context.Default.DelegationRecord(mock(MethodDescription.InDefinedShape.class), Visibility.PACKAGE_PRIVATE) {
            @Override
            public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                throw new AssertionError();
            }

            @Override
            protected Implementation.Context.Default.DelegationRecord with(MethodAccessorFactory.AccessType accessType) {
                throw new AssertionError();
            }
        }.prepend(mock(ByteCodeAppender.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Implementation.Context.Default.class).applyBasic();
        ObjectPropertyAssertion.of(Implementation.Context.Default.FieldCacheEntry.class).apply();
        ObjectPropertyAssertion.of(Implementation.Context.Default.AccessorMethodDelegation.class).refine(new ObjectPropertyAssertion.Refinement<Implementation.SpecialMethodInvocation>() {
            @Override
            public void apply(Implementation.SpecialMethodInvocation mock) {
                MethodDescription methodDescription = mock(MethodDescription.class);
                when(methodDescription.getReturnType()).thenReturn(TypeDescription.Generic.OBJECT);
                when(methodDescription.getDeclaringType()).thenReturn(TypeDescription.Generic.OBJECT);
                when(methodDescription.getParameters()).thenReturn(new ParameterList.Empty());
                when(methodDescription.getExceptionTypes()).thenReturn(new TypeList.Generic.Empty());
                when(mock.getMethodDescription()).thenReturn(methodDescription);
            }
        }).refine(new ObjectPropertyAssertion.Refinement<TypeDescription>() {
            @Override
            public void apply(TypeDescription mock) {
                when(mock.asErasure()).thenReturn(mock);
            }
        }).apply();
        ObjectPropertyAssertion.of(Implementation.Context.Default.FieldSetterDelegation.class).refine(new ObjectPropertyAssertion.Refinement<FieldDescription>() {
            @Override
            public void apply(FieldDescription mock) {
                when(mock.getType()).thenReturn(TypeDescription.Generic.OBJECT);
            }
        }).refine(new ObjectPropertyAssertion.Refinement<TypeDescription>() {
            @Override
            public void apply(TypeDescription mock) {
                when(mock.asErasure()).thenReturn(mock);
            }
        }).apply();
        ObjectPropertyAssertion.of(Implementation.Context.Default.FieldGetterDelegation.class).refine(new ObjectPropertyAssertion.Refinement<FieldDescription>() {
            @Override
            public void apply(FieldDescription mock) {
                when(mock.getType()).thenReturn(TypeDescription.Generic.OBJECT);
            }
        }).refine(new ObjectPropertyAssertion.Refinement<TypeDescription>() {
            @Override
            public void apply(TypeDescription mock) {
                when(mock.asErasure()).thenReturn(mock);
            }
        }).apply();
        ObjectPropertyAssertion.of(Implementation.Context.Default.Factory.class).apply();
    }
}
