/* *******************************************************************
 * Copyright (c) 2002 Palo Alto Research Center, Incorporated (PARC).
 *               2005 Contributors
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     PARC     initial implementation
 *     Adrian Colyer, Andy Clement, overhaul for generics, Abraham Nevado 
 * ******************************************************************/

package org.aspectj.weaver;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;

import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.IMessage.Kind;
import org.aspectj.bridge.IMessageHandler;
import org.aspectj.bridge.ISourceLocation;
import org.aspectj.bridge.Message;
import org.aspectj.bridge.MessageUtil;
import org.aspectj.bridge.context.PinpointingMessageHandler;
import org.aspectj.util.IStructureModel;
import org.aspectj.weaver.ResolvedType.Primitive;
import org.aspectj.weaver.UnresolvedType.TypeKind;
import org.aspectj.weaver.patterns.Declare;
import org.aspectj.weaver.patterns.DeclareAnnotation;
import org.aspectj.weaver.patterns.DeclareParents;
import org.aspectj.weaver.patterns.DeclarePrecedence;
import org.aspectj.weaver.patterns.DeclareSoft;
import org.aspectj.weaver.patterns.DeclareTypeErrorOrWarning;
import org.aspectj.weaver.patterns.Pointcut;
import org.aspectj.weaver.patterns.TypePattern;
import org.aspectj.weaver.tools.PointcutDesignatorHandler;
import org.aspectj.weaver.tools.Trace;
import org.aspectj.weaver.tools.TraceFactory;

/**
 * A World is a collection of known types and crosscutting members.
 */
public abstract class World implements Dump.INode {

    /**
     * handler for any messages produced during resolution etc.
     */
    private IMessageHandler messageHandler = IMessageHandler.SYSTEM_ERR;

    /**
     * handler for cross-reference information produced during the weaving process
     */
    private ICrossReferenceHandler xrefHandler = null;

    /**
     * Currently 'active' scope in which to lookup (resolve) typevariable references
     */
    private TypeVariableDeclaringElement typeVariableLookupScope;

    /**
     * The heart of the world, a map from type signatures to resolved types
     */
    protected TypeMap typeMap = new TypeMap();
    protected static TypeMap sTypeMap = new TypeMap();

    /**
     * New pointcut designators this world supports
     */
    private Set<PointcutDesignatorHandler> pointcutDesignators;

    // see pr145963
    /**
     * Should we create the hierarchy for binary classes and aspects
     */
    public static boolean createInjarHierarchy = true;

    /**
     * Calculator for working out aspect precedence
     */
    private final AspectPrecedenceCalculator precedenceCalculator;

    /**
     * All of the type and shadow mungers known to us
     */
    private final CrosscuttingMembersSet crosscuttingMembersSet = new CrosscuttingMembersSet(this);

    /**
     * The structure model for the compilation
     */
    private IStructureModel model = null;

    /**
     * for processing Xlint messages
     */
    private Lint lint = new Lint(this);

    /**
     * XnoInline option setting passed down to weaver
     */
    private boolean XnoInline;

    /**
     * XlazyTjp option setting passed down to weaver
     */
    private boolean XlazyTjp;

    /**
     * XhasMember option setting passed down to weaver
     */
    private boolean XhasMember = false;

    /**
     * Xpinpoint controls whether we put out developer info showing the source of messages
     */
    private boolean Xpinpoint = false;

    /**
     * When behaving in a Java 5 way autoboxing is considered
     */
    private boolean behaveInJava5Way = false;

    /**
     * Should timing information be reported (as info messages)?
     */
    private boolean timing = false;
    private boolean timingPeriodically = true;

    /**
     * Determines if this world could be used for multiple compiles
     */
    private boolean incrementalCompileCouldFollow = false;

    /**
     * The level of the aspectjrt.jar the code we generate needs to run on
     */
    private String targetAspectjRuntimeLevel = Constants.RUNTIME_LEVEL_DEFAULT;

    /**
     * Flags for the new joinpoints that are 'optional': -Xjoinpoints:arrayconstruction -Xjoinpoints:synchronization
     */
    private boolean optionalJoinpoint_ArrayConstruction = false;
    private boolean optionalJoinpoint_Synchronization = false;

    private boolean addSerialVerUID = false;

    private Properties extraConfiguration = null;
    private boolean checkedAdvancedConfiguration = false;
    private boolean synchronizationPointcutsInUse = false;
    // Xset'table options
    private boolean runMinimalMemory = false;
    private boolean transientTjpFields = false;
    private boolean runMinimalMemorySet = false;
    private boolean shouldPipelineCompilation = true;
    private boolean shouldGenerateStackMaps = false;
    protected boolean bcelRepositoryCaching = xsetBCEL_REPOSITORY_CACHING_DEFAULT.equalsIgnoreCase("true");
    private boolean fastMethodPacking = false;
    private int itdVersion = 2; // defaults to 2nd generation itds

    // Minimal Model controls whether model entities that are not involved in relationships are deleted post-build
    private boolean minimalModel = true;
    private boolean useFinal = true;
    private boolean targettingRuntime1_6_10 = false;

    private boolean completeBinaryTypes = false;
    private boolean overWeaving = false;
    private static boolean systemPropertyOverWeaving = false;
    public boolean forDEBUG_structuralChangesCode = false;
    public boolean forDEBUG_bridgingCode = false;
    public boolean optimizedMatching = true;
    public boolean generateNewLvts = true;
    protected long timersPerJoinpoint = 25000;
    protected long timersPerType = 250;

    public int infoMessagesEnabled = 0; // 0=uninitialized, 1=no, 2=yes

    private static Trace trace = TraceFactory.getTraceFactory().getTrace(World.class);

    private boolean errorThreshold;
    private boolean warningThreshold;

    /**
     * A list of RuntimeExceptions containing full stack information for every type we couldn't find.
     */
    private List<RuntimeException> dumpState_cantFindTypeExceptions = null;
    public static final Primitive BYTE = new Primitive("B", 1, 0);
    public static final Primitive CHAR = new Primitive("C", 1, 1);
    public static final Primitive DOUBLE = new Primitive("D", 2, 2);
    public static final Primitive FLOAT = new Primitive("F", 1, 3);
    public static final Primitive INT = new Primitive("I", 1, 4);
    public static final Primitive LONG = new Primitive("J", 2, 5);
    public static final Primitive SHORT = new Primitive("S", 1, 6);
    public static final Primitive BOOLEAN = new Primitive("Z", 1, 7);
    public static final Primitive VOID = new Primitive("V", 0, 8);

    static {
        sTypeMap.put("B", BYTE);
        sTypeMap.put("S", SHORT);
        sTypeMap.put("I", INT);
        sTypeMap.put("J", LONG);
        sTypeMap.put("F", FLOAT);
        sTypeMap.put("D", DOUBLE);
        sTypeMap.put("C", CHAR);
        sTypeMap.put("Z", BOOLEAN);
        sTypeMap.put("V", VOID);
        try {
            String value = System.getProperty("aspectj.overweaving", "false");
            if (value.equalsIgnoreCase("true")) {
                System.out.println("ASPECTJ: aspectj.overweaving=true: overweaving switched ON");
                systemPropertyOverWeaving = true;
            }
        } catch (Throwable t) {
            System.err.println("ASPECTJ: Unable to read system properties");
            t.printStackTrace();
        }
    }


    /**
     * Insert the primitives
     */
    protected World() {
        super();
        // Dump.registerNode(this.getClass(), this);

        typeMap = sTypeMap;
        precedenceCalculator = new AspectPrecedenceCalculator(this);
    }

    /**
     * Dump processing when a fatal error occurs
     */
    public void accept(Dump.IVisitor visitor) {
        // visitor.visitObject("Extra configuration:");
        // visitor.visitList(extraConfiguration.);
        visitor.visitObject("Shadow mungers:");
        visitor.visitList(crosscuttingMembersSet.getShadowMungers());
        visitor.visitObject("Type mungers:");
        visitor.visitList(crosscuttingMembersSet.getTypeMungers());
        visitor.visitObject("Late Type mungers:");
        visitor.visitList(crosscuttingMembersSet.getLateTypeMungers());
        if (dumpState_cantFindTypeExceptions != null) {
            visitor.visitObject("Cant find type problems:");
            visitor.visitList(dumpState_cantFindTypeExceptions);
            dumpState_cantFindTypeExceptions = null;
        }
    }

    // ==========================================================================
    // T Y P E R E S O L U T I O N
    // ==========================================================================

    /**
     * Resolve a type that we require to be present in the world
     */
    public ResolvedType resolve(UnresolvedType ty) {
        return resolve(ty, false);
    }

