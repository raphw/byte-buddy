package net.bytebuddy.asm;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.CompoundList;
import net.bytebuddy.utility.OpenedClassReader;
import net.bytebuddy.utility.nullability.MaybeNull;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A visitor wrapper that removes annotations from the instrumented type.
 */
@HashCodeAndEqualsPlugin.Enhance
public class AnnotationRemoval extends AsmVisitorWrapper.AbstractBase {

    private final boolean type;

    private final ElementMatcher<? super FieldDescription.InDefinedShape> fieldMatcher;

    private final ElementMatcher<? super MethodDescription> methodMatcher;

    private final ElementMatcher<? super AnnotationDescription> annotationMatcher;

    protected AnnotationRemoval(boolean type,
                                ElementMatcher<? super FieldDescription.InDefinedShape.InDefinedShape> fieldMatcher,
                                ElementMatcher<? super MethodDescription> methodMatcher,
                                ElementMatcher<? super AnnotationDescription> annotationMatcher) {
        this.type = type;
        this.fieldMatcher = fieldMatcher;
        this.methodMatcher = methodMatcher;
        this.annotationMatcher = annotationMatcher;
    }

    public static AnnotationRemoval strip(ElementMatcher<? super AnnotationDescription> matcher) {
        return new AnnotationRemoval(true, any(), any(), matcher);
    }

    public AsmVisitorWrapper onType() {
        return new AnnotationRemoval(true, none(), none(), annotationMatcher);
    }

    public AsmVisitorWrapper onFields(ElementMatcher<? super FieldDescription> matcher) {
        return new AnnotationRemoval(false, matcher, none(), annotationMatcher);
    }

    public AsmVisitorWrapper onMethods(ElementMatcher<? super MethodDescription> matcher) {
        return onInvokables(isMethod().and(matcher));
    }

    public AsmVisitorWrapper onConstructors(ElementMatcher<? super MethodDescription> matcher) {
        return onInvokables(isConstructor().and(matcher));
    }

    public AsmVisitorWrapper onInvokables(ElementMatcher<? super MethodDescription> matcher) {
        return new AnnotationRemoval(false, none(), matcher, annotationMatcher);
    }

    @Override
    public ClassVisitor wrap(TypeDescription instrumentedType,
                             ClassVisitor classVisitor,
                             Implementation.Context implementationContext,
                             TypePool typePool,
                             FieldList<FieldDescription.InDefinedShape> fields,
                             MethodList<?> methods,
                             int writerFlags,
                             int readerFlags) {
        Map<String, AnnotationDescription> mappedAnnotations = new HashMap<String, AnnotationDescription>();
        if (type) {
            for (AnnotationDescription annotation : instrumentedType.getDeclaredAnnotations()) {
                mappedAnnotations.put(annotation.getAnnotationType().getDescriptor(), annotation);
            }
        }
        Map<String, FieldDescription.InDefinedShape> mappedFields = new HashMap<String, FieldDescription.InDefinedShape>();
        for (FieldDescription.InDefinedShape fieldDescription : fields) {
            mappedFields.put(fieldDescription.getInternalName() + fieldDescription.getDescriptor(), fieldDescription);
        }
        Map<String, MethodDescription> mappedMethods = new HashMap<String, MethodDescription>();
        for (MethodDescription methodDescription : CompoundList.<MethodDescription>of(methods, new MethodDescription.Latent.TypeInitializer(instrumentedType))) {
            mappedMethods.put(methodDescription.getInternalName() + methodDescription.getDescriptor(), methodDescription);
        }
        return new AnnotationRemovingClassVisitor(classVisitor, fieldMatcher, methodMatcher, annotationMatcher, mappedFields, mappedMethods, mappedAnnotations);
    }

    private static class AnnotationRemovingClassVisitor extends ClassVisitor {

        private final ElementMatcher<? super FieldDescription.InDefinedShape> fieldMatcher;

        private final ElementMatcher<? super MethodDescription> methodMatcher;

        private final ElementMatcher<? super AnnotationDescription> annotationMatcher;

        private final Map<String, FieldDescription.InDefinedShape> fields;

        private final Map<String, MethodDescription> methods;

        private final Map<String, AnnotationDescription> annotations;

        private AnnotationRemovingClassVisitor(ClassVisitor classVisitor,
                                               ElementMatcher<? super FieldDescription.InDefinedShape> fieldMatcher,
                                               ElementMatcher<? super MethodDescription> methodMatcher,
                                               ElementMatcher<? super AnnotationDescription> annotationMatcher,
                                               Map<String, FieldDescription.InDefinedShape> fields,
                                               Map<String, MethodDescription> methods,
                                               Map<String, AnnotationDescription> annotations) {
            super(OpenedClassReader.ASM_API, classVisitor);
            this.fieldMatcher = fieldMatcher;
            this.methodMatcher = methodMatcher;
            this.annotationMatcher = annotationMatcher;
            this.fields = fields;
            this.methods = methods;
            this.annotations = annotations;
        }

