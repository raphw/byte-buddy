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
    private TypeDescription typeDescription;

    @Mock
    private MethodGraph.Compiler methodGraphCompiler;

    @Mock
    private ModifierContributor.ForType modifierContributorForType;

    @Mock
    private NamingStrategy.Unbound namingStrategy;

    @Mock
    private AuxiliaryType.NamingStrategy auxiliaryTypeNamingStrategy;

    @Mock
    private Implementation implementation;

    @Before
    public void setUp() throws Exception {
        when(modifierContributorForType.getMask()).thenReturn(MASK);
        when(typeDescription.isInterface()).thenReturn(true);
        when(typeDescription.asErasure()).thenReturn(typeDescription);
        when(typeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
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
                .withImplementing(typeDescription)
                .withMethodGraphCompiler(methodGraphCompiler)
                .withModifiers(modifierContributorForType)
                .withNamingStrategy(namingStrategy)
                .withNamingStrategy(auxiliaryTypeNamingStrategy));
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
                .withImplementing(typeDescription)
                .withMethodGraphCompiler(methodGraphCompiler)
                .withModifiers(modifierContributorForType)
                .withNamingStrategy(namingStrategy)
                .withNamingStrategy(auxiliaryTypeNamingStrategy)
                .method(methodMatcher).intercept(implementation));
    }

    @SuppressWarnings("unchecked")
    private void assertProperties(ByteBuddy byteBuddy) {
        assertThat(byteBuddy.getTypeAttributeAppender(), is(typeAttributeAppender));
        assertThat(byteBuddy.getClassFileVersion(), is(classFileVersion));
        assertThat(byteBuddy.getDefaultFieldAttributeAppenderFactory(), is(fieldAttributeAppenderFactory));
        assertThat(byteBuddy.getDefaultMethodAttributeAppenderFactory(), is(methodAttributeAppenderFactory));
        assertThat(byteBuddy.getIgnoredMethods(), is((ElementMatcher) methodMatcher));
        assertThat(byteBuddy.getInterfaceTypes().size(), is(1));
        assertThat(byteBuddy.getInterfaceTypes(), hasItem(typeDescription));
        assertThat(byteBuddy.getMethodGraphCompiler(), is(methodGraphCompiler));
        assertThat(byteBuddy.getModifiers().isDefined(), is(true));
        assertThat(byteBuddy.getModifiers().resolve(0), is(MASK));
        assertThat(byteBuddy.getNamingStrategy(), is(namingStrategy));
        assertThat(byteBuddy.getAuxiliaryTypeNamingStrategy(), is(auxiliaryTypeNamingStrategy));
        assertThat(byteBuddy.getClassVisitorWrapperChain(), instanceOf(ClassVisitorWrapper.Chain.class));
        ClassVisitor classVisitor = mock(ClassVisitor.class);
        byteBuddy.getClassVisitorWrapperChain().wrap(classVisitor);
        verify(classVisitorWrapper).wrap(classVisitor);
        verifyNoMoreInteractions(classVisitorWrapper);
    }

    @Test
    public void testClassFileVersionConstructor() throws Exception {
        assertThat(new ByteBuddy(ClassFileVersion.JAVA_V6).getClassFileVersion(), is(ClassFileVersion.JAVA_V6));
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
