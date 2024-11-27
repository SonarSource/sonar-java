/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.se;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;

public class SymbolicExecutionVisitor extends BaseTreeVisitor implements JavaFileScanner {

  private static final Logger LOG = LoggerFactory.getLogger(SymbolicExecutionVisitor.class);
  protected JavaFileScannerContext context;

  @VisibleForTesting
  public final BehaviorCache behaviorCache;
  private final ExplodedGraphWalker.ExplodedGraphWalkerFactory egwFactory;

  public SymbolicExecutionVisitor(List<SECheck> seChecks) {
    egwFactory = new ExplodedGraphWalker.ExplodedGraphWalkerFactory(seChecks);
    this.behaviorCache = new BehaviorCache();
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    behaviorCache.cleanup();
    behaviorCache.setFileContext(this);
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitMethod(MethodTree tree) {
    execute(tree);
    super.visitMethod(tree);
  }

  public void execute(MethodTree methodTree) {
    ExplodedGraphWalker walker = getWalker();
    try {
      Symbol.MethodSymbol methodSymbol = methodTree.symbol();
      if (methodCanNotBeOverridden(methodSymbol)) {
        MethodBehavior methodBehavior = behaviorCache.methodBehaviorForSymbol(methodSymbol);
        if (!methodBehavior.isVisited()) {
          walker.visitMethod(methodTree, methodBehavior);
        }
      } else {
        walker.visitMethod(methodTree);
      }
    } catch (ExplodedGraphWalker.MaximumStepsReachedException
      | ExplodedGraphWalker.ExplodedGraphTooBigException
      | ExplodedGraphWalker.MaximumStartingStatesException exception) {
      LOG.debug("Could not complete symbolic execution: {}", exception.getMessage());
      if (LOG.isTraceEnabled()) {
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        LOG.trace(sw.toString());
      }
      if (walker.methodBehavior != null) {
        walker.methodBehavior.visited();
      }
    }
  }

  @VisibleForTesting
  protected ExplodedGraphWalker getWalker() {
    return egwFactory.createWalker(behaviorCache, context);
  }

  public static boolean methodCanNotBeOverridden(Symbol.MethodSymbol methodSymbol) {
    if (methodSymbol.isNativeMethod()) {
      return false;
    }
    return !methodSymbol.isAbstract() &&
      (methodSymbol.isPrivate() || methodSymbol.isFinal() || methodSymbol.isStatic() || methodSymbol.owner().isFinal());
  }

}
