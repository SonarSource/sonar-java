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
import org.sonar.java.se.constraint.ObjectConstraint;
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
  final BehaviorCache behaviorCache = new BehaviorCache();
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
      MethodBehavior methodBehavior = new MethodBehavior(methodTree.symbol());
      behaviorCache.add(methodTree.symbol(), methodBehavior);
      ExplodedGraphWalker walker = egwFactory.createWalker(behaviorCache);
      methodBehavior = walker.visitMethod(methodTree, methodBehavior);
      methodBehavior.completed();
      return methodBehavior;
    } catch (ExplodedGraphWalker.MaximumStepsReachedException | ExplodedGraphWalker.ExplodedGraphTooBigException | BinaryRelation.TransitiveRelationExceededException exception) {
      LOG.debug("Could not complete symbolic execution: ", exception);
    }
    return null;
  }

  class BehaviorCache {
    final Map<Symbol.MethodSymbol, MethodBehavior> behaviors = new LinkedHashMap<>();

    void add(Symbol.MethodSymbol symbol, MethodBehavior behavior) {
      behaviors.put(symbol, behavior);
    }

    @CheckForNull
    public MethodBehavior get(Symbol.MethodSymbol symbol) {
      if (!behaviors.containsKey(symbol)) {
        if (isObjectsRequireNonNullMethod(symbol)) {
          behaviors.put(symbol, createRequireNonNullBehavior(symbol));
        } else {
          MethodTree declaration = symbol.declaration();
          if (declaration != null) {
            SymbolicExecutionVisitor.this.execute(declaration);
          }
        }
      }
      return behaviors.get(symbol);
    }

    private boolean isObjectsRequireNonNullMethod(Symbol symbol) {
      return symbol.owner().type().is("java.util.Objects") && "requireNonNull".equals(symbol.name());
    }

    /**
     * Creates method behavior for the three requireNonNull methods define in java.util.Objects
     * @param symbol the proper method symbol.
     * @return the behavior corresponding to that symbol.
     */
    private MethodBehavior createRequireNonNullBehavior(Symbol.MethodSymbol symbol) {
      MethodBehavior behavior = new MethodBehavior(symbol);
      MethodYield happyYield = new MethodYield(symbol.parameterTypes().size(), false);
      happyYield.exception = false;
      happyYield.parametersConstraints[0] = ObjectConstraint.notNull();
      happyYield.resultIndex = 0;
      happyYield.resultConstraint = happyYield.parametersConstraints[0];
      behavior.addYield(happyYield);

      MethodYield exceptionalYield = new MethodYield(symbol.parameterTypes().size(), false);
      exceptionalYield.exception = true;
      exceptionalYield.parametersConstraints[0] = ObjectConstraint.nullConstraint();
      behavior.addYield(exceptionalYield);

      behavior.completed();
      return behavior;
    }
  }


}
