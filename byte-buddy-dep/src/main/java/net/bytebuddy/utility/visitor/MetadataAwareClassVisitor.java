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

import net.bytebuddy.utility.nullability.MaybeNull;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.TypePath;

/**
 * A class visitor that traces invocations of visitation methods and notifies if a nest host or outer class was not visited.
 */
public abstract class MetadataAwareClassVisitor extends ClassVisitor {

    /**
     * {@code true} if the sources were not yet visited.
     */
    private boolean triggerSource;

    /**
     * {@code true} if the module information was not yet visited.
     */
    private boolean triggerModule;

    /**
     * {@code true} if the nest host was not yet visited.
     */
    private boolean triggerNestHost;

    /**
     * {@code true} if the outer class was not yet visited.
     */
    private boolean triggerOuterClass;

    /**
     * {@code true} if the attribute visitation is not yet completed.
     */
    private boolean triggerAttributes;

    /**
     * Creates a metadata aware class visitor.
     *
     * @param api          The API version.
     * @param classVisitor The class visitor to delegate to.
     */
    protected MetadataAwareClassVisitor(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
        triggerSource = true;
        triggerModule = true;
        triggerNestHost = true;
        triggerOuterClass = true;
        triggerAttributes = true;
    }

    /**
     * Invoked if the sources were not visited.
     */
    protected void onSource() {
        /* do nothing */
    }

    /**
     * Invoked if the module was not visited.
     */
    protected void onModule() {
        /* do nothing */
    }

