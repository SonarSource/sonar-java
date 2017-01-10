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
import org.sonar.java.se.symbolicvalues.BinaryRelation;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SymbolicExecutionVisitor extends SubscriptionVisitor {
  private static final Logger LOG = Loggers.get(SymbolicExecutionVisitor.class);

  @VisibleForTesting
  final Map<Symbol.MethodSymbol, MethodBehavior> behaviorCache = new LinkedHashMap<>();

  private final ExplodedGraphWalker.ExplodedGraphWalkerFactory egwFactory;

  public SymbolicExecutionVisitor(List<JavaFileScanner> executableScanners) {
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

  @CheckForNull
  public MethodBehavior execute(MethodTree methodTree) {
    try {
      MethodBehavior methodBehavior = behaviorCache.get(methodTree.symbol());
      if(methodBehavior == null) {
        methodBehavior = new MethodBehavior(methodTree.symbol());
        behaviorCache.put(methodTree.symbol(), methodBehavior);
        ExplodedGraphWalker walker = egwFactory.createWalker(this);
        methodBehavior = walker.visitMethod(methodTree, methodBehavior);
        methodBehavior.completed();
      }
      return methodBehavior;
    } catch (ExplodedGraphWalker.MaximumStepsReachedException | ExplodedGraphWalker.ExplodedGraphTooBigException | BinaryRelation.TransitiveRelationExceededException exception) {
      LOG.debug("Could not complete symbolic execution: ", exception);
    }
    return null;
  }


}
