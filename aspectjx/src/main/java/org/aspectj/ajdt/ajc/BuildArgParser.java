/* *******************************************************************
 * Copyright (c) 2002 Palo Alto Research Center, Incorporated (PARC).
 * All rights reserved.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     PARC     initial implementation
 * ******************************************************************/

package org.aspectj.ajdt.ajc;

import org.aspectj.ajdt.internal.compiler.lookup.EclipseSourceLocation;
import org.aspectj.ajdt.internal.core.builder.AjBuildConfig;
import org.aspectj.bridge.*;
import org.aspectj.org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.aspectj.org.eclipse.jdt.internal.compiler.apt.dispatch.AptProblem;
import org.aspectj.org.eclipse.jdt.internal.compiler.batch.Main;
import org.aspectj.org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.aspectj.org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.aspectj.util.FileUtil;
import org.aspectj.util.LangUtil;
import org.aspectj.weaver.Constants;
import org.aspectj.weaver.Dump;
import org.aspectj.weaver.WeaverMessages;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

@SuppressWarnings("unchecked")
public class BuildArgParser extends Main {

    private static final String BUNDLE_NAME = "org.aspectj.ajdt.ajc.messages";
    private static boolean LOADED_BUNDLE = false;

    static {
        Main.bundleName = BUNDLE_NAME;
        ResourceBundleFactory.getBundle(Locale.getDefault());
        if (!LOADED_BUNDLE) {
            LOADED_BUNDLE = true;
        }
    }

    /**
     * to initialize super's PrintWriter but refer to underlying StringWriter
     */
    private static class StringPrintWriter extends PrintWriter {
        public final StringWriter stringWriter;

        StringPrintWriter(StringWriter sw) {
            super(sw);
            this.stringWriter = sw;
        }
    }

    /**
     * @return multi-line String usage for the compiler
     */
    public static String getUsage() {
        return _bind("misc.usage", new String[]{_bind("compiler.name", (String[]) null)});
    }

    public static String getXOptionUsage() {
        return _bind("xoption.usage", new String[]{_bind("compiler.name", (String[]) null)});
    }

    /**
     * StringWriter sink for some errors. This only captures errors not handled by any IMessageHandler parameter and only when no
     * PrintWriter is set in the constructor. XXX This relies on (Sun's) implementation of StringWriter, which returns the actual
     * (not copy) internal StringBuffer.
     */
    private final StringBuffer errorSink;

    private IMessageHandler handler;

    /**
     * Overrides super's bundle.
     */
    public BuildArgParser(PrintWriter writer, IMessageHandler handler) {
        super(writer, writer, false, null, null);

        if (writer instanceof StringPrintWriter) {
            errorSink = ((StringPrintWriter) writer).stringWriter.getBuffer();
        } else {
            errorSink = null;
        }
        this.handler = handler;
    }

    /**
     * Set up to capture messages using getOtherMessages(boolean)
     */
    public BuildArgParser(IMessageHandler handler) {
        this(new StringPrintWriter(new StringWriter()), handler);
    }

    /**
     * Generate build configuration for the input args, passing to handler any error messages.
     *
     * @param args the String[] arguments for the build configuration
     * @return AjBuildConfig per args, which will be invalid unless there are no handler errors.
     */
    public AjBuildConfig genBuildConfig(String[] args) {
        final AjBuildConfig config = new AjBuildConfig(this);
        populateBuildConfig(config, args, true, null);
        return config;
    }

