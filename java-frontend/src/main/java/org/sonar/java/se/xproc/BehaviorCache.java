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
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.bytecode.se.BytecodeEGWalker;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodTree;

public class BehaviorCache {

  private final SquidClassLoader classLoader;
  private final boolean crossFileEnabled;
  private  SymbolicExecutionVisitor sev;
  private  SemanticModel semanticModel;
  @VisibleForTesting
  public final Map<String, MethodBehavior> behaviors = new LinkedHashMap<>();
  private final Map<String, MethodBehavior> bytecodeBehaviors = new LinkedHashMap<>();

  // methods known to be well covered using bytecode-generated behavior
  private static final Set<String> WHITELIST = ImmutableSet.of(
    "java.util.Objects#requireNonNull",
    "org.apache.commons.lang3.Validate#notEmpty",
    "org.apache.commons.lang3.Validate#notNull",
    "org.apache.commons.lang.Validate#notEmpty",
    "org.apache.commons.lang.Validate#notNull",
    "org.apache.logging.log4j.core.util.Assert#requireNonNull",
    "org.springframework.util.Assert#notNull",
    "org.springframework.util.Assert#notEmpty",
    "java.util.Objects#nonNull",
    "java.util.Objects#isNull",
    "com.google.common.base.Preconditions#checkNotNull",
    "com.google.common.base.Preconditions#checkArgument",
    "com.google.common.base.Preconditions#checkState",
    "org.springframework.util.CollectionUtils#isEmpty",
    "org.apache.commons.collections4.CollectionUtils#isEmpty",
    "org.apache.commons.collections4.CollectionUtils#isNotEmpty",
    "org.apache.commons.collections.CollectionUtils#isEmpty",
    "org.apache.commons.collections.CollectionUtils#isNotEmpty",
    "org.springframework.util.Assert#isNull",
    "org.apache.commons.lang3.StringUtils#isEmpty",
    "org.apache.commons.lang3.StringUtils#isNotEmpty",
    "org.apache.commons.lang3.StringUtils#isBlank",
    "org.apache.commons.lang3.StringUtils#isNotBlank",
    "org.apache.commons.lang.StringUtils#isEmpty",
    "org.apache.commons.lang.StringUtils#isNotEmpty",
    "org.apache.commons.lang.StringUtils#isBlank",
    "org.apache.commons.lang.StringUtils#isNotBlank",
    "org.eclipse.core.runtime.Assert#");

  public BehaviorCache(SquidClassLoader classLoader) {
    this(classLoader, true);
  }

  public BehaviorCache(SquidClassLoader classLoader, boolean crossFileEnabled) {
    this.classLoader = classLoader;
    this.crossFileEnabled = crossFileEnabled;
  }

  public void setFileContext(@Nullable SymbolicExecutionVisitor sev,@Nullable SemanticModel semanticModel) {
    this.sev = sev;
    this.semanticModel = semanticModel;
  }

  public void cleanup() {
    behaviors.clear();
  }

  public MethodBehavior methodBehaviorForSymbol(Symbol.MethodSymbol symbol) {
    String signature = ((JavaSymbol.MethodJavaSymbol) symbol).completeSignature();
    boolean varArgs = ((JavaSymbol.MethodJavaSymbol) symbol).isVarArgs();
    return behaviors.computeIfAbsent(signature, k -> new MethodBehavior(signature, varArgs));
  }

  public MethodBehavior methodBehaviorForSymbol(String signature) {
    return bytecodeBehaviors.computeIfAbsent(signature, k -> new MethodBehavior(signature));
  }

  @CheckForNull
  public MethodBehavior get(Symbol.MethodSymbol symbol) {
    String signature = ((JavaSymbol.MethodJavaSymbol) symbol).completeSignature();
    return get(signature, symbol);
  }

  @CheckForNull
  public MethodBehavior get(String signature) {
    return get(signature, null);
  }

  @CheckForNull
  private MethodBehavior get(String signature, @Nullable Symbol.MethodSymbol symbol) {
    MethodBehavior mb = behaviors.get(signature);
    if(mb != null) {
      return mb;
    }
    if (symbol != null) {
      MethodTree declaration = symbol.declaration();
      if (SymbolicExecutionVisitor.methodCanNotBeOverriden(symbol) && declaration != null) {
        sev.execute(declaration);
        return behaviors.get(signature);
      }
    }

    // disabled x-file analysis, behavior based on source code can still be used
    if (!crossFileEnabled && !isKnownSignature(signature)) {
      return null;
    }

    if (!bytecodeBehaviors.containsKey(signature)) {
      new BytecodeEGWalker(this, semanticModel).getMethodBehavior(signature, classLoader);
    }
    return bytecodeBehaviors.get(signature);
  }

  private static boolean isKnownSignature(String signature) {
    return WHITELIST.stream().anyMatch(signature::startsWith);
  }

}
