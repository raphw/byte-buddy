package com.blogspot.mydailyjava.bytebuddy.asm;

import org.objectweb.asm.ClassVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassVisitorWrapperChain implements ClassVisitorWrapper {

    private final List<ClassVisitorWrapper> classVisitorWrappers;

    public ClassVisitorWrapperChain() {
        this.classVisitorWrappers = Collections.emptyList();
    }

    protected ClassVisitorWrapperChain(List<ClassVisitorWrapper> classVisitorWrappers) {
        this.classVisitorWrappers = Collections.unmodifiableList(classVisitorWrappers);
    }

    public ClassVisitorWrapperChain prepend(ClassVisitorWrapper classVisitorWrapper) {
        List<ClassVisitorWrapper> appendedList = new ArrayList<ClassVisitorWrapper>(classVisitorWrappers.size() + 1);
        appendedList.add(classVisitorWrapper);
        appendedList.addAll(classVisitorWrappers);
        return new ClassVisitorWrapperChain(appendedList);
    }

    public ClassVisitorWrapperChain append(ClassVisitorWrapper classVisitorWrapper) {
        List<ClassVisitorWrapper> appendedList = new ArrayList<ClassVisitorWrapper>(classVisitorWrappers.size() + 1);
        appendedList.addAll(classVisitorWrappers);
        appendedList.add(classVisitorWrapper);
        return new ClassVisitorWrapperChain(appendedList);
    }

    @Override
    public ClassVisitor wrap(ClassVisitor classVisitor) {
        for(ClassVisitorWrapper classVisitorWrapper : classVisitorWrappers) {
            classVisitor = classVisitorWrapper.wrap(classVisitor);
        }
        return classVisitor;
    }
}
