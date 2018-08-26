package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.TypeVariableToken;
import net.bytebuddy.dynamic.Transformer;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class InstrumentedTypeFrozenTest {

    @Test
    public void testDelegation() throws Exception {
        for (Method method : TypeDescription.class.getDeclaredMethods()) {
            if (method.getParameterTypes().length == 0 && Modifier.isPublic(method.getModifiers()) && !method.isSynthetic()) {
                assertThat(method.invoke(new InstrumentedType.Frozen(TypeDescription.STRING, LoadedTypeInitializer.NoOp.INSTANCE)),
                        is(method.invoke(TypeDescription.STRING)));
            }
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldToken() throws Exception {
        new InstrumentedType.Frozen(TypeDescription.STRING, LoadedTypeInitializer.NoOp.INSTANCE).withField(mock(FieldDescription.Token.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodToken() throws Exception {
        new InstrumentedType.Frozen(TypeDescription.STRING, LoadedTypeInitializer.NoOp.INSTANCE).withMethod(mock(MethodDescription.Token.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testAnnotation() throws Exception {
        new InstrumentedType.Frozen(TypeDescription.STRING, LoadedTypeInitializer.NoOp.INSTANCE).withAnnotations(Collections.<AnnotationDescription>emptyList());
    }

    @Test(expected = IllegalStateException.class)
    public void testInitializer() throws Exception {
        new InstrumentedType.Frozen(TypeDescription.STRING, LoadedTypeInitializer.NoOp.INSTANCE).withInitializer(mock(ByteCodeAppender.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testWithTypeVariable() throws Exception {
        new InstrumentedType.Frozen(TypeDescription.STRING, LoadedTypeInitializer.NoOp.INSTANCE).withTypeVariable(mock(TypeVariableToken.class));
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("unchecked")
    public void testWithTypeVariables() throws Exception {
        new InstrumentedType.Frozen(TypeDescription.STRING, LoadedTypeInitializer.NoOp.INSTANCE).withTypeVariables(mock(ElementMatcher.class), mock(Transformer.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testWithName() throws Exception {
        new InstrumentedType.Frozen(TypeDescription.STRING, LoadedTypeInitializer.NoOp.INSTANCE).withName("foo");
    }

    @Test(expected = IllegalStateException.class)
    public void testWithDeclaringType() {
        new InstrumentedType.Frozen(TypeDescription.STRING, LoadedTypeInitializer.NoOp.INSTANCE).withDeclaringType(mock(TypeDescription.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testWithDeclaredType() {
        new InstrumentedType.Frozen(TypeDescription.STRING, LoadedTypeInitializer.NoOp.INSTANCE).withDeclaredTypes(mock(TypeList.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testWithEnclosingType() {
        new InstrumentedType.Frozen(TypeDescription.STRING, LoadedTypeInitializer.NoOp.INSTANCE).withEnclosingType(mock(TypeDescription.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testWithEnclosingMethod() {
        new InstrumentedType.Frozen(TypeDescription.STRING, LoadedTypeInitializer.NoOp.INSTANCE).withEnclosingMethod(mock(MethodDescription.InDefinedShape.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testWithNestHost() {
        new InstrumentedType.Frozen(TypeDescription.STRING, LoadedTypeInitializer.NoOp.INSTANCE).withNestHost(mock(TypeDescription.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testWithNestMember() {
        new InstrumentedType.Frozen(TypeDescription.STRING, LoadedTypeInitializer.NoOp.INSTANCE).withNestMembers(mock(TypeList.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testWithLocalClass() {
        new InstrumentedType.Frozen(TypeDescription.STRING, LoadedTypeInitializer.NoOp.INSTANCE).withLocalClass(true);
    }

    @Test(expected = IllegalStateException.class)
    public void testWithAnonymousClass() {
        new InstrumentedType.Frozen(TypeDescription.STRING, LoadedTypeInitializer.NoOp.INSTANCE).withAnonymousClass(true);
    }

    @Test
    public void testValidation() throws Exception {
        assertThat(new InstrumentedType.Frozen(TypeDescription.STRING, LoadedTypeInitializer.NoOp.INSTANCE).validated(), is(TypeDescription.STRING));
    }

    @Test
    public void testTypeInitializer() throws Exception {
        assertThat(new InstrumentedType.Frozen(TypeDescription.STRING, LoadedTypeInitializer.NoOp.INSTANCE).getTypeInitializer(),
                is((TypeInitializer) TypeInitializer.None.INSTANCE));
    }

    @Test
    public void testLoadedTypeInitializer() throws Exception {
        LoadedTypeInitializer loadedTypeInitializer = mock(LoadedTypeInitializer.class);
        assertThat(new InstrumentedType.Frozen(TypeDescription.STRING, loadedTypeInitializer).getLoadedTypeInitializer(), is(loadedTypeInitializer));
    }
}