    /**
     * Generate build configuration for the input arguments, passing to handler any error messages.
     *
     * @param args         the String[] arguments for the build configuration
     * @param setClasspath determines if the classpath should be parsed and set on the build configuration
     * @param configFile   can be null
     * @return AjBuildConfig per arguments, which will be invalid unless there are no handler errors.
     */
    public AjBuildConfig populateBuildConfig(AjBuildConfig buildConfig, String[] args, boolean setClasspath, File configFile) {
        Dump.saveCommandLine(args);
        buildConfig.setConfigFile(configFile);
        try {
            // sets filenames to be non-null in order to make sure that file parameters are ignored
            super.filenames = new String[]{""};

            AjcConfigParser parser = new AjcConfigParser(buildConfig, handler);
            parser.parseCommandLine(args);
            boolean swi = buildConfig.getShowWeavingInformation();
            // Now jump through firey hoops to turn them on/off
            if (handler instanceof CountingMessageHandler) {
                IMessageHandler delegate = ((CountingMessageHandler) handler).delegate;
                if (swi) {
                    delegate.dontIgnore(IMessage.WEAVEINFO);
                } else {
                    delegate.ignore(IMessage.WEAVEINFO);
                }
            }

            boolean incrementalMode = buildConfig.isIncrementalMode() || buildConfig.isIncrementalFileMode();

            List<File> xmlfileList = new ArrayList<File>();
            xmlfileList.addAll(parser.getXmlFiles());

            List<File> fileList = new ArrayList<File>();
            List<File> files = parser.getFiles();
            if (!LangUtil.isEmpty(files)) {
                if (incrementalMode) {
                    MessageUtil.error(handler, "incremental mode only handles source files using -sourceroots");
                } else {
                    fileList.addAll(files);
                }
            }

            List<String> javaArgList = new ArrayList<String>();
            // disable all special eclipse warnings by default - why???
            // ??? might want to instead override getDefaultOptions()
            javaArgList.add("-warn:none");
            // these next four lines are some nonsense to fool the eclipse batch compiler
            // without these it will go searching for reasonable values from properties
            // TODO fix org.eclipse.jdt.internal.compiler.batch.Main so this hack isn't needed
            javaArgList.add("-classpath");
            javaArgList.add(parser.classpath == null ? System.getProperty("user.dir") : parser.classpath);
            javaArgList.add("-bootclasspath");
            javaArgList.add(parser.bootclasspath == null ? System.getProperty("user.dir") : parser.bootclasspath);
            javaArgList.addAll(parser.getUnparsedArgs());
            super.configure(javaArgList.toArray(new String[javaArgList.size()]));

            if (!proceed) {
                buildConfig.doNotProceed();
                return buildConfig;
            }

            if (buildConfig.getSourceRoots() != null) {
                for (Iterator i = buildConfig.getSourceRoots().iterator(); i.hasNext(); ) {
                    fileList.addAll(collectSourceRootFiles((File) i.next()));
                }
            }

            buildConfig.setXmlFiles(xmlfileList);

            buildConfig.setFiles(fileList);
            if (destinationPath != null) { // XXX ?? unparsed but set?
                buildConfig.setOutputDir(new File(destinationPath));
            }

            if (setClasspath) {
                buildConfig.setClasspath(getClasspath(parser));
                buildConfig.setBootclasspath(getBootclasspath(parser));
            }

            if (incrementalMode && (0 == buildConfig.getSourceRoots().size())) {
                MessageUtil.error(handler, "specify a source root when in incremental mode");
            }

			/*
             * Ensure we don't overwrite injars, inpath or aspectpath with outjar bug-71339
			 */
            File outjar = buildConfig.getOutputJar();
            if (outjar != null) {

				/* Search injars */
                for (Iterator i = buildConfig.getInJars().iterator(); i.hasNext(); ) {
                    File injar = (File) i.next();
                    if (injar.equals(outjar)) {
                        String message = WeaverMessages.format(WeaverMessages.OUTJAR_IN_INPUT_PATH);
                        MessageUtil.error(handler, message);
                    }
                }

				/* Search inpath */
                for (Iterator i = buildConfig.getInpath().iterator(); i.hasNext(); ) {
                    File inPathElement = (File) i.next();
                    if (!inPathElement.isDirectory() && inPathElement.equals(outjar)) {
                        String message = WeaverMessages.format(WeaverMessages.OUTJAR_IN_INPUT_PATH);
                        MessageUtil.error(handler, message);
                    }
                }

				/* Search aspectpath */
                for (Iterator i = buildConfig.getAspectpath().iterator(); i.hasNext(); ) {
                    File pathElement = (File) i.next();
                    if (!pathElement.isDirectory() && pathElement.equals(outjar)) {
                        String message = WeaverMessages.format(WeaverMessages.OUTJAR_IN_INPUT_PATH);
                        MessageUtil.error(handler, message);
                    }
                }

            }

            setDebugOptions();
            buildConfig.getOptions().set(options);
        } catch (IllegalArgumentException iae) {
            ISourceLocation location = null;
            if (buildConfig.getConfigFile() != null) {
                location = new SourceLocation(buildConfig.getConfigFile(), 0);
            }
            IMessage m = new Message(iae.getMessage(), IMessage.ERROR, null, location);
            handler.handleMessage(m);
        }
        return buildConfig;
    }