    /**
     * Attempt to resolve a type - the source location gives you some context in which resolution is taking place. In the case of an
     * error where we can't find the type - we can then at least report why (source location) we were trying to resolve it.
     */
    public ResolvedType resolve(UnresolvedType ty, ISourceLocation isl) {
        ResolvedType ret = resolve(ty, true);
        if (ResolvedType.isMissing(ty)) {
            // IMessage msg = null;
            getLint().cantFindType.signal(WeaverMessages.format(WeaverMessages.CANT_FIND_TYPE, ty.getName()), isl);
            // if (isl!=null) {
            // msg = MessageUtil.error(WeaverMessages.format(WeaverMessages.
            // CANT_FIND_TYPE,ty.getName()),isl);
            // } else {
            // msg = MessageUtil.error(WeaverMessages.format(WeaverMessages.
            // CANT_FIND_TYPE,ty.getName()));
            // }
            // messageHandler.handleMessage(msg);
        }
        return ret;
    }

    /**
     * Convenience method for resolving an array of unresolved types in one hit. Useful for e.g. resolving type parameters in
     * signatures.
     */
    public ResolvedType[] resolve(UnresolvedType[] types) {
        if (types == null) {
            return ResolvedType.NONE;
        }

        ResolvedType[] ret = new ResolvedType[types.length];
        for (int i = 0; i < types.length; i++) {
            ret[i] = resolve(types[i]);
        }
        return ret;
    }

    /**
     * Resolve a type. This the hub of type resolution. The resolved type is added to the type map by signature.
     */
    public ResolvedType resolve(UnresolvedType ty, boolean allowMissing) {

        // special resolution processing for already resolved types.
        if (ty instanceof ResolvedType) {
            ResolvedType rty = (ResolvedType) ty;
            rty = resolve(rty);
            // A TypeVariableReferenceType may look like it is resolved (it extends ResolvedType) but the internal
            // type variable may not yet have been resolved
            if (!rty.isTypeVariableReference() || ((TypeVariableReferenceType) rty).isTypeVariableResolved()) {
                return rty;
            }
        }

        // dispatch back to the type variable reference to resolve its
        // constituent parts don't do this for other unresolved types otherwise
        // you'll end up in a
        // loop
        if (ty.isTypeVariableReference()) {
            return ty.resolve(this);
        }

        // if we've already got a resolved type for the signature, just return
        // it
        // after updating the world
        String signature = ty.getSignature();
        ResolvedType ret = typeMap.get(signature);
        if (ret != null) {
            ret.world = this; // Set the world for the RTX
            return ret;
        } else if (signature.equals("?") || signature.equals("*")) {
            // might be a problem here, not sure '?' should make it to here as a
            // signature, the
            // proper signature for wildcard '?' is '*'
            // fault in generic wildcard, can't be done earlier because of init
            // issues
            // TODO ought to be shared single instance representing this
            ResolvedType something = getWildcard();
            typeMap.put("?", something);
            return something;
        }

        // no existing resolved type, create one
        synchronized (buildingTypeLock) {
            if (ty.isArray()) {
                ResolvedType componentType = resolve(ty.getComponentType(), allowMissing);
                ret = new ArrayReferenceType(signature, "[" + componentType.getErasureSignature(), this, componentType);
            } else {
                ret = resolveToReferenceType(ty, allowMissing);
                if (!allowMissing && ret.isMissing()) {
                    ret = handleRequiredMissingTypeDuringResolution(ty);
                }
                if (completeBinaryTypes) {
                    completeBinaryType(ret);
                }
            }
        }

        // Pulling in the type may have already put the right entry in the map
        ResolvedType result = typeMap.get(signature);
        if (result == null && !ret.isMissing()) {
            ret = ensureRawTypeIfNecessary(ret);
            typeMap.put(signature, ret);
            return ret;
        }
        if (result == null) {
            return ret;
        } else {
            return result;
        }
    }

    private Object buildingTypeLock = new Object();

    // Only need one representation of '?' in a world - can be shared
    private BoundedReferenceType wildcard;

    private BoundedReferenceType getWildcard() {
        if (wildcard == null) {
            wildcard = new BoundedReferenceType(this);
        }
        return wildcard;
    }

    /**
     * Called when a type is resolved - enables its type hierarchy to be finished off before we proceed
     */
    protected void completeBinaryType(ResolvedType ret) {
    }

    /**
     * Return true if the classloader relating to this world is definetly the one that will define the specified class. Return false
     * otherwise or we don't know for certain.
     */
    public boolean isLocallyDefined(String classname) {
        return false;
    }

    /**
     * We tried to resolve a type and couldn't find it...
     */
    private ResolvedType handleRequiredMissingTypeDuringResolution(UnresolvedType ty) {
        // defer the message until someone asks a question of the type that we
        // can't answer
        // just from the signature.
        // MessageUtil.error(messageHandler,
        // WeaverMessages.format(WeaverMessages.CANT_FIND_TYPE,ty.getName()));
        if (dumpState_cantFindTypeExceptions == null) {
            dumpState_cantFindTypeExceptions = new ArrayList<RuntimeException>();
        }
        if (dumpState_cantFindTypeExceptions.size() < 100) { // limit growth
            dumpState_cantFindTypeExceptions.add(new RuntimeException("Can't find type " + ty.getName()));
        }
        return new MissingResolvedTypeWithKnownSignature(ty.getSignature(), this);
    }

    /**
     * Some TypeFactory operations create resolved types directly, but these won't be in the typeMap - this resolution process puts
     * them there. Resolved types are also told their world which is needed for the special autoboxing resolved types.
     */
    public ResolvedType resolve(ResolvedType ty) {
        if (ty.isTypeVariableReference()) {
            return ty; // until type variables have proper sigs...
        }
        ResolvedType resolved = typeMap.get(ty.getSignature());
        if (resolved == null) {
            resolved = ensureRawTypeIfNecessary(ty);
            typeMap.put(ty.getSignature(), resolved);
            resolved = ty;
        }
        resolved.world = this;
        return resolved;
    }

    /**
     * When the world is operating in 1.5 mode, the TypeMap should only contain RAW types and never directly generic types. The RAW
     * type will contain a reference to the generic type.
     *
     * @param type a possibly generic type for which the raw needs creating as it is not currently in the world
     * @return a type suitable for putting into the world
     */
    private ResolvedType ensureRawTypeIfNecessary(ResolvedType type) {
        if (!isInJava5Mode() || type.isRawType()) {
            return type;
        }
        // Key requirement here is if it is generic, create a RAW entry to be put in the map that points to it
        if (type instanceof ReferenceType && ((ReferenceType) type).getDelegate() != null && type.isGenericType()) {
            ReferenceType rawType = new ReferenceType(type.getSignature(), this);
            rawType.typeKind = TypeKind.RAW;
            ReferenceTypeDelegate delegate = ((ReferenceType) type).getDelegate();
            rawType.setDelegate(delegate);
            rawType.setGenericType((ReferenceType) type);
            return rawType;
        }
        // probably parameterized...
        return type;
    }

    /**
     * Convenience method for finding a type by name and resolving it in one step.
     */
    public ResolvedType resolve(String name) {
        // trace.enter("resolve", this, new Object[] {name});
        ResolvedType ret = resolve(UnresolvedType.forName(name));
        // trace.exit("resolve", ret);
        return ret;
    }

    public ReferenceType resolveToReferenceType(String name) {
        return (ReferenceType) resolve(name);
    }

    public ResolvedType resolve(String name, boolean allowMissing) {
        return resolve(UnresolvedType.forName(name), allowMissing);
    }

