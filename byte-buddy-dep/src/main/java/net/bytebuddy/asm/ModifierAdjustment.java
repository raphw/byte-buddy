/*
 * Copyright 2014 - 2018 Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.asm;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.modifier.ModifierContributor;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.CompoundList;
import net.bytebuddy.utility.OpenedClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * <p>
 * A visitor wrapper that adjusts the modifiers of the instrumented type or its members.
 * </p>
 * <p>
 * <b>Important</b>: The removal of the method is not reflected in the created {@link net.bytebuddy.dynamic.DynamicType}'s
 * type description of the instrumented type. The modifier changes are neither visible to element matchers during an instrumentation.
 * </p>
 *
 * @see net.bytebuddy.dynamic.Transformer.ForField#withModifiers(ModifierContributor.ForField...)
 * @see net.bytebuddy.dynamic.Transformer.ForMethod#withModifiers(ModifierContributor.ForMethod...)
 */
@HashCodeAndEqualsPlugin.Enhance
public class ModifierAdjustment extends AsmVisitorWrapper.AbstractBase {

    /**
     * A list of adjustments to apply to the instrumented type.
     */
    private final List<Adjustment<TypeDescription>> typeAdjustments;

    /**
     * A list of adjustments to apply to the instrumented type's declared fields.
     */
    private final List<Adjustment<FieldDescription.InDefinedShape>> fieldAdjustments;

    /**
     * A list of adjustments to apply to the instrumented type's methods.
     */
    private final List<Adjustment<MethodDescription>> methodAdjustments;

    /**
     * Creates a new modifier adjustment that does not adjust any modifiers.
     */
    public ModifierAdjustment() {
        this(Collections.<Adjustment<TypeDescription>>emptyList(),
                Collections.<Adjustment<FieldDescription.InDefinedShape>>emptyList(),
                Collections.<Adjustment<MethodDescription>>emptyList());
    }

    /**
     * Creates a new modifier adjustment.
     *
     * @param typeAdjustments   A list of adjustments to apply to the instrumented type.
     * @param fieldAdjustments  A list of adjustments to apply to the instrumented type's declared fields.
     * @param methodAdjustments A list of adjustments to apply to the instrumented type's methods.
     */
    protected ModifierAdjustment(List<Adjustment<TypeDescription>> typeAdjustments,
                                 List<Adjustment<FieldDescription.InDefinedShape>> fieldAdjustments,
                                 List<Adjustment<MethodDescription>> methodAdjustments) {
        this.typeAdjustments = typeAdjustments;
        this.fieldAdjustments = fieldAdjustments;
        this.methodAdjustments = methodAdjustments;
    }

    /**
     * Adjusts any instrumented type's modifiers.
     *
     * @param modifierContributor The modifier contributors to enforce.
     * @return A new modifier adjustment that enforces the given modifier contributors and any previous adjustments.
     */
    public ModifierAdjustment withTypeModifiers(ModifierContributor.ForType... modifierContributor) {
        return withTypeModifiers(Arrays.asList(modifierContributor));
    }

    /**
     * Adjusts any instrumented type's modifiers.
     *
     * @param modifierContributors The modifier contributors to enforce.
     * @return A new modifier adjustment that enforces the given modifier contributors and any previous adjustments.
     */
    public ModifierAdjustment withTypeModifiers(List<? extends ModifierContributor.ForType> modifierContributors) {
        return withTypeModifiers(any(), modifierContributors);
    }

    /**
     * Adjusts an instrumented type's modifiers if it matches the supplied matcher.
     *
     * @param matcher             The matcher that determines a type's eligibility.
     * @param modifierContributor The modifier contributors to enforce.
     * @return A new modifier adjustment that enforces the given modifier contributors and any previous adjustments.
     */
    public ModifierAdjustment withTypeModifiers(ElementMatcher<? super TypeDescription> matcher,
                                                ModifierContributor.ForType... modifierContributor) {
        return withTypeModifiers(matcher, Arrays.asList(modifierContributor));
    }

    /**
     * Adjusts an instrumented type's modifiers if it matches the supplied matcher.
     *
     * @param matcher              The matcher that determines a type's eligibility.
     * @param modifierContributors The modifier contributors to enforce.
     * @return A new modifier adjustment that enforces the given modifier contributors and any previous adjustments.
     */
    public ModifierAdjustment withTypeModifiers(ElementMatcher<? super TypeDescription> matcher,
                                                List<? extends ModifierContributor.ForType> modifierContributors) {
        return new ModifierAdjustment(CompoundList.of(new Adjustment<TypeDescription>(matcher,
                ModifierContributor.Resolver.of(modifierContributors)), typeAdjustments), fieldAdjustments, methodAdjustments);
    }