    public void printVersion() {
        final String version = bind("misc.version", //$NON-NLS-1$
                new String[]{bind("compiler.name"), //$NON-NLS-1$
                        Version.getText() + " - Built: " + Version.getTimeText(), bind("compiler.version"), //$NON-NLS-1$
                        bind("compiler.copyright") //$NON-NLS-1$
                });
        System.out.println(version);
    }

    @Override
    public void addExtraProblems(CategorizedProblem problem) {
        super.addExtraProblems(problem);
        if (problem instanceof AptProblem) {
            handler.handleMessage(newAptMessage((AptProblem) problem));
        }
    }

    private IMessage newAptMessage(AptProblem problem) {
        String message = problem.getMessage();
        boolean isError = problem.isError();
        if (problem._referenceContext != null) {
            return new Message(message,
                    new EclipseSourceLocation(problem._referenceContext.compilationResult(), problem.getSourceStart(), problem.getSourceEnd()),
                    isError);
        } else {
            return new Message(message, null, isError);
        }
    }

    @Override
    public void initializeAnnotationProcessorManager() {
        if (this.compilerOptions.complianceLevel < ClassFileConstants.JDK1_6 || !this.compilerOptions.processAnnotations)
            return;
        super.initializeAnnotationProcessorManager();
    }

    @Override
    public void printUsage() {
        System.out.println(getUsage());
        System.out.flush();
    }

    /**
     * Get messages not dumped to handler or any PrintWriter.
     *
     * @param flush if true, empty errors
     * @return null if none, String otherwise
     * @see BuildArgParser()
     */
    public String getOtherMessages(boolean flush) {
        if (null == errorSink) {
            return null;
        }

        String result = errorSink.toString().trim();
        if (0 == result.length()) {
            result = null;
        }
        if (flush) {
            errorSink.setLength(0);
        }
        return result;
    }

    private void setDebugOptions() {
        options.put(CompilerOptions.OPTION_LocalVariableAttribute, CompilerOptions.GENERATE);
        options.put(CompilerOptions.OPTION_LineNumberAttribute, CompilerOptions.GENERATE);
        options.put(CompilerOptions.OPTION_SourceFileAttribute, CompilerOptions.GENERATE);
    }

    private Collection collectSourceRootFiles(File dir) {
        return Arrays.asList(FileUtil.listFiles(dir, FileUtil.aspectjSourceFileFilter));
    }

    public List<String> getBootclasspath(AjcConfigParser parser) {
        List<String> ret = new ArrayList<String>();

        if (parser.bootclasspath == null) {
            addClasspath(System.getProperty("sun.boot.class.path", ""), ret);
        } else {
            addClasspath(parser.bootclasspath, ret);
        }
        return ret;
    }

    /**
     * If the classpath is not set, we use the environment's java.class.path, but remove the aspectjtools.jar entry from that list
     * in order to prevent wierd bootstrap issues (refer to bug#39959).
     */
    public List getClasspath(AjcConfigParser parser) {
        List ret = new ArrayList();

        // if (parser.bootclasspath == null) {
        // addClasspath(System.getProperty("sun.boot.class.path", ""), ret);
        // } else {
        // addClasspath(parser.bootclasspath, ret);
        // }

        String extdirs = parser.extdirs;
        if (extdirs == null) {
            extdirs = System.getProperty("java.ext.dirs", "");
        }
        addExtDirs(extdirs, ret);

        if (parser.classpath == null) {
            addClasspath(System.getProperty("java.class.path", ""), ret);
            List fixedList = new ArrayList();
            for (Iterator it = ret.iterator(); it.hasNext(); ) {
                String entry = (String) it.next();
                if (!entry.endsWith("aspectjtools.jar")) {
                    fixedList.add(entry);
                }
            }
            ret = fixedList;
        } else {
            addClasspath(parser.classpath, ret);
        }
        // ??? eclipse seems to put outdir on the classpath
        // ??? we're brave and believe we don't need it
        return ret;
    }

