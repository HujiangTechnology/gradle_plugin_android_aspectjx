/* *******************************************************************
 * Copyright (c) 2005-2012 Contributors.
 * All rights reserved.
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution and is available at
 * http://eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Adrian Colyer			Initial implementation
 *   Andy Clement			various fixes
 *   Trask Stanalker		#373195
 * ******************************************************************/
package org.aspectj.bridge.context;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * This class is responsible for tracking progress through the various phases of compilation and weaving.
 * When an exception occurs (or a message is issued, if desired), you can ask this class for a
 * "stack trace" that gives information about what the compiler was doing at the time.
 * The trace will say something like:
 * "when matching pointcut xyz when matching shadow sss when weaving type ABC when weaving shadow mungers"
 */
public class CompilationAndWeavingContext {

    private static int nextTokenId = 1;

    // unique constants for the different phases that can be registered

    // "FRONT END"
    public static final int BATCH_BUILD = 0;
    public static final int INCREMENTAL_BUILD = 1;
    public static final int PROCESSING_COMPILATION_UNIT = 2;
    public static final int RESOLVING_COMPILATION_UNIT = 3;
    public static final int ANALYSING_COMPILATION_UNIT = 4;
    public static final int GENERATING_UNWOVEN_CODE_FOR_COMPILATION_UNIT = 5;
    public static final int COMPLETING_TYPE_BINDINGS = 6;
    public static final int PROCESSING_DECLARE_PARENTS = 7;
    public static final int CHECK_AND_SET_IMPORTS = 8;
    public static final int CONNECTING_TYPE_HIERARCHY = 9;
    public static final int BUILDING_FIELDS_AND_METHODS = 10;
    public static final int COLLECTING_ITDS_AND_DECLARES = 11;
    public static final int PROCESSING_DECLARE_ANNOTATIONS = 12;
    public static final int WEAVING_INTERTYPE_DECLARATIONS = 13;
    public static final int RESOLVING_POINTCUT_DECLARATIONS = 14;
    public static final int ADDING_DECLARE_WARNINGS_AND_ERRORS = 15;
    public static final int VALIDATING_AT_ASPECTJ_ANNOTATIONS = 16;
    public static final int ACCESS_FOR_INLINE = 17;
    public static final int ADDING_AT_ASPECTJ_ANNOTATIONS = 18;
    public static final int FIXING_SUPER_CALLS_IN_ITDS = 19;
    public static final int FIXING_SUPER_CALLS = 20;
    public static final int OPTIMIZING_THIS_JOIN_POINT_CALLS = 21;

    // "BACK END"

    public static final int WEAVING = 22;
    public static final int PROCESSING_REWEAVABLE_STATE = 23;
    public static final int PROCESSING_TYPE_MUNGERS = 24;
    public static final int WEAVING_ASPECTS = 25;
    public static final int WEAVING_CLASSES = 26;
    public static final int WEAVING_TYPE = 27;
    public static final int MATCHING_SHADOW = 28;
    public static final int IMPLEMENTING_ON_SHADOW = 29;
    public static final int MATCHING_POINTCUT = 30;
    public static final int MUNGING_WITH = 31;
    public static final int PROCESSING_ATASPECTJTYPE_MUNGERS_ONLY = 32;

    // phase names
    public static final String[] PHASE_NAMES = new String[] { "batch building", "incrementally building",
            "processing compilation unit", "resolving types defined in compilation unit",
            "analysing types defined in compilation unit", "generating unwoven code for type defined in compilation unit",
            "completing type bindings", "processing declare parents", "checking and setting imports", "connecting type hierarchy",
            "building fields and methods", "collecting itds and declares", "processing declare annotations",
            "weaving intertype declarations", "resolving pointcut declarations", "adding declare warning and errors",
            "validating @AspectJPlugin annotations", "creating accessors for inlining", "adding @AspectJPlugin annotations",
            "fixing super calls in ITDs in interface context", "fixing super calls in ITDs",
            "optimizing thisJoinPoint calls",

            // BACK END

            "weaving", "processing reweavable state", "processing type mungers", "weaving aspects", "weaving classes",
            "weaving type", "matching shadow", "implementing on shadow", "matching pointcut", "type munging with",
            "type munging for @AspectJPlugin aspectOf" };

    // context stacks, one per thread
    private static ThreadLocal<Stack<ContextStackEntry>> contextMap = new ThreadLocal<Stack<ContextStackEntry>>();

    // single thread mode stack
    private static Stack<ContextStackEntry> contextStack = new Stack<ContextStackEntry>();

