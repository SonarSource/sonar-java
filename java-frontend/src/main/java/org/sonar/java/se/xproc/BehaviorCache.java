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
package org.sonar.java.se.xproc;

import com.google.common.annotations.VisibleForTesting;

import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;

import javax.annotation.CheckForNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class BehaviorCache {

  private final SymbolicExecutionVisitor sev;
  @VisibleForTesting
  public final Map<Symbol.MethodSymbol, MethodBehavior> behaviors = new LinkedHashMap<>();

  public BehaviorCache(SymbolicExecutionVisitor sev) {
    this.sev = sev;
  }

  public void add(Symbol.MethodSymbol symbol, MethodBehavior behavior) {
    behaviors.put(symbol, behavior);
  }

  @CheckForNull
  public MethodBehavior get(Symbol.MethodSymbol symbol) {
    if (!behaviors.containsKey(symbol)) {
      if (isObjectsRequireNonNullMethod(symbol)) {
        behaviors.put(symbol, createRequireNonNullBehavior(symbol));
      } else if (isObjectsNullMethod(symbol)) {
        behaviors.put(symbol, createIsNullBehavior(symbol));
      } else if (isStringUtilsMethod(symbol)) {
        MethodBehavior stringUtilsMethod = createStringUtilMethodBehavior(symbol);
        if (stringUtilsMethod != null) {
          behaviors.put(symbol, stringUtilsMethod);
        }
      } else if (isGuavaPrecondition(symbol)) {
        behaviors.put(symbol, createGuavaPreconditionsBehavior(symbol, "checkNotNull".equals(symbol.name())));
      } else {
        MethodTree declaration = symbol.declaration();
        if (declaration != null && SymbolicExecutionVisitor.methodCanNotBeOverriden(symbol)) {
          sev.execute(declaration);
        }
      }
    }
    return behaviors.get(symbol);
  }

  private static boolean isGuavaPrecondition(Symbol.MethodSymbol symbol) {
    String name = symbol.name();
    return symbol.owner().type().is("com.google.common.base.Preconditions")
      && ("checkNotNull".equals(name) || "checkArgument".equals(name) || "checkState".equals(name));
  }

  private static boolean isStringUtilsMethod(Symbol.MethodSymbol symbol) {
    Type ownerType = symbol.owner().type();
    return ownerType.is("org.apache.commons.lang3.StringUtils") || ownerType.is("org.apache.commons.lang.StringUtils");
  }

  private static boolean isObjectsNullMethod(Symbol.MethodSymbol symbol) {
    return symbol.owner().type().is("java.util.Objects") && ("nonNull".equals(symbol.name()) || "isNull".equals(symbol.name()));
  }

  private static boolean isObjectsRequireNonNullMethod(Symbol symbol) {
    return symbol.owner().type().is("java.util.Objects") && "requireNonNull".equals(symbol.name());
  }

  @CheckForNull
  private static MethodBehavior createStringUtilMethodBehavior(Symbol.MethodSymbol symbol) {
    MethodBehavior behavior;
    switch (symbol.name()) {
      case "isNotEmpty":
      case "isNotBlank":
        behavior = createIsEmptyOrBlankMethodBehavior(symbol, BooleanConstraint.FALSE);
        break;
      case "isEmpty":
      case "isBlank":
        behavior = createIsEmptyOrBlankMethodBehavior(symbol, BooleanConstraint.TRUE);
        break;
      default:
        behavior = null;
    }
    return behavior;
  }

  private static MethodBehavior createIsEmptyOrBlankMethodBehavior(Symbol.MethodSymbol symbol, Constraint constraint) {
    MethodBehavior behavior = new MethodBehavior(symbol);

    HappyPathYield nullYield = new HappyPathYield(behavior);
    nullYield.setParameterConstraint(0, ObjectConstraint.nullConstraint());
    nullYield.setResult(-1, constraint);
    behavior.addYield(nullYield);

    HappyPathYield notNullYield = new HappyPathYield(behavior);
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
  private static MethodBehavior createRequireNonNullBehavior(Symbol.MethodSymbol symbol) {
    MethodBehavior behavior = new MethodBehavior(symbol);

    HappyPathYield happyYield = new HappyPathYield(behavior);
    happyYield.setParameterConstraint(0, ObjectConstraint.notNull());
    happyYield.setResult(0, happyYield.parameterConstraint(0));
    behavior.addYield(happyYield);

    ExceptionalYield exceptionalYield = new ExceptionalYield(behavior);
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
  private static MethodBehavior createIsNullBehavior(Symbol.MethodSymbol symbol) {
    boolean isNull = "isNull".equals(symbol.name());
    ObjectConstraint<ObjectConstraint.Status> trueConstraint = isNull ? ObjectConstraint.nullConstraint() : ObjectConstraint.notNull();
    ObjectConstraint<ObjectConstraint.Status> falseConstraint = isNull ? ObjectConstraint.notNull() : ObjectConstraint.nullConstraint();

    MethodBehavior behavior = new MethodBehavior(symbol);

    HappyPathYield trueYield = new HappyPathYield(behavior);
    trueYield.setParameterConstraint(0, trueConstraint);
    trueYield.setResult(-1, BooleanConstraint.TRUE);
    behavior.addYield(trueYield);

    HappyPathYield falseYield = new HappyPathYield(behavior);
    falseYield.setParameterConstraint(0, falseConstraint);
    falseYield.setResult(-1, BooleanConstraint.FALSE);
    behavior.addYield(falseYield);

    behavior.completed();
    return behavior;
  }

  private static MethodBehavior createGuavaPreconditionsBehavior(Symbol.MethodSymbol symbol, boolean isCheckNotNull) {
    MethodBehavior behavior = new MethodBehavior(symbol);

    HappyPathYield happyPathYield = new HappyPathYield(behavior);
    happyPathYield.setParameterConstraint(0, isCheckNotNull ? ObjectConstraint.notNull() : BooleanConstraint.TRUE);
    happyPathYield.setResult(isCheckNotNull ? 0 : -1, isCheckNotNull ? happyPathYield.parameterConstraint(0) : null);
    behavior.addYield(happyPathYield);

    behavior.completed();
    return behavior;
  }
}
