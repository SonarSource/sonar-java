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
import org.sonar.java.se.constraint.BooleanConstraint;
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
        } else if(isObjectsNullMethod(symbol)) {
          behaviors.put(symbol, createIsNullBehavior(symbol));
        } else if(isStringUtilsMethod(symbol)) {
          MethodBehavior stringUtilsMethod = createStringUtilMethodBehavior(symbol);
          if(stringUtilsMethod != null) {
            behaviors.put(symbol, stringUtilsMethod);
          }
        } else {
          MethodTree declaration = symbol.declaration();
          if (declaration != null) {
            SymbolicExecutionVisitor.this.execute(declaration);
          }
        }
      }
      return behaviors.get(symbol);
    }

    private boolean isStringUtilsMethod(Symbol.MethodSymbol symbol) {
      return symbol.owner().type().is("org.apache.commons.lang3.StringUtils");
    }

    private boolean isObjectsNullMethod(Symbol.MethodSymbol symbol) {
      return symbol.owner().type().is("java.util.Objects") && ("nonNull".equals(symbol.name()) || "isNull".equals(symbol.name()));
    }

    private boolean isObjectsRequireNonNullMethod(Symbol symbol) {
      return symbol.owner().type().is("java.util.Objects") && "requireNonNull".equals(symbol.name());
    }

    @CheckForNull
    private MethodBehavior createStringUtilMethodBehavior(Symbol.MethodSymbol symbol) {
      MethodBehavior behavior;
      switch (symbol.name()) {
        case "isEmpty" :
        case "isBlank" :
        case "isNotEmpty" :
        case "isNotBlank" :
          behavior = new MethodBehavior(symbol);
          MethodYield nullYield = new MethodYield(symbol.parameterTypes().size(), false);
          nullYield.exception = false;
          nullYield.parametersConstraints[0] = ObjectConstraint.nullConstraint();
          nullYield.resultConstraint = symbol.name().contains("Not") ? BooleanConstraint.FALSE : BooleanConstraint.TRUE;
          behavior.addYield(nullYield);
          MethodYield notNullYield = new MethodYield(symbol.parameterTypes().size(), false);
          notNullYield.exception = false;
          notNullYield.parametersConstraints[0] = ObjectConstraint.notNull();
          behavior.addYield(notNullYield);
          behavior.completed();
          break;
        default:
          behavior = null;
      }
      return behavior;
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

    /**
     * Create behavior for java.util.Objects.isNull and nonNull methods
     * @param symbol the symbol of the associated method.
     * @return the behavior corresponding to the symbol passed as parameter.
     */
    private MethodBehavior createIsNullBehavior(Symbol.MethodSymbol symbol) {
      boolean isNull = "isNull".equals(symbol.name());
      ObjectConstraint<ObjectConstraint.Status> trueConstraint = isNull ? ObjectConstraint.nullConstraint() : ObjectConstraint.notNull();
      ObjectConstraint<ObjectConstraint.Status> falseConstraint = isNull ? ObjectConstraint.notNull() : ObjectConstraint.nullConstraint();

      MethodBehavior behavior = new MethodBehavior(symbol);

      MethodYield trueYield = new MethodYield(symbol.parameterTypes().size(), false);
      trueYield.exception = false;
      trueYield.parametersConstraints[0] = trueConstraint;
      trueYield.resultConstraint = BooleanConstraint.TRUE;
      behavior.addYield(trueYield);

      MethodYield falseYield = new MethodYield(symbol.parameterTypes().size(), false);
      falseYield.exception = false;
      falseYield.parametersConstraints[0] = falseConstraint;
      falseYield.resultConstraint = BooleanConstraint.FALSE;
      behavior.addYield(falseYield);

      behavior.completed();
      return behavior;
    }
  }


}