    /**
     * Resolve to a ReferenceType - simple, raw, parameterized, or generic. Raw, parameterized, and generic versions of a type share
     * a delegate.
     */
    private final ResolvedType resolveToReferenceType(UnresolvedType ty, boolean allowMissing) {
        if (ty.isParameterizedType()) {
            // ======= parameterized types ================
            ResolvedType rt = resolveGenericTypeFor(ty, allowMissing);
            if (rt.isMissing()) {
                return rt;
            }
            ReferenceType genericType = (ReferenceType) rt;
            ReferenceType parameterizedType = TypeFactory.createParameterizedType(genericType, ty.typeParameters, this);
            return parameterizedType;

        } else if (ty.isGenericType()) {
            // ======= generic types ======================
            ResolvedType rt = resolveGenericTypeFor(ty, false);
            ReferenceType genericType = (ReferenceType) rt;
            return genericType;

        } else if (ty.isGenericWildcard()) {
            // ======= generic wildcard types =============
            return resolveGenericWildcardFor((WildcardedUnresolvedType) ty);
        } else {
            // ======= simple and raw types ===============
            String erasedSignature = ty.getErasureSignature();
            ReferenceType simpleOrRawType = new ReferenceType(erasedSignature, this);
            if (ty.needsModifiableDelegate()) {
                simpleOrRawType.setNeedsModifiableDelegate(true);
            }
            ReferenceTypeDelegate delegate = resolveDelegate(simpleOrRawType);

            if (delegate == null) {
                return new MissingResolvedTypeWithKnownSignature(ty.getSignature(), erasedSignature, this);
            }

            if (delegate.isGeneric() && behaveInJava5Way) {
                // ======== raw type ===========
                simpleOrRawType.typeKind = TypeKind.RAW;
                if (simpleOrRawType.hasNewInterfaces()) { // debug 375777
                    throw new IllegalStateException(
                            "Simple type promoted forced to raw, but it had new interfaces/superclass.  Type is "
                                    + simpleOrRawType.getName());
                }
                ReferenceType genericType = makeGenericTypeFrom(delegate, simpleOrRawType);
                simpleOrRawType.setDelegate(delegate);
                genericType.setDelegate(delegate);
                simpleOrRawType.setGenericType(genericType);
                return simpleOrRawType;
            } else {
                // ======== simple type =========
                simpleOrRawType.setDelegate(delegate);
                return simpleOrRawType;
            }
        }
    }

    /**
     * Attempt to resolve a type that should be a generic type.
     */
    public ResolvedType resolveGenericTypeFor(UnresolvedType anUnresolvedType, boolean allowMissing) {
        // Look up the raw type by signature
        String rawSignature = anUnresolvedType.getRawType().getSignature();
        ResolvedType rawType = typeMap.get(rawSignature);
        if (rawType == null) {
            rawType = resolve(UnresolvedType.forSignature(rawSignature), allowMissing);
            typeMap.put(rawSignature, rawType);
        }
        if (rawType.isMissing()) {
            return rawType;
        }

        // Does the raw type know its generic form? (It will if we created the
        // raw type from a source type, it won't if its been created just
        // through
        // being referenced, e.g. java.util.List
        ResolvedType genericType = rawType.getGenericType();

        // There is a special case to consider here (testGenericsBang_pr95993
        // highlights it)
        // You may have an unresolvedType for a parameterized type but it
        // is backed by a simple type rather than a generic type. This occurs
        // for
        // inner types of generic types that inherit their enclosing types
        // type variables.
        if (rawType.isSimpleType() && (anUnresolvedType.typeParameters == null || anUnresolvedType.typeParameters.length == 0)) {
            rawType.world = this;
            return rawType;
        }

        if (genericType != null) {
            genericType.world = this;
            return genericType;
        } else {
            // Fault in the generic that underpins the raw type ;)
            ReferenceTypeDelegate delegate = resolveDelegate((ReferenceType) rawType);
            ReferenceType genericRefType = makeGenericTypeFrom(delegate, ((ReferenceType) rawType));
            ((ReferenceType) rawType).setGenericType(genericRefType);
            genericRefType.setDelegate(delegate);
            ((ReferenceType) rawType).setDelegate(delegate);
            return genericRefType;
        }
    }

    private ReferenceType makeGenericTypeFrom(ReferenceTypeDelegate delegate, ReferenceType rawType) {
        String genericSig = delegate.getDeclaredGenericSignature();
        if (genericSig != null) {
            return new ReferenceType(UnresolvedType.forGenericTypeSignature(rawType.getSignature(),
                    delegate.getDeclaredGenericSignature()), this);
        } else {
            return new ReferenceType(UnresolvedType.forGenericTypeVariables(rawType.getSignature(), delegate.getTypeVariables()),
                    this);
        }
    }

    /**
     * Go from an unresolved generic wildcard (represented by UnresolvedType) to a resolved version (BoundedReferenceType).
     */
    private ReferenceType resolveGenericWildcardFor(WildcardedUnresolvedType aType) {
        BoundedReferenceType ret = null;
        // FIXME asc doesnt take account of additional interface bounds (e.g. ? super R & Serializable - can you do that?)
        if (aType.isExtends()) {
            ResolvedType resolvedUpperBound = resolve(aType.getUpperBound());
            if (resolvedUpperBound.isMissing()) {
                return getWildcard();
            }
            ret = new BoundedReferenceType((ReferenceType) resolvedUpperBound, true, this);
        } else if (aType.isSuper()) {
            ResolvedType resolvedLowerBound = resolve(aType.getLowerBound());
            if (resolvedLowerBound.isMissing()) {
                return getWildcard();
            }
            ret = new BoundedReferenceType((ReferenceType) resolvedLowerBound, false, this);
        } else {
            // must be ? on its own!
            ret = getWildcard();
        }
        return ret;
    }

    /**
     * Find the ReferenceTypeDelegate behind this reference type so that it can fulfill its contract.
     */
    protected abstract ReferenceTypeDelegate resolveDelegate(ReferenceType ty);

    /**
     * Special resolution for "core" types like OBJECT. These are resolved just like any other type, but if they are not found it is
     * more serious and we issue an error message immediately.
     */
    // OPTIMIZE streamline path for core types? They are just simple types,
    // could look straight in the typemap?
    public ResolvedType getCoreType(UnresolvedType tx) {
        ResolvedType coreTy = resolve(tx, true);
        if (coreTy.isMissing()) {
            MessageUtil.error(messageHandler, WeaverMessages.format(WeaverMessages.CANT_FIND_CORE_TYPE, tx.getName()));
        }
        return coreTy;
    }

    /**
     * Lookup a type by signature, if not found then build one and put it in the map.
     */
    public ReferenceType lookupOrCreateName(UnresolvedType ty) {
        String signature = ty.getSignature();
        ReferenceType ret = lookupBySignature(signature);
        if (ret == null) {
            ret = ReferenceType.fromTypeX(ty, this);
            typeMap.put(signature, ret);
        }
        return ret;
    }

    /**
     * Lookup a reference type in the world by its signature. Returns null if not found.
     */
    public ReferenceType lookupBySignature(String signature) {
        return (ReferenceType) typeMap.get(signature);
    }

    // ==========================================================================
    // ===
    // T Y P E R E S O L U T I O N -- E N D
    // ==========================================================================
    // ===

    /**
     * Member resolution is achieved by resolving the declaring type and then looking up the member in the resolved declaring type.
     */
    public ResolvedMember resolve(Member member) {
        ResolvedType declaring = member.getDeclaringType().resolve(this);
        if (declaring.isRawType()) {
            declaring = declaring.getGenericType();
        }
        ResolvedMember ret;
        if (member.getKind() == Member.FIELD) {
            ret = declaring.lookupField(member);
        } else {
            ret = declaring.lookupMethod(member);
        }

        if (ret != null) {
            return ret;
        }

        return declaring.lookupSyntheticMember(member);
    }

    private boolean allLintIgnored = false;

    public void setAllLintIgnored() {
        allLintIgnored = true;
    }

    public boolean areAllLintIgnored() {
        return allLintIgnored;
    }

    public abstract IWeavingSupport getWeavingSupport();

    /**
     * Create an advice shadow munger from the given advice attribute
     */
    // public abstract Advice createAdviceMunger(AjAttribute.AdviceAttribute
    // attribute, Pointcut pointcut, Member signature);

    /**
     * Create an advice shadow munger for the given advice kind
     */
    public final Advice createAdviceMunger(AdviceKind kind, Pointcut p, Member signature, int extraParameterFlags,
                                           IHasSourceLocation loc, ResolvedType declaringAspect) {
        AjAttribute.AdviceAttribute attribute = new AjAttribute.AdviceAttribute(kind, p, extraParameterFlags, loc.getStart(),
                loc.getEnd(), loc.getSourceContext());
        return getWeavingSupport().createAdviceMunger(attribute, p, signature, declaringAspect);
    }

    /**
     * Same signature as org.aspectj.util.PartialOrder.PartialComparable.compareTo
     */
    public int compareByPrecedence(ResolvedType aspect1, ResolvedType aspect2) {
        return precedenceCalculator.compareByPrecedence(aspect1, aspect2);
    }

    public Integer getPrecedenceIfAny(ResolvedType aspect1, ResolvedType aspect2) {
        return precedenceCalculator.getPrecedenceIfAny(aspect1, aspect2);
    }

    /**
     * compares by precedence with the additional rule that a super-aspect is sorted before its sub-aspects
     */
    public int compareByPrecedenceAndHierarchy(ResolvedType aspect1, ResolvedType aspect2) {
        return precedenceCalculator.compareByPrecedenceAndHierarchy(aspect1, aspect2);
    }

    // simple property getter and setters
    // ===========================================================

