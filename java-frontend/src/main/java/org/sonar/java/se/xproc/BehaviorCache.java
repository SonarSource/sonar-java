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

import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.bytecode.se.BytecodeEGWalker;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodTree;

import javax.annotation.CheckForNull;
import java.util.LinkedHashMap;
import java.util.Map;

public class BehaviorCache {

  private static final String IS_NULL = "isNull";
  private final SymbolicExecutionVisitor sev;
  private final SquidClassLoader classLoader;
  @VisibleForTesting
  public final Map<Symbol.MethodSymbol, MethodBehavior> behaviors = new LinkedHashMap<>();
  private static final ConstraintsByDomain NULL_CONSTRAINTS = ConstraintsByDomain.empty().put(ObjectConstraint.NULL);
  private static final ConstraintsByDomain NOT_NULL_CONSTRAINTS = ConstraintsByDomain.empty().put(ObjectConstraint.NOT_NULL);

  public BehaviorCache(SymbolicExecutionVisitor sev) {
    this(sev, null);
  }
  public BehaviorCache(SymbolicExecutionVisitor sev, SquidClassLoader classLoader) {
    this.sev = sev;
    this.classLoader = classLoader;
  }

  public MethodBehavior methodBehaviorForSymbol(Symbol.MethodSymbol symbol) {
    return behaviors.computeIfAbsent(symbol, MethodBehavior::new);
  }

  @CheckForNull
  public MethodBehavior get(Symbol.MethodSymbol symbol) {
    if (!behaviors.containsKey(symbol)) {
      if (isRequireNonNullMethod(symbol)) {
        behaviors.put(symbol, createRequireNonNullBehavior(symbol));
      } else if (isObjectsNullMethod(symbol) || isGuavaPrecondition(symbol)) {
        return new BytecodeEGWalker(this).getMethodBehavior(symbol, classLoader);
      } else if (isStringUtilsMethod(symbol)) {
        MethodBehavior stringUtilsMethod = createStringUtilMethodBehavior(symbol);
        if (stringUtilsMethod != null) {
          behaviors.put(symbol, stringUtilsMethod);
        }
      } else if (isCollectionUtilsIsEmpty(symbol)) {
        behaviors.put(symbol, createCollectionUtilsBehavior(symbol));
      } else if (isSpringIsNull(symbol)) {
        behaviors.put(symbol, createRequireNullBehavior(symbol));
      } else {
        MethodTree declaration = symbol.declaration();
        if (declaration != null && SymbolicExecutionVisitor.methodCanNotBeOverriden(symbol)) {
          sev.execute(declaration);
        }
      }
    }
    return behaviors.get(symbol);
  }

  private static boolean isSpringIsNull(Symbol.MethodSymbol symbol) {
    return symbol.owner().type().is("org.springframework.util.Assert") && IS_NULL.equals(symbol.name());
  }

  private static boolean isRequireNonNullMethod(Symbol.MethodSymbol symbol) {
    return isObjectsRequireNonNullMethod(symbol) || isValidateMethod(symbol) || isLog4jOrSpringAssertNotNull(symbol);
  }

  private static boolean isValidateMethod(Symbol.MethodSymbol symbol) {
    Type type = symbol.owner().type();
    String name = symbol.name();
    return (type.is("org.apache.commons.lang3.Validate") || type.is("org.apache.commons.lang.Validate"))
      && ("notEmpty".equals(name) || "notNull".equals(name));
  }

  private static boolean isCollectionUtilsIsEmpty(Symbol.MethodSymbol symbol) {
    Type type = symbol.owner().type();
    return (type.is("org.apache.commons.collections4.CollectionUtils") || type.is("org.apache.commons.collections.CollectionUtils"))
      && ("isEmpty".equals(symbol.name()) || "isNotEmpty".equals(symbol.name()));
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
    return symbol.owner().type().is("java.util.Objects") && ("nonNull".equals(symbol.name()) || IS_NULL.equals(symbol.name()));
  }

  private static boolean isObjectsRequireNonNullMethod(Symbol symbol) {
    return symbol.owner().type().is("java.util.Objects") && "requireNonNull".equals(symbol.name());
  }

  private static boolean isLog4jOrSpringAssertNotNull(Symbol symbol) {
    Type ownerType = symbol.owner().type();
    return (ownerType.is("org.apache.logging.log4j.core.util.Assert") && "requireNonNull".equals(symbol.name()))
      || (ownerType.is("org.springframework.util.Assert") && ("notNull".equals(symbol.name()) || "notEmpty".equals(symbol.name())));
  }

  @CheckForNull
  private static MethodBehavior createStringUtilMethodBehavior(Symbol.MethodSymbol symbol) {
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

  private static MethodBehavior createIsEmptyOrBlankMethodBehavior(Symbol.MethodSymbol symbol, Constraint constraint) {
    MethodBehavior behavior = new MethodBehavior(symbol);
    HappyPathYield nullYield = new HappyPathYield(behavior);
    nullYield.parametersConstraints.add(NULL_CONSTRAINTS);
    nullYield.setResult(-1, ConstraintsByDomain.empty().put(constraint));
    behavior.addYield(nullYield);
    MethodYield notNullYield = new HappyPathYield(behavior);
    notNullYield.parametersConstraints.add(NOT_NULL_CONSTRAINTS);
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
    return createRequireNullnessBehavior(symbol, false);
  }

  private static MethodBehavior createRequireNullBehavior(Symbol.MethodSymbol symbol) {
    return createRequireNullnessBehavior(symbol, true);
  }

  private static MethodBehavior createRequireNullnessBehavior(Symbol.MethodSymbol symbol, boolean mustBeNull) {
    MethodBehavior behavior = new MethodBehavior(symbol);
    HappyPathYield happyYield = new HappyPathYield(behavior);
    happyYield.parametersConstraints.add(mustBeNull ? NULL_CONSTRAINTS : NOT_NULL_CONSTRAINTS);
    for (int i = 1; i < symbol.parameterTypes().size(); i++) {
      happyYield.parametersConstraints.add(ConstraintsByDomain.empty());
    }
    happyYield.setResult(0, happyYield.parametersConstraints.get(0));
    behavior.addYield(happyYield);

    ExceptionalYield exceptionalYield = new ExceptionalYield(behavior);
    exceptionalYield.parametersConstraints.add(mustBeNull ? NOT_NULL_CONSTRAINTS : NULL_CONSTRAINTS);
    for (int i = 1; i < symbol.parameterTypes().size(); i++) {
      exceptionalYield.parametersConstraints.add(ConstraintsByDomain.empty());
    }
    behavior.addYield(exceptionalYield);

    behavior.completed();
    return behavior;
  }

  private static MethodBehavior createCollectionUtilsBehavior(Symbol.MethodSymbol symbol) {
    MethodBehavior behavior = new MethodBehavior(symbol);

    HappyPathYield happyPathYield = new HappyPathYield(behavior);
    happyPathYield.parametersConstraints.add(NULL_CONSTRAINTS);
    BooleanConstraint constraint = symbol.name().contains("Not") ? BooleanConstraint.FALSE : BooleanConstraint.TRUE;
    happyPathYield.setResult(-1, ConstraintsByDomain.empty().put(constraint));
    behavior.addYield(happyPathYield);

    happyPathYield = new HappyPathYield(behavior);
    happyPathYield.parametersConstraints.add(NOT_NULL_CONSTRAINTS);
    happyPathYield.setResult(-1, ConstraintsByDomain.empty());
    behavior.addYield(happyPathYield);

    behavior.completed();
    return behavior;
  }

}
