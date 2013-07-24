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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.ast.visitors.JavaAstCheck;
import org.sonar.sslr.ast.AstSelect;

import java.util.Map;
import java.util.Stack;

@Rule(
  key = "HiddenFieldCheck",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class HiddenFieldCheck extends JavaAstCheck {

  private static final AstNodeType[] LOCAL_VARIABLE_TYPES = new AstNodeType[] {
    JavaGrammar.LOCAL_VARIABLE_DECLARATION_STATEMENT,
    JavaGrammar.VARIABLE_DECLARATOR_ID,
    JavaGrammar.FOR_INIT
  };

  private final Stack<Map<String, Integer>> fields = new Stack<Map<String, Integer>>();

  @Override
  public void init() {
    subscribeTo(JavaGrammar.CLASS_BODY);
    subscribeTo(LOCAL_VARIABLE_TYPES);
  }

  @Override
  public void visitFile(AstNode node) {
    fields.clear();
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.is(JavaGrammar.CLASS_BODY)) {
      fields.push(getFields(node));
    } else if (!isInConstructorOrSetter(node)) {
      checkLocalVariables(node);
    }
  }

  @Override
  public void leaveNode(AstNode node) {
    if (node.is(JavaGrammar.CLASS_BODY)) {
      fields.pop();
    }
  }

  private void checkLocalVariables(AstNode node) {
    if (node.is(JavaGrammar.VARIABLE_DECLARATOR_ID)) {
      checkLocalVariable(node.getFirstChild(JavaTokenType.IDENTIFIER));
    } else {
      AstNode variableDeclarators = node.getFirstChild(JavaGrammar.VARIABLE_DECLARATORS);
      if (variableDeclarators != null) {
        for (AstNode variableDeclarator : variableDeclarators.getChildren(JavaGrammar.VARIABLE_DECLARATOR)) {
          checkLocalVariable(variableDeclarator.getFirstChild(JavaTokenType.IDENTIFIER));
        }
      }
    }
  }

  private void checkLocalVariable(AstNode node) {
    for (Map<String, Integer> classFields : Lists.reverse(fields)) {
      String identifier = node.getTokenOriginalValue();
      Integer hiddenFieldLine = classFields.get(identifier);
      if (hiddenFieldLine != null) {
        getContext().createLineViolation(this, "Rename \"" + identifier + "\" which hides the field declared at line " + hiddenFieldLine + ".", node);
        return;
      }
    }
  }

  private static Map<String, Integer> getFields(AstNode node) {
    AstSelect fieldIdentifiers = node.select()
        .children(JavaGrammar.CLASS_BODY_DECLARATION)
        .children(JavaGrammar.MEMBER_DECL)
        .children(JavaGrammar.FIELD_DECLARATION)
        .children(JavaGrammar.VARIABLE_DECLARATORS)
        .children(JavaGrammar.VARIABLE_DECLARATOR)
        .children(JavaTokenType.IDENTIFIER);

    ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();

    for (AstNode fieldIdentifier : fieldIdentifiers) {
      builder.put(fieldIdentifier.getTokenOriginalValue(), fieldIdentifier.getTokenLine());
    }

    return builder.build();
  }

  private static boolean isInConstructorOrSetter(AstNode node) {
    AstNode ancestor = getFirstAncestor(node,
        JavaGrammar.CLASS_INIT_DECLARATION, JavaGrammar.CLASS_BODY_DECLARATION,
        JavaGrammar.INTERFACE_BODY_DECLARATION);

    return ancestor != null && (isConstructor(ancestor) || isSetter(ancestor));
  }

  private static boolean isConstructor(AstNode node) {
    AstNode memberDecl = getActualMemberDecl(node);

    return node.is(JavaGrammar.CLASS_BODY_DECLARATION) &&
      memberDecl != null &&
      memberDecl.hasDirectChildren(JavaGrammar.CONSTRUCTOR_DECLARATOR_REST);
  }

  private static boolean isSetter(AstNode node) {
    AstNode memberDecl = getActualMemberDecl(node);
    return node.is(JavaGrammar.CLASS_BODY_DECLARATION) &&
      memberDecl != null &&
      memberDecl.getFirstChild(JavaTokenType.IDENTIFIER).getTokenOriginalValue().startsWith("set");
  }

  private static AstNode getActualMemberDecl(AstNode node) {
    AstNode memberDecl = node.getFirstChild(JavaGrammar.MEMBER_DECL);
    if (memberDecl == null) {
      return null;
    }

    AstNode genericMethodOrConstructor = memberDecl.getFirstChild(JavaGrammar.GENERIC_METHOD_OR_CONSTRUCTOR_REST);
    return genericMethodOrConstructor == null ? memberDecl : genericMethodOrConstructor;
  }

  private static AstNode getFirstAncestor(AstNode node, AstNodeType... types) {
    for (AstNode ancestor = node.getParent(); ancestor != null; ancestor = ancestor.getParent()) {
      for (AstNodeType type : types) {
        if (ancestor.getType() == type) {
          return ancestor;
        }
      }
    }

    return null;
  }

}
