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
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class BehaviorCache {

  private final SymbolicExecutionVisitor sev;
  private final SquidClassLoader classLoader;
  @VisibleForTesting
  public final Map<String, MethodBehavior> behaviors = new LinkedHashMap<>();
  private static final ConstraintsByDomain NULL_CONSTRAINTS = ConstraintsByDomain.empty().put(ObjectConstraint.NULL);
  private static final ConstraintsByDomain NOT_NULL_CONSTRAINTS = ConstraintsByDomain.empty().put(ObjectConstraint.NOT_NULL);

  public BehaviorCache(SymbolicExecutionVisitor sev, SquidClassLoader classLoader) {
    this.sev = sev;
    this.classLoader = classLoader;
  }

  public MethodBehavior methodBehaviorForSymbol(Symbol.MethodSymbol symbol) {
    return behaviors.computeIfAbsent(((JavaSymbol.MethodJavaSymbol) symbol).completeSignature(), k -> new MethodBehavior(symbol));
  }

  public MethodBehavior methodBehaviorForSymbol(String signature) {
    return behaviors.computeIfAbsent(signature, k -> new MethodBehavior(signature));
  }

  @CheckForNull
  public MethodBehavior get(Symbol.MethodSymbol symbol) {
    String signature = ((JavaSymbol.MethodJavaSymbol) symbol).completeSignature();
    return get(signature, symbol);
  }

  public MethodBehavior get(String signature) {
    return get(signature, null);
  }

  private MethodBehavior get(String signature, @Nullable Symbol.MethodSymbol symbol) {
    if (!behaviors.containsKey(signature)) {
      if (isRequireNonNullMethod(signature)) {
        behaviors.put(signature, createRequireNonNullBehavior(symbol));
      } else if (isObjectsNullMethod(signature) || isGuavaPrecondition(signature) || isCollectionUtilsIsEmpty(signature)) {
        return new BytecodeEGWalker(this).getMethodBehavior(signature, classLoader);
      } else if (isStringUtilsMethod(signature)) {
        MethodBehavior stringUtilsMethod = createStringUtilMethodBehavior(symbol);
        if (stringUtilsMethod != null) {
          behaviors.put(signature, stringUtilsMethod);
        }
      } else if (isSpringIsNull(signature)) {
        behaviors.put(signature, createRequireNullBehavior(symbol));
      } else if(symbol != null) {
        MethodTree declaration = symbol.declaration();
        if (declaration != null && SymbolicExecutionVisitor.methodCanNotBeOverriden(symbol)) {
          sev.execute(declaration);
        }
      }
    }
    return behaviors.get(signature);
  }

  private static boolean isSpringIsNull(String signature) {
    return signature.startsWith("org.springframework.util.Assert#isNull");
  }

  private static boolean isRequireNonNullMethod(String signature) {
    return isObjectsRequireNonNullMethod(signature) || isValidateMethod(signature) || isLog4jOrSpringAssertNotNull(signature);
  }

  private static boolean isValidateMethod(String signature) {
    return Stream.of(
      "org.apache.commons.lang3.Validate#notEmpty",
      "org.apache.commons.lang3.Validate#notNull",
      "org.apache.commons.lang.Validate#notEmpty",
      "org.apache.commons.lang.Validate#notNull").anyMatch(signature::startsWith);
  }

  private static boolean isCollectionUtilsIsEmpty(String signature) {
    return Stream.of(
      "org.apache.commons.collections4.CollectionUtils#isEmpty",
      "org.apache.commons.collections4.CollectionUtils#isNotEmpty",
      "org.apache.commons.collections.CollectionUtils#isEmpty",
      "org.apache.commons.collections.CollectionUtils#isNotEmpty").anyMatch(signature::startsWith);
  }

  private static boolean isGuavaPrecondition(String signature) {
    return Stream.of(
      "com.google.common.base.Preconditions#checkNotNull",
      "com.google.common.base.Preconditions#checkArgument",
      "com.google.common.base.Preconditions#checkState").anyMatch(signature::startsWith);
  }

  private static boolean isStringUtilsMethod(String signature) {
    return signature.startsWith("org.apache.commons.lang3.StringUtils") || signature.startsWith("org.apache.commons.lang.StringUtils");
  }

  private static boolean isObjectsNullMethod(String signature) {
    return signature.startsWith("java.util.Objects#nonNull") || signature.startsWith("java.util.Objects#isNull");
  }

  private static boolean isObjectsRequireNonNullMethod(String signature) {
    return signature.startsWith("java.util.Objects#requireNonNull");
  }

  private static boolean isLog4jOrSpringAssertNotNull(String signature) {
    return Stream.of(
      "org.apache.logging.log4j.core.util.Assert#requireNonNull",
      "org.springframework.util.Assert#notNull",
      "org.springframework.util.Assert#notEmpty").anyMatch(signature::startsWith);
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

}
