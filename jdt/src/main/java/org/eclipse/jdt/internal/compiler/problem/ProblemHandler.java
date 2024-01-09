/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
/**
 * Override ECJ ProblemHandler to prevent AbortCompilation exception when resolving unknown types.
 * Initial implementation was:
 * https://github.com/eclipse-jdt/eclipse.jdt.core/blob/R4_30/org.eclipse.jdt.core.compiler.batch/src/org/eclipse/jdt/internal/compiler/problem/ProblemHandler.java
 */
/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stephan Herrmann - Contribution for
 *								Bug 458396 - NPE in CodeStream.invoke()
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.problem;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.impl.ReferenceContext;
import org.eclipse.jdt.internal.compiler.util.Util;

/*
 * Compiler error handler, responsible to determine whether
 * a problem is actually a warning or an error; also will
 * decide whether the compilation task can be processed further or not.
 *
 * Behavior : will request its current policy if need to stop on
 *	first error, and if should proceed (persist) with problems.
 */

public class ProblemHandler {

  public final static String[] NoArgument = CharOperation.NO_STRINGS;

  public IErrorHandlingPolicy policy;
  public final IProblemFactory problemFactory;
  public final CompilerOptions options;

  /* When temporarily switching policies, store here the original root policy (for temporary resume). */
  private IErrorHandlingPolicy rootPolicy;

  protected boolean suppressTagging = false;
  /*
   * Problem handler can be supplied with a policy to specify
   * its behavior in error handling. Also see static methods for
   * built-in policies.
   */
  public ProblemHandler(IErrorHandlingPolicy policy, CompilerOptions options, IProblemFactory problemFactory) {
    this.policy = policy;
    this.problemFactory = problemFactory;
    this.options = options;
  }

