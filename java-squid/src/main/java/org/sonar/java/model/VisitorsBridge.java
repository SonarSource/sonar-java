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
package org.sonar.java.model;

import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.squid.SquidAstVisitor;
import org.sonar.sslr.parser.LexerlessGrammar;

import javax.annotation.Nullable;
import java.util.List;

public class VisitorsBridge extends SquidAstVisitor<LexerlessGrammar> {

  private final JavaTreeMaker treeMaker = new JavaTreeMaker();
  private final TreeVisitorsDispatcher reflection;
  private final List<JavaTreeVisitor> visitors;

  public VisitorsBridge(List<SquidAstVisitor<LexerlessGrammar>> visitors) {
    ImmutableList.Builder<JavaTreeVisitor> standardVisitors = ImmutableList.builder();
    ImmutableList.Builder<TreeVisitor> reflectionVisitors = ImmutableList.builder();
    for (SquidAstVisitor visitor : visitors) {
      if (visitor instanceof TreeVisitor) {
        reflectionVisitors.add((TreeVisitor) visitor);
      }
      if (visitor instanceof JavaTreeVisitorProvider) {
        standardVisitors.add(((JavaTreeVisitorProvider) visitor).createJavaTreeVisitor());
      }
    }
    this.reflection = new TreeVisitorsDispatcher(reflectionVisitors.build());
    this.visitors = standardVisitors.build();
  }

  @Override
  public void visitFile(@Nullable AstNode astNode) {
    if (astNode != null) {
      Tree tree = treeMaker.compilationUnit(astNode);
      reflection.scan(tree);

      for (JavaTreeVisitor visitor : visitors) {
        ((JavaTree) tree).accept(visitor);
      }
    }
  }

}