    // formatters, by phase id
    private static Map<Integer, ContextFormatter> formatterMap = new HashMap<Integer, ContextFormatter>();

    private static ContextFormatter defaultFormatter = new DefaultFormatter();

    private static boolean multiThreaded = true;

    /**
     * this is a static service
     */
    private CompilationAndWeavingContext() {
    }

    public static void reset() {
//        if (!multiThreaded) {
//            contextMap.remove();
//            contextStack.clear();
//            formatterMap.clear();
//            nextTokenId = 1;
//        } else {
//            contextMap.remove();
//            // TODO what about formatterMap?
//            // TODO what about nextTokenId?
//        }
    }

    public static void setMultiThreaded(boolean mt) {
        multiThreaded = true;
    }

    public static void registerFormatter(int phaseId, ContextFormatter aFormatter) {
        formatterMap.put(new Integer(phaseId), aFormatter);
    }

    /**
     * Returns a string description of what the compiler/weaver is currently doing
     */
    public static String getCurrentContext() {
        Stack<ContextStackEntry> contextStack = getContextStack();
        Stack<String> explanationStack = new Stack<String>();
        for (ContextStackEntry entry : contextStack) {
            Object data = entry.getData();
            if (data != null) {
                explanationStack.push(getFormatter(entry).formatEntry(entry.phaseId, data));
            }
        }
        StringBuffer sb = new StringBuffer();
        while (!explanationStack.isEmpty()) {
            sb.append("when ");
            sb.append(explanationStack.pop().toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    public static ContextToken enteringPhase(int phaseId, Object data) {
        Stack<ContextStackEntry> contextStack = getContextStack();
        ContextTokenImpl nextToken = nextToken();
        contextStack.push(new ContextStackEntry(nextToken, phaseId, new WeakReference<Object>(data)));
        return nextToken;
    }

    /**
     * Exit a phase, all stack entries from the one with the given token down will be removed.
     */
    public static void leavingPhase(ContextToken aToken) {
//        System.out.println(" 自定义的  leavingPhase");
        Stack<ContextStackEntry> contextStack = getContextStack();
        while (!contextStack.isEmpty()) {
            ContextStackEntry entry = contextStack.pop();
            if (entry.contextToken == aToken) {
                break;
            }
        }
    }

    /**
     * Forget about the context for the current thread
     */
    public static void resetForThread() {
        if (!multiThreaded) {
            return;
        }
        contextMap.remove();
    }

    private static Stack<ContextStackEntry> getContextStack() {
        if (!multiThreaded) {
            return contextStack;
        } else {
            Stack<ContextStackEntry> contextStack = contextMap.get();
            if (contextStack == null) {
                contextStack = new Stack<ContextStackEntry>();
                contextMap.set(contextStack);
            }
            return contextStack;
        }
    }

    private static ContextTokenImpl nextToken() {
        return new ContextTokenImpl(nextTokenId++);
    }

    private static ContextFormatter getFormatter(ContextStackEntry entry) {
        Integer key = new Integer(entry.phaseId);
        if (formatterMap.containsKey(key)) {
            return formatterMap.get(key);
        } else {
            return defaultFormatter;
        }
    }

    private static class ContextTokenImpl implements ContextToken {
        public int tokenId;

        public ContextTokenImpl(int id) {
            this.tokenId = id;
        }
    }

    // dumb data structure
    private static class ContextStackEntry {
        public ContextTokenImpl contextToken;
        public int phaseId;
        private WeakReference<Object> dataRef;

        public ContextStackEntry(ContextTokenImpl ct, int phase, WeakReference<Object> data) {
            this.contextToken = ct;
            this.phaseId = phase;
            this.dataRef = data;
        }

        public Object getData() {
            return dataRef.get();
        }

        public String toString() {
            Object data = getData();
            if (data == null) {
                return "referenced context entry has gone out of scope";
            } else {
                return CompilationAndWeavingContext.getFormatter(this).formatEntry(phaseId, data);
            }
        }
    }

    private static class DefaultFormatter implements ContextFormatter {

        public String formatEntry(int phaseId, Object data) {
            StringBuffer sb = new StringBuffer();
            sb.append(PHASE_NAMES[phaseId]);
            sb.append(" ");
            if (data instanceof char[]) {
                sb.append(new String((char[]) data));
            } else {
                try {
                    sb.append(data.toString());
                } catch (RuntimeException ex) {
                    // don't lose vital info because of bad toString
                    sb.append("** broken toString in data object **");
                }
            }
            return sb.toString();
        }

    }
}
