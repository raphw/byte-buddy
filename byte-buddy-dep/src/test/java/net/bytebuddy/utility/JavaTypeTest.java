package net.bytebuddy.utility;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.objectweb.asm.Opcodes;

import java.io.Serializable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Member;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class JavaTypeTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    public void testConstable() throws Exception {
        assertThat(JavaType.CONSTABLE.getTypeStub().getName(), is("java.lang.constant.Constable"));
        assertThat(JavaType.CONSTABLE.getTypeStub().getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE));
        assertThat(JavaType.CONSTABLE.getTypeStub().getSuperClass(), nullValue(TypeDescription.Generic.class));
        assertThat(JavaType.CONSTABLE.getTypeStub().getInterfaces().size(), is(0));
    }

    @Test
    public void testTypeDescriptor() throws Exception {
        assertThat(JavaType.TYPE_DESCRIPTOR.getTypeStub().getName(), is("java.lang.invoke.TypeDescriptor"));
        assertThat(JavaType.TYPE_DESCRIPTOR.getTypeStub().getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE));
        assertThat(JavaType.TYPE_DESCRIPTOR.getTypeStub().getSuperClass(), nullValue(TypeDescription.Generic.class));
        assertThat(JavaType.TYPE_DESCRIPTOR.getTypeStub().getInterfaces().size(), is(0));
    }

    @Test
    public void testTypeDescriptorOfField() throws Exception {
        assertThat(JavaType.TYPE_DESCRIPTOR_OF_FIELD.getTypeStub().getName(), is("java.lang.invoke.TypeDescriptor$OfField"));
        assertThat(JavaType.TYPE_DESCRIPTOR_OF_FIELD.getTypeStub().getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE));
        assertThat(JavaType.TYPE_DESCRIPTOR_OF_FIELD.getTypeStub().getSuperClass(), nullValue(TypeDescription.Generic.class));
        assertThat(JavaType.TYPE_DESCRIPTOR_OF_FIELD.getTypeStub().getInterfaces().size(), is(1));
        assertThat(JavaType.TYPE_DESCRIPTOR_OF_FIELD.getTypeStub().getInterfaces(), hasItems(JavaType.TYPE_DESCRIPTOR.getTypeStub().asGenericType()));
    }

    @Test
    public void testTypeDescriptorOfMethod() throws Exception {
        assertThat(JavaType.TYPE_DESCRIPTOR_OF_METHOD.getTypeStub().getName(), is("java.lang.invoke.TypeDescriptor$OfMethod"));
        assertThat(JavaType.TYPE_DESCRIPTOR_OF_METHOD.getTypeStub().getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE));
        assertThat(JavaType.TYPE_DESCRIPTOR_OF_METHOD.getTypeStub().getSuperClass(), nullValue(TypeDescription.Generic.class));
        assertThat(JavaType.TYPE_DESCRIPTOR_OF_METHOD.getTypeStub().getInterfaces().size(), is(1));
        assertThat(JavaType.TYPE_DESCRIPTOR_OF_METHOD.getTypeStub().getInterfaces(), hasItems(JavaType.TYPE_DESCRIPTOR.getTypeStub().asGenericType()));
    }

    @Test
    public void testMethodHandle() throws Exception {
        assertThat(JavaType.METHOD_HANDLE.getTypeStub().getName(), is("java.lang.invoke.MethodHandle"));
        assertThat(JavaType.METHOD_HANDLE.getTypeStub().getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT));
        assertThat(JavaType.METHOD_HANDLE.getTypeStub().getSuperClass(), is(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)));
        assertThat(JavaType.METHOD_HANDLE.getTypeStub().getInterfaces().size(), is(1));
        assertThat(JavaType.METHOD_HANDLE.getTypeStub().getInterfaces(), hasItems(JavaType.CONSTABLE.getTypeStub().asGenericType()));
    }

    @Test
    public void testMethodHandles() throws Exception {
        assertThat(JavaType.METHOD_HANDLES.getTypeStub().getName(), is("java.lang.invoke.MethodHandles"));
        assertThat(JavaType.METHOD_HANDLES.getTypeStub().getModifiers(), is(Opcodes.ACC_PUBLIC));
        assertThat(JavaType.METHOD_HANDLES.getTypeStub().getSuperClass(), is(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)));
        assertThat(JavaType.METHOD_HANDLES.getTypeStub().getInterfaces().size(), is(0));
    }

    @Test
    public void testMethodType() throws Exception {
        assertThat(JavaType.METHOD_TYPE.getTypeStub().getName(), is("java.lang.invoke.MethodType"));
        assertThat(JavaType.METHOD_TYPE.getTypeStub().getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL));
        assertThat(JavaType.METHOD_TYPE.getTypeStub().getSuperClass(), is(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)));
        assertThat(JavaType.METHOD_TYPE.getTypeStub().getInterfaces().size(), is(3));
        assertThat(JavaType.METHOD_TYPE.getTypeStub().getInterfaces(), CoreMatchers.hasItems(JavaType.CONSTABLE.getTypeStub().asGenericType(),
                JavaType.TYPE_DESCRIPTOR_OF_METHOD.getTypeStub().asGenericType(),
                TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Serializable.class)));
    }

    @Test
    public void testMethodTypesLookup() throws Exception {
        assertThat(JavaType.METHOD_HANDLES_LOOKUP.getTypeStub().getName(), is("java.lang.invoke.MethodHandles$Lookup"));
        assertThat(JavaType.METHOD_HANDLES_LOOKUP.getTypeStub().getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL));
        assertThat(JavaType.METHOD_HANDLES_LOOKUP.getTypeStub().getSuperClass(), is(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)));
        assertThat(JavaType.METHOD_HANDLES_LOOKUP.getTypeStub().getInterfaces().size(), is(0));
    }

    @Test
    public void testCallSite() throws Exception {
        assertThat(JavaType.CALL_SITE.getTypeStub().getName(), is("java.lang.invoke.CallSite"));
        assertThat(JavaType.CALL_SITE.getTypeStub().getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT));
        assertThat(JavaType.CALL_SITE.getTypeStub().getSuperClass(), is(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)));
        assertThat(JavaType.CALL_SITE.getTypeStub().getInterfaces().size(), is(0));
    }

    @Test
    public void testVarHandle() throws Exception {
        assertThat(JavaType.VAR_HANDLE.getTypeStub().getName(), is("java.lang.invoke.VarHandle"));
        assertThat(JavaType.VAR_HANDLE.getTypeStub().getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT));
        assertThat(JavaType.VAR_HANDLE.getTypeStub().getSuperClass(), is(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)));
        assertThat(JavaType.VAR_HANDLE.getTypeStub().getInterfaces().size(), is(1));
        assertThat(JavaType.VAR_HANDLE.getTypeStub().getInterfaces(), hasItems(JavaType.CONSTABLE.getTypeStub().asGenericType()));
    }

    @Test
    public void testParameter() throws Exception {
        assertThat(JavaType.PARAMETER.getTypeStub().getName(), is("java.lang.reflect.Parameter"));
        assertThat(JavaType.PARAMETER.getTypeStub().getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL));
        assertThat(JavaType.PARAMETER.getTypeStub().getSuperClass(), is(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)));
        assertThat(JavaType.PARAMETER.getTypeStub().getInterfaces().size(), is(1));
        assertThat(JavaType.PARAMETER.getTypeStub().getInterfaces(),
                hasItems(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(AnnotatedElement.class)));
    }

    @Test
    public void testExecutable() throws Exception {
        assertThat(JavaType.EXECUTABLE.getTypeStub().getName(), is("java.lang.reflect.Executable"));
        assertThat(JavaType.EXECUTABLE.getTypeStub().getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT));
        assertThat(JavaType.EXECUTABLE.getTypeStub().getSuperClass(), is((TypeDefinition) TypeDescription.ForLoadedType.of(AccessibleObject.class)));
        assertThat(JavaType.EXECUTABLE.getTypeStub().getInterfaces().size(), is(2));
        assertThat(JavaType.EXECUTABLE.getTypeStub().getInterfaces(), hasItems(
                TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Member.class),
                TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(GenericDeclaration.class)));
    }

    @Test
    public void testModule() throws Exception {
        assertThat(JavaType.MODULE.getTypeStub().getName(), is("java.lang.Module"));
        assertThat(JavaType.MODULE.getTypeStub().getModifiers(), is(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL));
        assertThat(JavaType.MODULE.getTypeStub().getSuperClass(), is(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)));
        assertThat(JavaType.MODULE.getTypeStub().getInterfaces().size(), is(1));
        assertThat(JavaType.MODULE.getTypeStub().getInterfaces(),
                hasItems(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(AnnotatedElement.class)));
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testJava7Types() throws Exception {
        assertThat(JavaType.METHOD_HANDLE.load(), notNullValue(Class.class));
        assertThat(JavaType.METHOD_HANDLE.loadAsDescription(), notNullValue(TypeDescription.class));
        assertThat(JavaType.METHOD_TYPE.load(), notNullValue(Class.class));
        assertThat(JavaType.METHOD_TYPE.loadAsDescription(), notNullValue(TypeDescription.class));
        assertThat(JavaType.METHOD_HANDLES_LOOKUP.load(), notNullValue(Class.class));
        assertThat(JavaType.METHOD_HANDLES_LOOKUP.loadAsDescription(), notNullValue(TypeDescription.class));
        assertThat(JavaType.CALL_SITE.load(), notNullValue(Class.class));
        assertThat(JavaType.CALL_SITE.loadAsDescription(), notNullValue(TypeDescription.class));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testJava8Types() throws Exception {
        assertThat(JavaType.PARAMETER.load(), notNullValue(Class.class));
        assertThat(JavaType.PARAMETER.loadAsDescription(), notNullValue(TypeDescription.class));
        assertThat(JavaType.EXECUTABLE.load(), notNullValue(Class.class));
        assertThat(JavaType.EXECUTABLE.loadAsDescription(), notNullValue(TypeDescription.class));
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testJava9Types() throws Exception {
        assertThat(JavaType.VAR_HANDLE.load(), notNullValue(Class.class));
        assertThat(JavaType.VAR_HANDLE.loadAsDescription(), notNullValue(TypeDescription.class));
        assertThat(JavaType.MODULE.load(), notNullValue(Class.class));
        assertThat(JavaType.MODULE.loadAsDescription(), notNullValue(TypeDescription.class));
    }

    @Test
    @JavaVersionRule.Enforce(12)
    public void testJava12Types() throws Exception {
        assertThat(JavaType.CONSTABLE.load(), notNullValue(Class.class));
        assertThat(JavaType.CONSTABLE.loadAsDescription(), notNullValue(TypeDescription.class));
        assertThat(JavaType.TYPE_DESCRIPTOR.load(), notNullValue(Class.class));
        assertThat(JavaType.TYPE_DESCRIPTOR.loadAsDescription(), notNullValue(TypeDescription.class));
        assertThat(JavaType.TYPE_DESCRIPTOR_OF_FIELD.load(), notNullValue(Class.class));
        assertThat(JavaType.TYPE_DESCRIPTOR_OF_FIELD.loadAsDescription(), notNullValue(TypeDescription.class));
        assertThat(JavaType.TYPE_DESCRIPTOR_OF_METHOD.load(), notNullValue(Class.class));
        assertThat(JavaType.TYPE_DESCRIPTOR_OF_METHOD.loadAsDescription(), notNullValue(TypeDescription.class));
        assertThat(JavaType.CONSTANT_BOOTSTRAPS.load(), notNullValue(Class.class));
        assertThat(JavaType.CONSTANT_BOOTSTRAPS.loadAsDescription(), notNullValue(TypeDescription.class));
    }

    @Test
    @JavaVersionRule.Enforce(14)
    public void testJava14Types() throws Exception {
        assertThat(JavaType.RECORD.load(), notNullValue(Class.class));
        assertThat(JavaType.RECORD.loadAsDescription(), notNullValue(TypeDescription.class));
        assertThat(JavaType.OBJECT_METHODS.load(), notNullValue(Class.class));
        assertThat(JavaType.OBJECT_METHODS.loadAsDescription(), notNullValue(TypeDescription.class));
    }
}