        @Override
        @MaybeNull
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            AnnotationDescription annotation = annotations.get(descriptor);
            return annotation != null && annotationMatcher.matches(annotation)
                    ? null
                    : super.visitAnnotation(descriptor, visible);
        }

        @Override
        @MaybeNull
        public FieldVisitor visitField(int modifiers, String internalName, String descriptor, String signature, Object value) {
            FieldVisitor fieldVisitor = super.visitField(modifiers, internalName, descriptor, signature, value);
            if (fieldVisitor == null) {
                return null;
            }
            FieldDescription.InDefinedShape fieldDescription = fields.get(internalName + descriptor);
            if (fieldDescription != null && fieldMatcher.matches(fieldDescription)) {
                Map<String, AnnotationDescription> mappedAnnotations = new HashMap<String, AnnotationDescription>();
                for (AnnotationDescription annotation : fieldDescription.getDeclaredAnnotations()) {
                    mappedAnnotations.put(annotation.getAnnotationType().getDescriptor(), annotation);
                }
                return new AnnotationRemovingFieldVisitor(fieldVisitor, annotationMatcher, mappedAnnotations);
            } else {
                return fieldVisitor;
            }
        }

        @Override
        @MaybeNull
        public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exception) {
            MethodVisitor methodVisitor = super.visitMethod(modifiers, internalName, descriptor, signature, exception);
            if (methodVisitor == null) {
                return null;
            }
            MethodDescription methodDescription = methods.get(internalName + descriptor);
            if (methodDescription != null && methodMatcher.matches(methodDescription)) {
                Map<Integer, Map<String, AnnotationDescription>> mappedParameterAnnotations = new HashMap<Integer, Map<String, AnnotationDescription>>();
                for (ParameterDescription parameter : methodDescription.getParameters()) {
                    Map<String, AnnotationDescription> mappedAnnotations = new HashMap<String, AnnotationDescription>();
                    for (AnnotationDescription annotation : methodDescription.getDeclaredAnnotations()) {
                        mappedAnnotations.put(annotation.getAnnotationType().getDescriptor(), annotation);
                    }
                    mappedParameterAnnotations.put(parameter.getIndex(), mappedAnnotations);
                }
                Map<String, AnnotationDescription> mappedAnnotations = new HashMap<String, AnnotationDescription>();
                for (AnnotationDescription annotation : methodDescription.getDeclaredAnnotations()) {
                    mappedAnnotations.put(annotation.getAnnotationType().getDescriptor(), annotation);
                }
                return new AnnotationRemovingMethodVisitor(methodVisitor, annotationMatcher, mappedParameterAnnotations, mappedAnnotations);
            } else {
                return methodVisitor;
            }
        }
    }

    private static class AnnotationRemovingFieldVisitor extends FieldVisitor {

        private final ElementMatcher<? super AnnotationDescription> annotationMatcher;

        private final Map<String, AnnotationDescription> annotations;

        private AnnotationRemovingFieldVisitor(FieldVisitor fieldVisitor,
                                               ElementMatcher<? super AnnotationDescription> annotationMatcher,
                                               Map<String, AnnotationDescription> annotations) {
            super(OpenedClassReader.ASM_API, fieldVisitor);
            this.annotationMatcher = annotationMatcher;
            this.annotations = annotations;
        }

        @Override
        @MaybeNull
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            AnnotationDescription annotation = annotations.get(descriptor);
            return annotation != null && annotationMatcher.matches(annotation)
                    ? null
                    : super.visitAnnotation(descriptor, visible);
        }
    }

    private static class AnnotationRemovingMethodVisitor extends MethodVisitor {

        private final ElementMatcher<? super AnnotationDescription> annotationMatcher;

        private final Map<Integer, Map<String, AnnotationDescription>> parameterAnnotations;

        private final Map<String, AnnotationDescription> annotations;

        private AnnotationRemovingMethodVisitor(MethodVisitor methodVisitor,
                                                ElementMatcher<? super AnnotationDescription> annotationMatcher,
                                                Map<Integer, Map<String, AnnotationDescription>> parameterAnnotations,
                                                Map<String, AnnotationDescription> annotations) {
            super(OpenedClassReader.ASM_API, methodVisitor);
            this.annotationMatcher = annotationMatcher;
            this.parameterAnnotations = parameterAnnotations;
            this.annotations = annotations;
        }

        @Override
        @MaybeNull
        public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
            AnnotationDescription annotation = parameterAnnotations
                    .getOrDefault(parameter, Collections.<String, AnnotationDescription>emptyMap())
                    .get(descriptor);
            return annotation != null && annotationMatcher.matches(annotation)
                    ? null
                    : super.visitParameterAnnotation(parameter, descriptor, visible);
        }

        @Override
        @MaybeNull
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            AnnotationDescription annotation = annotations.get(descriptor);
            return annotation != null && annotationMatcher.matches(annotation)
                    ? null
                    : super.visitAnnotation(descriptor, visible);
        }
    }
}
