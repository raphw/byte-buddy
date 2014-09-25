package net.bytebuddy.depiction;

import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.field.FieldList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.objectweb.asm.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public interface TypePool {

    TypeDescription describe(String name);

    void clear();

    static abstract class AbstractBase implements TypePool {

        protected static final Map<String, TypeDescription> PRIMITIVE_TYPES;

        static {
            Map<String, TypeDescription> primitiveTypes = new HashMap<String, TypeDescription>();
            for (Class<?> primitiveType : new Class<?>[]{boolean.class, byte.class, short.class, char.class, int.class, long.class, float.class, double.class}) {
                primitiveTypes.put(primitiveType.getName(), new TypeDescription.ForLoadedType(primitiveType));
            }
            PRIMITIVE_TYPES = Collections.unmodifiableMap(primitiveTypes);
        }

        @Override
        public TypeDescription describe(String name) {
            int arity = 0;
            while (name.endsWith("[]")) {
                arity++;
                name = name.substring(0, name.length() - 2);
            }
            TypeDescription typeDescription = PRIMITIVE_TYPES.get(name);
            return TypeDescription.ArrayProjection.of(typeDescription == null ? doDescribe(name) : typeDescription, arity);
        }

        protected abstract TypeDescription doDescribe(String name);
    }

    static class Default extends AbstractBase {

        private static final int ASM_VERSION = Opcodes.ASM5;

        private static final int ASM_MANUAL = 0;

        public static TypePool ofClassPath() {
            return new Default(new TypeSourceLocator.ForClassLoader(ClassLoader.getSystemClassLoader()));
        }

        private final TypeSourceLocator typeSourceLocator;

        private final ConcurrentHashMap<String, TypeDescription> typeCache;

        public Default(TypeSourceLocator typeSourceLocator) {
            this.typeSourceLocator = typeSourceLocator;
            typeCache = new ConcurrentHashMap<String, TypeDescription>();
        }

        @Override
        protected TypeDescription doDescribe(String name) {
            TypeDescription typeDescription = typeCache.get(name);
            if (typeDescription != null) {
                return typeDescription;
            }
            byte[] binaryRepresentation = typeSourceLocator.locate(name);
            if (binaryRepresentation == null) {
                throw new IllegalArgumentException("Cannot locate " + name + " using " + typeSourceLocator);
            }
            typeDescription = parse(binaryRepresentation);
            TypeDescription cachedDescription = typeCache.putIfAbsent(name, typeDescription);
            return cachedDescription == null ? typeCache.get(name) : cachedDescription;
        }

        private TypeDescription parse(byte[] binaryRepresentation) {
            ClassReader classReader = new ClassReader(binaryRepresentation);
            TypeInterpreter typeInterpreter = new TypeInterpreter();
            classReader.accept(typeInterpreter, ASM_MANUAL);
            return typeInterpreter.toTypeDescription();
        }

        @Override
        public void clear() {
            typeCache.clear();
        }

        private class TypeInterpreter extends ClassVisitor {

            private int modifiers;

            private String name;

            private String superTypeName;

            private String[] interfaceName;

            private final List<UnloadedTypeDescription.FieldToken> fieldTokens;

            private final List<UnloadedTypeDescription.MethodToken> methodTokens;

            private TypeInterpreter() {
                super(ASM_VERSION);
                fieldTokens = new LinkedList<UnloadedTypeDescription.FieldToken>();
                methodTokens = new LinkedList<UnloadedTypeDescription.MethodToken>();
            }

            @Override
            public void visit(int classFileVersion,
                              int modifiers,
                              String name,
                              String genericSignature,
                              String superTypeName,
                              String[] interfaceName) {
                this.modifiers = modifiers;
                this.name = name;
                this.superTypeName = superTypeName;
                this.interfaceName = interfaceName;
            }

            @Override
            public void visitInnerClass(String name, String outerName, String innerName, int modifiers) {
                if (name.equals(this.name)) {
                    this.modifiers = modifiers;
                }
            }

            @Override
            public FieldVisitor visitField(int modifiers,
                                           String name,
                                           String descriptor,
                                           String genericSignature,
                                           Object defaultValue) {
                fieldTokens.add(new UnloadedTypeDescription.FieldToken(modifiers, name, descriptor));
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int modifiers,
                                             String name,
                                             String descriptor,
                                             String genericSignature,
                                             String[] exceptionName) {
                if (name.equals(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME)) {
                    return null;
                }
                methodTokens.add(new UnloadedTypeDescription.MethodToken(modifiers, name, descriptor, exceptionName));
                return null;
            }

            public TypeDescription toTypeDescription() {
                return new UnloadedTypeDescription(Default.this,
                        modifiers,
                        name,
                        superTypeName,
                        interfaceName,
                        fieldTokens,
                        methodTokens);
            }
        }
    }

    static class UnloadedTypeDescription extends TypeDescription.AbstractTypeDescription.ForSimpleType {

        private final TypePool typePool;

        private final int modifiers;

        private final String name;

        private final String superTypeName;

        private final String[] interfaceName;

        private final List<FieldDescription> declaredFields;

        private final List<MethodDescription> declaredMethods;

        protected UnloadedTypeDescription(TypePool typePool,
                                          int modifiers,
                                          String name,
                                          String superTypeName,
                                          String[] interfaceName,
                                          List<FieldToken> fieldTokens,
                                          List<MethodToken> methodTokens) {
            this.typePool = typePool;
            this.modifiers = modifiers;
            this.name = name.replace('/', '.');
            this.superTypeName = superTypeName.replace('/', '.');
            this.interfaceName = new String[interfaceName.length];
            int index = 0;
            for (String anInterfaceName : interfaceName) {
                this.interfaceName[index++] = anInterfaceName.replace('/', '.');
            }
            declaredFields = new ArrayList<FieldDescription>(fieldTokens.size());
            for (FieldToken fieldToken : fieldTokens) {
                declaredFields.add(fieldToken.toFieldDescription(this));
            }
            declaredMethods = new ArrayList<MethodDescription>(methodTokens.size());
            for (MethodToken methodToken : methodTokens) {
                declaredMethods.add(methodToken.toMethodDescription(this));
            }
        }

        @Override
        public TypeDescription getSupertype() {
            return typePool.describe(superTypeName);
        }

        @Override
        public TypeList getInterfaces() {
            return null;
        }

        @Override
        public MethodDescription getEnclosingMethod() {
            return null;
        }

        @Override
        public TypeDescription getEnclosingClass() {
            return null;
        }

        @Override
        public String getCanonicalName() {
            return null;
        }

        @Override
        public boolean isAnonymousClass() {
            return false;
        }

        @Override
        public boolean isLocalClass() {
            return false;
        }

        @Override
        public boolean isMemberClass() {
            return false;
        }

        @Override
        public FieldList getDeclaredFields() {
            return new FieldList.Explicit(declaredFields);
        }

        @Override
        public MethodList getDeclaredMethods() {
            return new MethodList.Explicit(declaredMethods);
        }

        @Override
        public boolean isSealed() {
            return false;
        }

        @Override
        public ClassLoader getClassLoader() {
            return null;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
            return false;
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return null;
        }

        @Override
        public Annotation[] getAnnotations() {
            return new Annotation[0];
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return new Annotation[0];
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public TypeDescription getDeclaringType() {
            return null;
        }

        @Override
        public int getModifiers() {
            return modifiers;
        }

        protected static class FieldToken {

            private final int modifiers;

            private final String name;

            private final String descriptor;

            protected FieldToken(int modifiers, String name, String descriptor) {
                this.modifiers = modifiers;
                this.name = name;
                this.descriptor = descriptor;
            }

            protected int getModifiers() {
                return modifiers;
            }

            protected String getName() {
                return name;
            }

            protected String getDescriptor() {
                return descriptor;
            }

            private FieldDescription toFieldDescription(UnloadedTypeDescription unloadedTypeDescription) {
                return unloadedTypeDescription.new UnloadedFieldDescription(getModifiers(), getName(), getDescriptor());
            }
        }

        private class UnloadedFieldDescription extends FieldDescription.AbstractFieldDescription {

            private final int modifiers;

            private final String name;

            private final String fieldTypeName;

            private UnloadedFieldDescription(int modifiers, String name, String descriptor) {
                this.modifiers = modifiers;
                this.name = name;
                fieldTypeName = Type.getType(descriptor).getClassName();
            }

            @Override
            public TypeDescription getFieldType() {
                return typePool.describe(fieldTypeName);
            }

            @Override
            public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
                return false;
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return null;
            }

            @Override
            public Annotation[] getAnnotations() {
                return new Annotation[0];
            }

            @Override
            public Annotation[] getDeclaredAnnotations() {
                return new Annotation[0];
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public TypeDescription getDeclaringType() {
                return UnloadedTypeDescription.this;
            }

            @Override
            public int getModifiers() {
                return modifiers;
            }
        }

        protected static class MethodToken {

            private final int modifiers;

            private final String name;

            private final String descriptor;

            private final String[] exceptionName;

            protected MethodToken(int modifiers, String name, String descriptor, String[] exceptionName) {
                this.modifiers = modifiers;
                this.name = name;
                this.descriptor = descriptor;
                this.exceptionName = exceptionName;
            }

            protected int getModifiers() {
                return modifiers;
            }

            protected String getName() {
                return name;
            }

            protected String getDescriptor() {
                return descriptor;
            }

            protected String[] getExceptionName() {
                return exceptionName;
            }

            private MethodDescription toMethodDescription(UnloadedTypeDescription unloadedTypeDescription) {
                return unloadedTypeDescription.new UnloadedMethodDescription(getModifiers(), getName(), getDescriptor(), getExceptionName());
            }
        }

        private class UnloadedMethodDescription extends MethodDescription.AbstractMethodDescription {

            private final int modifiers;

            private final String internalName;

            private final String returnTypeName;

            private final TypeList parameterTypes;

            private final TypeList exceptionTypes;

            private UnloadedMethodDescription(int modifiers,
                                              String internalName,
                                              String methodDescriptor,
                                              String[] exceptionInternalName) {
                this.modifiers = modifiers;
                this.internalName = internalName;
                returnTypeName = Type.getReturnType(methodDescriptor).getClassName();
                parameterTypes = new TypePoolTypeList(methodDescriptor);
                exceptionTypes = exceptionInternalName == null
                        ? new TypeList.Empty()
                        : new TypePoolTypeList(exceptionInternalName);
            }

            @Override
            public TypeDescription getReturnType() {
                return typePool.describe(returnTypeName);
            }

            @Override
            public TypeList getParameterTypes() {
                return parameterTypes;
            }

            @Override
            public Annotation[][] getParameterAnnotations() {
                return new Annotation[0][0];
            }

            @Override
            public TypeList getExceptionTypes() {
                return exceptionTypes;
            }

            @Override
            public boolean isConstructor() {
                return internalName.equals(MethodDescription.CONSTRUCTOR_INTERNAL_NAME);
            }

            @Override
            public boolean isTypeInitializer() {
                return false;
            }

            @Override
            public boolean represents(Method method) {
                return equals(new ForLoadedMethod(method));
            }

            @Override
            public boolean represents(Constructor<?> constructor) {
                return equals(new ForLoadedConstructor(constructor));
            }

            @Override
            public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
                return false;
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return null;
            }

            @Override
            public Annotation[] getAnnotations() {
                return new Annotation[0];
            }

            @Override
            public Annotation[] getDeclaredAnnotations() {
                return new Annotation[0];
            }

            @Override
            public String getInternalName() {
                return internalName;
            }

            @Override
            public TypeDescription getDeclaringType() {
                return UnloadedTypeDescription.this;
            }

            @Override
            public int getModifiers() {
                return modifiers;
            }
        }

        protected class TypePoolTypeList extends AbstractList<TypeDescription> implements TypeList {

            private final String[] name;

            private final String[] internalName;

            private final int stackSize;

            protected TypePoolTypeList(String methodDescriptor) {
                Type[] parameterType = Type.getArgumentTypes(methodDescriptor);
                name = new String[parameterType.length];
                internalName = new String[parameterType.length];
                int index = 0, stackSize = 0;
                for (Type aParameterType : parameterType) {
                    name[index++] = aParameterType.getClassName();
                    internalName[index++] = aParameterType.getInternalName();
                    stackSize += aParameterType.getSize();
                }
                this.stackSize = stackSize;
            }

            protected TypePoolTypeList(String[] internalName) {
                name = new String[internalName.length];
                this.internalName = internalName;
                int index = 0;
                for (String anInternalName : internalName) {
                    name[index++] = anInternalName.replace('/', '.');
                }
                stackSize = index;
            }

            @Override
            public TypeDescription get(int index) {
                return typePool.describe(name[index]);
            }

            @Override
            public int size() {
                return name.length;
            }

            @Override
            public String[] toInternalNames() {
                return internalName.length == 0 ? null : internalName;
            }

            @Override
            public int getStackSize() {
                return stackSize;
            }
        }
    }
}