    /**
     * Nobody should hold onto a copy of this message handler, or setMessageHandler won't work right.
     */
    public IMessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(IMessageHandler messageHandler) {
        if (this.isInPinpointMode()) {
            this.messageHandler = new PinpointingMessageHandler(messageHandler);
        } else {
            this.messageHandler = messageHandler;
        }
    }

    /**
     * convenenience method for creating and issuing messages via the message handler - if you supply two locations you will get two
     * messages.
     */
    public void showMessage(Kind kind, String message, ISourceLocation loc1, ISourceLocation loc2) {
        if (loc1 != null) {
            messageHandler.handleMessage(new Message(message, kind, null, loc1));
            if (loc2 != null) {
                messageHandler.handleMessage(new Message(message, kind, null, loc2));
            }
        } else {
            messageHandler.handleMessage(new Message(message, kind, null, loc2));
        }
    }

    public void setCrossReferenceHandler(ICrossReferenceHandler xrefHandler) {
        this.xrefHandler = xrefHandler;
    }

    /**
     * Get the cross-reference handler for the world, may be null.
     */
    public ICrossReferenceHandler getCrossReferenceHandler() {
        return xrefHandler;
    }

    public void setTypeVariableLookupScope(TypeVariableDeclaringElement scope) {
        typeVariableLookupScope = scope;
    }

    public TypeVariableDeclaringElement getTypeVariableLookupScope() {
        return typeVariableLookupScope;
    }

    public List<DeclareParents> getDeclareParents() {
        return crosscuttingMembersSet.getDeclareParents();
    }

    public List<DeclareAnnotation> getDeclareAnnotationOnTypes() {
        return crosscuttingMembersSet.getDeclareAnnotationOnTypes();
    }

    public List<DeclareAnnotation> getDeclareAnnotationOnFields() {
        return crosscuttingMembersSet.getDeclareAnnotationOnFields();
    }

    public List<DeclareAnnotation> getDeclareAnnotationOnMethods() {
        return crosscuttingMembersSet.getDeclareAnnotationOnMethods();
    }

    public List<DeclareTypeErrorOrWarning> getDeclareTypeEows() {
        return crosscuttingMembersSet.getDeclareTypeEows();
    }

    public List<DeclareSoft> getDeclareSoft() {
        return crosscuttingMembersSet.getDeclareSofts();
    }

    public CrosscuttingMembersSet getCrosscuttingMembersSet() {
        return crosscuttingMembersSet;
    }

    public IStructureModel getModel() {
        return model;
    }

    public void setModel(IStructureModel model) {
        this.model = model;
    }

    public Lint getLint() {
        return lint;
    }

    public void setLint(Lint lint) {
        this.lint = lint;
    }

    public boolean isXnoInline() {
        return XnoInline;
    }

    public void setXnoInline(boolean xnoInline) {
        XnoInline = xnoInline;
    }

    public boolean isXlazyTjp() {
        return XlazyTjp;
    }

    public void setXlazyTjp(boolean b) {
        XlazyTjp = b;
    }

    public boolean isHasMemberSupportEnabled() {
        return XhasMember;
    }

    public void setXHasMemberSupportEnabled(boolean b) {
        XhasMember = b;
    }

    public boolean isInPinpointMode() {
        return Xpinpoint;
    }

    public void setPinpointMode(boolean b) {
        Xpinpoint = b;
    }

    public boolean useFinal() {
        return useFinal;
    }

    public boolean isMinimalModel() {
        ensureAdvancedConfigurationProcessed();
        return minimalModel;
    }

    public boolean isTargettingRuntime1_6_10() {
        ensureAdvancedConfigurationProcessed();
        return targettingRuntime1_6_10;
    }

    public void setBehaveInJava5Way(boolean b) {
        behaveInJava5Way = b;
    }

    /**
     * Set the timing option (whether to collect timing info), this will also need INFO messages turned on for the message handler
     * being used. The reportPeriodically flag should be set to false under AJDT so numbers just come out at the end.
     */
    public void setTiming(boolean timersOn, boolean reportPeriodically) {
        timing = timersOn;
        timingPeriodically = reportPeriodically;
    }

    /**
     * Set the error and warning threashold which can be taken from CompilerOptions (see bug 129282)
     *
     * @param errorThreshold
     * @param warningThreshold
     */
    public void setErrorAndWarningThreshold(boolean errorThreshold, boolean warningThreshold) {
        this.errorThreshold = errorThreshold;
        this.warningThreshold = warningThreshold;
    }

    /**
     * @return true if ignoring the UnusedDeclaredThrownException and false if this compiler option is set to error or warning
     */
    public boolean isIgnoringUnusedDeclaredThrownException() {
        // the 0x800000 is CompilerOptions.UnusedDeclaredThrownException
        // which is ASTNode.bit24
        return errorThreshold || warningThreshold;
//		if ((errorThreshold & 0x800000) != 0 || (warningThreshold & 0x800000) != 0) {
//			return false;
//		}
//		return true;
    }

    public void performExtraConfiguration(String config) {
        if (config == null) {
            return;
        }
        // Bunch of name value pairs to split
        extraConfiguration = new Properties();
        int pos = -1;
        while ((pos = config.indexOf(",")) != -1) {
            String nvpair = config.substring(0, pos);
            int pos2 = nvpair.indexOf("=");
            if (pos2 != -1) {
                String n = nvpair.substring(0, pos2);
                String v = nvpair.substring(pos2 + 1);
                extraConfiguration.setProperty(n, v);
            }
            config = config.substring(pos + 1);
        }
        if (config.length() > 0) {
            int pos2 = config.indexOf("=");
            if (pos2 != -1) {
                String n = config.substring(0, pos2);
                String v = config.substring(pos2 + 1);
                extraConfiguration.setProperty(n, v);
            }
        }
        ensureAdvancedConfigurationProcessed();
    }

    public boolean areInfoMessagesEnabled() {
        if (infoMessagesEnabled == 0) {
            infoMessagesEnabled = (messageHandler.isIgnoring(IMessage.INFO) ? 1 : 2);
        }
        return infoMessagesEnabled == 2;
    }

    /**
     * may return null
     */
    public Properties getExtraConfiguration() {
        return extraConfiguration;
    }

    public final static String xsetAVOID_FINAL = "avoidFinal"; // default true

    public final static String xsetWEAVE_JAVA_PACKAGES = "weaveJavaPackages"; // default
    // false
    // -
    // controls
    // LTW
    public final static String xsetWEAVE_JAVAX_PACKAGES = "weaveJavaxPackages"; // default
    // false
    // -
    // controls
    // LTW
    public final static String xsetCAPTURE_ALL_CONTEXT = "captureAllContext"; // default
    // false
    public final static String xsetRUN_MINIMAL_MEMORY = "runMinimalMemory"; // default
    // true
    public final static String xsetDEBUG_STRUCTURAL_CHANGES_CODE = "debugStructuralChangesCode"; // default
    // false
    public final static String xsetDEBUG_BRIDGING = "debugBridging"; // default
    // false
    public final static String xsetTRANSIENT_TJP_FIELDS = "makeTjpFieldsTransient"; // default false
    public final static String xsetBCEL_REPOSITORY_CACHING = "bcelRepositoryCaching";
    public final static String xsetPIPELINE_COMPILATION = "pipelineCompilation";
    public final static String xsetGENERATE_STACKMAPS = "generateStackMaps";
    public final static String xsetPIPELINE_COMPILATION_DEFAULT = "true";
    public final static String xsetCOMPLETE_BINARY_TYPES = "completeBinaryTypes";
    public final static String xsetCOMPLETE_BINARY_TYPES_DEFAULT = "false";
    public final static String xsetTYPE_DEMOTION = "typeDemotion";
    public final static String xsetTYPE_DEMOTION_DEBUG = "typeDemotionDebug";
    public final static String xsetTYPE_REFS = "useWeakTypeRefs";
    public final static String xsetBCEL_REPOSITORY_CACHING_DEFAULT = "true";
    public final static String xsetFAST_PACK_METHODS = "fastPackMethods"; // default true
    public final static String xsetOVERWEAVING = "overWeaving";
    public final static String xsetOPTIMIZED_MATCHING = "optimizedMatching";
    public final static String xsetTIMERS_PER_JOINPOINT = "timersPerJoinpoint";
    public final static String xsetTIMERS_PER_FASTMATCH_CALL = "timersPerFastMatchCall";
    public final static String xsetITD_VERSION = "itdVersion";
    public final static String xsetITD_VERSION_ORIGINAL = "1";
    public final static String xsetITD_VERSION_2NDGEN = "2";
    public final static String xsetITD_VERSION_DEFAULT = xsetITD_VERSION_2NDGEN;
    public final static String xsetMINIMAL_MODEL = "minimalModel";
    public final static String xsetTARGETING_RUNTIME_1610 = "targetRuntime1_6_10";

