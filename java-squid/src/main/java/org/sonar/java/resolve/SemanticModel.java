/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.resolve;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SemanticModel {

  private final BiMap<Tree, Symbol> symbolsTree = HashBiMap.create();
  private Multimap<Symbol, IdentifierTree> usagesTree = HashMultimap.create();

  private final Map<Symbol, Resolve.Env> symbolEnvs = Maps.newHashMap();
  private final BiMap<Tree, Resolve.Env> envs = HashBiMap.create();
  private final Map<Tree, Tree> parentLink = Maps.newHashMap();
  private BytecodeCompleter bytecodeCompleter;

  public static SemanticModel createFor(CompilationUnitTree tree, List<File> projectClasspath) {
    ParametrizedTypeCache parametrizedTypeCache = new ParametrizedTypeCache();
    BytecodeCompleter bytecodeCompleter = new BytecodeCompleter(projectClasspath, parametrizedTypeCache);
    Symbols symbols = new Symbols(bytecodeCompleter);
    SemanticModel semanticModel = new SemanticModel();
    semanticModel.bytecodeCompleter = bytecodeCompleter;
    semanticModel.createParentLink((JavaTree) tree);
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

  public void done(){
    bytecodeCompleter.done();
  }

  /**
   * Handles missing types in Syntax Tree to prevent NPE in subsequent steps of analysis.
   */
  public static void handleMissingTypes(Tree tree) {
    // (Godin): Another and probably better (safer) way to do the same - is to assign default value during creation of nodes, so that to guarantee that this step won't be skipped.
    tree.accept(new BaseTreeVisitor() {

      @Override
      protected void scan(@Nullable Tree tree) {
        if (tree instanceof AbstractTypedTree) {
          AbstractTypedTree typedNode = (AbstractTypedTree) tree;
          if (!typedNode.isTypeSet()) {
            typedNode.setType(Symbols.unknownType);
          }
        }
        super.scan(tree);
      }

      @Override
      protected void scan(ListTree<? extends Tree> listTree) {
        if (listTree != null) {
          scan((List<? extends Tree>) listTree);
        }
      }
    });
  }

  @VisibleForTesting
  SemanticModel() {
  }

  private void createParentLink(JavaTree tree) {
    if (!tree.isLeaf()) {
      for (Iterator<Tree> iter = tree.childrenIterator(); iter.hasNext(); ) {
        Tree next = iter.next();
        if (next != null) {
          parentLink.put(next, tree);
          createParentLink((JavaTree) next);
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
    JavaTree javaTree = (JavaTree) tree;
    Resolve.Env result = null;
    while (result == null && javaTree != null) {
      result = envs.get(javaTree);
      javaTree = (JavaTree) parentLink.get(javaTree);
    }
    return result;
  }

  public Symbol getEnclosingClass(Tree tree) {
    return getEnv(tree).enclosingClass;
  }

  public void associateSymbol(Tree tree, Symbol symbol) {
    Preconditions.checkNotNull(symbol);
    symbolsTree.put(tree, symbol);
  }

  @Nullable
  public Symbol getSymbol(Tree tree) {
    return symbolsTree.get(tree);
  }

  @Nullable
  public Tree getTree(Symbol symbol) {
    return symbolsTree.inverse().get(symbol);
  }


  public void associateReference(IdentifierTree tree, Symbol symbol) {
    usagesTree.put(symbol, tree);
  }

  @VisibleForTesting
  Map<Tree, Symbol> getSymbolsTree() {
    return Collections.unmodifiableMap(symbolsTree);
  }

  @VisibleForTesting
  Collection<Symbol> getSymbolUsed() {
    return usagesTree.keySet();
  }

}