  /*
   * Given the current configuration, answers which category the problem
   * falls into:
   *		Error | Warning | Ignore
   */
  public int computeSeverity(int problemId){

    return ProblemSeverities.Error; // by default all problems are errors
  }
  public CategorizedProblem createProblem(
    char[] fileName,
    int problemId,
    String[] problemArguments,
    String[] messageArguments,
    int severity,
    int problemStartPosition,
    int problemEndPosition,
    int lineNumber,
    int columnNumber) {

    return this.problemFactory.createProblem(
      fileName,
      problemId,
      problemArguments,
      messageArguments,
      severity,
      problemStartPosition,
      problemEndPosition,
      lineNumber,
      columnNumber);
  }
  public CategorizedProblem createProblem(
    char[] fileName,
    int problemId,
    String[] problemArguments,
    int elaborationId,
    String[] messageArguments,
    int severity,
    int problemStartPosition,
    int problemEndPosition,
    int lineNumber,
    int columnNumber) {
    return this.problemFactory.createProblem(
      fileName,
      problemId,
      problemArguments,
      elaborationId,
      messageArguments,
      severity,
      problemStartPosition,
      problemEndPosition,
      lineNumber,
      columnNumber);
  }
  public void handle(
    int problemId,
    String[] problemArguments,
    int elaborationId,
    String[] messageArguments,
    int severity,
    int problemStartPosition,
    int problemEndPosition,
    ReferenceContext referenceContext,
    CompilationResult unitResult) {

    if (severity == ProblemSeverities.Ignore)
      return;

    boolean mandatory = (severity & (ProblemSeverities.Error | ProblemSeverities.Optional)) == ProblemSeverities.Error;
    if ((severity & ProblemSeverities.InternalError) == 0 && this.policy.ignoreAllErrors()) {
      // Error is not to be exposed, but clients may need still notification as to whether there are silently-ignored-errors.
      // if no reference context, we need to abort from the current compilation process
      if (referenceContext == null) {
        return; // ignore non reportable problems
      }
      if (mandatory)
        referenceContext.tagAsHavingIgnoredMandatoryErrors(problemId);
      return;
    }

    if ((severity & ProblemSeverities.Optional) != 0 && problemId != IProblem.Task  && !this.options.ignoreSourceFolderWarningOption) {
      ICompilationUnit cu = unitResult.getCompilationUnit();
      try{
        if (cu != null && cu.ignoreOptionalProblems())
          return;
        // workaround for illegal implementation of ICompilationUnit, see https://bugs.eclipse.org/372351
      } catch (AbstractMethodError ex) {
        // continue
      }
    }

    // if no reference context, we need to abort from the current compilation process
    if (referenceContext == null) {
      return; // ignore non reportable problems
    }

    int[] lineEnds;
    int lineNumber = problemStartPosition >= 0
      ? Util.getLineNumber(problemStartPosition, lineEnds = unitResult.getLineSeparatorPositions(), 0, lineEnds.length-1)
      : 0;
    int columnNumber = problemStartPosition >= 0
      ? Util.searchColumnNumber(unitResult.getLineSeparatorPositions(), lineNumber, problemStartPosition)
      : 0;
    CategorizedProblem problem =
      this.createProblem(
        unitResult.getFileName(),
        problemId,
        problemArguments,
        elaborationId,
        messageArguments,
        severity,
        problemStartPosition,
        problemEndPosition,
        lineNumber,
        columnNumber);

    if (problem == null) return; // problem couldn't be created, ignore

    switch (severity & ProblemSeverities.Error) {
      case ProblemSeverities.Error :
        record(problem, unitResult, referenceContext, mandatory);
        if ((severity & ProblemSeverities.Fatal) != 0) {
          // don't abort or tag as error if the error is suppressed
          if (!referenceContext.hasErrors() && !mandatory && this.options.suppressOptionalErrors) {
            CompilationUnitDeclaration unitDecl = referenceContext.getCompilationUnitDeclaration();
            if (unitDecl != null && unitDecl.isSuppressed(problem)) {
              return;
            }
          }
          if (!this.suppressTagging || this.options.treatOptionalErrorAsFatal) {
            referenceContext.tagAsHavingErrors();
          }
          // should abort ?
          int abortLevel;
          if ((abortLevel = this.policy.stopOnFirstError() ? ProblemSeverities.AbortCompilation : severity & ProblemSeverities.Abort) != 0) {
            referenceContext.abort(abortLevel, problem);
          }
        }
        break;
      case ProblemSeverities.Warning :
        record(problem, unitResult, referenceContext, false);
        break;
    }
  }
  /**
   * Standard problem handling API, the actual severity (warning/error/ignore) is deducted
   * from the problem ID and the current compiler options.
   */
  public void handle(
    int problemId,
    String[] problemArguments,
    String[] messageArguments,
    int problemStartPosition,
    int problemEndPosition,
    ReferenceContext referenceContext,
    CompilationResult unitResult) {

    this.handle(
      problemId,
      problemArguments,
      0, // no message elaboration
      messageArguments,
      computeSeverity(problemId), // severity inferred using the ID
      problemStartPosition,
      problemEndPosition,
      referenceContext,
      unitResult);
  }
  public void record(CategorizedProblem problem, CompilationResult unitResult, ReferenceContext referenceContext, boolean mandatoryError) {
    unitResult.record(problem, referenceContext, mandatoryError);
  }
  /** @return old policy. */
  public IErrorHandlingPolicy switchErrorHandlingPolicy(IErrorHandlingPolicy newPolicy) {
    if (this.rootPolicy == null)
      this.rootPolicy = this.policy;
    IErrorHandlingPolicy presentPolicy = this.policy;
    this.policy = newPolicy;
    return presentPolicy;
  }
  /**
   * Temporarily suspend a temporary error handling policy.
   * @return old policy.
   */
  public IErrorHandlingPolicy suspendTempErrorHandlingPolicy() {
    IErrorHandlingPolicy presentPolicy = this.policy;
    if (this.rootPolicy != null)
      this.policy = this.rootPolicy;
    return presentPolicy;
  }
  /**
   * Resume from a corresponding {@link #suspendTempErrorHandlingPolicy()}.
   * @param previousPolicy the result value of the matching suspend call
   */
  public void resumeTempErrorHandlingPolicy(IErrorHandlingPolicy previousPolicy) {
    this.policy = previousPolicy;
  }
}
