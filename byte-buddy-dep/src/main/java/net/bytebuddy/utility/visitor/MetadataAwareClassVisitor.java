/*
 * Copyright 2014 - Present Rafael Winterhalter
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
package net.bytebuddy.utility.visitor;

import org.objectweb.asm.*;

/**
 * A class visitor that traces invocations of visitation methods and notifies if a nest host or outer class was not visited.
 */
public abstract class MetadataAwareClassVisitor extends ClassVisitor {

    /**
     * {@code true} if the nest host was not yet visited.
     */
    private boolean triggerNestHost;

    /**
     * {@code true} if the permitted subclass visitation is not yet completed.
     */
    private boolean triggerPermittedSubclasses;

    /**
     * {@code true} if the outer class was not yet visited.
     */
    private boolean triggerOuterClass;

    /**
     * {@code true} if the attribute visitation is not yet completed.
     */
    private boolean triggerAttributes;

    /**
     * {@code true} if the record component visitation is not yet triggered.
     */
    private boolean triggerRecordComponents;

    /**
     * Creates a metadata aware class visitor.
     *
     * @param api          The API version.
     * @param classVisitor The class visitor to delegate to.
     */
    protected MetadataAwareClassVisitor(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
        triggerNestHost = true;
        triggerPermittedSubclasses = true;
        triggerOuterClass = true;
        triggerAttributes = true;
        triggerRecordComponents = true;
    }

    /**
     * Invoked if the nest host was not visited.
     */
    protected void onNestHost() {
        /* do nothing */
    }

    /**
     * Invoked if the permitted subclass visitation is about to complete.
     */
    protected void onAfterPermittedSubclasses() {
        /* do nothing */
    }

    /**
     * Invoked if the outer class was not visited.
     */
    protected void onOuterType() {
        /* do nothing */
    }

    /**
     * Invoked if the attribute visitation is about to complete.
     */
    protected void onAfterAttributes() {
        /* do nothing */
    }

    /**
     * Invoked after all record components are visited or none is found.
     */
    protected void onAfterRecordComponents() {
        /* do nothing */
    }

    /**
     * Considers triggering a nest host visitation.
     */
    private void considerTriggerNestHost() {
        if (triggerNestHost) {
            triggerNestHost = false;
            onNestHost();
        }
    }

    /**
     * Considers triggering an outer class visitation.
     */
    private void considerTriggerOuterClass() {
        if (triggerOuterClass) {
            triggerOuterClass = false;
            onOuterType();
        }
    }

    /**
     * Considers triggering the after permitted subclass visitation.
     */
    private void considerTriggerAfterPermittedSubclasses() {
        if (triggerPermittedSubclasses) {
            triggerPermittedSubclasses = false;
            onAfterPermittedSubclasses();
        }
    }

    /**
     * Considers triggering the after attribute visitation.
     */
    private void considerTriggerAfterAttributes() {
        if (triggerAttributes) {
            triggerAttributes = false;
            onAfterAttributes();
        }
    }

    /**
     * Considers triggering the after record components visitation.
     */
    private void considerTriggerAfterRecordComponents() {
        if (triggerRecordComponents) {
            triggerRecordComponents = false;
            onAfterRecordComponents();
        }
    }

    @Override
    public final void visitNestHost(String nestHost) {
        triggerNestHost = false;
        onVisitNestHost(nestHost);
    }

    /**
     * An order-sensitive invocation of {@link ClassVisitor#visitNestHost(String)}.
     *
     * @param nestHost The internal name of the nest host.
     */
    protected void onVisitNestHost(String nestHost) {
        super.visitNestHost(nestHost);
    }

    @Override
    public final void visitPermittedSubclass(String permittedSubclass) {
        onVisitPermittedSubclass(permittedSubclass);
    }

    /**
     * An order-sensitive invocation of {@code ClassVisitor#visitPermittedSubclass}.
     *
     * @param permittedSubclass The internal name of the permitted subclass.
     */
    protected void onVisitPermittedSubclass(String permittedSubclass) {
        super.visitPermittedSubclass(permittedSubclass);
    }

    @Override
    public final void visitOuterClass(String owner, String name, String descriptor) {
        considerTriggerNestHost();
        considerTriggerAfterPermittedSubclasses();
        triggerOuterClass = false;
        onVisitOuterClass(owner, name, descriptor);
    }

    /**
     * An order-sensitive invocation of {@link ClassVisitor#visitOuterClass(String, String, String)}.
     *
     * @param owner      The outer class's internal name.
     * @param name       The outer method's name or {@code null} if it does not exist.
     * @param descriptor The outer method's descriptor or {@code null} if it does not exist.
     */
    protected void onVisitOuterClass(String owner, String name, String descriptor) {
        super.visitOuterClass(owner, name, descriptor);
    }