    private void addExtDirs(String extdirs, List classpathCollector) {
        StringTokenizer tokenizer = new StringTokenizer(extdirs, File.pathSeparator);
        while (tokenizer.hasMoreTokens()) {
            // classpathCollector.add(tokenizer.nextToken());
            File dirFile = new File(tokenizer.nextToken());
            if (dirFile.canRead() && dirFile.isDirectory()) {
                File[] files = dirFile.listFiles(FileUtil.ZIP_FILTER);
                for (int i = 0; i < files.length; i++) {
                    classpathCollector.add(files[i].getAbsolutePath());
                }
            } else {
                // XXX alert on invalid -extdirs entries
            }
        }
    }

    private void addClasspath(String classpath, List<String> classpathCollector) {
        StringTokenizer tokenizer = new StringTokenizer(classpath, File.pathSeparator);
        while (tokenizer.hasMoreTokens()) {
            classpathCollector.add(tokenizer.nextToken());
        }
    }

    private class AjcConfigParser extends ConfigParser {
        private String bootclasspath = null;
        private String classpath = null;
        private String extdirs = null;
        private List unparsedArgs = new ArrayList();
        private AjBuildConfig buildConfig;
        private IMessageHandler handler;

        public AjcConfigParser(AjBuildConfig buildConfig, IMessageHandler handler) {
            this.buildConfig = buildConfig;
            this.handler = handler;
        }

        public List getUnparsedArgs() {
            return unparsedArgs;
        }