    /**
     * Adjusts any field's modifiers.
     *
     * @param modifierContributor The modifier contributors to enforce.
     * @return A new modifier adjustment that enforces the given modifier contributors and any previous adjustments.
     */
    public ModifierAdjustment withFieldModifiers(ModifierContributor.ForField... modifierContributor) {
        return withFieldModifiers(Arrays.asList(modifierContributor));
    }

    /**
     * Adjusts any field's modifiers.
     *
     * @param modifierContributors The modifier contributors to enforce.
     * @return A new modifier adjustment that enforces the given modifier contributors and any previous adjustments.
     */
    public ModifierAdjustment withFieldModifiers(List<? extends ModifierContributor.ForField> modifierContributors) {
        return withFieldModifiers(any(), modifierContributors);
    }

    /**
     * Adjusts a field's modifiers if it fulfills the supplied matcher.
     *
     * @param matcher             The matcher that determines if a field's modifiers should be adjusted.
     * @param modifierContributor The modifier contributors to enforce.
     * @return A new modifier adjustment that enforces the given modifier contributors and any previous adjustments.
     */
    public ModifierAdjustment withFieldModifiers(ElementMatcher<? super FieldDescription.InDefinedShape> matcher,
                                                 ModifierContributor.ForField... modifierContributor) {
        return withFieldModifiers(matcher, Arrays.asList(modifierContributor));
    }

    /**
     * Adjusts a field's modifiers if it fulfills the supplied matcher.
     *
     * @param matcher              The matcher that determines if a field's modifiers should be adjusted.
     * @param modifierContributors The modifier contributors to enforce.
     * @return A new modifier adjustment that enforces the given modifier contributors and any previous adjustments.
     */
    public ModifierAdjustment withFieldModifiers(ElementMatcher<? super FieldDescription.InDefinedShape> matcher,
                                                 List<? extends ModifierContributor.ForField> modifierContributors) {
        return new ModifierAdjustment(typeAdjustments, CompoundList.of(new Adjustment<FieldDescription.InDefinedShape>(matcher,
                ModifierContributor.Resolver.of(modifierContributors)), fieldAdjustments), methodAdjustments);
    }

    /**
     * Adjusts any method's modifiers.
     *
     * @param modifierContributor The modifier contributors to enforce.
     * @return A new modifier adjustment that enforces the given modifier contributors and any previous adjustments.
     */
    public ModifierAdjustment withMethodModifiers(ModifierContributor.ForMethod... modifierContributor) {
        return withMethodModifiers(Arrays.asList(modifierContributor));
    }

    /**
     * Adjusts any method's modifiers.
     *
     * @param modifierContributors The modifier contributors to enforce.
     * @return A new modifier adjustment that enforces the given modifier contributors and any previous adjustments.
     */
    public ModifierAdjustment withMethodModifiers(List<? extends ModifierContributor.ForMethod> modifierContributors) {
        return withMethodModifiers(any(), modifierContributors);
    }

    /**
     * Adjusts a method's modifiers if it fulfills the supplied matcher.
     *
     * @param matcher             The matcher that determines if a method's modifiers should be adjusted.
     * @param modifierContributor The modifier contributors to enforce.
     * @return A new modifier adjustment that enforces the given modifier contributors and any previous adjustments.
     */
    public ModifierAdjustment withMethodModifiers(ElementMatcher<? super MethodDescription> matcher,
                                                  ModifierContributor.ForMethod... modifierContributor) {
        return withMethodModifiers(matcher, Arrays.asList(modifierContributor));
    }

    /**
     * Adjusts a method's modifiers if it fulfills the supplied matcher.
     *
     * @param matcher              The matcher that determines if a method's modifiers should be adjusted.
     * @param modifierContributors The modifier contributors to enforce.
     * @return A new modifier adjustment that enforces the given modifier contributors and any previous adjustments.
     */
    public ModifierAdjustment withMethodModifiers(ElementMatcher<? super MethodDescription> matcher,
                                                  List<? extends ModifierContributor.ForMethod> modifierContributors) {
        return withInvokableModifiers(isMethod().and(matcher), modifierContributors);
    }

    /**
     * Adjusts any constructor's modifiers.
     *
     * @param modifierContributor The modifier contributors to enforce.
     * @return A new modifier adjustment that enforces the given modifier contributors and any previous adjustments.
     */
    public ModifierAdjustment withConstructorModifiers(ModifierContributor.ForMethod... modifierContributor) {
        return withConstructorModifiers(Arrays.asList(modifierContributor));
    }

