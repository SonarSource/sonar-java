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
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.BinaryRelation;
import org.sonar.java.se.xproc.ExceptionalYield;
import org.sonar.java.se.xproc.HappyPathYield;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SymbolicExecutionVisitor extends SubscriptionVisitor {
  private static final Logger LOG = Loggers.get(SymbolicExecutionVisitor.class);

  @VisibleForTesting
  public final BehaviorCache behaviorCache = new BehaviorCache();
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
      Symbol.MethodSymbol methodSymbol = methodTree.symbol();
      ExplodedGraphWalker walker = egwFactory.createWalker(behaviorCache);
      if (methodCanNotBeOverriden(methodSymbol)) {
        MethodBehavior methodBehavior = new MethodBehavior(methodSymbol);
        behaviorCache.add(methodSymbol, methodBehavior);
        methodBehavior = walker.visitMethod(methodTree, methodBehavior);
        methodBehavior.completed();
        return methodBehavior;
      } else {
        return walker.visitMethod(methodTree);
      }
    } catch (ExplodedGraphWalker.MaximumStepsReachedException | ExplodedGraphWalker.ExplodedGraphTooBigException | BinaryRelation.TransitiveRelationExceededException exception) {
      LOG.debug("Could not complete symbolic execution: ", exception);
    }
    return null;
  }

  private static boolean methodCanNotBeOverriden(Symbol.MethodSymbol methodSymbol) {
    if ((((JavaSymbol.MethodJavaSymbol) methodSymbol).flags() & Flags.NATIVE) != 0) {
      return false;
    }
    return !methodSymbol.isAbstract() &&
      (methodSymbol.isPrivate() || methodSymbol.isFinal() || methodSymbol.isStatic() || methodSymbol.owner().isFinal());
  }

  @VisibleForTesting
  public class BehaviorCache {
    @VisibleForTesting
    public final Map<Symbol.MethodSymbol, MethodBehavior> behaviors = new LinkedHashMap<>();

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
        } else if(isGuavaPrecondition(symbol)) {
          behaviors.put(symbol, createGuavaPreconditionsBehavior(symbol, "checkNotNull".equals(symbol.name())));
        } else {
          MethodTree declaration = symbol.declaration();
          if (declaration != null && methodCanNotBeOverriden(symbol)) {
            SymbolicExecutionVisitor.this.execute(declaration);
          }
        }
      }
      return behaviors.get(symbol);
    }

    private boolean isGuavaPrecondition(Symbol.MethodSymbol symbol) {
      String name = symbol.name();
      return symbol.owner().type().is("com.google.common.base.Preconditions")
        && ("checkNotNull".equals(name) || "checkArgument".equals(name) || "checkState".equals(name) );
    }

    private boolean isStringUtilsMethod(Symbol.MethodSymbol symbol) {
      Type ownerType = symbol.owner().type();
      return ownerType.is("org.apache.commons.lang3.StringUtils") || ownerType.is("org.apache.commons.lang.StringUtils");
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
        case "isNotEmpty" :
        case "isNotBlank" :
          behavior = createIsEmptyOrBlankMethodBehavior(symbol, BooleanConstraint.FALSE);
          break;
        case "isEmpty" :
        case "isBlank" :
          behavior = createIsEmptyOrBlankMethodBehavior(symbol, BooleanConstraint.TRUE);
          break;
        default:
          behavior = null;
      }
      return behavior;
    }

    private MethodBehavior createIsEmptyOrBlankMethodBehavior(Symbol.MethodSymbol symbol, Constraint constraint) {
      MethodBehavior behavior = new MethodBehavior(symbol);
      int arity = symbol.parameterTypes().size();

      HappyPathYield nullYield = new HappyPathYield(arity, false);
      nullYield.setParameterConstraint(0, ObjectConstraint.nullConstraint());
      nullYield.setResult(-1, constraint);
      behavior.addYield(nullYield);

      HappyPathYield notNullYield = new HappyPathYield(arity, false);
      notNullYield.setParameterConstraint(0, ObjectConstraint.notNull());
      behavior.addYield(notNullYield);

      behavior.completed();
      return behavior;
    }

    /**
     * Creates method behavior for the three requireNonNull methods define in java.util.Objects
     * @param symbol the proper method symbol.
     * @return the behavior corresponding to that symbol.
     */
    private MethodBehavior createRequireNonNullBehavior(Symbol.MethodSymbol symbol) {
      MethodBehavior behavior = new MethodBehavior(symbol);
      int arity = symbol.parameterTypes().size();

      HappyPathYield happyYield = new HappyPathYield(arity, false);
      happyYield.setParameterConstraint(0, ObjectConstraint.notNull());
      happyYield.setResult(0, happyYield.parameterConstraint(0));
      behavior.addYield(happyYield);

      ExceptionalYield exceptionalYield = new ExceptionalYield(arity, false);
      exceptionalYield.setParameterConstraint(0, ObjectConstraint.nullConstraint());
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
      int arity = symbol.parameterTypes().size();

      HappyPathYield trueYield = new HappyPathYield(arity, false);
      trueYield.setParameterConstraint(0, trueConstraint);
      trueYield.setResult(-1, BooleanConstraint.TRUE);
      behavior.addYield(trueYield);

      HappyPathYield falseYield = new HappyPathYield(arity, false);
      falseYield.setParameterConstraint(0, falseConstraint);
      falseYield.setResult(-1, BooleanConstraint.FALSE);
      behavior.addYield(falseYield);

      behavior.completed();
      return behavior;
    }

    private MethodBehavior createGuavaPreconditionsBehavior(Symbol.MethodSymbol symbol, boolean isCheckNotNull) {
      MethodBehavior behavior = new MethodBehavior(symbol);

      HappyPathYield happyPathYield = new HappyPathYield(symbol.parameterTypes().size(), ((JavaSymbol.MethodJavaSymbol) symbol).isVarArgs());
      happyPathYield.setParameterConstraint(0, isCheckNotNull ? ObjectConstraint.notNull() : BooleanConstraint.TRUE);
      happyPathYield.setResult(isCheckNotNull ? 0 : -1, isCheckNotNull ? happyPathYield.parameterConstraint(0) : null);
      behavior.addYield(happyPathYield);

      behavior.completed();
      return behavior;
    }
  }


}