        /**
         * Extract AspectJ-specific options (except for argfiles). Caller should warn when sourceroots is empty but in incremental
         * mode. Signals warnings or errors through handler set in constructor.
         */
        public void parseOption(String arg, LinkedList args) { // XXX use ListIterator.remove()
            int nextArgIndex = args.indexOf(arg) + 1; // XXX assumes unique
            // trim arg?
            buildConfig.setXlazyTjp(true); // now default - MINOR could be pushed down and made default at a lower level
            if (LangUtil.isEmpty(arg)) {
                showWarning("empty arg found");
            } else if (arg.equals("-infiles")) {
                if (args.size() > nextArgIndex) {
                    // buildConfig.getAjOptions().put(AjCompilerOptions.OPTION_Inpath, CompilerOptions.PRESERVE);

                    List<File> inPath = buildConfig.getInpath();
                    StringTokenizer st = new StringTokenizer(((Arg) args.get(nextArgIndex)).getValue(),
                            File.pathSeparator);
                    File dir = null;
                    while (st.hasMoreTokens()) {
                        String filename = st.nextToken();
                        File file = makeFile(filename);
                        if (file.isDirectory()) {
                            dir = file;
                            continue;
                        }
                        if (dir != null) {
                            buildConfig.addBinarySourceFile(dir, file);
                        }
                    }

                    args.remove(args.get(nextArgIndex));
                }
            } else if (arg.equals("-inpath")) {
                if (args.size() > nextArgIndex) {
                    // buildConfig.getAjOptions().put(AjCompilerOptions.OPTION_Inpath, CompilerOptions.PRESERVE);

                    List<File> inPath = buildConfig.getInpath();
                    StringTokenizer st = new StringTokenizer(((Arg) args.get(nextArgIndex)).getValue(),
                            File.pathSeparator);
                    while (st.hasMoreTokens()) {
                        String filename = st.nextToken();
                        File file = makeFile(filename);
                        if (FileUtil.isZipFile(file)) {
                            inPath.add(file);
                        } else {
                            if (file.isDirectory()) {
                                inPath.add(file);
                            } else {
                                showWarning("skipping missing, empty or corrupt inpath entry: " + filename);
                            }
                        }
                    }
                    buildConfig.setInPath(inPath);
                    args.remove(args.get(nextArgIndex));
                }
            } else if (arg.equals("-injars")) {
                if (args.size() > nextArgIndex) {
                    // buildConfig.getAjOptions().put(AjCompilerOptions.OPTION_InJARs, CompilerOptions.PRESERVE);

                    StringTokenizer st = new StringTokenizer(((Arg) args.get(nextArgIndex)).getValue(),
                            File.pathSeparator);
                    while (st.hasMoreTokens()) {
                        String filename = st.nextToken();
                        File jarFile = makeFile(filename);
                        if (FileUtil.isZipFile(jarFile)) {
                            buildConfig.getInJars().add(jarFile);
                        } else {
                            File dirFile = makeFile(filename);
                            if (dirFile.isDirectory()) {
                                buildConfig.getInJars().add(dirFile);
                            } else {
                                showWarning("skipping missing, empty or corrupt injar: " + filename);
                            }
                        }
                    }

                    args.remove(args.get(nextArgIndex));
                }
            } else if (arg.equals("-aspectpath")) {
                if (args.size() > nextArgIndex) {
                    StringTokenizer st = new StringTokenizer(((Arg) args.get(nextArgIndex)).getValue(),
                            File.pathSeparator);
                    while (st.hasMoreTokens()) {
                        String filename = st.nextToken();
                        File jarFile = makeFile(filename);
                        if (FileUtil.isZipFile(jarFile) || jarFile.isDirectory()) {
                            buildConfig.getAspectpath().add(jarFile);
                        } else {
                            showWarning("skipping missing, empty or corrupt aspectpath entry: " + filename);
                        }
                    }

                    args.remove(args.get(nextArgIndex));
                }
            } else if (arg.equals("-makeAjReflectable")) {
                buildConfig.setMakeReflectable(true);
            } else if (arg.equals("-sourceroots")) {
                if (args.size() > nextArgIndex) {
                    List<File> sourceRoots = new ArrayList<File>();
                    StringTokenizer st = new StringTokenizer(((Arg) args.get(nextArgIndex)).getValue(),
                            File.pathSeparator);
                    while (st.hasMoreTokens()) {
                        File f = makeFile(st.nextToken());
                        if (f.isDirectory() && f.canRead()) {
                            sourceRoots.add(f);
                        } else {
                            showError("bad sourceroot: " + f);
                        }
                    }
                    if (0 < sourceRoots.size()) {
                        buildConfig.setSourceRoots(sourceRoots);
                    }
                    args.remove(args.get(nextArgIndex));
                } else {
                    showError("-sourceroots requires list of directories");
                }
            } else if (arg.equals("-outjar")) {
                if (args.size() > nextArgIndex) {
                    // buildConfig.getAjOptions().put(AjCompilerOptions.OPTION_OutJAR, CompilerOptions.GENERATE);
                    File jarFile = makeFile(((Arg) args.get(nextArgIndex)).getValue());
                    if (!jarFile.isDirectory()) {
                        try {
                            if (!jarFile.exists()) {
                                jarFile.createNewFile();
                            }
                            buildConfig.setOutputJar(jarFile);
                        } catch (IOException ioe) {
                            showError("unable to create outjar file: " + jarFile);
                        }
                    } else {
                        showError("invalid -outjar file: " + jarFile);
                    }
                    args.remove(args.get(nextArgIndex));
                } else {
                    showError("-outjar requires jar path argument");
                }
            } else if (arg.equals("-outxml")) {
                buildConfig.setOutxmlName(org.aspectj.bridge.Constants.AOP_AJC_XML);
            } else if (arg.equals("-outxmlfile")) {
                if (args.size() > nextArgIndex) {
                    String name = ((Arg) args.get(nextArgIndex)).getValue();
                    buildConfig.setOutxmlName(name);
                    args.remove(args.get(nextArgIndex));
                } else {
                    showError("-outxmlfile requires file name argument");
                }
            } else if (arg.equals("-log")) {
                // remove it as it's already been handled in org.aspectj.tools.ajc.Main
                args.remove(args.get(nextArgIndex));
            } else if (arg.equals("-messageHolder")) {
                // remove it as it's already been handled in org.aspectj.tools.ajc.Main
                args.remove(args.get(nextArgIndex));
            } else if (arg.equals("-incremental")) {
                buildConfig.setIncrementalMode(true);
            } else if (arg.equals("-XincrementalFile")) {
                if (args.size() > nextArgIndex) {
                    File file = makeFile(((Arg) args.get(nextArgIndex)).getValue());
                    buildConfig.setIncrementalFile(file);
                    if (!file.canRead()) {
                        showError("bad -XincrementalFile : " + file);
                        // if not created before recompile test, stop after first compile
                    }
                    args.remove(args.get(nextArgIndex));
                } else {
                    showError("-XincrementalFile requires file argument");
                }
            } else if (arg.equals("-crossrefs")) {
                buildConfig.setGenerateCrossRefsMode(true);
                buildConfig.setGenerateModelMode(true);
            } else if (arg.startsWith("-checkRuntimeVersion:")) {
                String lcArg = arg.toLowerCase();
                if (lcArg.endsWith(":false")) {
                    buildConfig.setCheckRuntimeVersion(false);
                } else if (lcArg.endsWith(":true")) {
                    buildConfig.setCheckRuntimeVersion(true);
                } else {
                    showError("bad value for -checkRuntimeVersion option, must be true or false");
                }
            } else if (arg.equals("-emacssym")) {
                buildConfig.setEmacsSymMode(true);
                buildConfig.setGenerateModelMode(true);
            } else if (arg.equals("-XjavadocsInModel")) {
                buildConfig.setGenerateModelMode(true);
                buildConfig.setGenerateJavadocsInModelMode(true);
            } else if (arg.equals("-Xdev:NoAtAspectJProcessing")) {
                buildConfig.setNoAtAspectJAnnotationProcessing(true);
            } else if (arg.equals("-XaddSerialVersionUID")) {
                buildConfig.setAddSerialVerUID(true);
            } else if (arg.equals("-xmlConfigured")) {
                buildConfig.setXmlConfigured(true);
            } else if (arg.equals("-Xdev:Pinpoint")) {
                buildConfig.setXdevPinpointMode(true);
            } else if (arg.startsWith("-Xjoinpoints:")) {
                buildConfig.setXJoinpoints(arg.substring(13));
            } else if (arg.equals("-noWeave") || arg.equals("-XnoWeave")) {
                showWarning("the noweave option is no longer required and is being ignored");
            } else if (arg.equals("-XterminateAfterCompilation")) {
                buildConfig.setTerminateAfterCompilation(true);
            } else if (arg.equals("-XserializableAspects")) {
                buildConfig.setXserializableAspects(true);
            } else if (arg.equals("-XlazyTjp")) {
                // do nothing as this is now on by default
                showWarning("-XlazyTjp should no longer be used, build tjps lazily is now the default");
            } else if (arg.startsWith("-Xreweavable")) {
                showWarning("-Xreweavable is on by default");
                if (arg.endsWith(":compress")) {
                    showWarning("-Xreweavable:compress is no longer available - reweavable is now default");
                }
            } else if (arg.startsWith("-Xset:")) {
                buildConfig.setXconfigurationInfo(arg.substring(6));
            } else if (arg.startsWith("-aspectj.pushin=")) {
                // a little dirty but this should never be used in the IDE
                try {
                    System.setProperty("aspectj.pushin", arg.substring(16));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (arg.startsWith("-XnotReweavable")) {
                buildConfig.setXnotReweavable(true);
            } else if (arg.equals("-XnoInline")) {
                buildConfig.setXnoInline(true);
            } else if (arg.equals("-XhasMember")) {
                buildConfig.setXHasMemberSupport(true);
            } else if (arg.startsWith("-showWeaveInfo")) {
                buildConfig.setShowWeavingInformation(true);
            } else if (arg.equals("-Xlintfile")) {
                if (args.size() > nextArgIndex) {
                    File lintSpecFile = makeFile(((Arg) args.get(nextArgIndex)).getValue());
                    // XXX relax restriction on props file suffix?
                    if (lintSpecFile.canRead() && lintSpecFile.getName().endsWith(".properties")) {
                        buildConfig.setLintSpecFile(lintSpecFile);
                    } else {
                        showError("bad -Xlintfile file: " + lintSpecFile);
                        buildConfig.setLintSpecFile(null);
                    }
                    args.remove(args.get(nextArgIndex));
                } else {
                    showError("-Xlintfile requires .properties file argument");
                }
            } else if (arg.equals("-Xlint")) {
                // buildConfig.getAjOptions().put(
                // AjCompilerOptions.OPTION_Xlint,
                // CompilerOptions.GENERATE);
                buildConfig.setLintMode(AjBuildConfig.AJLINT_DEFAULT);
            } else if (arg.startsWith("-Xlint:")) {
                if (7 < arg.length()) {
                    buildConfig.setLintMode(arg.substring(7));
                } else {
                    showError("invalid lint option " + arg);
                }
            } else if (arg.equals("-bootclasspath")) {
                if (args.size() > nextArgIndex) {
                    String bcpArg = ((Arg) args.get(nextArgIndex)).getValue();
                    StringBuffer bcp = new StringBuffer();
                    StringTokenizer strTok = new StringTokenizer(bcpArg, File.pathSeparator);
                    while (strTok.hasMoreTokens()) {
                        bcp.append(makeFile(strTok.nextToken()));
                        if (strTok.hasMoreTokens()) {
                            bcp.append(File.pathSeparator);
                        }
                    }
                    bootclasspath = bcp.toString();
                    args.remove(args.get(nextArgIndex));
                } else {
                    showError("-bootclasspath requires classpath entries");
                }
            } else if (arg.equals("-classpath") || arg.equals("-cp")) {
                if (args.size() > nextArgIndex) {
                    String cpArg = ((Arg) args.get(nextArgIndex)).getValue();
                    StringBuffer cp = new StringBuffer();
                    StringTokenizer strTok = new StringTokenizer(cpArg, File.pathSeparator);
                    while (strTok.hasMoreTokens()) {
                        cp.append(makeFile(strTok.nextToken()));
                        if (strTok.hasMoreTokens()) {
                            cp.append(File.pathSeparator);
                        }
                    }
                    classpath = cp.toString();
                    args.remove(args.get(nextArgIndex));
                } else {
                    showError("-classpath requires classpath entries");
                }
            } else if (arg.equals("-extdirs")) {
                if (args.size() > nextArgIndex) {
                    String extdirsArg = ((Arg) args.get(nextArgIndex)).getValue();
                    StringBuffer ed = new StringBuffer();
                    StringTokenizer strTok = new StringTokenizer(extdirsArg, File.pathSeparator);
                    while (strTok.hasMoreTokens()) {
                        ed.append(makeFile(strTok.nextToken()));
                        if (strTok.hasMoreTokens()) {
                            ed.append(File.pathSeparator);
                        }
                    }
                    extdirs = ed.toString();
                    args.remove(args.get(nextArgIndex));
                } else {
                    showError("-extdirs requires list of external directories");
                }
                // error on directory unless -d, -{boot}classpath, or -extdirs
            } else if (arg.equals("-d")) {
                dirLookahead(arg, args, nextArgIndex);
                // } else if (arg.equals("-classpath")) {
                // dirLookahead(arg, args, nextArgIndex);
                // } else if (arg.equals("-bootclasspath")) {
                // dirLookahead(arg, args, nextArgIndex);
                // } else if (arg.equals("-extdirs")) {
                // dirLookahead(arg, args, nextArgIndex);
            } else if (arg.equals("-proceedOnError")) {
                buildConfig.setProceedOnError(true);
            } else if (arg.equals("-processorpath")) { // -processorpath <directories and ZIP archives separated by pathseporator
                addPairToUnparsed(args, arg, nextArgIndex, "-processorpath requires list of external directories or zip archives");
            } else if (arg.equals("-processor")) { // -processor <class1[,class2,...]>
                addPairToUnparsed(args, arg, nextArgIndex, "-processor requires list of processors' classes");
            } else if (arg.equals("-s")) { // -s <dir> destination directory for generated source files
                addPairToUnparsed(args, arg, nextArgIndex, "-s requires directory");
            } else if (arg.equals("-classNames")) { // -classNames <className1[,className2,...]>
                addPairToUnparsed(args, arg, nextArgIndex, "-classNames requires list of classes");
            } else if (new File(arg).isDirectory()) {
                showError("dir arg not permitted: " + arg);
            } else if (arg.startsWith("-Xajruntimetarget")) {
                if (arg.endsWith(":1.2")) {
                    buildConfig.setTargetAspectjRuntimeLevel(Constants.RUNTIME_LEVEL_12);
                } else if (arg.endsWith(":1.5")) {
                    buildConfig.setTargetAspectjRuntimeLevel(Constants.RUNTIME_LEVEL_15);
                } else {
                    showError("-Xajruntimetarget:<level> only supports a target level of 1.2 or 1.5");
                }
            } else if (arg.equals("-timers")) {
                buildConfig.setTiming(true);
                // swallow - it is dealt with in Main.runMain()
            } else if (arg.equals("-1.5")) {
                buildConfig.setBehaveInJava5Way(true);
                unparsedArgs.add("-1.5");
                // this would enable the '-source 1.5' to do the same as '-1.5' but doesnt sound quite right as
                // as an option right now as it doesnt mean we support 1.5 source code - people will get confused...
            } else if (arg.equals("-1.6")) {
                buildConfig.setBehaveInJava5Way(true);
                unparsedArgs.add("-1.6");
            } else if (arg.equals("-1.7")) {
                buildConfig.setBehaveInJava5Way(true);
                unparsedArgs.add("-1.7");
            } else if (arg.equals("-1.8")) {
                buildConfig.setBehaveInJava5Way(true);
                unparsedArgs.add("-1.8");
            } else if (arg.equals("-source")) {
                if (args.size() > nextArgIndex) {
                    String level = ((Arg) args.get(nextArgIndex)).getValue();
                    if (level.equals("1.5") || level.equals("5") || level.equals("1.6") || level.equals("6") || level.equals("1.7")
                            || level.equals("7") || level.equals("8") || level.equals("1.8")) {
                        buildConfig.setBehaveInJava5Way(true);
                    }
                    unparsedArgs.add("-source");
                    unparsedArgs.add(level);
                    args.remove(args.get(nextArgIndex));
                }
            } else {
                // argfile, @file parsed by superclass
                // no eclipse options parsed:
                // -d args, -help (handled),
                // -classpath, -target, -1.3, -1.4, -source [1.3|1.4]
                // -nowarn, -warn:[...], -deprecation, -noImportError,
                // -g:[...], -preserveAllLocals,
                // -referenceInfo, -encoding, -verbose, -log, -time
                // -noExit, -repeat
                // (Actually, -noExit grabbed by Main)
                unparsedArgs.add(arg);
            }
        }

        protected void dirLookahead(String arg, LinkedList argList, int nextArgIndex) {
            unparsedArgs.add(arg);
            Arg next = (Arg) argList.get(nextArgIndex);
            String value = next.getValue();
            if (!LangUtil.isEmpty(value)) {
                if (new File(value).isDirectory()) {
                    unparsedArgs.add(value);
                    argList.remove(next);
                    return;
                }
            }
        }

        public void showError(String message) {
            ISourceLocation location = null;
            if (buildConfig.getConfigFile() != null) {
                location = new SourceLocation(buildConfig.getConfigFile(), 0);
            }
            IMessage errorMessage = new Message(CONFIG_MSG + message, IMessage.ERROR, null, location);
            handler.handleMessage(errorMessage);
            // MessageUtil.error(handler, CONFIG_MSG + message);
        }

        protected void showWarning(String message) {
            ISourceLocation location = null;
            if (buildConfig.getConfigFile() != null) {
                location = new SourceLocation(buildConfig.getConfigFile(), 0);
            }
            IMessage errorMessage = new Message(CONFIG_MSG + message, IMessage.WARNING, null, location);
            handler.handleMessage(errorMessage);
            // MessageUtil.warn(handler, message);
        }

        protected File makeFile(File dir, String name) {
            name = name.replace('/', File.separatorChar);
            File ret = new File(name);
            if (dir == null || ret.isAbsolute()) {
                return ret;
            }
            try {
                dir = dir.getCanonicalFile();
            } catch (IOException ioe) {
            }
            return new File(dir, name);
        }

        private void addPairToUnparsed(LinkedList<Arg> args, String arg, int nextArgIndex, String errorMessage) {
            if (args.size() <= nextArgIndex) {
                showError(errorMessage);
                return;
            }
            final Arg nextArg = args.get(nextArgIndex);
            args.remove(nextArg);
            unparsedArgs.add(arg);
            unparsedArgs.add(nextArg.getValue());
        }

        private int indexOf(LinkedList<Arg> args, String arg) {
            int index = 0;
            for (Arg argument : args) {
                if (arg.equals(argument.getValue())) {
                    return index;
                }
                index++;
            }
            return -1;
        }

    }
}
