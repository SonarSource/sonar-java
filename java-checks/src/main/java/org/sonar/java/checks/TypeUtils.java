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

import com.sonar.sslr.api.AstNode;
import org.apache.log4j.Logger;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.HashSet;
import java.util.Set;

public class TypeUtils {
  private static final Logger logger = Logger.getLogger(TypeUtils.class);

  public static String getFullQualifiedTypeName(Tree typeTree, boolean onlyFull) {
    AstNode blockAstNode = ((JavaTree) typeTree).getAstNode();
    String type = getTypeName(typeTree, blockAstNode);
    AstNode cu = getCompilationUnit(blockAstNode);
    Set<String> pkgs = getImportedPackages(cu);
    if(pkgs == null) {
      return onlyFull?null:type;
    }
    for(String pkg: pkgs) {
      if(pkg.endsWith(type)) {
        return pkg;
      }
    }
    return onlyFull?null:type;
  }

  public static String getTypeName(Tree typeTree) {
    return getTypeName(typeTree, ((JavaTree) typeTree).getAstNode());
  }

  private static String getTypeName(Tree typeTree, AstNode node) {
    if(node != null && node.getType() == JavaTokenType.IDENTIFIER) {
      if(node.getParent() != null && node.getParent().getType() == JavaGrammar.CLASS_TYPE) {
        return buildNodeTokenValue(node.getParent());
      }
    }
    if (typeTree != null) {
      if (typeTree.is(Tree.Kind.IDENTIFIER)) {
        return ((IdentifierTree) typeTree).name();
      } else if (typeTree.is(Tree.Kind.MEMBER_SELECT)) {
        return ((MemberSelectExpressionTree) typeTree).identifier().name();
      } else if (typeTree.is(Tree.Kind.PARAMETERIZED_TYPE)) {
        return getTypeName(((ParameterizedTypeTree) typeTree).type());
      }
    }
    return "";
  }

  private static AstNode getCompilationUnit(AstNode node) {
    if(node == null) {
      return null;
    }
    if(node.getType() ==  JavaGrammar.COMPILATION_UNIT) {
      return node;
    }
    return getCompilationUnit(node.getParent());
  }

  private static Set<String> getImportedPackages(AstNode compilationUnit) {
    if(compilationUnit == null || compilationUnit.getType() !=  JavaGrammar.COMPILATION_UNIT) {
      logger.error("getImportedPackages: compilationUnit should be an \"" + JavaGrammar.COMPILATION_UNIT.toString() + "\" node");
      return null;
    }

    Set<String> ret = new HashSet<String>();
    for(AstNode children: compilationUnit.getChildren()) {
      if(children.getType() == JavaGrammar.IMPORT_DECLARATION) {
        String pkg = getImportPackage(children);
        if(pkg != null) {
          ret.add(pkg);
        }
      }
    }
    return ret;
  }

  private static String getImportPackage(AstNode importNode) {
    if(importNode == null || importNode.getType() !=  JavaGrammar.IMPORT_DECLARATION) {
      logger.error("getImportPackage: importNode should be an \"" + JavaGrammar.IMPORT_DECLARATION.toString() + "\" node");
      return null;
    }

    AstNode qualifiedNode = null;
    for(AstNode children: importNode.getChildren()) {
      if(children.getType() == JavaGrammar.QUALIFIED_IDENTIFIER) {
        qualifiedNode = children;
      }
    }

    if(qualifiedNode == null) {
      logger.error("getImportPackage: no qualifiedIdentifier found");
      return null;
    }
    return getQualifiedIdentifier(qualifiedNode);
  }

  private static String getQualifiedIdentifier(AstNode qualifiedIdentifier) {
    if(qualifiedIdentifier == null || qualifiedIdentifier.getType() !=  JavaGrammar.QUALIFIED_IDENTIFIER) {
      logger.error("getQualifiedIdentifier: qualifiedIdentifier should be an \"" + JavaGrammar.QUALIFIED_IDENTIFIER.toString() + "\" node");
      return null;
    }
    return buildNodeTokenValue(qualifiedIdentifier);
  }

  private static String buildNodeTokenValue(AstNode node) {
    StringBuilder stringBuilder = new StringBuilder();
    for(AstNode children: node.getChildren()) {
      stringBuilder.append(children.getTokenValue());
    }
    return stringBuilder.toString();
  }
}