    // This option allows you to prevent AspectJ adding local variable tables - some tools (e.g. dex) may
    // not like what gets created because even though it is valid, the bytecode they are processing has
    // unexpected quirks that mean the table entries are violated in the code. See issue:
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=470658
    public final static String xsetGENERATE_NEW_LVTS = "generateNewLocalVariableTables";

    public boolean isInJava5Mode() {
        return behaveInJava5Way;
    }

    public boolean isTimingEnabled() {
        return timing;
    }

    public void setTargetAspectjRuntimeLevel(String s) {
        targetAspectjRuntimeLevel = s;
    }

    public void setOptionalJoinpoints(String jps) {
        if (jps == null) {
            return;
        }
        if (jps.indexOf("arrayconstruction") != -1) {
            optionalJoinpoint_ArrayConstruction = true;
        }
        if (jps.indexOf("synchronization") != -1) {
            optionalJoinpoint_Synchronization = true;
        }
    }

    public boolean isJoinpointArrayConstructionEnabled() {
        return optionalJoinpoint_ArrayConstruction;
    }

    public boolean isJoinpointSynchronizationEnabled() {
        return optionalJoinpoint_Synchronization;
    }

    public String getTargetAspectjRuntimeLevel() {
        return targetAspectjRuntimeLevel;
    }

    // OPTIMIZE are users falling foul of not supplying -1.5 and so targetting
    // the old runtime?
    public boolean isTargettingAspectJRuntime12() {
        boolean b = false; // pr116679
        if (!isInJava5Mode()) {
            b = true;
        } else {
            b = getTargetAspectjRuntimeLevel().equals(Constants.RUNTIME_LEVEL_12);
        }
        // System.err.println("Asked if targetting runtime 1.2 , returning: "+b);
        return b;
    }

    /*
     * Map of types in the world, can have 'references' to expendable ones which can be garbage collected to recover memory. An
     * expendable type is a reference type that is not exposed to the weaver (ie just pulled in for type resolution purposes).
     * Generic types have their raw form added to the map, which has a pointer to the underlying generic.
     */
    public static class TypeMap {

        // Strategy for entries in the expendable map
        public final static int DONT_USE_REFS = 0; // Hang around forever
        public final static int USE_WEAK_REFS = 1; // Collected asap
        public final static int USE_SOFT_REFS = 2; // Collected when short on memory

        public List<String> addedSinceLastDemote;
        public List<String> writtenClasses;

        private static boolean debug = false;
        public static boolean useExpendableMap = true; // configurable for reliable testing
        private boolean demotionSystemActive;
        private boolean debugDemotion = false;

        public int policy = USE_WEAK_REFS;

        // Map of types that never get thrown away
        final Map<String, ResolvedType> tMap = new HashMap<String, ResolvedType>();

        // Map of types that may be ejected from the cache if we need space
        final Map<String, Reference<ResolvedType>> expendableMap = Collections
                .synchronizedMap(new WeakHashMap<String, Reference<ResolvedType>>());

        // profiling tools...
        private boolean memoryProfiling = false;
        private int maxExpendableMapSize = -1;
        private int collectedTypes = 0;
        private final ReferenceQueue<ResolvedType> rq = new ReferenceQueue<ResolvedType>();

        TypeMap() {
            // Demotion activated when switched on and loadtime weaving or in AJDT
            demotionSystemActive = false;
            addedSinceLastDemote = new ArrayList<String>();
            writtenClasses = new ArrayList<String>();
//            this.w = w;
            memoryProfiling = false;
            // INFO);
        }

        public void setWord(World w) {
        }

        // For testing
        public Map<String, Reference<ResolvedType>> getExpendableMap() {
            return expendableMap;
        }

        // For testing
        public Map<String, ResolvedType> getMainMap() {
            return tMap;
        }

        public int demote() {
            return demote(false);
        }

        /**
         * Go through any types added during the previous file weave. If any are suitable for demotion, then put them in the
         * expendable map where GC can claim them at some point later. Demotion means: the type is not an aspect, the type is not
         * java.lang.Object, the type is not primitive and the type is not affected by type mungers in any way. Further refinements
         * of these conditions may allow for more demotions.
         *
         * @return number of types demoted
         */
        public int demote(boolean atEndOfCompile) {
            return 0;
        }

        private void insertInExpendableMap(String key, ResolvedType type) {
            if (useExpendableMap) {
                if (!expendableMap.containsKey(key)) {
                    if (policy == USE_SOFT_REFS) {
                        expendableMap.put(key, new SoftReference<ResolvedType>(type));
                    } else {
                        expendableMap.put(key, new WeakReference<ResolvedType>(type));
                    }
                }
            }
        }

        /**
         * Add a new type into the map, the key is the type signature. Some types do *not* go in the map, these are ones involving
         * *member* type variables. The reason is that when all you have is the signature which gives you a type variable name, you
         * cannot guarantee you are using the type variable in the same way as someone previously working with a similarly named
         * type variable. So, these do not go into the map: - TypeVariableReferenceType. - ParameterizedType where a member type
         * variable is involved. - BoundedReferenceType when one of the bounds is a type variable.
         * <p>
         * definition: "member type variables" - a tvar declared on a generic method/ctor as opposed to those you see declared on a
         * generic type.
         */
        public synchronized ResolvedType put(String key, ResolvedType type) {
            if (!type.isCacheable()) {
                return type;
            }
            if (type.isParameterizedType() && type.isParameterizedWithTypeVariable()) {
                if (debug) {
                    System.err
                            .println("Not putting a parameterized type that utilises member declared type variables into the typemap: key="
                                    + key + " type=" + type);
                }
                return type;
            }
            if (type.isTypeVariableReference()) {
                if (debug) {
                    System.err.println("Not putting a type variable reference type into the typemap: key=" + key + " type=" + type);
                }
                return type;
            }
            // this test should be improved - only avoid putting them in if one
            // of the
            // bounds is a member type variable
            if (type instanceof BoundedReferenceType) {
                if (debug) {
                    System.err.println("Not putting a bounded reference type into the typemap: key=" + key + " type=" + type);
                }
                return type;
            }
            if (type instanceof MissingResolvedTypeWithKnownSignature) {
                if (debug) {
                    System.err.println("Not putting a missing type into the typemap: key=" + key + " type=" + type);
                }
                return type;
            }

            if ((type instanceof ReferenceType) && (((ReferenceType) type).getDelegate() == null)) {
                if (debug) {
                    System.err.println("Not putting expendable ref type with null delegate into typemap: key=" + key + " type="
                            + type);
                }
                return type;
            }

            // TODO should this be in as a permanent assertion?
            /*
             * if ((type instanceof ReferenceType) && type.getWorld().isInJava5Mode() && (((ReferenceType) type).getDelegate() !=
			 * null) && type.isGenericType()) { throw new BCException("Attempt to add generic type to typemap " + type.toString() +
			 * " (should be raw)"); }
			 */

            if (isExpendable(type)) {
                if (useExpendableMap) {
                    // Dont use reference queue for tracking if not profiling...
                    if (policy == USE_WEAK_REFS) {
                        if (memoryProfiling) {
                            expendableMap.put(key, new WeakReference<ResolvedType>(type, rq));
                        } else {
                            expendableMap.put(key, new WeakReference<ResolvedType>(type));
                        }
                    } else if (policy == USE_SOFT_REFS) {
                        if (memoryProfiling) {
                            expendableMap.put(key, new SoftReference<ResolvedType>(type, rq));
                        } else {
                            expendableMap.put(key, new SoftReference<ResolvedType>(type));
                        }
                        // } else {
                        // expendableMap.put(key, type);
                    }
                }
                if (memoryProfiling && expendableMap.size() > maxExpendableMapSize) {
                    maxExpendableMapSize = expendableMap.size();
                }
                return type;
            } else {
                if (demotionSystemActive) {
                    // System.out.println("Added since last demote " + key);
                    addedSinceLastDemote.add(key);
                }

                return tMap.put(key, type);
            }
        }

        protected boolean isExpendable(ResolvedType type) {
            return !type.equals(UnresolvedType.OBJECT) && !type.isExposedToWeaver() && !type.isPrimitiveType()
                    && !type.isPrimitiveArray();
        }


        public void report() {
            if (!memoryProfiling) {
                return;
            }
            checkq();
//            w.getMessageHandler().handleMessage(
//                    MessageUtil.info("MEMORY: world expendable type map reached maximum size of #" + maxExpendableMapSize
//                            + " entries"));
//            w.getMessageHandler().handleMessage(
//                    MessageUtil.info("MEMORY: types collected through garbage collection #" + collectedTypes + " entries"));
        }

