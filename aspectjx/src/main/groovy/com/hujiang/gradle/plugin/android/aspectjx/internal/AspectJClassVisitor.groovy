package com.hujiang.gradle.plugin.android.aspectjx.internal

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-02-01
 */

class AspectJClassVisitor extends ClassVisitor {

    boolean isAspectClass = false

    AspectJClassVisitor(ClassWriter classWriter) {
        super(Opcodes.ASM5, classWriter)
    }

    @Override
    AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        isAspectClass = (desc == 'Lorg/aspectj/lang/annotation/Aspect;')

        return super.visitAnnotation(desc, visible)
    }
}