    @Override
    public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
        considerTriggerNestHost();
        considerTriggerAfterPermittedSubclasses();
        considerTriggerOuterClass();
        considerTriggerAfterAttributes();
        return onVisitRecordComponent(name, descriptor, signature);
    }

    /**
     * An order-sensitive invocation of {@link ClassVisitor#visitRecordComponent(String, String, String)}.
     *
     * @param name       The record component's name.
     * @param descriptor The record component's descriptor.
     * @param signature  The record component's generic signature or {@code null} if the record component's type is non-generic.
     * @return The record component visitor or {@code null} if the component should not be visited.
     */
    protected RecordComponentVisitor onVisitRecordComponent(String name, String descriptor, String signature) {
        return super.visitRecordComponent(name, descriptor, signature);
    }

    @Override
    public final AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        considerTriggerNestHost();
        considerTriggerAfterPermittedSubclasses();
        considerTriggerOuterClass();
        return onVisitAnnotation(descriptor, visible);
    }

    /**
     * An order-sensitive invocation of {@link ClassVisitor#visitAnnotation(String, boolean)}.
     *
     * @param descriptor The annotation type's descriptor.
     * @param visible    {@code true} if the annotation is visible at runtime.
     * @return An annotation visitor or {@code null} if the annotation should be ignored.
     */
    protected AnnotationVisitor onVisitAnnotation(String descriptor, boolean visible) {
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public final AnnotationVisitor visitTypeAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
        considerTriggerNestHost();
        considerTriggerAfterPermittedSubclasses();
        considerTriggerOuterClass();
        return onVisitTypeAnnotation(typeReference, typePath, descriptor, visible);
    }

    /**
     * An order-sensitive invocation of {@link ClassVisitor#visitTypeAnnotation(int, TypePath, String, boolean)}.
     *
     * @param typeReference The type reference of the type annotation.
     * @param typePath      The type path of the type annotation.
     * @param descriptor    The descriptor of the annotation type.
     * @param visible       {@code true} if the annotation is visible at runtime.
     * @return An annotation visitor or {@code null} if the annotation should be ignored.
     */
    protected AnnotationVisitor onVisitTypeAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
        return super.visitTypeAnnotation(typeReference, typePath, descriptor, visible);
    }

    @Override
    public final void visitAttribute(Attribute attribute) {
        considerTriggerNestHost();
        considerTriggerAfterPermittedSubclasses();
        considerTriggerOuterClass();
        onVisitAttribute(attribute);
    }

    /**
     * An order-sensitive invocation of {@link ClassVisitor#visitAttribute(Attribute)}.
     *
     * @param attribute The attribute to visit.
     */
    protected void onVisitAttribute(Attribute attribute) {
        super.visitAttribute(attribute);
    }

    @Override
    public final void visitNestMember(String nestMember) {
        considerTriggerNestHost();
        considerTriggerAfterPermittedSubclasses();
        considerTriggerOuterClass();
        considerTriggerAfterAttributes();
        onVisitNestMember(nestMember);
    }

    /**
     * An order-sensitive invocation of {@link ClassVisitor#visitNestMember(String)}.
     *
     * @param nestMember The internal name of the nest member.
     */
    protected void onVisitNestMember(String nestMember) {
        super.visitNestMember(nestMember);
    }

    @Override
    public final void visitInnerClass(String name, String outerName, String innerName, int modifiers) {
        considerTriggerNestHost();
        considerTriggerAfterPermittedSubclasses();
        considerTriggerOuterClass();
        considerTriggerAfterAttributes();
        onVisitInnerClass(name, outerName, innerName, modifiers);
    }

    /**
     * An order-sensitive invocation of {@link ClassVisitor#visitInnerClass(String, String, String, int)}.
     *
     * @param name      The internal name of the inner class.
     * @param outerName The internal name of the outer class.
     * @param innerName The inner class's simple name or {@code null} for an anonymous class.
     * @param modifiers The inner class's source code modifiers.
     */
    protected void onVisitInnerClass(String name, String outerName, String innerName, int modifiers) {
        super.visitInnerClass(name, outerName, innerName, modifiers);
    }

    @Override
    public final FieldVisitor visitField(int modifiers, String internalName, String descriptor, String signature, Object defaultValue) {
        considerTriggerNestHost();
        considerTriggerAfterPermittedSubclasses();
        considerTriggerOuterClass();
        considerTriggerAfterAttributes();
        considerTriggerAfterRecordComponents();
        return onVisitField(modifiers, internalName, descriptor, signature, defaultValue);
    }

    /**
     * An order-sensitive invocation of {@link ClassVisitor#visitField(int, String, String, String, Object)}.
     *
     * @param modifiers    The field's modifiers.
     * @param internalName The field's internal name.
     * @param descriptor   The field type's descriptor.
     * @param signature    The field's generic signature or {@code null} if the field is not generic.
     * @param defaultValue The field's default value or {@code null} if no such value exists.
     * @return A field visitor to visit the field or {@code null} to ignore it.
     */
    protected FieldVisitor onVisitField(int modifiers, String internalName, String descriptor, String signature, Object defaultValue) {
        return super.visitField(modifiers, internalName, descriptor, signature, defaultValue);
    }

    @Override
    public final MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exception) {
        considerTriggerNestHost();
        considerTriggerAfterPermittedSubclasses();
        considerTriggerOuterClass();
        considerTriggerAfterAttributes();
        considerTriggerAfterRecordComponents();
        return onVisitMethod(modifiers, internalName, descriptor, signature, exception);
    }

    /**
     * An order-sensitive invocation of {@link ClassVisitor#visitMethod(int, String, String, String, String[])}.
     *
     * @param modifiers    The method's modifiers.
     * @param internalName The method's internal name.
     * @param descriptor   The field type's descriptor.
     * @param signature    The method's generic signature or {@code null} if the method is not generic.
     * @param exception    The method's declared exceptions or {@code null} if no exceptions are declared.
     * @return A method visitor to visit the method or {@code null} to ignore it.
     */
    protected MethodVisitor onVisitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exception) {
        return super.visitMethod(modifiers, internalName, descriptor, signature, exception);
    }

    @Override
    public final void visitEnd() {
        considerTriggerNestHost();
        considerTriggerAfterPermittedSubclasses();
        considerTriggerOuterClass();
        considerTriggerAfterAttributes();
        considerTriggerAfterRecordComponents();
        onVisitEnd();
    }

    /**
     * An order-sensitive invocation of {@link ClassVisitor#visitEnd()}.
     */
    protected void onVisitEnd() {
        super.visitEnd();
    }
}
