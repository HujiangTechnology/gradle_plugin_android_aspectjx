package com.hujiang.gradle.plugin.android.aspectjx

import com.android.SdkConstants
import com.android.build.api.transform.Context
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformOutputProvider
import com.google.common.collect.ImmutableSet
import org.aspectj.util.FileUtil
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile


/**
 * aspectj transform
 * @author simon
 * @version 1.0.0
 * @since 2016-03-29
 */
class AspectTransform extends Transform {

    static final ASPECTJRT = "aspectjrt"

    Project project
    String encoding
    String bootClassPath
    String sourceCompatibility
    String targetCompatibility

    public AspectTransform(Project proj) {
        project = proj

        def configuration = new AndroidConfiguration(project)

        project.afterEvaluate {
            configuration.variants.all { variant ->
                JavaCompile javaCompile = variant.hasProperty('javaCompiler') ? variant.javaCompiler : variant.javaCompile

                encoding = javaCompile.options.encoding
                bootClassPath = configuration.bootClasspath.join(File.pathSeparator)
                sourceCompatibility = javaCompile.sourceCompatibility
                targetCompatibility = javaCompile.targetCompatibility
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

        println "aspect start.........."
        AspectWork aspectWork = new AspectWork(project)
        aspectWork.encoding = encoding
        aspectWork.bootClassPath = bootClassPath
        aspectWork.sourceCompatibility = sourceCompatibility
        aspectWork.targetCompatibility = targetCompatibility

        //clean
        outputProvider.deleteAll()

        //create aspect destination dir
        File resultDir = outputProvider.getContentLocation("aspect", getOutputTypes(), getScopes(), Format.DIRECTORY);
        if (resultDir.exists()) {
            FileUtils.deleteFolder(resultDir)
        }
        FileUtils.mkdirs(resultDir);

        aspectWork.destinationDir = resultDir.absolutePath

        List<String> includeJarFilter = project.aspectjx.includeJarFilter
        List<String> excludeJarFilter = project.aspectjx.excludeJarFilter
        
        aspectWork.setAjcArgs(project.aspectjx.ajcArgs);

        for (TransformInput transformInput : inputs) {
            for (DirectoryInput directoryInput : transformInput.directoryInputs) {

                println "directoryInput:::${directoryInput.file.absolutePath}"

                aspectWork.aspectPath << directoryInput.file
                aspectWork.inPath << directoryInput.file
                aspectWork.classPath << directoryInput.file
            }

            for (JarInput jarInput : transformInput.jarInputs) {

                aspectWork.aspectPath << jarInput.file
                aspectWork.classPath << jarInput.file

                String jarPath = jarInput.file.absolutePath
                if (isIncludeFilterMatched(jarPath, includeJarFilter)
                    && !isExcludeFilterMatched(jarPath, excludeJarFilter)) {
                    println "includeJar:::${jarPath}"
                    aspectWork.inPath << jarInput.file
                } else {
                    println "excludeJar:::${jarPath}"
                    copyJar(outputProvider, jarInput)
                }
            }
        }

        def hasAjRt = aspectWork.classPath.find { it.name.contains(ASPECTJRT) }

        if (hasAjRt) {
            //aspect work
            aspectWork.doWork()
        } else {
            println "there is no aspectjrt dependencies in classpath"
        }


        //add class file to aspect result jar
        println "aspect jar merging.........."
        if (resultDir.listFiles().length > 0) {
            File jarFile = outputProvider.getContentLocation("aspected", getOutputTypes(), getScopes(), Format.JAR);
            FileUtils.mkdirs(jarFile.getParentFile())
            FileUtils.deleteIfExists(jarFile)

            JarMerger jarMerger = new JarMerger(jarFile);
            try {
                jarMerger.setFilter(new JarMerger.IZipEntryFilter() {
                    @Override
                    public boolean checkEntry(String archivePath)
                            throws JarMerger.IZipEntryFilter.ZipAbortException {
                        return archivePath.endsWith(SdkConstants.DOT_CLASS);
                    }
                });


                jarMerger.addFolder(resultDir)
            } catch (Exception e) {
                throw new TransformException(e)
            } finally {
                jarMerger.close()
            }

        }

        FileUtils.deleteFolder(resultDir)

        println "aspect done..................."
    }

    boolean isExcludeFilterMatched(String str, List<String> filters) {
        return isFilterMatched(str, filters, FilterPolicy.EXCLUDE)
    }

    boolean  isIncludeFilterMatched(String str, List<String> filters) {
        return isFilterMatched(str, filters, FilterPolicy.INCLUDE)
    }

    boolean isFilterMatched(String str, List<String> filters, FilterPolicy filterPolicy) {
        if(str == null) {
            return false
        }

        if (filters == null || filters.isEmpty()) {
            return filterPolicy == FilterPolicy.INCLUDE
        }

        for (String s : filters) {
            if (isContained(str, s)) {
                return true
            }
        }

        return false
    }

    boolean copyJar(TransformOutputProvider outputProvider, JarInput jarInput) {
        if (outputProvider == null || jarInput == null) {
            return false
        }

        String jarName = jarInput.name
        if (jarName.endsWith(".jar")) {
            jarName = jarName.substring(0, jarName.length() - 4)
        }

        File dest = outputProvider.getContentLocation(jarName, jarInput.contentTypes, jarInput.scopes, Format.JAR)

        FileUtil.copyFile(jarInput.file, dest)

        return true
    }

    boolean isContained(String str, String filter) {
        if (str == null) {
            return false
        }

        String filterTmp = filter
        if (str.contains(filterTmp)) {
            return true
        } else {
            if (filterTmp.contains("/")) {
                return str.contains(filterTmp.replace("/", File.separator))
            } else if (filterTmp.contains("\\")) {
                return str.contains(filterTmp.replace("\\", File.separator))
            }
        }

        return false
    }

    enum FilterPolicy {
        INCLUDE
        , EXCLUDE
    }
}
