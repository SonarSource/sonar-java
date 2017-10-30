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
  private  SymbolicExecutionVisitor sev;
  private  SemanticModel semanticModel;
  @VisibleForTesting
  public final Map<String, MethodBehavior> behaviors = new LinkedHashMap<>();
  private final Map<String, MethodBehavior> bytecodeBehaviors = new LinkedHashMap<>();

  private static final Set<String> SIGNATURE_BLACKLIST = ImmutableSet.of("java.lang.Class#", "java.lang.Object#wait", "java.util.Optional#");

  public BehaviorCache(SquidClassLoader classLoader) {
    this.classLoader = classLoader;
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
    if (SIGNATURE_BLACKLIST.stream().anyMatch(signature::startsWith)) {
      return null;
    }
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

    if (!bytecodeBehaviors.containsKey(signature)) {
      new BytecodeEGWalker(this, semanticModel).getMethodBehavior(signature, classLoader);
    }
    return bytecodeBehaviors.get(signature);
  }
}
