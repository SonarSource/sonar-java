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
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.SymbolicExecutionVisitor;
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
  private final SemanticModel semanticModel;
  @VisibleForTesting
  public final Map<String, MethodBehavior> behaviors = new LinkedHashMap<>();

  public BehaviorCache(SymbolicExecutionVisitor sev, SquidClassLoader classLoader, SemanticModel semanticModel) {
    this.sev = sev;
    this.classLoader = classLoader;
    this.semanticModel = semanticModel;
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
      if (isRequireNonNullMethod(signature)
        || isObjectsNullMethod(signature)
        || isGuavaPrecondition(signature)
        || isCollectionUtilsIsEmpty(signature)
        || isSpringIsNull(signature)
        || isStringUtilsMethod(signature)
        || isEclipseAssert(signature)
        ) {
        return new BytecodeEGWalker(this, semanticModel).getMethodBehavior(signature, symbol, classLoader);
      } else if(symbol != null) {
        MethodTree declaration = symbol.declaration();
        if (declaration != null && SymbolicExecutionVisitor.methodCanNotBeOverriden(symbol)) {
          sev.execute(declaration);
        }
      }
    }
    return behaviors.get(signature);
  }

  private static boolean isEclipseAssert(String signature) {
    return signature.startsWith("org.eclipse.core.runtime.Assert#");
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
      "org.springframework.util.CollectionUtils#isEmpty",
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
    return Stream.of("isEmpty", "isNotEmpty", "isBlank", "isNotBlank")
      .flatMap(m -> Stream.of("org.apache.commons.lang3.StringUtils#" + m, "org.apache.commons.lang.StringUtils#" + m))
      .anyMatch(signature::startsWith);
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

}
