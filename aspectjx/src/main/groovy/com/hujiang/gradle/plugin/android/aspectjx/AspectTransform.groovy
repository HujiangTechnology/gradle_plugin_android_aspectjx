package com.hujiang.gradle.plugin.android.aspectjx

import com.android.build.api.transform.Context
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformOutputProvider
import com.android.utils.FileUtils
import com.google.common.collect.ImmutableSet
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

/**
 * aspectj transform
 * @author simon
 * @version 1.0.0
 * @since 2016-03-29
 */
class AspectTransform extends Transform {

    Project project
    AspectWork aspectWork
    public AspectTransform(Project proj) {
        project = proj

        aspectWork = new AspectWork(project)

        def configuration = new AndroidConfiguration(project)

        project.afterEvaluate {
            configuration.variants.all { variant ->
                JavaCompile javaCompile = variant.hasProperty('javaCompiler') ? variant.javaCompiler : variant.javaCompile

                aspectWork.encoding = javaCompile.options.encoding
                aspectWork.bootClassPath = configuration.bootClasspath.join(File.pathSeparator)
                aspectWork.sourceCompatibility = javaCompile.sourceCompatibility
                aspectWork.targetCompatibility = javaCompile.targetCompatibility
            }
        }
    }

    @Override
    String getName() {
        return "AspectTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return ImmutableSet.<QualifiedContent.ContentType>of(QualifiedContent.DefaultContentType.CLASSES)
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return ImmutableSet.<QualifiedContent.Scope>of(QualifiedContent.Scope.PROJECT
                , QualifiedContent.Scope.PROJECT_LOCAL_DEPS
                , QualifiedContent.Scope.EXTERNAL_LIBRARIES
                , QualifiedContent.Scope.SUB_PROJECTS
                , QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS)
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(Context context
                   , Collection<TransformInput> inputs
                   , Collection<TransformInput> referencedInputs
                   , TransformOutputProvider outputProvider
                   , boolean isIncremental) throws IOException, TransformException, InterruptedException {

        //create aspect destination dir
        File resultDir = outputProvider.getContentLocation("aspect", getOutputTypes(), getScopes(), Format.DIRECTORY);
        if (resultDir.exists()) {
            FileUtils.deleteFolder(resultDir)
        }
        FileUtils.mkdirs(resultDir);


        aspectWork.destinationDir = resultDir.absolutePath
        //
        for (TransformInput transformInput : inputs) {
            for (DirectoryInput directoryInput : transformInput.directoryInputs) {
                aspectWork.aspectPath << directoryInput.file
                aspectWork.inPath << directoryInput.file
                aspectWork.classPath << directoryInput.file
            }

            for (JarInput jarInput : transformInput.jarInputs) {
                aspectWork.aspectPath << jarInput.file
                aspectWork.inPath << jarInput.file
                aspectWork.classPath << jarInput.file
            }
        }

        aspectWork.doWork()
    }
}