        public void checkq() {
            if (!memoryProfiling) {
                return;
            }
            Reference r = null;
            while ((r = rq.poll()) != null) {
                collectedTypes++;
            }
        }

        /**
         * Lookup a type by its signature, always look in the real map before the expendable map
         */
        public ResolvedType get(String key) {
            checkq();
            ResolvedType ret = tMap.get(key);
            if (ret == null) {
                if (policy == USE_WEAK_REFS) {
                    WeakReference<ResolvedType> ref = (WeakReference<ResolvedType>) expendableMap.get(key);
                    if (ref != null) {
                        ret = ref.get();
//						if (ret==null) {
//							expendableMap.remove(key);
//						}
                    }
                } else if (policy == USE_SOFT_REFS) {
                    SoftReference<ResolvedType> ref = (SoftReference<ResolvedType>) expendableMap.get(key);
                    if (ref != null) {
                        ret = ref.get();
//						if (ret==null) {
//							expendableMap.remove(key);
//						}
                    }
                    // } else {
                    // return (ResolvedType) expendableMap.get(key);
                }
            }
            return ret;
        }

        /**
         * Remove a type from the map
         */
        public ResolvedType remove(String key) {
            ResolvedType ret = tMap.remove(key);
            if (ret == null) {
                if (policy == USE_WEAK_REFS) {
                    WeakReference<ResolvedType> wref = (WeakReference<ResolvedType>) expendableMap.remove(key);
                    if (wref != null) {
                        ret = wref.get();
                    }
                } else if (policy == USE_SOFT_REFS) {
                    SoftReference<ResolvedType> wref = (SoftReference<ResolvedType>) expendableMap.remove(key);
                    if (wref != null) {
                        ret = wref.get();
                    }
                    // } else {
                    // ret = (ResolvedType) expendableMap.remove(key);
                }
            }
            return ret;
        }

        public void classWriteEvent(String classname) {
            // that is a name com.Foo and not a signature Lcom/Foo; boooooooooo!
            if (demotionSystemActive) {
                writtenClasses.add(classname);
            }
            if (debugDemotion) {
                System.out.println("Class write event for " + classname);
            }
        }

        public void demote(ResolvedType type) {
            String key = type.getSignature();
            if (debugDemotion) {
                addedSinceLastDemote.remove(key);
            }
            tMap.remove(key);
            insertInExpendableMap(key, type);
        }

        // public ResolvedType[] getAllTypes() {
        // List/* ResolvedType */results = new ArrayList();
        //
        // collectTypes(expendableMap, results);
        // collectTypes(tMap, results);
        // return (ResolvedType[]) results.toArray(new
        // ResolvedType[results.size()]);
        // }
        //
        // private void collectTypes(Map map, List/* ResolvedType */results) {
        // for (Iterator iterator = map.keySet().iterator();
        // iterator.hasNext();) {
        // String key = (String) iterator.next();
        // ResolvedType type = get(key);
        // if (type != null)
        // results.add(type);
        // else
        // System.err.println("null!:" + key);
        // }
        // }

    }

    /**
     * This class is used to compute and store precedence relationships between aspects.
     */
    private static class AspectPrecedenceCalculator {

        private final World world;
        private final Map<PrecedenceCacheKey, Integer> cachedResults;

        public AspectPrecedenceCalculator(World forSomeWorld) {
            world = forSomeWorld;
            cachedResults = new HashMap<PrecedenceCacheKey, Integer>();
        }

        /**
         * Ask every declare precedence in the world to order the two aspects. If more than one declare precedence gives an
         * ordering, and the orderings conflict, then that's an error.
         */
        public int compareByPrecedence(ResolvedType firstAspect, ResolvedType secondAspect) {
            PrecedenceCacheKey key = new PrecedenceCacheKey(firstAspect, secondAspect);
            if (cachedResults.containsKey(key)) {
                return (cachedResults.get(key)).intValue();
            } else {
                int order = 0;
                DeclarePrecedence orderer = null; // Records the declare
                // precedence statement that
                // gives the first ordering
                for (Iterator<Declare> i = world.getCrosscuttingMembersSet().getDeclareDominates().iterator(); i.hasNext(); ) {
                    DeclarePrecedence d = (DeclarePrecedence) i.next();
                    int thisOrder = d.compare(firstAspect, secondAspect);
                    if (thisOrder != 0) {
                        if (orderer == null) {
                            orderer = d;
                        }
                        if (order != 0 && order != thisOrder) {
                            ISourceLocation[] isls = new ISourceLocation[2];
                            isls[0] = orderer.getSourceLocation();
                            isls[1] = d.getSourceLocation();
                            Message m = new Message("conflicting declare precedence orderings for aspects: "
                                    + firstAspect.getName() + " and " + secondAspect.getName(), null, true, isls);
                            world.getMessageHandler().handleMessage(m);
                        } else {
                            order = thisOrder;
                        }
                    }
                }
                cachedResults.put(key, new Integer(order));
                return order;
            }
        }

        public Integer getPrecedenceIfAny(ResolvedType aspect1, ResolvedType aspect2) {
            return cachedResults.get(new PrecedenceCacheKey(aspect1, aspect2));
        }

        public int compareByPrecedenceAndHierarchy(ResolvedType firstAspect, ResolvedType secondAspect) {
            if (firstAspect.equals(secondAspect)) {
                return 0;
            }

            int ret = compareByPrecedence(firstAspect, secondAspect);
            if (ret != 0) {
                return ret;
            }

            if (firstAspect.isAssignableFrom(secondAspect)) {
                return -1;
            } else if (secondAspect.isAssignableFrom(firstAspect)) {
                return +1;
            }

            return 0;
        }

        private static class PrecedenceCacheKey {
            public ResolvedType aspect1;
            public ResolvedType aspect2;

