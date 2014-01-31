package com.blogspot.mydailyjava.bytebuddy.asm;

import org.objectweb.asm.ClassVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface ClassVisitorWrapper {

    static class Chain implements ClassVisitorWrapper {

        private final List<ClassVisitorWrapper> classVisitorWrappers;

        public Chain() {
            this.classVisitorWrappers = Collections.emptyList();
        }

        protected Chain(List<ClassVisitorWrapper> classVisitorWrappers) {
            this.classVisitorWrappers = Collections.unmodifiableList(classVisitorWrappers);
        }

        public Chain prepend(ClassVisitorWrapper classVisitorWrapper) {
            List<ClassVisitorWrapper> appendedList = new ArrayList<ClassVisitorWrapper>(classVisitorWrappers.size() + 1);
            appendedList.add(classVisitorWrapper);
            appendedList.addAll(classVisitorWrappers);
            return new Chain(appendedList);
        }

        public Chain append(ClassVisitorWrapper classVisitorWrapper) {
            List<ClassVisitorWrapper> appendedList = new ArrayList<ClassVisitorWrapper>(classVisitorWrappers.size() + 1);
            appendedList.addAll(classVisitorWrappers);
            appendedList.add(classVisitorWrapper);
            return new Chain(appendedList);
        }

        @Override
        public ClassVisitor wrap(ClassVisitor classVisitor) {
            for (ClassVisitorWrapper classVisitorWrapper : classVisitorWrappers) {
                classVisitor = classVisitorWrapper.wrap(classVisitor);
            }
            return classVisitor;
        }
    }

    ClassVisitor wrap(ClassVisitor classVisitor);
}