    /**
     * Adjusts any constructor's modifiers.
     *
     * @param modifierContributors The modifier contributors to enforce.
     * @return A new modifier adjustment that enforces the given modifier contributors and any previous adjustments.
     */
    public ModifierAdjustment withConstructorModifiers(List<? extends ModifierContributor.ForMethod> modifierContributors) {
        return withConstructorModifiers(any(), modifierContributors);
    }

    /**
     * Adjusts a constructor's modifiers if it fulfills the supplied matcher.
     *
     * @param matcher             The matcher that determines if a constructor's modifiers should be adjusted.
     * @param modifierContributor The modifier contributors to enforce.
     * @return A new modifier adjustment that enforces the given modifier contributors and any previous adjustments.
     */
    public ModifierAdjustment withConstructorModifiers(ElementMatcher<? super MethodDescription> matcher,
                                                       ModifierContributor.ForMethod... modifierContributor) {
        return withConstructorModifiers(matcher, Arrays.asList(modifierContributor));
    }

    /**
     * Adjusts a constructor's modifiers if it fulfills the supplied matcher.
     *
     * @param matcher              The matcher that determines if a constructor's modifiers should be adjusted.
     * @param modifierContributors The modifier contributors to enforce.
     * @return A new modifier adjustment that enforces the given modifier contributors and any previous adjustments.
     */
    public ModifierAdjustment withConstructorModifiers(ElementMatcher<? super MethodDescription> matcher,
                                                       List<? extends ModifierContributor.ForMethod> modifierContributors) {
        return withInvokableModifiers(isConstructor().and(matcher), modifierContributors);
    }

    /**
     * Adjusts any method's or constructor's modifiers.
     *
     * @param modifierContributor The modifier contributors to enforce.
     * @return A new modifier adjustment that enforces the given modifier contributors and any previous adjustments.
     */
    public ModifierAdjustment withInvokableModifiers(ModifierContributor.ForMethod... modifierContributor) {
        return withInvokableModifiers(Arrays.asList(modifierContributor));
    }

    /**
     * Adjusts any method's or constructor's modifiers.
     *
     * @param modifierContributors The modifier contributors to enforce.
     * @return A new modifier adjustment that enforces the given modifier contributors and any previous adjustments.
     */
    public ModifierAdjustment withInvokableModifiers(List<? extends ModifierContributor.ForMethod> modifierContributors) {
        return withInvokableModifiers(any(), modifierContributors);
    }

    /**
     * Adjusts a method's or constructor's modifiers if it fulfills the supplied matcher.
     *
     * @param matcher             The matcher that determines if a method's or constructor's modifiers should be adjusted.
     * @param modifierContributor The modifier contributors to enforce.
     * @return A new modifier adjustment that enforces the given modifier contributors and any previous adjustments.
     */
    public ModifierAdjustment withInvokableModifiers(ElementMatcher<? super MethodDescription> matcher,
                                                     ModifierContributor.ForMethod... modifierContributor) {
        return withInvokableModifiers(matcher, Arrays.asList(modifierContributor));
    }

    /**
     * Adjusts a method's or constructor's modifiers if it fulfills the supplied matcher.
     *
     * @param matcher              The matcher that determines if a method's or constructor's modifiers should be adjusted.
     * @param modifierContributors The modifier contributors to enforce.
     * @return A new modifier adjustment that enforces the given modifier contributors and any previous adjustments.
     */
    public ModifierAdjustment withInvokableModifiers(ElementMatcher<? super MethodDescription> matcher,
                                                     List<? extends ModifierContributor.ForMethod> modifierContributors) {
        return new ModifierAdjustment(typeAdjustments, fieldAdjustments, CompoundList.of(new Adjustment<MethodDescription>(matcher,
                ModifierContributor.Resolver.of(modifierContributors)), methodAdjustments));
    }

    /**
     * {@inheritDoc}
     */
    public ModifierAdjustingClassVisitor wrap(TypeDescription instrumentedType,
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
        return new ModifierAdjustingClassVisitor(classVisitor,
                typeAdjustments,
                fieldAdjustments,
                methodAdjustments,
                instrumentedType,
                mappedFields,
                mappedMethods);
    }

    /**
     * A description of a conditional adjustment.
     *
     * @param <T> The type of the adjusted element's description.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class Adjustment<T> implements ElementMatcher<T> {

        /**
         * The matcher to determine an adjustment.
         */
        private final ElementMatcher<? super T> matcher;

        /**
         * The resolver to apply.
         */
        private final ModifierContributor.Resolver<?> resolver;

