//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.aspectj.ajdt.internal.compiler;

import java.util.Stack;

import org.aspectj.internal.lang.annotation.ajcPrivileged;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.org.eclipse.jdt.internal.compiler.Compiler;
import org.aspectj.org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.aspectj.org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.aspectj.org.eclipse.jdt.internal.compiler.problem.AbortCompilation;

@Aspect
@ajcPrivileged
public class CompilerAdapter {
    private static ICompilerAdapterFactory adapterFactory;
    private final ThreadLocal<Stack<ICompilerAdapter>> compilerAdapterStack = new ThreadLocal<>();
    static ThreadLocal<ICompilerAdapterFactory> compilerAdapterFactory = new ThreadLocal<>();

    static {
        try {
            adapterFactory = new ICompilerAdapterFactory() {
                public ICompilerAdapter getAdapter(Compiler forCompiler) {
                    return new DefaultCompilerAdapter(forCompiler);
                }
            };
            compilerAdapterFactory.set(adapterFactory);

        } catch (Throwable var1) {
        }

    }

    public CompilerAdapter() {
    }

    public static void setCompilerAdapterFactory(ICompilerAdapterFactory factory) {
        compilerAdapterFactory.set(factory);
    }

    @Before(
            value = "compiling(compiler, sourceUnits)",
            argNames = "compiler,sourceUnits"
    )
    public void ajc$before$org_aspectj_ajdt_internal_compiler_CompilerAdapter$1$4c37d260(Compiler compiler, ICompilationUnit[] sourceUnits) {
        ICompilerAdapter compilerAdapter = compilerAdapterFactory.get().getAdapter(compiler);
        if (this.compilerAdapterStack.get() == null) {
            this.compilerAdapterStack.set(new Stack<ICompilerAdapter>());
        }
        this.compilerAdapterStack.get().push(compilerAdapter);
        compilerAdapter.beforeCompiling(sourceUnits);
    }

    @AfterReturning(
            pointcut = "compiling(compiler, ICompilationUnit)",
            returning = "",
            argNames = "compiler"
    )
    public void ajc$afterReturning$org_aspectj_ajdt_internal_compiler_CompilerAdapter$2$f9cc9ca0(Compiler compiler) {
        try {
            ICompilerAdapter e = get().pop();
            e.afterCompiling(compiler.unitsToProcess);
        } catch (AbortCompilation e) {
//            compiler.handleInternalException(e, null);
        } catch (Error var9) {
            throw var9;
        } catch (RuntimeException var10) {
            throw var10;
        } finally {
            if (this.compilerAdapterStack.get() != null && this.compilerAdapterStack.get().isEmpty()) {
                compiler.reset();
            }
        }

    }

    public Stack<ICompilerAdapter> get() {
        return this.compilerAdapterStack.get();
    }

    @Before(
            value = "processing(unit, index)",
            argNames = "unit,index"
    )
    public void ajc$before$org_aspectj_ajdt_internal_compiler_CompilerAdapter$3$6b855184(CompilationUnitDeclaration unit, int index) {
        ((ICompilerAdapter) this.compilerAdapterStack.get().peek()).beforeProcessing(unit);
    }

    @AfterReturning(
            pointcut = "dietParsing(compiler)",
            returning = "",
            argNames = "compiler"
    )
    public void ajc$afterReturning$org_aspectj_ajdt_internal_compiler_CompilerAdapter$4$2cef295e(Compiler compiler) {
        ((ICompilerAdapter) this.compilerAdapterStack.get().peek()).afterDietParsing(compiler.unitsToProcess);
    }

    @After(
            value = "processing(unit, index)",
            argNames = "unit,index"
    )
    public void ajc$after$org_aspectj_ajdt_internal_compiler_CompilerAdapter$5$6b855184(CompilationUnitDeclaration unit, int index) {
        ((ICompilerAdapter) this.compilerAdapterStack.get().peek()).afterProcessing(unit, index);
    }

    @Before(
            value = "resolving(unit)",
            argNames = "unit"
    )
    public void ajc$before$org_aspectj_ajdt_internal_compiler_CompilerAdapter$6$bc8e0e6(CompilationUnitDeclaration unit) {
        ((ICompilerAdapter) this.compilerAdapterStack.get().peek()).beforeResolving(unit);
    }

    @AfterReturning(
            pointcut = "resolving(unit)",
            returning = "",
            argNames = "unit"
    )
    public void ajc$afterReturning$org_aspectj_ajdt_internal_compiler_CompilerAdapter$7$bc8e0e6(CompilationUnitDeclaration unit) {
        ((ICompilerAdapter) this.compilerAdapterStack.get().peek()).afterResolving(unit);
    }

    @Before(
            value = "analysing(unit)",
            argNames = "unit"
    )
    public void ajc$before$org_aspectj_ajdt_internal_compiler_CompilerAdapter$8$db78446d(CompilationUnitDeclaration unit) {
        ((ICompilerAdapter) this.compilerAdapterStack.get().peek()).beforeAnalysing(unit);
    }

    @AfterReturning(
            pointcut = "analysing(unit)",
            returning = "",
            argNames = "unit"
    )
    public void ajc$afterReturning$org_aspectj_ajdt_internal_compiler_CompilerAdapter$9$db78446d(CompilationUnitDeclaration unit) {
        ((ICompilerAdapter) this.compilerAdapterStack.get().peek()).afterAnalysing(unit);
    }

    @Before(
            value = "generating(unit)",
            argNames = "unit"
    )
    public void ajc$before$org_aspectj_ajdt_internal_compiler_CompilerAdapter$10$eba4db6f(CompilationUnitDeclaration unit) {
        ((ICompilerAdapter) this.compilerAdapterStack.get().peek()).beforeGenerating(unit);
    }

    @AfterReturning(
            pointcut = "generating(unit)",
            returning = "",
            argNames = "unit"
    )
    public void ajc$afterReturning$org_aspectj_ajdt_internal_compiler_CompilerAdapter$11$eba4db6f(CompilationUnitDeclaration unit) {
        ((ICompilerAdapter) this.compilerAdapterStack.get().peek()).afterGenerating(unit);
    }

    static ThreadLocal<CompilerAdapter> threadLocal = new ThreadLocal<>();

    public static CompilerAdapter aspectOf() {
        if (threadLocal.get() == null) {
            threadLocal.set(new CompilerAdapter());
        }
        return threadLocal.get();
    }

    public static boolean hasAspect() {
        return true;
    }
}
