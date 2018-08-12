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
        triggerNestHost = true;
        triggerOuterClass = true;
        triggerAttributes = true;
    }

    /**
     * Invoked if the nest host was not visited.
     */
    protected abstract void onNestHost();

    /**
     * Invoked if the outer class was not visited.
     */
    protected abstract void onOuterClass();

    /**
     * Invoked if the attribute visitation is about to complete.
     */
    protected abstract void onAfterAttributes();

    /**
     * Considers triggering a nest host visitation.
     */
    protected void considerTriggerNestHost() {
        if (triggerNestHost) {
            triggerNestHost = false;
            onNestHost();
        }
    }

    /**
     * Considers triggering an outer class visitation.
     */
    protected void considerTriggerOuterClass() {
        if (triggerOuterClass) {
            triggerOuterClass = false;
            onOuterClass();
        }
    }

    /**
     * Considers triggering the after attribute visitation.
     */
    protected void considerTriggerAfterAttributes() {
        if (triggerAttributes) {
            triggerAttributes = false;
            onAfterAttributes();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void visitNestHostExperimental(String nestHost) {
        triggerNestHost = false;
        super.visitNestHostExperimental(nestHost);
    }

    @Override
    public void visitOuterClass(String owner, String name, String descriptor) {
        considerTriggerNestHost();
        triggerOuterClass = false;
        super.visitOuterClass(owner, name, descriptor);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        considerTriggerNestHost();
        considerTriggerOuterClass();
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
        considerTriggerNestHost();
        considerTriggerOuterClass();
        return super.visitTypeAnnotation(typeReference, typePath, descriptor, visible);
    }

    @Override
    public void visitAttribute(Attribute attribute) {
        considerTriggerNestHost();
        considerTriggerOuterClass();
        super.visitAttribute(attribute);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void visitNestMemberExperimental(String nestMember) {
        considerTriggerNestHost();
        considerTriggerOuterClass();
        considerTriggerAfterAttributes();
        super.visitNestMemberExperimental(nestMember);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int modifiers) {
        considerTriggerNestHost();
        considerTriggerOuterClass();
        considerTriggerAfterAttributes();
        super.visitInnerClass(name, outerName, innerName, modifiers);
    }

    @Override
    public FieldVisitor visitField(int modifiers, String internalName, String descriptor, String signature, Object defaultValue) {
        considerTriggerNestHost();
        considerTriggerOuterClass();
        considerTriggerAfterAttributes();
        return super.visitField(modifiers, internalName, descriptor, signature, defaultValue);
    }

    @Override
    public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exception) {
        considerTriggerNestHost();
        considerTriggerOuterClass();
        considerTriggerAfterAttributes();
        return super.visitMethod(modifiers, internalName, descriptor, signature, exception);
    }

    @Override
    public void visitEnd() {
        considerTriggerNestHost();
        considerTriggerOuterClass();
        considerTriggerAfterAttributes();
        super.visitEnd();
    }
}
