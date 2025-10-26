package net.bytebuddy.dynamic;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.modifier.MethodManifestation;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.ProvisioningState;
import net.bytebuddy.description.modifier.TypeManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeVariableToken;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.InjectionClassLoader;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.ExceptionMethod;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.constant.NullConstant;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.test.utility.CallTraceable;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.AsmClassWriter;
import net.bytebuddy.utility.OpenedClassReader;
import net.bytebuddy.utility.visitor.ContextClassVisitor;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.stubbing.Answer;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.isTypeInitializer;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public abstract class AbstractDynamicTypeBuilderTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", TO_STRING = "toString";

    private static final String TYPE_VARIABLE_NAME = "net.bytebuddy.test.precompiled.v8.TypeAnnotation", VALUE = "value";

    private static final int MODIFIERS = Opcodes.ACC_PUBLIC;

    private static final boolean BOOLEAN_VALUE = true;

    private static final int INTEGER_VALUE = 42;

    private static final long LONG_VALUE = 42L;

    private static final float FLOAT_VALUE = 42f;

    private static final double DOUBLE_VALUE = 42d;

    private static final String BOOLEAN_FIELD = "booleanField";

    private static final String BYTE_FIELD = "byteField";

    private static final String CHARACTER_FIELD = "characterField";

    private static final String SHORT_FIELD = "shortField";

    private static final String INTEGER_FIELD = "integerField";

    private static final String LONG_FIELD = "longField";

    private static final String FLOAT_FIELD = "floatField";

    private static final String DOUBLE_FIELD = "doubleField";

    private static final String STRING_FIELD = "stringField";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    private Type list, fooVariable;

    protected abstract DynamicType.Builder<?> createPlain();

    protected abstract DynamicType.Builder<?> createPlainWithoutValidation();

    protected abstract DynamicType.Builder<?> createPlainEmpty();

    @Before
    public void setUp() throws Exception {
        list = Holder.class.getDeclaredField("list").getGenericType();
        fooVariable = ((ParameterizedType) Holder.class.getDeclaredField("fooList").getGenericType()).getActualTypeArguments()[0];
    }

    @Test
    public void testMethodDefinition() throws Exception {
        Class<?> type = createPlain()
                .defineMethod(FOO, Object.class, Visibility.PUBLIC)
                .throwing(Exception.class)
                .intercept(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Method method = type.getDeclaredMethod(FOO);
        assertThat(method.getReturnType(), CoreMatchers.<Class<?>>is(Object.class));
        assertThat(method.getExceptionTypes(), is(new Class<?>[]{Exception.class}));
        assertThat(method.getModifiers(), is(Modifier.PUBLIC));
        assertThat(method.invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testAbstractMethodDefinition() throws Exception {
        Class<?> type = createPlain()
                .modifiers(Visibility.PUBLIC, TypeManifestation.ABSTRACT)
                .defineMethod(FOO, Object.class, Visibility.PUBLIC)
                .throwing(Exception.class)
                .withoutCode()
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Method method = type.getDeclaredMethod(FOO);
        assertThat(method.getReturnType(), CoreMatchers.<Class<?>>is(Object.class));
        assertThat(method.getExceptionTypes(), is(new Class<?>[]{Exception.class}));
        assertThat(method.getModifiers(), is(Modifier.PUBLIC | Modifier.ABSTRACT));
    }

    @Test
    public void testConstructorDefinition() throws Exception {
        Class<?> type = createPlain()
                .defineConstructor(Visibility.PUBLIC).withParameters(Void.class)
                .throwing(Exception.class)
                .intercept(MethodCall.invoke(Object.class.getDeclaredConstructor()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Constructor<?> constructor = type.getDeclaredConstructor(Void.class);
        assertThat(constructor.getExceptionTypes(), is(new Class<?>[]{Exception.class}));
        assertThat(constructor.getModifiers(), is(Modifier.PUBLIC));
        assertThat(constructor.newInstance((Object) null), notNullValue(Object.class));
    }

    @Test
    public void testFieldDefinition() throws Exception {
        Class<?> type = createPlain()
                .defineField(FOO, Void.class, Visibility.PUBLIC)
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Field field = type.getDeclaredField(FOO);
        assertThat(field.getType(), CoreMatchers.<Class<?>>is(Void.class));
        assertThat(field.getModifiers(), is(Modifier.PUBLIC));
    }

    @Test
    public void testFieldDefaultValueDefinition() throws Exception {
        Class<?> type = createPlain()
                .defineField(BOOLEAN_FIELD, boolean.class, Visibility.PUBLIC, Ownership.STATIC).value(BOOLEAN_VALUE)
                .defineField(BYTE_FIELD, byte.class, Visibility.PUBLIC, Ownership.STATIC).value(INTEGER_VALUE)
                .defineField(SHORT_FIELD, short.class, Visibility.PUBLIC, Ownership.STATIC).value(INTEGER_VALUE)
                .defineField(CHARACTER_FIELD, char.class, Visibility.PUBLIC, Ownership.STATIC).value(INTEGER_VALUE)
                .defineField(INTEGER_FIELD, int.class, Visibility.PUBLIC, Ownership.STATIC).value(INTEGER_VALUE)
                .defineField(LONG_FIELD, long.class, Visibility.PUBLIC, Ownership.STATIC).value(LONG_VALUE)
                .defineField(FLOAT_FIELD, float.class, Visibility.PUBLIC, Ownership.STATIC).value(FLOAT_VALUE)
                .defineField(DOUBLE_FIELD, double.class, Visibility.PUBLIC, Ownership.STATIC).value(DOUBLE_VALUE)
                .defineField(STRING_FIELD, String.class, Visibility.PUBLIC, Ownership.STATIC).value(FOO)
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredField(BOOLEAN_FIELD).get(null), is((Object) BOOLEAN_VALUE));
        assertThat(type.getDeclaredField(BYTE_FIELD).get(null), is((Object) (byte) INTEGER_VALUE));
        assertThat(type.getDeclaredField(SHORT_FIELD).get(null), is((Object) (short) INTEGER_VALUE));
        assertThat(type.getDeclaredField(CHARACTER_FIELD).get(null), is((Object) (char) INTEGER_VALUE));
        assertThat(type.getDeclaredField(INTEGER_FIELD).get(null), is((Object) INTEGER_VALUE));
        assertThat(type.getDeclaredField(LONG_FIELD).get(null), is((Object) LONG_VALUE));
        assertThat(type.getDeclaredField(FLOAT_FIELD).get(null), is((Object) FLOAT_VALUE));
        assertThat(type.getDeclaredField(DOUBLE_FIELD).get(null), is((Object) DOUBLE_VALUE));
        assertThat(type.getDeclaredField(STRING_FIELD).get(null), is((Object) FOO));
    }

    @Test
    public void testApplicationOrder() throws Exception {
        assertThat(createPlain()
                .method(named(TO_STRING)).intercept(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE))
                .method(named(TO_STRING)).intercept(new Implementation.Simple(new TextConstant(BAR), MethodReturn.REFERENCE))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()
                .toString(), is(BAR));
    }

    @Test
    public void testTypeInitializer() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassFileLocator.ForClassLoader.readToNames(Bar.class));
        Class<?> type = createPlain()
                .invokable(isTypeInitializer()).intercept(MethodCall.invoke(Bar.class.getDeclaredMethod("invoke")))
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredConstructor().newInstance(), notNullValue(Object.class));
        Class<?> foo = classLoader.loadClass(Bar.class.getName());
        assertThat(foo.getDeclaredField(FOO).get(null), is((Object) FOO));
    }

    @Test
    public void testConstructorInvokingMethod() throws Exception {
        Class<?> type = createPlain()
                .defineMethod(FOO, Object.class, Visibility.PUBLIC)
                .intercept(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Method method = type.getDeclaredMethod(FOO);
        assertThat(method.invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testMethodTransformation() throws Exception {
        Class<?> type = createPlain()
                .method(named(TO_STRING))
                .intercept(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE))
                .transform(Transformer.ForMethod.withModifiers(MethodManifestation.FINAL))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredConstructor().newInstance().toString(), is(FOO));
        assertThat(type.getDeclaredMethod(TO_STRING).getModifiers(), is(Opcodes.ACC_FINAL | Opcodes.ACC_PUBLIC));
    }

    @Test
    public void testFieldTransformation() throws Exception {
        Class<?> type = createPlain()
                .defineField(FOO, Void.class)
                .field(named(FOO))
                .transform(Transformer.ForField.withModifiers(Visibility.PUBLIC))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredField(FOO).getModifiers(), is(Opcodes.ACC_PUBLIC));
    }

    @Test
    public void testIgnoredMethod() throws Exception {
        Class<?> type = createPlain()
                .ignoreAlso(named(TO_STRING))
                .method(named(TO_STRING))
                .intercept(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredConstructor().newInstance().toString(), CoreMatchers.not(FOO));
    }

    @Test
    public void testIgnoredMethodDoesNotApplyForDefined() throws Exception {
        Class<?> type = createPlain()
                .ignoreAlso(named(FOO))
                .defineMethod(FOO, String.class, Visibility.PUBLIC)
                .intercept(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    public void testPreparedField() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassFileLocator.ForClassLoader.readToNames(SampleAnnotation.class));
        Class<?> type = createPlain()
                .defineMethod(BAR, String.class, Visibility.PUBLIC)
                .intercept(new PreparedField())
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredFields().length, is(1));
        assertThat(type.getDeclaredField(FOO).getName(), is(FOO));
        assertThat(type.getDeclaredField(FOO).getType(), CoreMatchers.<Class<?>>is(Object.class));
        assertThat(type.getDeclaredField(FOO).getModifiers(), is(MODIFIERS));
        assertThat(type.getDeclaredField(FOO).getAnnotations().length, is(1));
        Annotation annotation = type.getDeclaredField(FOO).getAnnotations()[0];
        assertThat(annotation.annotationType().getName(), is(SampleAnnotation.class.getName()));
        Method foo = annotation.annotationType().getDeclaredMethod(FOO);
        assertThat(foo.invoke(annotation), is((Object) BAR));
    }

    @Test
    public void testPreparedMethod() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassFileLocator.ForClassLoader.readToNames(SampleAnnotation.class));
        Class<?> type = createPlain()
                .defineMethod(BAR, String.class, Visibility.PUBLIC)
                .intercept(new PreparedMethod())
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethods().length, is(2));
        assertThat(type.getDeclaredMethod(FOO, Object.class).getName(), is(FOO));
        assertThat(type.getDeclaredMethod(FOO, Object.class).getReturnType(), CoreMatchers.<Class<?>>is(Object.class));
        assertThat(type.getDeclaredMethod(FOO, Object.class).getParameterTypes().length, is(1));
        assertThat(type.getDeclaredMethod(FOO, Object.class).getParameterTypes()[0], CoreMatchers.<Class<?>>is(Object.class));
        assertThat(type.getDeclaredMethod(FOO, Object.class).getModifiers(), is(MODIFIERS));
        assertThat(type.getDeclaredMethod(FOO, Object.class).getAnnotations().length, is(1));
        Annotation methodAnnotation = type.getDeclaredMethod(FOO, Object.class).getAnnotations()[0];
        assertThat(methodAnnotation.annotationType().getName(), is(SampleAnnotation.class.getName()));
        Method methodMethod = methodAnnotation.annotationType().getDeclaredMethod(FOO);
        assertThat(methodMethod.invoke(methodAnnotation), is((Object) BAR));
        assertThat(type.getDeclaredMethod(FOO, Object.class).getParameterAnnotations()[0].length, is(1));
        Annotation parameterAnnotation = type.getDeclaredMethod(FOO, Object.class).getParameterAnnotations()[0][0];
        assertThat(parameterAnnotation.annotationType().getName(), is(SampleAnnotation.class.getName()));
        Method parameterMethod = parameterAnnotation.annotationType().getDeclaredMethod(FOO);
        assertThat(parameterMethod.invoke(parameterAnnotation), is((Object) QUX));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWriterHint() throws Exception {
        AsmVisitorWrapper asmVisitorWrapper = mock(AsmVisitorWrapper.class);
        when(asmVisitorWrapper.wrap(any(TypeDescription.class),
                any(ClassVisitor.class),
                any(Implementation.Context.class),
                any(TypePool.class),
                any(FieldList.class),
                any(MethodList.class),
                anyInt(),
                anyInt())).then(new Answer<ClassVisitor>() {
            public ClassVisitor answer(InvocationOnMock invocationOnMock) throws Throwable {
                return new ClassVisitor(OpenedClassReader.ASM_API, (ClassVisitor) invocationOnMock.getArguments()[1]) {
                    @Override
                    public void visitEnd() {
                        MethodVisitor mv = visitMethod(Opcodes.ACC_PUBLIC, FOO, "()Ljava/lang/String;", null, null);
                        mv.visitCode();
                        mv.visitLdcInsn(FOO);
                        mv.visitInsn(Opcodes.ARETURN);
                        mv.visitMaxs(-1, -1);
                        mv.visitEnd();
                        super.visitEnd();
                    }
                };
            }
        });
        when(asmVisitorWrapper.mergeWriter(0)).thenReturn(ClassWriter.COMPUTE_MAXS);
        Class<?> type = createPlain()
                .visit(asmVisitorWrapper)
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        verify(asmVisitorWrapper).mergeWriter(0);
        verify(asmVisitorWrapper, atMost(1)).mergeReader(0);
        verify(asmVisitorWrapper).wrap(any(TypeDescription.class),
                any(ClassVisitor.class),
                any(Implementation.Context.class),
                any(TypePool.class),
                any(FieldList.class),
                any(MethodList.class),
                anyInt(),
                anyInt());
        verifyNoMoreInteractions(asmVisitorWrapper);
    }

    @Test
    public void testExplicitTypeInitializer() throws Exception {
        assertThat(createPlain()
                .defineField(FOO, String.class, Ownership.STATIC, Visibility.PUBLIC)
                .initializer(new ByteCodeAppender() {
                    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                        return new Size(new StackManipulation.Compound(
                                new TextConstant(FOO),
                                FieldAccess.forField(instrumentedMethod.getDeclaringType().getDeclaredFields().filter(named(FOO)).getOnly()).write()
                        ).apply(methodVisitor, implementationContext).getMaximalSize(), instrumentedMethod.getStackSize());
                    }
                }).make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredField(FOO)
                .get(null), is((Object) FOO));
    }

    @Test
    public void testSerialVersionUid() throws Exception {
        Class<?> type = createPlain()
                .serialVersionUid(42L)
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Field field = type.getDeclaredField("serialVersionUID");
        field.setAccessible(true);
        assertThat((Long) field.get(null), is(42L));
        assertThat(field.getType(), is((Object) long.class));
        assertThat(field.getModifiers(), is(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL));
    }

    @Test
    public void testTypeVariable() throws Exception {
        Class<?> type = createPlain()
                .typeVariable(FOO)
                .typeVariable(BAR, String.class)
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getTypeParameters().length, is(2));
        assertThat(type.getTypeParameters()[0].getName(), is(FOO));
        assertThat(type.getTypeParameters()[0].getBounds().length, is(1));
        assertThat(type.getTypeParameters()[0].getBounds()[0], is((Object) Object.class));
        assertThat(type.getTypeParameters()[1].getName(), is(BAR));
        assertThat(type.getTypeParameters()[1].getBounds().length, is(1));
        assertThat(type.getTypeParameters()[1].getBounds()[0], is((Object) String.class));
    }

    @Test
    public void testTypeVariableTransformation() throws Exception {
        Class<?> type = createPlain()
                .typeVariable(FOO)
                .typeVariable(BAR, String.class)
                .transform(named(BAR), new Transformer<TypeVariableToken>() {
                    public TypeVariableToken transform(TypeDescription instrumentedType, TypeVariableToken target) {
                        return new TypeVariableToken(target.getSymbol(), Collections.singletonList(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Integer.class)));
                    }
                })
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getTypeParameters().length, is(2));
        assertThat(type.getTypeParameters()[0].getName(), is(FOO));
        assertThat(type.getTypeParameters()[0].getBounds().length, is(1));
        assertThat(type.getTypeParameters()[0].getBounds()[0], is((Object) Object.class));
        assertThat(type.getTypeParameters()[1].getName(), is(BAR));
        assertThat(type.getTypeParameters()[1].getBounds().length, is(1));
        assertThat(type.getTypeParameters()[1].getBounds()[0], is((Object) Integer.class));
    }

    @Test
    public void testGenericFieldDefinition() throws Exception {
        Class<?> type = createPlain()
                .defineField(QUX, list)
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredField(QUX).getGenericType(), is(list));
    }

    @Test
    public void testGenericMethodDefinition() throws Exception {
        Class<?> type = createPlain()
                .defineMethod(QUX, list)
                .withParameter(list, BAR, ProvisioningState.MANDATED)
                .throwing(fooVariable)
                .typeVariable(FOO, Exception.class)
                .intercept(StubMethod.INSTANCE)
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(QUX, List.class).getTypeParameters().length, is(1));
        assertThat(type.getDeclaredMethod(QUX, List.class).getTypeParameters()[0].getName(), is(FOO));
        assertThat(type.getDeclaredMethod(QUX, List.class).getTypeParameters()[0].getBounds().length, is(1));
        assertThat(type.getDeclaredMethod(QUX, List.class).getTypeParameters()[0].getBounds()[0], is((Object) Exception.class));
        assertThat(type.getDeclaredMethod(QUX, List.class).getGenericReturnType(), is(list));
        assertThat(type.getDeclaredMethod(QUX, List.class).getGenericExceptionTypes()[0], is((Type) type.getDeclaredMethod(QUX, List.class).getTypeParameters()[0]));
        assertThat(type.getDeclaredMethod(QUX, List.class).getGenericParameterTypes().length, is(1));
        assertThat(type.getDeclaredMethod(QUX, List.class).getGenericParameterTypes()[0], is(list));
    }

    @Test
    public void testHashCodeMethod() throws Exception {
        Class<?> type = createPlain()
                .defineField(FOO, String.class, Visibility.PUBLIC)
                .withHashCodeEquals()
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object left = type.getDeclaredConstructor().newInstance(), right = type.getDeclaredConstructor().newInstance();
        left.getClass().getDeclaredField(FOO).set(left, FOO);
        right.getClass().getDeclaredField(FOO).set(right, FOO);
        assertThat(left.hashCode(), is(right.hashCode()));
        assertThat(left, is(right));
    }

    @Test
    public void testToString() throws Exception {
        Class<?> type = createPlain()
                .defineField(FOO, String.class, Visibility.PUBLIC)
                .withToString()
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        instance.getClass().getDeclaredField(FOO).set(instance, BAR);
        assertThat(instance.toString(), CoreMatchers.endsWith("{foo=bar}"));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testGenericMethodDefinitionMetaDataParameter() throws Exception {
        Class<?> type = createPlain()
                .defineMethod(QUX, list)
                .withParameter(list, BAR, ProvisioningState.MANDATED)
                .throwing(fooVariable)
                .typeVariable(FOO, Exception.class)
                .intercept(StubMethod.INSTANCE)
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(TypeDefinition.Sort.describe(type).getDeclaredMethods().filter(named(QUX)).getOnly().getParameters().getOnly().getName(), is(BAR));
        assertThat(TypeDefinition.Sort.describe(type).getDeclaredMethods().filter(named(QUX)).getOnly().getParameters().getOnly().getModifiers(),
                is(ProvisioningState.MANDATED.getMask()));
    }

    @Test(expected = ClassFormatError.class)
    public void testUnvalidated() throws Exception {
        createPlainWithoutValidation()
                .defineField(FOO, void.class)
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeVariableOnTypeAnnotationClassBound() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = TypeDescription.ForLoadedType.of(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Class<?> type = createPlain()
                .typeVariable(FOO, TypeDescription.Generic.Builder.rawType(Object.class)
                        .build(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE * 2).build()))
                .annotateTypeVariable(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE).build())
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getTypeParameters().length, is(1));
        assertThat(type.getTypeParameters()[0].getBounds().length, is(1));
        assertThat(type.getTypeParameters()[0].getBounds()[0], is((Object) Object.class));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(type.getTypeParameters()[0]).asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(type.getTypeParameters()[0]).asList().ofType(typeAnnotationType)
                .getValue(value).resolve(Integer.class), is(INTEGER_VALUE));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(type.getTypeParameters()[0]).ofTypeVariableBoundType(0)
                .asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(type.getTypeParameters()[0]).ofTypeVariableBoundType(0)
                .asList().ofType(typeAnnotationType).getValue(value).resolve(Integer.class), is(INTEGER_VALUE * 2));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeVariableOnTypeAnnotationInterfaceBound() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = TypeDescription.ForLoadedType.of(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Class<?> type = createPlain()
                .typeVariable(FOO, TypeDescription.Generic.Builder.rawType(Runnable.class)
                        .build(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE * 2).build()))
                .annotateTypeVariable(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE).build())
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getTypeParameters().length, is(1));
        assertThat(type.getTypeParameters()[0].getBounds().length, is(1));
        assertThat(type.getTypeParameters()[0].getBounds()[0], is((Object) Runnable.class));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(type.getTypeParameters()[0]).asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(type.getTypeParameters()[0]).asList().ofType(typeAnnotationType)
                .getValue(value).resolve(Integer.class), is(INTEGER_VALUE));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(type.getTypeParameters()[0]).ofTypeVariableBoundType(0)
                .asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(type.getTypeParameters()[0]).ofTypeVariableBoundType(0)
                .asList().ofType(typeAnnotationType).getValue(value).resolve(Integer.class), is(INTEGER_VALUE * 2));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeVariableOnTypeAnnotationTypeVariableBound() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = TypeDescription.ForLoadedType.of(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Class<?> type = createPlain()
                .typeVariable(FOO)
                .annotateTypeVariable(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE).build())
                .typeVariable(BAR, TypeDescription.Generic.Builder.rawType(Object.class)
                        .build(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE * 3).build()))
                .annotateTypeVariable(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE * 2).build())
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getTypeParameters().length, is(2));
        assertThat(type.getTypeParameters()[0].getBounds().length, is(1));
        assertThat(type.getTypeParameters()[0].getBounds()[0], is((Object) Object.class));
        assertThat(type.getTypeParameters()[1].getBounds().length, is(1));
        assertThat(type.getTypeParameters()[1].getBounds()[0], is((Object) Object.class));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(type.getTypeParameters()[0]).asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(type.getTypeParameters()[0]).asList().ofType(typeAnnotationType)
                .getValue(value).resolve(Integer.class), is(INTEGER_VALUE));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(type.getTypeParameters()[1]).asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(type.getTypeParameters()[1]).asList().ofType(typeAnnotationType)
                .getValue(value).resolve(Integer.class), is(INTEGER_VALUE * 2));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(type.getTypeParameters()[1]).ofTypeVariableBoundType(0)
                .asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(type.getTypeParameters()[1]).ofTypeVariableBoundType(0)
                .asList().ofType(typeAnnotationType).getValue(value).resolve(Integer.class), is(INTEGER_VALUE * 3));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeAnnotationOnInterfaceType() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = TypeDescription.ForLoadedType.of(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Class<?> type = createPlain()
                .merge(TypeManifestation.ABSTRACT)
                .implement(TypeDescription.Generic.Builder.rawType(Runnable.class)
                        .build(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE).build()))
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getInterfaces().length, is(1));
        assertThat(type.getInterfaces()[0], is((Object) Runnable.class));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedInterface(type, 0).asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedInterface(type, 0).asList().ofType(typeAnnotationType)
                .getValue(value).resolve(Integer.class), is(INTEGER_VALUE));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testAnnotationTypeOnFieldType() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = TypeDescription.ForLoadedType.of(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Field field = createPlain()
                .defineField(FOO, TypeDescription.Generic.Builder.rawType(Object.class)
                        .build(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE).build()))
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getDeclaredField(FOO);
        assertThat(field.getType(), is((Object) Object.class));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedField(field).asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedField(field).asList().ofType(typeAnnotationType)
                .getValue(value).resolve(Integer.class), is(INTEGER_VALUE));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeVariableOnMethodAnnotationClassBound() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = TypeDescription.ForLoadedType.of(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Method method = createPlain()
                .merge(TypeManifestation.ABSTRACT)
                .defineMethod(FOO, void.class)
                .typeVariable(FOO, TypeDescription.Generic.Builder.rawType(Object.class)
                        .build(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE * 2).build()))
                .annotateTypeVariable(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE).build())
                .withoutCode()
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getDeclaredMethod(FOO);
        assertThat(method.getTypeParameters().length, is(1));
        assertThat(method.getTypeParameters()[0].getBounds().length, is(1));
        assertThat(method.getTypeParameters()[0].getBounds()[0], is((Object) Object.class));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(method.getTypeParameters()[0]).asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(method.getTypeParameters()[0]).asList().ofType(typeAnnotationType)
                .getValue(value).resolve(Integer.class), is(INTEGER_VALUE));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(method.getTypeParameters()[0]).ofTypeVariableBoundType(0)
                .asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(method.getTypeParameters()[0]).ofTypeVariableBoundType(0)
                .asList().ofType(typeAnnotationType).getValue(value).resolve(Integer.class), is(INTEGER_VALUE * 2));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeVariableOnMethodAnnotationInterfaceBound() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = TypeDescription.ForLoadedType.of(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Method method = createPlain()
                .merge(TypeManifestation.ABSTRACT)
                .defineMethod(FOO, void.class)
                .typeVariable(FOO, TypeDescription.Generic.Builder.rawType(Runnable.class)
                        .build(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE * 2).build()))
                .annotateTypeVariable(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE).build())
                .withoutCode()
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getDeclaredMethod(FOO);
        assertThat(method.getTypeParameters().length, is(1));
        assertThat(method.getTypeParameters()[0].getBounds().length, is(1));
        assertThat(method.getTypeParameters()[0].getBounds()[0], is((Object) Runnable.class));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(method.getTypeParameters()[0]).asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(method.getTypeParameters()[0]).asList().ofType(typeAnnotationType)
                .getValue(value).resolve(Integer.class), is(INTEGER_VALUE));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(method.getTypeParameters()[0]).ofTypeVariableBoundType(0)
                .asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(method.getTypeParameters()[0]).ofTypeVariableBoundType(0)
                .asList().ofType(typeAnnotationType).getValue(value).resolve(Integer.class), is(INTEGER_VALUE * 2));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testTypeVariableOnMethodAnnotationTypeVariableBound() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = TypeDescription.ForLoadedType.of(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Method method = createPlain()
                .merge(TypeManifestation.ABSTRACT)
                .defineMethod(FOO, void.class)
                .typeVariable(FOO).annotateTypeVariable(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE).build())
                .typeVariable(BAR, TypeDescription.Generic.Builder.rawType(Object.class)
                        .build(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE * 3).build()))
                .annotateTypeVariable(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE * 2).build())
                .withoutCode()
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getDeclaredMethod(FOO);
        assertThat(method.getTypeParameters().length, is(2));
        assertThat(method.getTypeParameters()[0].getBounds().length, is(1));
        assertThat(method.getTypeParameters()[0].getBounds()[0], is((Object) Object.class));
        assertThat(method.getTypeParameters()[1].getBounds().length, is(1));
        assertThat(method.getTypeParameters()[1].getBounds()[0], is((Object) Object.class));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(method.getTypeParameters()[0]).asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(method.getTypeParameters()[0]).asList().ofType(typeAnnotationType)
                .getValue(value).resolve(Integer.class), is(INTEGER_VALUE));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(method.getTypeParameters()[1]).asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(method.getTypeParameters()[1]).asList().ofType(typeAnnotationType)
                .getValue(value).resolve(Integer.class), is(INTEGER_VALUE * 2));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(method.getTypeParameters()[1]).ofTypeVariableBoundType(0)
                .asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedTypeVariable(method.getTypeParameters()[1]).ofTypeVariableBoundType(0)
                .asList().ofType(typeAnnotationType).getValue(value).resolve(Integer.class), is(INTEGER_VALUE * 3));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testAnnotationTypeOnMethodReturnType() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = TypeDescription.ForLoadedType.of(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Method method = createPlain()
                .merge(TypeManifestation.ABSTRACT)
                .defineMethod(FOO, TypeDescription.Generic.Builder.rawType(Object.class)
                        .build(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE).build()))
                .withoutCode()
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getDeclaredMethod(FOO);
        assertThat(method.getReturnType(), is((Object) Object.class));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedMethodReturnType(method).asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedMethodReturnType(method).asList().ofType(typeAnnotationType)
                .getValue(value).resolve(Integer.class), is(INTEGER_VALUE));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testAnnotationTypeOnMethodParameterType() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = TypeDescription.ForLoadedType.of(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Method method = createPlain()
                .merge(TypeManifestation.ABSTRACT)
                .defineMethod(FOO, void.class).withParameters(TypeDescription.Generic.Builder.rawType(Object.class)
                        .build(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE).build()))
                .withoutCode()
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getDeclaredMethod(FOO, Object.class);
        assertThat(method.getParameterTypes().length, is(1));
        assertThat(method.getParameterTypes()[0], is((Object) Object.class));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedExecutableParameterType(method, 0).asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedExecutableParameterType(method, 0).asList().ofType(typeAnnotationType)
                .getValue(value).resolve(Integer.class), is(INTEGER_VALUE));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testAnnotationTypeOnMethodExceptionType() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = TypeDescription.ForLoadedType.of(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Method method = createPlain()
                .merge(TypeManifestation.ABSTRACT)
                .defineMethod(FOO, void.class).throwing(TypeDescription.Generic.Builder.rawType(Exception.class)
                        .build(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE).build()))
                .withoutCode()
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getDeclaredMethod(FOO);
        assertThat(method.getExceptionTypes().length, is(1));
        assertThat(method.getExceptionTypes()[0], is((Object) Exception.class));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedExecutableExceptionType(method, 0).asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedExecutableExceptionType(method, 0).asList().ofType(typeAnnotationType)
                .getValue(value).resolve(Integer.class), is(INTEGER_VALUE));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testAnnotationTypeOnWildcardWithoutBound() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = TypeDescription.ForLoadedType.of(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Field field = createPlain()
                .defineField(FOO, TypeDescription.Generic.Builder.parameterizedType(TypeDescription.ForLoadedType.of(Collection.class),
                        TypeDescription.Generic.Builder.unboundWildcard(AnnotationDescription.Builder.ofType(typeAnnotationType)
                                .define(VALUE, INTEGER_VALUE).build())).build())
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getDeclaredField(FOO);
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedField(field).ofTypeArgument(0).asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedField(field).ofTypeArgument(0).asList().ofType(typeAnnotationType)
                .getValue(value).resolve(Integer.class), is(INTEGER_VALUE));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testAnnotationTypeOnWildcardUpperBoundBound() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = TypeDescription.ForLoadedType.of(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Field field = createPlain()
                .defineField(FOO, TypeDescription.Generic.Builder.parameterizedType(TypeDescription.ForLoadedType.of(Collection.class),
                        TypeDescription.Generic.Builder.rawType(Object.class)
                                .annotate(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE).build())
                                .asWildcardUpperBound()).build())
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getDeclaredField(FOO);
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedField(field).ofTypeArgument(0).ofWildcardUpperBoundType(0).asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedField(field).ofTypeArgument(0).ofWildcardUpperBoundType(0).asList()
                .ofType(typeAnnotationType).getValue(value).resolve(Integer.class), is(INTEGER_VALUE));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testAnnotationTypeOnWildcardLowerBoundBound() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = TypeDescription.ForLoadedType.of(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Field field = createPlain()
                .defineField(FOO, TypeDescription.Generic.Builder.parameterizedType(TypeDescription.ForLoadedType.of(Collection.class),
                        TypeDescription.Generic.Builder.rawType(Object.class)
                                .annotate(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE).build())
                                .asWildcardLowerBound()).build())
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getDeclaredField(FOO);
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedField(field).ofTypeArgument(0).ofWildcardLowerBoundType(0).asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedField(field).ofTypeArgument(0).ofWildcardLowerBoundType(0).asList()
                .ofType(typeAnnotationType).getValue(value).resolve(Integer.class), is(INTEGER_VALUE));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testAnnotationTypeOnGenericComponentType() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = TypeDescription.ForLoadedType.of(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Field field = createPlain()
                .defineField(FOO, TypeDescription.Generic.Builder.parameterizedType(TypeDescription.ForLoadedType.of(Collection.class),
                                TypeDescription.Generic.Builder.unboundWildcard())
                        .annotate(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE).build())
                        .asArray()
                        .build())
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getDeclaredField(FOO);
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedField(field).ofComponentType().asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedField(field).ofComponentType().asList()
                .ofType(typeAnnotationType).getValue(value).resolve(Integer.class), is(INTEGER_VALUE));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testAnnotationTypeOnNonGenericComponentType() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = TypeDescription.ForLoadedType.of(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Field field = createPlain()
                .defineField(FOO, TypeDescription.Generic.Builder.rawType(Object.class)
                        .annotate(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE).build())
                        .asArray()
                        .build())
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getDeclaredField(FOO);
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedField(field).ofComponentType().asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedField(field).ofComponentType().asList()
                .ofType(typeAnnotationType).getValue(value).resolve(Integer.class), is(INTEGER_VALUE));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testAnnotationTypeOnParameterizedType() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = TypeDescription.ForLoadedType.of(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Field field = createPlain()
                .defineField(FOO, TypeDescription.Generic.Builder.parameterizedType(TypeDescription.ForLoadedType.of(Collection.class),
                                TypeDescription.Generic.Builder.unboundWildcard(AnnotationDescription.Builder.ofType(typeAnnotationType)
                                        .define(VALUE, INTEGER_VALUE).build()))
                        .build())
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getDeclaredField(FOO);
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedField(field).ofTypeArgument(0).asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedField(field).ofTypeArgument(0).asList()
                .ofType(typeAnnotationType).getValue(value).resolve(Integer.class), is(INTEGER_VALUE));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @SuppressWarnings("unchecked")
    public void testAnnotationTypeOnNestedType() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = TypeDescription.ForLoadedType.of(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Field field = createPlain()
                .defineField(FOO, TypeDescription.Generic.Builder.rawType(TypeDescription.ForLoadedType.of(Nested.Inner.class),
                                TypeDescription.Generic.Builder.rawType(Nested.class).build())
                        .annotate(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE).build())
                        .build())
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getDeclaredField(FOO);
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedField(field).asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedField(field).asList()
                .ofType(typeAnnotationType).getValue(value).resolve(Integer.class), is(INTEGER_VALUE));
    }

    @Test
    @JavaVersionRule.Enforce(15)
    @SuppressWarnings("unchecked")
    public void testAnnotationTypeOnNestedParameterizedType() throws Exception {
        Class<? extends Annotation> typeAnnotationType = (Class<? extends Annotation>) Class.forName(TYPE_VARIABLE_NAME);
        MethodDescription.InDefinedShape value = TypeDescription.ForLoadedType.of(typeAnnotationType).getDeclaredMethods().filter(named(VALUE)).getOnly();
        Field field = createPlain()
                .defineField(FOO, TypeDescription.Generic.Builder.parameterizedType(TypeDescription.ForLoadedType.of(GenericNested.Inner.class),
                                TypeDescription.Generic.Builder.parameterizedType(GenericNested.class, Void.class).build(),
                                Collections.<TypeDefinition>emptyList())
                        .annotate(AnnotationDescription.Builder.ofType(typeAnnotationType).define(VALUE, INTEGER_VALUE).build())
                        .build())
                .make()
                .load(typeAnnotationType.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getDeclaredField(FOO);
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedField(field).asList().size(), is(1));
        assertThat(new TypeDescription.Generic.AnnotationReader.Delegator.ForLoadedField(field).asList()
                .ofType(typeAnnotationType).getValue(value).resolve(Integer.class), is(INTEGER_VALUE));
    }

    @Test
    public void testBridgeResolutionAmbiguous() throws Exception {
        Class<?> type = createPlain()
                .defineMethod(QUX, String.class, Visibility.PUBLIC)
                .intercept(FixedValue.value(FOO))
                .defineMethod(QUX, Object.class, Visibility.PUBLIC)
                .intercept(FixedValue.value(BAR))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        for (Method method : type.getDeclaredMethods()) {
            if (method.getReturnType() == String.class) {
                assertThat(method.getName(), is(QUX));
                assertThat(method.getParameterTypes().length, is(0));
                assertThat(method.invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
            } else if (method.getReturnType() == Object.class) {
                assertThat(method.getName(), is(QUX));
                assertThat(method.getParameterTypes().length, is(0));
                assertThat(method.invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
            } else {
                throw new AssertionError();
            }
        }
    }

    @Test
    public void testCanOverloadMethodByReturnType() throws Exception {
        Class<?> type = createPlain()
                .defineMethod(QUX, String.class, Visibility.PUBLIC)
                .intercept(FixedValue.value(FOO))
                .defineMethod(QUX, Object.class, Ownership.STATIC, Visibility.PUBLIC) // Is static to avoid method graph compiler.
                .intercept(FixedValue.value(BAR))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        for (Method method : type.getDeclaredMethods()) {
            if (method.getReturnType() == String.class) {
                assertThat(method.getName(), is(QUX));
                assertThat(method.getParameterTypes().length, is(0));
                assertThat(method.invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
            } else if (method.getReturnType() == Object.class) {
                assertThat(method.getName(), is(QUX));
                assertThat(method.getParameterTypes().length, is(0));
                assertThat(method.invoke(null), is((Object) BAR));
            } else {
                throw new AssertionError();
            }
        }
    }

    @Test
    public void testCanOverloadFieldByType() throws Exception {
        Class<?> type = createPlain()
                .defineField(QUX, String.class, Ownership.STATIC, Visibility.PUBLIC)
                .value(FOO)
                .defineField(QUX, long.class, Ownership.STATIC, Visibility.PUBLIC)
                .value(42L)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        for (Field field : type.getDeclaredFields()) {
            if (field.getType() == String.class) {
                assertThat(field.getName(), is(QUX));
                assertThat(field.get(null), is((Object) FOO));
            } else if (field.getType() == long.class) {
                assertThat(field.getName(), is(QUX));
                assertThat(field.get(null), is((Object) 42L));
            } else {
                throw new AssertionError();
            }
        }
    }

    @Test
    public void testInterfaceInterception() throws Exception {
        assertThat(((SampleInterface) createPlain()
                .implement(SampleInterface.class)
                .intercept(FixedValue.value(FOO))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()).foo(), is(FOO));
    }

    @Test
    public void testInterfaceInterceptionPreviousSuperseeded() throws Exception {
        assertThat(((SampleInterface) createPlain()
                .method(named(FOO))
                .intercept(ExceptionMethod.throwing(AssertionError.class))
                .implement(SampleInterface.class)
                .intercept(FixedValue.value(FOO))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()).foo(), is(FOO));
    }

    @Test
    public void testInterfaceInterceptionLaterSuperseeding() throws Exception {
        assertThat(((SampleInterface) createPlain()
                .implement(SampleInterface.class)
                .intercept(ExceptionMethod.throwing(AssertionError.class))
                .method(named(FOO))
                .intercept(FixedValue.value(FOO))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()).foo(), is(FOO));
    }

    @Test
    public void testInterfaceInterceptionSubClass() throws Exception {
        assertThat(((SampleInterface) createPlain()
                .implement(SampleInterface.SubInterface.class)
                .intercept(FixedValue.value(FOO))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()).foo(), is(FOO));
    }

    @Test
    public void testInterfaceMakesClassMethodPublic() throws Exception {
        Class<?> type = createPlain()
                .implement(Cloneable.class)
                .method(named("clone"))
                .intercept(FixedValue.self())
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        Cloneable cloneable = (Cloneable) type.getDeclaredConstructor().newInstance();
        assertThat(cloneable.clone(), sameInstance((Object) cloneable));
    }

    @Test
    public void testTopLevelType() throws Exception {
        Class<?> type = createPlainWithoutValidation()
                .topLevelType()
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getDeclaringClass(), nullValue(Class.class));
        assertThat(type.getEnclosingClass(), nullValue(Class.class));
        assertThat(type.getEnclosingMethod(), nullValue(Method.class));
        assertThat(type.getEnclosingConstructor(), nullValue(Constructor.class));
        assertThat(type.isAnonymousClass(), is(false));
        assertThat(type.isLocalClass(), is(false));
        assertThat(type.isMemberClass(), is(false));
    }

    @Test
    public void testDeclaredAsMemberType() throws Exception {
        TypeDescription sample = new TypeDescription.Latent("foo.Bar$Qux", Opcodes.ACC_PUBLIC, TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)) {
            @Override
            public String getSimpleName() {
                return "Qux";
            }

            @Override
            public boolean isAnonymousType() {
                return false;
            }

            @Override
            public boolean isMemberType() {
                return true;
            }
        };
        Class<?> outer = new ByteBuddy()
                .subclass(Object.class)
                .name("foo.Bar")
                .declaredTypes(sample)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST.opened())
                .getLoaded();
        Class<?> type = createPlainWithoutValidation()
                .name(sample.getName())
                .innerTypeOf(outer).asMemberType()
                .make()
                .load((InjectionClassLoader) outer.getClassLoader(), InjectionClassLoader.Strategy.INSTANCE)
                .getLoaded();
        assertThat(type.getDeclaringClass(), is((Object) outer));
        assertThat(type.getEnclosingClass(), is((Object) outer));
        assertThat(type.getEnclosingMethod(), nullValue(Method.class));
        assertThat(type.getEnclosingConstructor(), nullValue(Constructor.class));
        assertThat(type.isAnonymousClass(), is(false));
        assertThat(type.isLocalClass(), is(false));
        assertThat(type.isMemberClass(), is(true));
    }

    @Test
    public void testDeclaredAsAnonymousType() throws Exception {
        // Older JVMs derive the anonymous class property from a naming convention.
        TypeDescription sample = new TypeDescription.Latent("foo.Bar$1", Opcodes.ACC_PUBLIC, TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)) {
            @Override
            public String getSimpleName() {
                return "";
            }

            @Override
            public boolean isAnonymousType() {
                return true;
            }

            @Override
            public boolean isMemberType() {
                return false;
            }
        };
        Class<?> outer = new ByteBuddy()
                .subclass(Object.class)
                .name("foo.Bar")
                .declaredTypes(sample)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST.opened())
                .getLoaded();
        Class<?> type = createPlainWithoutValidation()
                .name(sample.getName())
                .innerTypeOf(outer).asAnonymousType()
                .make()
                .load((InjectionClassLoader) outer.getClassLoader(), InjectionClassLoader.Strategy.INSTANCE)
                .getLoaded();
        assertThat(type.getDeclaringClass(), nullValue(Class.class));
        assertThat(type.getEnclosingClass(), is((Object) outer));
        assertThat(type.getEnclosingMethod(), nullValue(Method.class));
        assertThat(type.getEnclosingConstructor(), nullValue(Constructor.class));
        assertThat(type.isAnonymousClass(), is(true));
        assertThat(type.isLocalClass(), is(false));
        assertThat(type.isMemberClass(), is(false));
    }

    @Test
    public void testDeclaredAsLocalType() throws Exception {
        TypeDescription sample = new TypeDescription.Latent("foo.Bar$Qux", Opcodes.ACC_PUBLIC, TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)) {
            @Override
            public String getSimpleName() {
                return "Qux";
            }

            @Override
            public boolean isAnonymousType() {
                return false;
            }

            @Override
            public boolean isMemberType() {
                return false;
            }
        };
        Class<?> outer = new ByteBuddy()
                .subclass(Object.class)
                .name("foo.Bar")
                .declaredTypes(sample)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST.opened())
                .getLoaded();
        Class<?> type = createPlainWithoutValidation()
                .name(sample.getName())
                .innerTypeOf(outer)
                .make()
                .load((InjectionClassLoader) outer.getClassLoader(), InjectionClassLoader.Strategy.INSTANCE)
                .getLoaded();
        assertThat(type.getDeclaringClass(), nullValue(Class.class));
        assertThat(type.getEnclosingClass(), is((Object) outer));
        assertThat(type.getEnclosingMethod(), nullValue(Method.class));
        assertThat(type.getEnclosingConstructor(), nullValue(Constructor.class));
        assertThat(type.isAnonymousClass(), is(false));
        assertThat(type.isLocalClass(), is(true));
        assertThat(type.isMemberClass(), is(false));
    }

    @Test
    public void testDeclaredAsAnonymousTypeInMethod() throws Exception {
        // Older JVMs derive the anonymous class property from a naming convention.
        TypeDescription sample = new TypeDescription.Latent("foo.Bar$1", Opcodes.ACC_PUBLIC, TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)) {
            @Override
            public String getSimpleName() {
                return "";
            }

            @Override
            public boolean isAnonymousType() {
                return true;
            }

            @Override
            public boolean isMemberType() {
                return false;
            }
        };
        Class<?> outer = new ByteBuddy()
                .subclass(Object.class)
                .name("foo.Bar")
                .declaredTypes(sample)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST.opened())
                .getLoaded();
        Class<?> type = createPlainWithoutValidation()
                .name(sample.getName())
                .innerTypeOf(outer.getConstructor()).asAnonymousType()
                .make()
                .load((InjectionClassLoader) outer.getClassLoader(), InjectionClassLoader.Strategy.INSTANCE)
                .getLoaded();
        assertThat(type.getDeclaringClass(), nullValue(Class.class));
        assertThat(type.getEnclosingClass(), is((Object) outer));
        assertThat(type.getEnclosingMethod(), nullValue(Method.class));
        assertThat(type.getEnclosingConstructor(), is((Object) outer.getConstructor()));
        assertThat(type.isAnonymousClass(), is(true));
        assertThat(type.isLocalClass(), is(false));
        assertThat(type.isMemberClass(), is(false));
    }

    @Test
    public void testDeclaredAsLocalTypeInInitializer() throws Exception {
        // Older JVMs derive the anonymous class property from a naming convention.
        TypeDescription sample = new TypeDescription.Latent("foo.Bar$Qux", Opcodes.ACC_PUBLIC, TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)) {
            @Override
            public String getSimpleName() {
                return "Qux";
            }

            @Override
            public boolean isAnonymousType() {
                return false;
            }

            @Override
            public boolean isMemberType() {
                return false;
            }
        };
        Class<?> outer = new ByteBuddy()
                .subclass(Object.class)
                .name("foo.Bar")
                .declaredTypes(sample)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST.opened())
                .getLoaded();
        Class<?> type = createPlainWithoutValidation()
                .name(sample.getName())
                .innerTypeOf(new MethodDescription.Latent.TypeInitializer(TypeDescription.ForLoadedType.of(outer)))
                .make()
                .load((InjectionClassLoader) outer.getClassLoader(), InjectionClassLoader.Strategy.INSTANCE)
                .getLoaded();
        assertThat(type.getDeclaringClass(), nullValue(Class.class));
        assertThat(type.getEnclosingClass(), is((Object) outer));
        assertThat(type.getEnclosingMethod(), nullValue(Method.class));
        assertThat(type.getEnclosingConstructor(), nullValue(Constructor.class));
        assertThat(type.isAnonymousClass(), is(false));
        assertThat(type.isLocalClass(), is(true));
        assertThat(type.isMemberClass(), is(false));
    }

    @Test
    public void testDeclaredAsAnonymousTypeInInitializer() throws Exception {
        // Older JVMs derive the anonymous class property from a naming convention.
        TypeDescription sample = new TypeDescription.Latent("foo.Bar$1", Opcodes.ACC_PUBLIC, TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)) {
            @Override
            public String getSimpleName() {
                return "";
            }

            @Override
            public boolean isAnonymousType() {
                return true;
            }

            @Override
            public boolean isMemberType() {
                return false;
            }
        };
        Class<?> outer = new ByteBuddy()
                .subclass(Object.class)
                .name("foo.Bar")
                .declaredTypes(sample)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST.opened())
                .getLoaded();
        Class<?> type = createPlainWithoutValidation()
                .name(sample.getName())
                .innerTypeOf(new MethodDescription.Latent.TypeInitializer(TypeDescription.ForLoadedType.of(outer))).asAnonymousType()
                .make()
                .load((InjectionClassLoader) outer.getClassLoader(), InjectionClassLoader.Strategy.INSTANCE)
                .getLoaded();
        assertThat(type.getDeclaringClass(), nullValue(Class.class));
        assertThat(type.getEnclosingClass(), is((Object) outer));
        assertThat(type.getEnclosingMethod(), nullValue(Method.class));
        assertThat(type.getEnclosingConstructor(), nullValue(Constructor.class));
        assertThat(type.isAnonymousClass(), is(true));
        assertThat(type.isLocalClass(), is(false));
        assertThat(type.isMemberClass(), is(false));
    }

    @Test
    public void testDeclaredAsLocalTypeInMethod() throws Exception {
        TypeDescription sample = new TypeDescription.Latent("foo.Bar$Qux", Opcodes.ACC_PUBLIC, TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)) {
            @Override
            public String getSimpleName() {
                return "Qux";
            }

            @Override
            public boolean isAnonymousType() {
                return false;
            }

            @Override
            public boolean isMemberType() {
                return false;
            }
        };
        Class<?> outer = new ByteBuddy()
                .subclass(Object.class)
                .name("foo.Bar")
                .defineMethod("foo", void.class, Visibility.PUBLIC)
                .intercept(StubMethod.INSTANCE)
                .declaredTypes(sample)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST.opened())
                .getLoaded();
        Class<?> type = createPlainWithoutValidation()
                .name(sample.getName())
                .innerTypeOf(outer.getMethod(FOO))
                .make()
                .load((InjectionClassLoader) outer.getClassLoader(), InjectionClassLoader.Strategy.INSTANCE)
                .getLoaded();
        assertThat(type.getDeclaringClass(), nullValue(Class.class));
        assertThat(type.getEnclosingClass(), is((Object) outer));
        assertThat(type.getEnclosingMethod(), is(outer.getMethod(FOO)));
        assertThat(type.getEnclosingConstructor(), nullValue(Constructor.class));
        assertThat(type.isAnonymousClass(), is(false));
        assertThat(type.isLocalClass(), is(true));
        assertThat(type.isMemberClass(), is(false));
    }

    @Test
    public void testDeclaredAsLocalTypeInConstructor() throws Exception {
        TypeDescription sample = new TypeDescription.Latent("foo.Bar$Qux", Opcodes.ACC_PUBLIC, TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)) {
            @Override
            public String getSimpleName() {
                return "Qux";
            }

            @Override
            public boolean isAnonymousType() {
                return false;
            }

            @Override
            public boolean isMemberType() {
                return false;
            }
        };
        Class<?> outer = new ByteBuddy()
                .subclass(Object.class)
                .name("foo.Bar")
                .declaredTypes(sample)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST.opened())
                .getLoaded();
        Class<?> type = createPlainWithoutValidation()
                .name(sample.getName())
                .innerTypeOf(outer.getConstructor())
                .make()
                .load((InjectionClassLoader) outer.getClassLoader(), InjectionClassLoader.Strategy.INSTANCE)
                .getLoaded();
        assertThat(type.getDeclaringClass(), nullValue(Class.class));
        assertThat(type.getEnclosingClass(), is((Object) outer));
        assertThat(type.getEnclosingMethod(), nullValue(Method.class));
        assertThat(type.getEnclosingConstructor(), is((Object) outer.getConstructor()));
        assertThat(type.isAnonymousClass(), is(false));
        assertThat(type.isLocalClass(), is(true));
        assertThat(type.isMemberClass(), is(false));
    }

    @Test
    @JavaVersionRule.Enforce(11)
    public void testNestMates() throws Exception {
        TypeDescription sample = new TypeDescription.Latent("foo.Bar", Opcodes.ACC_PUBLIC, TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class));
        Class<?> outer = new ByteBuddy()
                .subclass(Object.class)
                .name("foo.Qux")
                .nestMembers(sample)
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST.opened())
                .getLoaded();
        Class<?> type = createPlainWithoutValidation()
                .visit(new JavaVersionAdjustment())
                .name(sample.getName())
                .nestHost(outer)
                .make()
                .load((InjectionClassLoader) outer.getClassLoader(), InjectionClassLoader.Strategy.INSTANCE)
                .getLoaded();
        assertThat(Class.class.getMethod("getNestHost").invoke(outer), is((Object) outer));
        assertThat(Class.class.getMethod("getNestMembers").invoke(outer), is((Object) new Class<?>[]{outer, type}));
        assertThat(Class.class.getMethod("getNestHost").invoke(type), is((Object) outer));
        assertThat(Class.class.getMethod("getNestMembers").invoke(type), is((Object) new Class<?>[]{outer, type}));
    }

    @Test
    @JavaVersionRule.Enforce(17)
    public void testPermittedSubclasses() throws Exception {
        TypeDescription sample = new TypeDescription.Latent("foo.Qux",
                Opcodes.ACC_PUBLIC,
                new TypeDescription.Latent("foo.Bar", Opcodes.ACC_PUBLIC, TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class)).asGenericType());
        Class<?> type = createPlainEmpty()
                .visit(new JavaVersionAdjustment())
                .permittedSubclass(sample)
                .name("foo.Bar")
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST.opened())
                .getLoaded();
        Class<?> subclass = new ByteBuddy()
                .subclass(type)
                .merge(TypeManifestation.FINAL)
                .name("foo.Qux")
                .make()
                .load((InjectionClassLoader) type.getClassLoader(), InjectionClassLoader.Strategy.INSTANCE)
                .getLoaded();
        Class<?>[] permittedSubclass = (Class<?>[]) Class.class.getMethod("getPermittedSubclasses").invoke(type);
        assertThat(permittedSubclass, notNullValue(Class[].class));
        assertThat(permittedSubclass.length, is(1));
        assertThat(permittedSubclass[0], is((Object) subclass));
    }

    @Test
    public void testAuxiliaryTypes() throws Exception {
        Map<TypeDescription, byte[]> auxiliaryTypes = createPlain()
                .require(TypeDescription.ForLoadedType.of(void.class), new byte[]{1, 2, 3})
                .make()
                .getAuxiliaryTypes();
        assertThat(auxiliaryTypes.size(), is(1));
        assertThat(auxiliaryTypes.get(TypeDescription.ForLoadedType.of(void.class)).length, is(3));
    }

    @Test
    public void testWrapClassVisitor() throws Exception {
        TypeDescription typeDescription = createPlain()
                .make()
                .getTypeDescription();
        AsmClassWriter classWriter = AsmClassWriter.Factory.Default.IMPLICIT.make(AsmVisitorWrapper.NO_FLAGS);
        ContextClassVisitor classVisitor = createPlain()
                .defineMethod(FOO, Object.class, Visibility.PUBLIC, Ownership.STATIC)
                .throwing(Exception.class)
                .intercept(new Implementation.Simple(new TextConstant(FOO), MethodReturn.REFERENCE))
                .wrap(classWriter.getVisitor());
        classVisitor.visit(ClassFileVersion.ofThisVm().getMinorMajorVersion(),
                typeDescription.getActualModifiers(true),
                typeDescription.getInternalName(),
                typeDescription.getGenericSignature(),
                typeDescription.getSuperClass().asErasure().getInternalName(),
                typeDescription.getInterfaces().asErasures().toInternalNames());
        classVisitor.visitEnd();
        assertThat(classVisitor.getAuxiliaryTypes().size(), is(0));
        assertThat(classVisitor.getLoadedTypeInitializer().isAlive(), is(false));
        Class<?> type = new DynamicType.Default.Unloaded<Object>(typeDescription,
                classWriter.getBinaryRepresentation(),
                LoadedTypeInitializer.NoOp.INSTANCE,
                Collections.<DynamicType>emptyList(),
                TypeResolutionStrategy.Passive.INSTANCE).load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        assertThat(type.getName(), is(typeDescription.getName()));
        Method method = type.getDeclaredMethod(FOO);
        assertThat(method.getReturnType(), CoreMatchers.<Class<?>>is(Object.class));
        assertThat(method.getExceptionTypes(), is(new Class<?>[]{Exception.class}));
        assertThat(method.getModifiers(), is(Modifier.PUBLIC | Modifier.STATIC));
        assertThat(method.invoke(null), is((Object) FOO));
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface SampleAnnotation {

        String foo();
    }

    public static class Foo {
        /* empty */
    }

    public static class Bar {

        public static String foo;

        public static void invoke() {
            foo = FOO;
        }
    }

    public static class BridgeRetention<T> extends CallTraceable {

        public T foo() {
            register(FOO);
            return null;
        }

        public static class Inner extends BridgeRetention<String> {
            /* empty */
        }
    }

    public static class CallSuperMethod<T> extends CallTraceable {

        public T foo(T value) {
            register(FOO);
            return value;
        }

        public static class Inner extends CallSuperMethod<String> {
            /* empty */
        }
    }

    private static class PreparedField implements Implementation {

        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType.withField(new FieldDescription.Token(FOO,
                    MODIFIERS,
                    TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class),
                    Collections.singletonList(AnnotationDescription.Builder.ofType(SampleAnnotation.class).define(FOO, BAR).build())));
        }

        public ByteCodeAppender appender(Target implementationTarget) {
            return new ByteCodeAppender.Simple(NullConstant.INSTANCE, MethodReturn.REFERENCE);
        }
    }

    private static class PreparedMethod implements Implementation {

        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType.withMethod(new MethodDescription.Token(FOO,
                    MODIFIERS,
                    Collections.<TypeVariableToken>emptyList(),
                    TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class),
                    Collections.singletonList(new ParameterDescription.Token(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Object.class),
                            Collections.singletonList(AnnotationDescription.Builder.ofType(SampleAnnotation.class).define(FOO, QUX).build()))),
                    Collections.singletonList(TypeDescription.Generic.OfNonGenericType.ForLoadedType.of(Exception.class)),
                    Collections.singletonList(AnnotationDescription.Builder.ofType(SampleAnnotation.class).define(FOO, BAR).build()),
                    AnnotationValue.UNDEFINED,
                    TypeDescription.Generic.UNDEFINED));
        }

        public ByteCodeAppender appender(Target implementationTarget) {
            return new ByteCodeAppender.Simple(NullConstant.INSTANCE, MethodReturn.REFERENCE);
        }
    }

    public static class InterfaceOverrideInterceptor {

        public static String intercept(@SuperCall Callable<String> zuper) throws Exception {
            return zuper.call() + BAR;
        }
    }

    @SuppressWarnings("unused")
    private static class Holder<foo> {

        List<?> list;

        List<foo> fooList;
    }

    @SuppressWarnings("unused")
    public static class Nested {

        public class Inner {
            /* empty */
        }
    }

    @SuppressWarnings("unused")
    public static class GenericNested<T> {

        public class Inner {
            /* empty */
        }
    }

    public interface Cloneable {

        Object clone();
    }

    public interface SampleInterface {

        String foo();

        interface SubInterface extends SampleInterface {

        }
    }

    private static class JavaVersionAdjustment extends AsmVisitorWrapper.AbstractBase {

        public ClassVisitor wrap(TypeDescription instrumentedType,
                                 ClassVisitor classVisitor,
                                 Implementation.Context implementationContext,
                                 TypePool typePool,
                                 FieldList<FieldDescription.InDefinedShape> fields,
                                 MethodList<?> methods,
                                 int writerFlags,
                                 int readerFlags) {
            return new ClassVisitor(OpenedClassReader.ASM_API, classVisitor) {

                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    super.visit(Math.max(version, ClassFileVersion.ofThisVm().getMinorMajorVersion()), access, name, signature, superName, interfaces);
                }
            };
        }
    }
}
