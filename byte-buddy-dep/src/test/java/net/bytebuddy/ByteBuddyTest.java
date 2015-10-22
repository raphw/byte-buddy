package net.bytebuddy;

import net.bytebuddy.asm.ClassVisitorWrapper;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ByteBuddyTest {

    private static final int MASK = Opcodes.ACC_PUBLIC;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeAttributeAppender typeAttributeAppender;

    @Mock
    private ClassFileVersion classFileVersion;

    @Mock
    private ClassVisitorWrapper classVisitorWrapper;

    @Mock
    private FieldAttributeAppender.Factory fieldAttributeAppenderFactory;

    @Mock
    private MethodAttributeAppender.Factory methodAttributeAppenderFactory;

    @Mock
    private ElementMatcher<? super MethodDescription> methodMatcher;

    @Mock
    private TypeDescription interfaceTypes;

    @Mock
    private MethodGraph.Compiler methodGraphCompiler;

    @Mock
    private ModifierContributor.ForType modifierContributorForType;

    @Mock
    private NamingStrategy.Unbound namingStrategy;

    @Mock
    private AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy;

    @Mock
    private Implementation.Context.Factory implementationContextFactory;

    @Mock
    private Implementation implementation;

    @Before
    public void setUp() throws Exception {
        when(modifierContributorForType.getMask()).thenReturn(MASK);
        when(interfaceTypes.isInterface()).thenReturn(true);
        when(interfaceTypes.asErasure()).thenReturn(interfaceTypes);
        when(interfaceTypes.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
    }

    @Test
    public void testDomainSpecificLanguage() throws Exception {
        assertProperties(new ByteBuddy()
                .method(methodMatcher).intercept(implementation)
                .withAttribute(typeAttributeAppender)
                .withClassFileVersion(classFileVersion)
                .withClassVisitor(classVisitorWrapper)
                .withDefaultFieldAttributeAppender(fieldAttributeAppenderFactory)
                .withDefaultMethodAttributeAppender(methodAttributeAppenderFactory)
                .withIgnoredMethods(methodMatcher)
                .withImplementing(interfaceTypes)
                .withMethodGraphCompiler(methodGraphCompiler)
                .withModifiers(modifierContributorForType)
                .withNamingStrategy(namingStrategy)
                .withNamingStrategy(auxiliaryTypeNamingStrategy)
                .withContext(implementationContextFactory));
    }

    @Test
    public void testDomainSpecificLanguageOnAnnotationTarget() throws Exception {
        assertProperties(new ByteBuddy()
                .withAttribute(typeAttributeAppender)
                .withClassFileVersion(classFileVersion)
                .withClassVisitor(classVisitorWrapper)
                .withDefaultFieldAttributeAppender(fieldAttributeAppenderFactory)
                .withDefaultMethodAttributeAppender(methodAttributeAppenderFactory)
                .withIgnoredMethods(methodMatcher)
                .withImplementing(interfaceTypes)
                .withMethodGraphCompiler(methodGraphCompiler)
                .withModifiers(modifierContributorForType)
                .withNamingStrategy(namingStrategy)
                .withNamingStrategy(auxiliaryTypeNamingStrategy)
                .withContext(implementationContextFactory)
                .method(methodMatcher).intercept(implementation));
    }

    @SuppressWarnings("unchecked")
    private void assertProperties(ByteBuddy byteBuddy) {
        assertThat(byteBuddy.typeAttributeAppender, is(typeAttributeAppender));
        assertThat(byteBuddy.classFileVersion, is(classFileVersion));
        assertThat(byteBuddy.defaultFieldAttributeAppenderFactory, is(fieldAttributeAppenderFactory));
        assertThat(byteBuddy.defaultMethodAttributeAppenderFactory, is(methodAttributeAppenderFactory));
        assertThat(byteBuddy.ignoredMethods, is((ElementMatcher) methodMatcher));
        assertThat(byteBuddy.interfaceTypes.size(), is(1));
        assertThat(byteBuddy.interfaceTypes, hasItem(interfaceTypes));
        assertThat(byteBuddy.methodGraphCompiler, is(methodGraphCompiler));
        assertThat(byteBuddy.modifiers.isDefined(), is(true));
        assertThat(byteBuddy.modifiers.resolve(0), is(MASK));
        assertThat(byteBuddy.namingStrategy, is(namingStrategy));
        assertThat(byteBuddy.auxiliaryTypeNamingStrategy, is(auxiliaryTypeNamingStrategy));
        assertThat(byteBuddy.implementationContextFactory, is(implementationContextFactory));
        assertThat(byteBuddy.classVisitorWrapper, instanceOf(ClassVisitorWrapper.Compound.class));
        ClassVisitor classVisitor = mock(ClassVisitor.class);
        byteBuddy.classVisitorWrapper.wrap(classVisitor);
        verify(classVisitorWrapper).wrap(classVisitor);
        verifyNoMoreInteractions(classVisitorWrapper);
    }

    @Test
    public void testClassFileVersionConstructor() throws Exception {
        assertThat(new ByteBuddy(ClassFileVersion.JAVA_V6).classFileVersion, is(ClassFileVersion.JAVA_V6));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteBuddy.class).apply();
        ObjectPropertyAssertion.of(ByteBuddy.EnumerationImplementation.class).apply();
        ObjectPropertyAssertion.of(ByteBuddy.EnumerationImplementation.ValuesMethodAppender.class).apply();
        ObjectPropertyAssertion.of(ByteBuddy.EnumerationImplementation.InitializationAppender.class).apply();
        ObjectPropertyAssertion.of(ByteBuddy.MethodAnnotationTarget.class).apply();
        ObjectPropertyAssertion.of(ByteBuddy.OptionalMethodInterception.class).apply();
    }
}
