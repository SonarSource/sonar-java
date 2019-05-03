/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.resolve;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import javax.annotation.CheckForNull;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SemanticModel {

  private final Map<Tree, Symbol> symbolsTree = new HashMap<>();

  private final Map<Symbol, Resolve.Env> symbolEnvs = new HashMap<>();
  private final BiMap<Tree, Resolve.Env> envs = HashBiMap.create();
  private final BytecodeCompleter bytecodeCompleter;

  @VisibleForTesting
  SemanticModel(BytecodeCompleter bytecodeCompleter) {
    this.bytecodeCompleter = bytecodeCompleter;
  }

  public static SemanticModel createFor(CompilationUnitTree tree, SquidClassLoader classLoader) {
    ParametrizedTypeCache parametrizedTypeCache = new ParametrizedTypeCache();
    BytecodeCompleter bytecodeCompleter = new BytecodeCompleter(classLoader, parametrizedTypeCache);
    Symbols symbols = new Symbols(bytecodeCompleter);
    SemanticModel semanticModel = new SemanticModel(bytecodeCompleter);
    try {
      Resolve resolve = new Resolve(symbols, bytecodeCompleter, parametrizedTypeCache);
      TypeAndReferenceSolver typeAndReferenceSolver = new TypeAndReferenceSolver(semanticModel, symbols, resolve, parametrizedTypeCache);
      new FirstPass(semanticModel, symbols, resolve, parametrizedTypeCache, typeAndReferenceSolver).visitCompilationUnit(tree);
      typeAndReferenceSolver.visitCompilationUnit(tree);
      new LabelsVisitor(semanticModel).visitCompilationUnit(tree);
    } finally {
      handleMissingTypes(tree);
    }
    return semanticModel;
  }

  /**
   * Handles missing types in Syntax Tree to prevent NPE in subsequent steps of analysis.
   */
  public static void handleMissingTypes(Tree tree) {
    // (Godin): Another and probably better (safer) way to do the same - is to assign default value during creation of nodes, so that to guarantee that this step won't be skipped.
    new MissingTypeHandler().handleMissingTypes(tree);
  }


  private static class MissingTypeHandler {
    private Deque<JavaTree> parentList = new LinkedList<>();

    void handleMissingTypes(Tree tree) {
      parentList.push((JavaTree) tree);
      while (!parentList.isEmpty()) {
        JavaTree parent = parentList.pop();
        if (parent instanceof AbstractTypedTree) {
          ((AbstractTypedTree) parent).completeMissingType();
        }
        if (!parent.isLeaf()) {
          for (Tree nextTree : parent.getChildren()) {
            JavaTree next = (JavaTree) nextTree;
            if (next != null) {
              next.setParent(parent);
              parentList.push(next);
            }
          }
        }
      }
    }
  }

  public void saveEnv(Symbol symbol, Resolve.Env env) {
    symbolEnvs.put(symbol, env);
  }

  public Resolve.Env getEnv(Symbol symbol) {
    return symbolEnvs.get(symbol);
  }

  public void associateEnv(Tree tree, Resolve.Env env) {
    envs.put(tree, env);
  }

  public Tree getTree(Resolve.Env env) {
    return envs.inverse().get(env);
  }

  public Resolve.Env getEnv(Tree tree) {
    Resolve.Env result = null;
    Tree node = tree;
    while (result == null && node != null) {
      result = envs.get(node);
      node = node.parent();
    }
    return result;
  }

  public Symbol getEnclosingClass(Tree tree) {
    return getEnv(tree).enclosingClass;
  }

  public Type getClassType(String fullyQualifiedName) {
    return bytecodeCompleter.loadClass(fullyQualifiedName).type();
  }

  public void associateSymbol(Tree tree, Symbol symbol) {
    Objects.requireNonNull(symbol);
    symbolsTree.put(tree, symbol);
  }

  @Nullable
  public Symbol getSymbol(Tree tree) {
    return symbolsTree.get(tree);
  }

  @VisibleForTesting
  Map<Tree, Symbol> getSymbolsTree() {
    return Collections.unmodifiableMap(symbolsTree);
  }

  public Set<String> classesNotFound() {
    return bytecodeCompleter.classesNotFound();
  }

  @CheckForNull
  public Object constantValue(Symbol owner, String constantName) {
    return bytecodeCompleter.constantValue(owner, constantName);
  }
}
