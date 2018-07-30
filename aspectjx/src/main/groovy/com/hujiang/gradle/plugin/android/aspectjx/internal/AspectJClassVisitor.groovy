/*
 * Copyright 2018 firefly1126, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.gradle_plugin_android_aspectjx
 */
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
        isAspectClass |= (desc == 'Lorg/aspectj/lang/annotation/Aspect;')

        return super.visitAnnotation(desc, visible)
    }
}
