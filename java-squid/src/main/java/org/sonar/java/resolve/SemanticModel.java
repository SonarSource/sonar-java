/*
 * Sonar Java
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.impl.ast.AstWalker;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.visitors.JavaAstVisitor;

import java.util.Map;

public class SemanticModel {

  final BiMap<AstNode, Symbol> symbols = HashBiMap.create();
  final Map<AstNode, Symbol> references = Maps.newHashMap();
  final ArrayListMultimap<Symbol, AstNode> usages = ArrayListMultimap.create();

  private final Map<Symbol, Resolve.Env> symbolEnvs = Maps.newHashMap();
  private final Map<AstNode, Resolve.Env> envs = Maps.newHashMap();

  public static SemanticModel createFor(AstNode astNode) {
    SemanticModel semanticModel = new SemanticModel();
    Resolve resolve = new Resolve();
    visit(astNode, new FirstPass(semanticModel, resolve));
    visit(astNode, new ThirdPass(semanticModel, resolve));
    return semanticModel;
  }

  private static void visit(AstNode astNode, JavaAstVisitor pass) {
    pass.init();
    AstWalker astWalker = new AstWalker();
    astWalker.addVisitor(pass);
    astWalker.walkAndVisit(astNode);
  }

  public void saveEnv(Symbol symbol, Resolve.Env env) {
    symbolEnvs.put(symbol, env);
  }

  public Resolve.Env getEnv(Symbol symbol) {
    return symbolEnvs.get(symbol);
  }

  /**
   * Associates given AstNode with given environment.
   */
  public void associateEnv(AstNode astNode, Resolve.Env env) {
    envs.put(astNode, env);
  }

  public Resolve.Env getEnv(AstNode astNode) {
    Resolve.Env result = null;
    while (result == null && astNode != null) {
      result = envs.get(astNode);
      astNode = astNode.getParent();
    }
    return result;
  }

  /**
   * Associates given AstNode with given Symbol.
   */
  public void associateSymbol(AstNode astNode, Symbol symbol) {
    Preconditions.checkArgument(astNode.is(JavaTokenType.IDENTIFIER));
    Preconditions.checkNotNull(symbol);
    symbols.put(astNode, symbol);
  }

  public Symbol getSymbol(AstNode astNode) {
    Preconditions.checkArgument(astNode.is(JavaTokenType.IDENTIFIER));
    return symbols.get(astNode);
  }

  public AstNode getAstNode(Symbol symbol) {
    return symbols.inverse().get(symbol);
  }

  public void associateReference(AstNode astNode, Symbol symbol) {
    Preconditions.checkNotNull(astNode);
    Preconditions.checkNotNull(symbol);
    references.put(astNode, symbol);
    usages.put(symbol, astNode);
  }

  public Multimap<Symbol, AstNode> getUsages() {
    return usages;
  }

}
