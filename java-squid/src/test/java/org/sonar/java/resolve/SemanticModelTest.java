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

import com.sonar.sslr.api.AstNode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.java.ast.parser.JavaGrammar;

public class SemanticModelTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private SemanticModel semanticModel = new SemanticModel();

  @Test
  public void symbol_can_be_associated_only_with_identifier() {
    AstNode astNode = new AstNode(JavaGrammar.TYPE, "", null);
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Expected AST node with identifier, got: " + astNode);
    semanticModel.associateSymbol(astNode, null);
  }

  @Test
  public void symbol_associated_only_with_identifier() {
    AstNode astNode = new AstNode(JavaGrammar.TYPE, "", null);
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Expected AST node with identifier, got: " + astNode);
    semanticModel.getSymbol(astNode);
  }

  @Test
  public void reference_can_be_associated_only_with_identifier() {
    AstNode astNode = new AstNode(JavaGrammar.TYPE, "", null);
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Expected AST node with identifier, got: " + astNode);
    semanticModel.associateReference(astNode, null);
  }

}
