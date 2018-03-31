package net.bytebuddy.asm;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.CompoundList;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;

/**
 * <p>
 * A visitor wrapper that removes fields or methods that match a given {@link ElementMatcher}.
 * </p>
 * <p>
 * <b>Important</b>: This matcher is not capable of removing synthetic bridge methods which will be retained if they are
 * declared by the same class. As bridge methods only invoke an overridden method, the dispatch should however not be
 * influenced by their retention.
 * </p>
 * <p>
 * <b>Important</b>: The removal of the method is not reflected in the created {@link net.bytebuddy.dynamic.DynamicType}'s
 * type description of the instrumented type.
 * </p>
 */
@HashCodeAndEqualsPlugin.Enhance
public class MemberRemoval extends AsmVisitorWrapper.AbstractBase {

    /**
     * The matcher that decides upon field removal.
     */
    private final ElementMatcher.Junction<FieldDescription.InDefinedShape> fieldMatcher;

    /**
     * The matcher that decides upon method removal.
     */
    private final ElementMatcher.Junction<MethodDescription> methodMatcher;

    /**
     * Creates a new member removal instance that does not specify the removal of any methods.
     */
    public MemberRemoval() {
        this(ElementMatchers.<FieldDescription.InDefinedShape>none(), ElementMatchers.<MethodDescription>none());
    }

    /**
     * Creates a new member removal instance.
     *
     * @param fieldMatcher  The matcher that decides upon field removal.
     * @param methodMatcher The matcher that decides upon field removal.
     */
    protected MemberRemoval(ElementMatcher.Junction<FieldDescription.InDefinedShape> fieldMatcher,
                            ElementMatcher.Junction<MethodDescription> methodMatcher) {
        this.fieldMatcher = fieldMatcher;
        this.methodMatcher = methodMatcher;
    }

    /**
     * Specifies that any field that matches the specified matcher should be removed.
     *
     * @param matcher The matcher that decides upon field removal.
     * @return A new member removal instance that removes all previously specified members and any fields that match the specified matcher.
     */
    public MemberRemoval stripFields(ElementMatcher<? super FieldDescription.InDefinedShape> matcher) {
        return new MemberRemoval(fieldMatcher.or(matcher), methodMatcher);
    }

    /**
     * Specifies that any method that matches the specified matcher should be removed.
     *
     * @param matcher The matcher that decides upon method removal.
     * @return A new member removal instance that removes all previously specified members and any method that matches the specified matcher.
     */
    public MemberRemoval stripMethods(ElementMatcher<? super MethodDescription> matcher) {
        return stripInvokables(isMethod().and(matcher));
    }

    /**
     * Specifies that any constructor that matches the specified matcher should be removed.
     *
     * @param matcher The matcher that decides upon constructor removal.
     * @return A new member removal instance that removes all previously specified members and any constructor that matches the specified matcher.
     */
    public MemberRemoval stripConstructors(ElementMatcher<? super MethodDescription> matcher) {
        return stripInvokables(isConstructor().and(matcher));
    }

    /**
     * Specifies that any method or constructor that matches the specified matcher should be removed.
     *
     * @param matcher The matcher that decides upon method and constructor removal.
     * @return A new member removal instance that removes all previously specified members and any method or constructor that matches the specified matcher.
     */
    public MemberRemoval stripInvokables(ElementMatcher<? super MethodDescription> matcher) {
        return new MemberRemoval(fieldMatcher, methodMatcher.or(matcher));
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
        Map<String, FieldDescription.InDefinedShape> mappedFields = new HashMap<String, FieldDescription.InDefinedShape>();
        for (FieldDescription.InDefinedShape fieldDescription : fields) {
            mappedFields.put(fieldDescription.getInternalName() + fieldDescription.getDescriptor(), fieldDescription);
        }
        Map<String, MethodDescription> mappedMethods = new HashMap<String, MethodDescription>();
        for (MethodDescription methodDescription : CompoundList.<MethodDescription>of(methods, new MethodDescription.Latent.TypeInitializer(instrumentedType))) {
            mappedMethods.put(methodDescription.getInternalName() + methodDescription.getDescriptor(), methodDescription);
        }
        return new MemberRemovingClassVisitor(classVisitor, fieldMatcher, methodMatcher, mappedFields, mappedMethods);
    }

    /**
     * A class visitor that removes members based on element matchers.
     */
    protected static class MemberRemovingClassVisitor extends ClassVisitor {

        /**
         * Indicates the removal of a field.
         */
        private static final FieldVisitor REMOVE_FIELD = null;

        /**
         * Indicates the removal of a method.
         */
        private static final MethodVisitor REMOVE_METHOD = null;

        /**
         * The matcher that determines field removal.
         */
        private final ElementMatcher.Junction<FieldDescription.InDefinedShape> fieldMatcher;

        /**
         * The matcher that determines method removal.
         */
        private final ElementMatcher.Junction<MethodDescription> methodMatcher;

        /**
         * A mapping of field names and descriptors to their description.
         */
        private final Map<String, FieldDescription.InDefinedShape> fields;

        /**
         * A mapping of method names and descriptors to their description.
         */
        private final Map<String, MethodDescription> methods;

        /**
         * Creates a new member removing class visitor.
         *
         * @param classVisitor  The class visitor to delegate to.
         * @param fieldMatcher  The matcher that determines field removal.
         * @param methodMatcher The matcher that determines method removal.
         * @param fields        A mapping of field names and descriptors to their description.
         * @param methods       A mapping of method names and descriptors to their description.
         */
        protected MemberRemovingClassVisitor(ClassVisitor classVisitor,
                                             ElementMatcher.Junction<FieldDescription.InDefinedShape> fieldMatcher,
                                             ElementMatcher.Junction<MethodDescription> methodMatcher,
                                             Map<String, FieldDescription.InDefinedShape> fields,
                                             Map<String, MethodDescription> methods) {
            super(Opcodes.ASM6, classVisitor);
            this.fieldMatcher = fieldMatcher;
            this.methodMatcher = methodMatcher;
            this.fields = fields;
            this.methods = methods;
        }

        @Override
        public FieldVisitor visitField(int modifiers, String internalName, String descriptor, String signature, Object value) {
            FieldDescription.InDefinedShape fieldDescription = fields.get(internalName + descriptor);
            return fieldDescription != null && fieldMatcher.matches(fieldDescription)
                    ? REMOVE_FIELD
                    : super.visitField(modifiers, internalName, descriptor, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exception) {
            MethodDescription methodDescription = methods.get(internalName + descriptor);
            return methodDescription != null && methodMatcher.matches(methodDescription)
                    ? REMOVE_METHOD
                    : super.visitMethod(modifiers, internalName, descriptor, signature, exception);
        }
    }
}