            public PrecedenceCacheKey(ResolvedType a1, ResolvedType a2) {
                aspect1 = a1;
                aspect2 = a2;
            }

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof PrecedenceCacheKey)) {
                    return false;
                }
                PrecedenceCacheKey other = (PrecedenceCacheKey) obj;
                return (aspect1 == other.aspect1 && aspect2 == other.aspect2);
            }

            @Override
            public int hashCode() {
                return aspect1.hashCode() + aspect2.hashCode();
            }
        }
    }

    public void validateType(UnresolvedType type) {
    }

    // --- with java5 we can get into a recursive mess if we aren't careful when
    // resolving types (*cough* java.lang.Enum) ---

    public boolean isDemotionActive() {
        return true;
    }

    // --- this first map is for java15 delegates which may try and recursively
    // access the same type variables.
    // --- I would rather stash this against a reference type - but we don't
    // guarantee referencetypes are unique for
    // so we can't :(
    private final Map<Class<?>, TypeVariable[]> workInProgress1 = new HashMap<Class<?>, TypeVariable[]>();

    public TypeVariable[] getTypeVariablesCurrentlyBeingProcessed(Class<?> baseClass) {
        return workInProgress1.get(baseClass);
    }

    public void recordTypeVariablesCurrentlyBeingProcessed(Class<?> baseClass, TypeVariable[] typeVariables) {
        workInProgress1.put(baseClass, typeVariables);
    }

    public void forgetTypeVariablesCurrentlyBeingProcessed(Class<?> baseClass) {
        workInProgress1.remove(baseClass);
    }

    public void setAddSerialVerUID(boolean b) {
        addSerialVerUID = b;
    }

    public boolean isAddSerialVerUID() {
        return addSerialVerUID;
    }

    /**
     * be careful calling this - pr152257
     */
    public void flush() {
//        typeMap.expendableMap.clear();
    }

    public void ensureAdvancedConfigurationProcessed() {

        // Check *once* whether the user has switched asm support off
        if (!checkedAdvancedConfiguration) {
            Properties p = getExtraConfiguration();
            if (p != null) {

                String s = p.getProperty(xsetBCEL_REPOSITORY_CACHING, xsetBCEL_REPOSITORY_CACHING_DEFAULT);
                bcelRepositoryCaching = s.equalsIgnoreCase("true");
                if (!bcelRepositoryCaching) {
                    getMessageHandler().handleMessage(
                            MessageUtil
                                    .info("[bcelRepositoryCaching=false] AspectJ will not use a bcel cache for class information"));
                }

                // ITD Versions
                // 1 is the first version in use up to AspectJ 1.6.8
                // 2 is from 1.6.9 onwards
                s = p.getProperty(xsetITD_VERSION, xsetITD_VERSION_DEFAULT);
                if (s.equals(xsetITD_VERSION_ORIGINAL)) {
                    itdVersion = 1;
                }

                s = p.getProperty(xsetAVOID_FINAL, "false");
                if (s.equalsIgnoreCase("true")) {
                    useFinal = false; // if avoidFinal=true, then set useFinal to false
                }

                s = p.getProperty(xsetMINIMAL_MODEL, "true");
                if (s.equalsIgnoreCase("false")) {
                    minimalModel = false;
                }

                s = p.getProperty(xsetTARGETING_RUNTIME_1610, "false");
                if (s.equalsIgnoreCase("true")) {
                    targettingRuntime1_6_10 = true;
                }

                s = p.getProperty(xsetFAST_PACK_METHODS, "true");
                fastMethodPacking = s.equalsIgnoreCase("true");

                s = p.getProperty(xsetPIPELINE_COMPILATION, xsetPIPELINE_COMPILATION_DEFAULT);
                shouldPipelineCompilation = s.equalsIgnoreCase("true");

                s = p.getProperty(xsetGENERATE_STACKMAPS, "false");
                shouldGenerateStackMaps = s.equalsIgnoreCase("true");

                s = p.getProperty(xsetCOMPLETE_BINARY_TYPES, xsetCOMPLETE_BINARY_TYPES_DEFAULT);
                completeBinaryTypes = s.equalsIgnoreCase("true");
                if (completeBinaryTypes) {
                    getMessageHandler().handleMessage(
                            MessageUtil.info("[completeBinaryTypes=true] Completion of binary types activated"));
                }

                s = p.getProperty(xsetTYPE_DEMOTION); // default is: ON
                if (s != null) {
                    boolean b = typeMap.demotionSystemActive;
                    if (b && s.equalsIgnoreCase("false")) {
                        System.out.println("typeDemotion=false: type demotion switched OFF");
                        typeMap.demotionSystemActive = false;
                    } else if (!b && s.equalsIgnoreCase("true")) {
                        System.out.println("typeDemotion=true: type demotion switched ON");
                        typeMap.demotionSystemActive = true;
                    }
                }

                s = p.getProperty(xsetOVERWEAVING, "false");
                if (s.equalsIgnoreCase("true")) {
                    overWeaving = true;
                }

                s = p.getProperty(xsetTYPE_DEMOTION_DEBUG, "false");
                if (s.equalsIgnoreCase("true")) {
                    typeMap.debugDemotion = true;
                }
                s = p.getProperty(xsetTYPE_REFS, "true");
                if (s.equalsIgnoreCase("false")) {
                    typeMap.policy = TypeMap.USE_SOFT_REFS;
                }

                runMinimalMemorySet = p.getProperty(xsetRUN_MINIMAL_MEMORY) != null;
                s = p.getProperty(xsetRUN_MINIMAL_MEMORY, "false");
                runMinimalMemory = s.equalsIgnoreCase("true");
                // if (runMinimalMemory)
                // getMessageHandler().handleMessage(MessageUtil.info(
                // "[runMinimalMemory=true] Optimizing bcel processing (and cost of performance) to use less memory"
                // ));

                s = p.getProperty(xsetDEBUG_STRUCTURAL_CHANGES_CODE, "false");
                forDEBUG_structuralChangesCode = s.equalsIgnoreCase("true");

                s = p.getProperty(xsetTRANSIENT_TJP_FIELDS, "false");
                transientTjpFields = s.equalsIgnoreCase("true");

                s = p.getProperty(xsetDEBUG_BRIDGING, "false");
                forDEBUG_bridgingCode = s.equalsIgnoreCase("true");

                s = p.getProperty(xsetGENERATE_NEW_LVTS, "true");
                generateNewLvts = s.equalsIgnoreCase("true");
                if (!generateNewLvts) {
                    getMessageHandler().handleMessage(MessageUtil.info("[generateNewLvts=false] for methods without an incoming local variable table, do not generate one"));
                }

                s = p.getProperty(xsetOPTIMIZED_MATCHING, "true");
                optimizedMatching = s.equalsIgnoreCase("true");
                if (!optimizedMatching) {
                    getMessageHandler().handleMessage(MessageUtil.info("[optimizedMatching=false] optimized matching turned off"));
                }

                s = p.getProperty(xsetTIMERS_PER_JOINPOINT, "25000");
                try {
                    timersPerJoinpoint = Integer.parseInt(s);
                } catch (Exception e) {
                    getMessageHandler().handleMessage(MessageUtil.error("unable to process timersPerJoinpoint value of " + s));
                    timersPerJoinpoint = 25000;
                }

                s = p.getProperty(xsetTIMERS_PER_FASTMATCH_CALL, "250");
                try {
                    timersPerType = Integer.parseInt(s);
                } catch (Exception e) {
                    getMessageHandler().handleMessage(MessageUtil.error("unable to process timersPerType value of " + s));
                    timersPerType = 250;
                }

            }
            try {
                if (systemPropertyOverWeaving) {
                    overWeaving = true;
                }
                String value = null;
                value = System.getProperty("aspectj.typeDemotion", "false");
                if (value.equalsIgnoreCase("true")) {
                    System.out.println("ASPECTJ: aspectj.typeDemotion=true: type demotion switched ON");
                    typeMap.demotionSystemActive = true;
                }
                value = System.getProperty("aspectj.minimalModel", "false");
                if (value.equalsIgnoreCase("true")) {
                    System.out.println("ASPECTJ: aspectj.minimalModel=true: minimal model switched ON");
                    minimalModel = true;
                }
            } catch (Throwable t) {
                System.err.println("ASPECTJ: Unable to read system properties");
                t.printStackTrace();
            }
            checkedAdvancedConfiguration = true;
        }
    }

    public boolean isRunMinimalMemory() {
        ensureAdvancedConfigurationProcessed();
        return runMinimalMemory;
    }

    public boolean isTransientTjpFields() {
        ensureAdvancedConfigurationProcessed();
        return transientTjpFields;
    }

    public boolean isRunMinimalMemorySet() {
        ensureAdvancedConfigurationProcessed();
        return runMinimalMemorySet;
    }

    public boolean shouldFastPackMethods() {
        ensureAdvancedConfigurationProcessed();
        return fastMethodPacking;
    }

    public boolean shouldPipelineCompilation() {
        ensureAdvancedConfigurationProcessed();
        return shouldPipelineCompilation;
    }

    public boolean shouldGenerateStackMaps() {
        ensureAdvancedConfigurationProcessed();
        return shouldGenerateStackMaps;
    }

    public void setIncrementalCompileCouldFollow(boolean b) {
        incrementalCompileCouldFollow = b;
    }

    public boolean couldIncrementalCompileFollow() {
        return incrementalCompileCouldFollow;
    }

    public void setSynchronizationPointcutsInUse() {
        if (trace.isTraceEnabled()) {
            trace.enter("setSynchronizationPointcutsInUse", this);
        }
        synchronizationPointcutsInUse = true;
        if (trace.isTraceEnabled()) {
            trace.exit("setSynchronizationPointcutsInUse");
        }
    }

    public boolean areSynchronizationPointcutsInUse() {
        return synchronizationPointcutsInUse;
    }

    /**
     * Register a new pointcut designator handler with the world - this can be used by any pointcut parsers attached to the world.
     *
     * @param designatorHandler handler for the new pointcut
     */
    public void registerPointcutHandler(PointcutDesignatorHandler designatorHandler) {
        if (pointcutDesignators == null) {
            pointcutDesignators = new HashSet<PointcutDesignatorHandler>();
        }
        pointcutDesignators.add(designatorHandler);
    }

    public Set<PointcutDesignatorHandler> getRegisteredPointcutHandlers() {
        if (pointcutDesignators == null) {
            return Collections.emptySet();
        }
        return pointcutDesignators;
    }

    public void reportMatch(ShadowMunger munger, Shadow shadow) {

    }

    public boolean isOverWeaving() {
        return overWeaving;
    }

    public void reportCheckerMatch(Checker checker, Shadow shadow) {
    }

    /**
     * @return true if this world has the activation and scope of application of the aspects controlled via aop.xml files
     */
    public boolean isXmlConfigured() {
        return false;
    }

    public boolean isAspectIncluded(ResolvedType aspectType) {
        return true;
    }

    /**
     * Determine if the named aspect requires a particular type around in order to be useful. The type is named in the aop.xml file
     * against the aspect.
     *
     * @return true if there is a type missing that this aspect really needed around
     */
    public boolean hasUnsatisfiedDependency(ResolvedType aspectType) {
        return false;
    }

    public TypePattern getAspectScope(ResolvedType declaringType) {
        return null;
    }

    public Map<String, ResolvedType> getFixed() {
        return typeMap.tMap;
    }

    public Map<String, Reference<ResolvedType>> getExpendable() {
        return typeMap.expendableMap;
    }

    /**
     * Ask the type map to demote any types it can - we don't want them anchored forever.
     */
    public void demote() {
        typeMap.demote();
    }

    // protected boolean isExpendable(ResolvedType type) {
    // if (type.equals(UnresolvedType.OBJECT))
    // return false;
    // if (type == null)
    // return false;
    // boolean isExposed = type.isExposedToWeaver();
    // boolean nullDele = (type instanceof ReferenceType) ? ((ReferenceType) type).getDelegate() != null : true;
    // if (isExposed || !isExposed && nullDele)
    // return false;
    // return !type.isPrimitiveType();
    // }

    /**
     * Reference types we don't intend to weave may be ejected from the cache if we need the space.
     */
    protected boolean isExpendable(ResolvedType type) {
        return !type.equals(UnresolvedType.OBJECT) && !type.isExposedToWeaver() && !type.isPrimitiveType()
                && !type.isPrimitiveArray();
    }

    // map from aspect > excluded types
    // memory issue here?
    private Map<ResolvedType, Set<ResolvedType>> exclusionMap = new HashMap<ResolvedType, Set<ResolvedType>>();

    public Map<ResolvedType, Set<ResolvedType>> getExclusionMap() {
        return exclusionMap;
    }

    private TimeCollector timeCollector = null;

    /**
     * Record the time spent matching a pointcut - this will accumulate over the lifetime of this world/weaver and be reported every
     * 25000 join points.
     */
    public void record(Pointcut pointcut, long timetaken) {
        if (timeCollector == null) {
            ensureAdvancedConfigurationProcessed();
            timeCollector = new TimeCollector(this);
        }
        timeCollector.record(pointcut, timetaken);
    }

    /**
     * Record the time spent fastmatching a pointcut - this will accumulate over the lifetime of this world/weaver and be reported
     * every 250 types.
     */
    public void recordFastMatch(Pointcut pointcut, long timetaken) {
        if (timeCollector == null) {
            ensureAdvancedConfigurationProcessed();
            timeCollector = new TimeCollector(this);
        }
        timeCollector.recordFastMatch(pointcut, timetaken);
    }

    public void reportTimers() {
        if (timeCollector != null && !timingPeriodically) {
            timeCollector.report();
            timeCollector = new TimeCollector(this);
        }
    }

    private static class TimeCollector {
        private World world;
        long joinpointCount;
        long typeCount;
        long perJoinpointCount;
        long perTypes;
        Map<String, Long> joinpointsPerPointcut = new HashMap<String, Long>();
        Map<String, Long> timePerPointcut = new HashMap<String, Long>();
        Map<String, Long> fastMatchTimesPerPointcut = new HashMap<String, Long>();
        Map<String, Long> fastMatchTypesPerPointcut = new HashMap<String, Long>();

        TimeCollector(World world) {
            this.perJoinpointCount = world.timersPerJoinpoint;
            this.perTypes = world.timersPerType;
            this.world = world;
            this.joinpointCount = 0;
            this.typeCount = 0;
            this.joinpointsPerPointcut = new HashMap<String, Long>();
            this.timePerPointcut = new HashMap<String, Long>();
        }

        public void report() {
            long totalTime = 0L;
            for (String p : joinpointsPerPointcut.keySet()) {
                totalTime += timePerPointcut.get(p);
            }
            world.getMessageHandler().handleMessage(
                    MessageUtil.info("Pointcut matching cost (total=" + (totalTime / 1000000) + "ms for " + joinpointCount
                            + " joinpoint match calls):"));
            for (String p : joinpointsPerPointcut.keySet()) {
                StringBuffer sb = new StringBuffer();
                sb.append("Time:" + (timePerPointcut.get(p) / 1000000) + "ms (jps:#" + joinpointsPerPointcut.get(p)
                        + ") matching against " + p);
                world.getMessageHandler().handleMessage(MessageUtil.info(sb.toString()));
            }
            world.getMessageHandler().handleMessage(MessageUtil.info("---"));

            totalTime = 0L;
            for (String p : fastMatchTimesPerPointcut.keySet()) {
                totalTime += fastMatchTimesPerPointcut.get(p);
            }
            world.getMessageHandler().handleMessage(
                    MessageUtil.info("Pointcut fast matching cost (total=" + (totalTime / 1000000) + "ms for " + typeCount
                            + " fast match calls):"));
            for (String p : fastMatchTimesPerPointcut.keySet()) {
                StringBuffer sb = new StringBuffer();
                sb.append("Time:" + (fastMatchTimesPerPointcut.get(p) / 1000000) + "ms (types:#" + fastMatchTypesPerPointcut.get(p)
                        + ") fast matching against " + p);
                world.getMessageHandler().handleMessage(MessageUtil.info(sb.toString()));
            }
            world.getMessageHandler().handleMessage(MessageUtil.info("---"));

        }

        void record(Pointcut pointcut, long timetakenInNs) {
            joinpointCount++;
            String pointcutText = pointcut.toString();
            Long jpcounter = joinpointsPerPointcut.get(pointcutText);
            if (jpcounter == null) {
                jpcounter = 1L;
            } else {
                jpcounter++;
            }
            joinpointsPerPointcut.put(pointcutText, jpcounter);

            Long time = timePerPointcut.get(pointcutText);
            if (time == null) {
                time = timetakenInNs;
            } else {
                time += timetakenInNs;
            }
            timePerPointcut.put(pointcutText, time);
            if (world.timingPeriodically) {
                if ((joinpointCount % perJoinpointCount) == 0) {
                    long totalTime = 0L;
                    for (String p : joinpointsPerPointcut.keySet()) {
                        totalTime += timePerPointcut.get(p);
                    }
                    world.getMessageHandler().handleMessage(
                            MessageUtil.info("Pointcut matching cost (total=" + (totalTime / 1000000) + "ms for " + joinpointCount
                                    + " joinpoint match calls):"));
                    for (String p : joinpointsPerPointcut.keySet()) {
                        StringBuffer sb = new StringBuffer();
                        sb.append("Time:" + (timePerPointcut.get(p) / 1000000) + "ms (jps:#" + joinpointsPerPointcut.get(p)
                                + ") matching against " + p);
                        world.getMessageHandler().handleMessage(MessageUtil.info(sb.toString()));
                    }
                    world.getMessageHandler().handleMessage(MessageUtil.info("---"));
                }
            }
        }

        void recordFastMatch(Pointcut pointcut, long timetakenInNs) {
            typeCount++;
            String pointcutText = pointcut.toString();
            Long typecounter = fastMatchTypesPerPointcut.get(pointcutText);
            if (typecounter == null) {
                typecounter = 1L;
            } else {
                typecounter++;
            }
            fastMatchTypesPerPointcut.put(pointcutText, typecounter);

            Long time = fastMatchTimesPerPointcut.get(pointcutText);
            if (time == null) {
                time = timetakenInNs;
            } else {
                time += timetakenInNs;
            }
            fastMatchTimesPerPointcut.put(pointcutText, time);
            if (world.timingPeriodically) {
                if ((typeCount % perTypes) == 0) {
                    long totalTime = 0L;
                    for (String p : fastMatchTimesPerPointcut.keySet()) {
                        totalTime += fastMatchTimesPerPointcut.get(p);
                    }
                    world.getMessageHandler().handleMessage(
                            MessageUtil.info("Pointcut fast matching cost (total=" + (totalTime / 1000000) + "ms for " + typeCount
                                    + " fast match calls):"));
                    for (String p : fastMatchTimesPerPointcut.keySet()) {
                        StringBuffer sb = new StringBuffer();
                        sb.append("Time:" + (fastMatchTimesPerPointcut.get(p) / 1000000) + "ms (types:#"
                                + fastMatchTypesPerPointcut.get(p) + ") fast matching against " + p);
                        world.getMessageHandler().handleMessage(MessageUtil.info(sb.toString()));
                    }
                    world.getMessageHandler().handleMessage(MessageUtil.info("---"));
                }
            }
        }
    }

    public TypeMap getTypeMap() {
        return typeMap;
    }

    public static void reset() {
        // ResolvedType.resetPrimitives();
    }

    /**
     * Returns the version of ITD that this world wants to create. The default is the new style (2) but in some cases where there
     * might be a clash, the old style can be used. It is set through the option -Xset:itdVersion=1
     *
     * @return the ITD version this world wants to create - 1=oldstyle 2=new, transparent style
     */
    public int getItdVersion() {
        return itdVersion;
    }

    // if not loadtime weaving then we are compile time weaving or post-compile time weaving
    public abstract boolean isLoadtimeWeaving();

    public void classWriteEvent(char[][] compoundName) {
        // override if interested in write events
    }

}