/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
import com.sonar.sslr.api.AstNode;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class SemanticModel {

  private final BiMap<Tree, Symbol> symbolsTree = HashBiMap.create();
  private Multimap<Symbol, IdentifierTree> usagesTree = HashMultimap.create();

  private final Map<Symbol, Resolve.Env> symbolEnvs = Maps.newHashMap();
  private final Map<AstNode, Resolve.Env> envs = Maps.newHashMap();

  public static SemanticModel createFor(CompilationUnitTree tree) {
    Symbols symbols = new Symbols();
    Resolve resolve = new Resolve(symbols);
    SemanticModel semanticModel = new SemanticModel();
    new FirstPass(semanticModel, resolve).visitCompilationUnit(tree);
    new ExpressionVisitor(semanticModel, symbols, resolve).visitCompilationUnit(tree);
    new LabelsVisitor(semanticModel).visitCompilationUnit(tree);
    return semanticModel;
  }

  @VisibleForTesting
  SemanticModel() {
  }

  public void saveEnv(Symbol symbol, Resolve.Env env) {
    symbolEnvs.put(symbol, env);
  }

  public Resolve.Env getEnv(Symbol symbol) {
    return symbolEnvs.get(symbol);
  }

  public void associateEnv(Tree tree, Resolve.Env env) {
    //TODO associate the tree directly but how can we navigate up in the hierarchy to retrieve env ??
    envs.put(((JavaTree) tree).getAstNode(), env);
  }

  public Resolve.Env getEnv(Tree tree) {
    AstNode astNode = ((JavaTree) tree).getAstNode();
    Resolve.Env result = null;
    while (result == null && astNode != null) {
      result = envs.get(astNode);
      astNode = astNode.getParent();
    }
    return result;
  }

  //FIXME we should have an IdentifierTree and not a Tree here.
  // This is not the case because VariableTree EnumConstantTree ClassTree, etc. use simple name and not identifiers.
  public void associateSymbol(Tree tree, Symbol symbol) {
    Preconditions.checkNotNull(symbol);
    symbolsTree.put(tree, symbol);
  }

  public Symbol getSymbol(Tree tree) {
    return symbolsTree.get(tree);
  }

  public Tree getTree(Symbol symbol) {
    return symbolsTree.inverse().get(symbol);
  }


  public void associateReference(IdentifierTree tree, Symbol symbol) {
    usagesTree.put(symbol, tree);
  }

  public Map<Tree, Symbol> getSymbolsTree() {
    return Collections.unmodifiableMap(symbolsTree);
  }

  public Collection<IdentifierTree> getUsagesTree(Symbol symbol) {
    return Collections.unmodifiableCollection(usagesTree.get(symbol));
  }

}