    /**
     * Invoked if the nest host was not visited.
     */
    protected void onNestHost() {
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
     * Considers triggering a source visitation.
     */
    private void considerTriggerSource() {
        if (triggerSource) {
            triggerSource = false;
            onSource();
        }
    }

    /**
     * Considers triggering a module visitation.
     */
    private void considerTriggerModule() {
        if (triggerModule) {
            triggerModule = false;
            onModule();
        }
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
     * Considers triggering the after attribute visitation.
     */
    private void considerTriggerAfterAttributes() {
        if (triggerAttributes) {
            triggerAttributes = false;
            onAfterAttributes();
        }
    }

    @Override
    public final void visitSource(@MaybeNull String source, @MaybeNull String debug) {
        triggerSource = false;
        onVisitSource(source, debug);
    }

    /**
     * An order-sensitive invocation og {@link ClassVisitor#visitSource(String, String)}.
     *
     * @param source The name of the source file or {@code null} if not available.
     * @param debug  Additional debug information or {@code null} if not available.
     */
    protected void onVisitSource(@MaybeNull String source, @MaybeNull String debug) {
        super.visitSource(source, debug);
    }

    @Override
    @MaybeNull
    public final ModuleVisitor visitModule(String name, int access, @MaybeNull String version) {
        considerTriggerSource();
        triggerModule = false;
        return onVisitModule(name, access, version);
    }

    /**
     * An order-sensitive invocation og {@link ClassVisitor#visitModule(String, int, String)}.
     *
     * @param name      The name of the module
     * @param modifiers The modifiers of the module.
     * @param version   The module version or {@code null} if not available.
     * @return A visitor for the module information or {@code null} if skipped.
     */
    @MaybeNull
    protected ModuleVisitor onVisitModule(String name, int modifiers, @MaybeNull String version) {
        return super.visitModule(name, modifiers, version);
    }

    @Override
    public final void visitNestHost(String nestHost) {
        considerTriggerSource();
        considerTriggerModule();
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
    public final void visitOuterClass(String owner, @MaybeNull String name, @MaybeNull String descriptor) {
        considerTriggerSource();
        considerTriggerModule();
        considerTriggerNestHost();
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
    protected void onVisitOuterClass(String owner, @MaybeNull String name, @MaybeNull String descriptor) {
        super.visitOuterClass(owner, name, descriptor);
    }

    @Override
    public final void visitPermittedSubclass(String permittedSubclass) {
        considerTriggerSource();
        considerTriggerModule();
        considerTriggerNestHost();
        considerTriggerOuterClass();
        considerTriggerAfterAttributes();
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
    @MaybeNull
    public final RecordComponentVisitor visitRecordComponent(String name, String descriptor, @MaybeNull String signature) {
        considerTriggerSource();
        considerTriggerModule();
        considerTriggerNestHost();
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
    @MaybeNull
    protected RecordComponentVisitor onVisitRecordComponent(String name, String descriptor, @MaybeNull String signature) {
        return super.visitRecordComponent(name, descriptor, signature);
    }

    @Override
    @MaybeNull
    public final AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        considerTriggerSource();
        considerTriggerModule();
        considerTriggerNestHost();
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
    @MaybeNull
    protected AnnotationVisitor onVisitAnnotation(String descriptor, boolean visible) {
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    @MaybeNull
    public final AnnotationVisitor visitTypeAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
        considerTriggerSource();
        considerTriggerModule();
        considerTriggerNestHost();
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
    @MaybeNull
    protected AnnotationVisitor onVisitTypeAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
        return super.visitTypeAnnotation(typeReference, typePath, descriptor, visible);
    }

    @Override
    public final void visitAttribute(Attribute attribute) {
        considerTriggerSource();
        considerTriggerModule();
        considerTriggerNestHost();
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
        considerTriggerSource();
        considerTriggerModule();
        considerTriggerNestHost();
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
    public final void visitInnerClass(String name, @MaybeNull String outerName, @MaybeNull String innerName, int modifiers) {
        considerTriggerSource();
        considerTriggerModule();
        considerTriggerNestHost();
        considerTriggerOuterClass();
        considerTriggerAfterAttributes();
        onVisitInnerClass(name, outerName, innerName, modifiers);
    }

    /**
     * An order-sensitive invocation of {@link ClassVisitor#visitInnerClass(String, String, String, int)}.
     *
     * @param internalName The internal name of the inner class.
     * @param outerName    The internal name of the outer class or {@code null} for a member class.
     * @param innerName    The inner class's simple name or {@code null} for an anonymous class.
     * @param modifiers    The inner class's source code modifiers.
     */
    protected void onVisitInnerClass(String internalName, @MaybeNull String outerName, @MaybeNull String innerName, int modifiers) {
        super.visitInnerClass(internalName, outerName, innerName, modifiers);
    }

    @Override
    @MaybeNull
    public final FieldVisitor visitField(int modifiers, String internalName, String descriptor, @MaybeNull String signature, @MaybeNull Object value) {
        considerTriggerSource();
        considerTriggerModule();
        considerTriggerNestHost();
        considerTriggerOuterClass();
        considerTriggerAfterAttributes();
        return onVisitField(modifiers, internalName, descriptor, signature, value);
    }

    /**
     * An order-sensitive invocation of {@link ClassVisitor#visitField(int, String, String, String, Object)}.
     *
     * @param modifiers    The field's modifiers.
     * @param internalName The field's internal name.
     * @param descriptor   The field type's descriptor.
     * @param signature    The field's generic signature or {@code null} if the field is not generic.
     * @param value        The field's default value or {@code null} if no such value exists.
     * @return A field visitor to visit the field or {@code null} to ignore it.
     */
    @MaybeNull
    protected FieldVisitor onVisitField(int modifiers, String internalName, String descriptor, @MaybeNull String signature, @MaybeNull Object value) {
        return super.visitField(modifiers, internalName, descriptor, signature, value);
    }

    @Override
    @MaybeNull
    public final MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, @MaybeNull String signature, @MaybeNull String[] exception) {
        considerTriggerSource();
        considerTriggerModule();
        considerTriggerNestHost();
        considerTriggerOuterClass();
        considerTriggerAfterAttributes();
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
    @MaybeNull
    protected MethodVisitor onVisitMethod(int modifiers, String internalName, String descriptor, @MaybeNull String signature, @MaybeNull String[] exception) {
        return super.visitMethod(modifiers, internalName, descriptor, signature, exception);
    }

    @Override
    public final void visitEnd() {
        considerTriggerSource();
        considerTriggerModule();
        considerTriggerNestHost();
        considerTriggerOuterClass();
        considerTriggerAfterAttributes();
        onVisitEnd();
    }

    /**
     * An order-sensitive invocation of {@link ClassVisitor#visitEnd()}.
     */
    protected void onVisitEnd() {
        super.visitEnd();
    }
}