        /**
         * Creates a new adjustment.
         *
         * @param matcher  The matcher to determine an adjustment.
         * @param resolver The resolver to apply.
         */
        protected Adjustment(ElementMatcher<? super T> matcher, ModifierContributor.Resolver<?> resolver) {
            this.matcher = matcher;
            this.resolver = resolver;
        }

        /**
         * {@inheritDoc}
         */
        public boolean matches(T target) {
            return matcher.matches(target);
        }

        /**
         * Resolves a modifier.
         *
         * @param modifiers The original modifiers.
         * @return The resolved modifiers.
         */
        protected int resolve(int modifiers) {
            return resolver.resolve(modifiers);
        }
    }

    /**
     * A class visitor that enforces a collection of modifier adjustments.
     */
    protected static class ModifierAdjustingClassVisitor extends ClassVisitor {

        /**
         * A list of type modifier adjustments to apply.
         */
        private final List<Adjustment<TypeDescription>> typeAdjustments;

        /**
         * A list of field modifier adjustments to apply.
         */
        private final List<Adjustment<FieldDescription.InDefinedShape>> fieldAdjustments;

        /**
         * A list of method modifier adjustments to apply.
         */
        private final List<Adjustment<MethodDescription>> methodAdjustments;

        /**
         * The instrumented type.
         */
        private final TypeDescription instrumentedType;

        /**
         * A mapping of field names and descriptors to their description.
         */
        private final Map<String, FieldDescription.InDefinedShape> fields;

        /**
         * A mapping of method names and descriptors to their description.
         */
        private final Map<String, MethodDescription> methods;

        /**
         * Creates a new modifier adjusting visitor.
         *
         * @param classVisitor      The class visitor to delegate to.
         * @param typeAdjustments   A list of type modifier adjustments to apply.
         * @param fieldAdjustments  A list of field modifier adjustments to apply.
         * @param methodAdjustments A list of method modifier adjustments to apply.
         * @param instrumentedType  The instrumented type.
         * @param fields            A mapping of field names and descriptors to their description.
         * @param methods           A mapping of method names and descriptors to their description.
         */
        protected ModifierAdjustingClassVisitor(ClassVisitor classVisitor,
                                                List<Adjustment<TypeDescription>> typeAdjustments,
                                                List<Adjustment<FieldDescription.InDefinedShape>> fieldAdjustments,
                                                List<Adjustment<MethodDescription>> methodAdjustments,
                                                TypeDescription instrumentedType,
                                                Map<String, FieldDescription.InDefinedShape> fields,
                                                Map<String, MethodDescription> methods) {
            super(OpenedClassReader.ASM_API, classVisitor);
            this.typeAdjustments = typeAdjustments;
            this.fieldAdjustments = fieldAdjustments;
            this.methodAdjustments = methodAdjustments;
            this.instrumentedType = instrumentedType;
            this.fields = fields;
            this.methods = methods;
        }

        @Override
        public void visit(int version, int modifiers, String internalName, String signature, String superClassName, String[] interfaceName) {
            for (Adjustment<TypeDescription> adjustment : typeAdjustments) {
                if (adjustment.matches(instrumentedType)) {
                    modifiers = adjustment.resolve(modifiers);
                    break;
                }
            }
            super.visit(version, modifiers, internalName, signature, superClassName, interfaceName);
        }

        @Override
        public void visitInnerClass(String internalName, String outerName, String innerName, int modifiers) {
            if (instrumentedType.getInternalName().equals(internalName)) {
                for (Adjustment<TypeDescription> adjustment : typeAdjustments) {
                    if (adjustment.matches(instrumentedType)) {
                        modifiers = adjustment.resolve(modifiers);
                        break;
                    }
                }
            }
            super.visitInnerClass(internalName, outerName, innerName, modifiers);
        }

        @Override
        public FieldVisitor visitField(int modifiers, String internalName, String descriptor, String signature, Object value) {
            FieldDescription.InDefinedShape fieldDescription = fields.get(internalName + descriptor);
            if (fieldDescription != null) {
                for (Adjustment<FieldDescription.InDefinedShape> adjustment : fieldAdjustments) {
                    if (adjustment.matches(fieldDescription)) {
                        modifiers = adjustment.resolve(modifiers);
                        break;
                    }
                }
            }
            return super.visitField(modifiers, internalName, descriptor, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exception) {
            MethodDescription methodDescription = methods.get(internalName + descriptor);
            if (methodDescription != null) {
                for (Adjustment<MethodDescription> adjustment : methodAdjustments) {
                    if (adjustment.matches(methodDescription)) {
                        modifiers = adjustment.resolve(modifiers);
                        break;
                    }
                }
            }
            return super.visitMethod(modifiers, internalName, descriptor, signature, exception);
        }
    }
}
