/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.resolve.Flags;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.symbolicvalues.RelationalSymbolicValue;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

public class SymbolicExecutionVisitor extends SubscriptionVisitor {
  private static final Logger LOG = Loggers.get(SymbolicExecutionVisitor.class);

  @VisibleForTesting
  public final BehaviorCache behaviorCache;
  private final ExplodedGraphWalker.ExplodedGraphWalkerFactory egwFactory;

  public SymbolicExecutionVisitor(List<JavaFileScanner> executableScanners) {
    behaviorCache = new BehaviorCache(this);
    egwFactory = new ExplodedGraphWalker.ExplodedGraphWalkerFactory(executableScanners);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Lists.newArrayList(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    execute((MethodTree) tree);
  }

  public void execute(MethodTree methodTree) {
    ExplodedGraphWalker walker = egwFactory.createWalker(behaviorCache, (SemanticModel) context.getSemanticModel());
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
      | RelationalSymbolicValue.TransitiveRelationExceededException exception) {
      LOG.debug("Could not complete symbolic execution: ", exception);
      if (walker.methodBehavior != null) {
        walker.methodBehavior.visited();
      }
    }
  }

  public static boolean methodCanNotBeOverriden(Symbol.MethodSymbol methodSymbol) {
    if ((((JavaSymbol.MethodJavaSymbol) methodSymbol).flags() & Flags.NATIVE) != 0) {
      return false;
    }
    return !methodSymbol.isAbstract() &&
      (methodSymbol.isPrivate() || methodSymbol.isFinal() || methodSymbol.isStatic() || methodSymbol.owner().isFinal());
  }
}
