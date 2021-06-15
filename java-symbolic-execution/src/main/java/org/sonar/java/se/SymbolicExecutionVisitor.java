/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.se;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.model.JUtils;
import org.sonar.java.model.Sema;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

public class SymbolicExecutionVisitor extends SubscriptionVisitor {
  private static final Logger LOG = Loggers.get(SymbolicExecutionVisitor.class);

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
    super.scanFile(context);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    execute((MethodTree) tree);
  }

  public void execute(MethodTree methodTree) {
    ExplodedGraphWalker walker = getWalker();
    try {
      Symbol.MethodSymbol methodSymbol = methodTree.symbol();
      if (methodCanNotBeOverriden(methodSymbol)) {
        MethodBehavior methodBehavior = behaviorCache.methodBehaviorForSymbol(methodSymbol);
        if (!methodBehavior.isVisited()) {
          methodBehavior = walker.visitMethod(methodTree, methodBehavior);
          methodBehavior.completed();
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
    return egwFactory.createWalker(behaviorCache, (Sema) context.getSemanticModel());
  }

  public static boolean methodCanNotBeOverriden(Symbol.MethodSymbol methodSymbol) {
    if (JUtils.isNativeMethod(methodSymbol)) {
      return false;
    }
    return !methodSymbol.isAbstract() &&
      (methodSymbol.isPrivate() || methodSymbol.isFinal() || methodSymbol.isStatic() || methodSymbol.owner().isFinal());
  }
}
