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

import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.ast.AstSelect;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.List;

@Rule(
  key = "S1118",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class UtilityClassWithPublicConstructorCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.CLASS_BODY);
  }

  @Override
  public void visitNode(AstNode node) {
    if (!extendsAnotherClass(node) && hasStaticMembers(node) && !hasInstanceMembers(node)) {
      boolean hasImplicitPublicConstructor = true;

      for (AstNode explicitConstructor : getExplicitConstructors(node)) {
        hasImplicitPublicConstructor = false;

        if (isPublicConstructor(explicitConstructor)) {
          getContext().createLineViolation(this, "Hide this public constructor.", explicitConstructor);
        }
      }

      if (hasImplicitPublicConstructor) {
        getContext().createLineViolation(this, "Add a private constructor to hide the implicit public one.", node);
      }
    }
  }

  private static boolean extendsAnotherClass(AstNode node) {
    return node.getParent().hasDirectChildren(JavaKeyword.EXTENDS);
  }

  private static boolean hasStaticMembers(AstNode node) {
    for (AstNode member : node.getChildren(JavaGrammar.CLASS_BODY_DECLARATION)) {
      if (hasStaticModifier(member)) {
        return true;
      }
    }

    return false;
  }

  private static boolean hasStaticModifier(AstNode node) {
    for (AstNode modifier : node.getChildren(JavaGrammar.MODIFIER)) {
      if (modifier.hasDirectChildren(JavaKeyword.STATIC)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasInstanceMembers(AstNode node) {
    for (AstNode member : node.getChildren(JavaGrammar.CLASS_BODY_DECLARATION)) {
      if (!isExcludedInstanceMember(member) && !hasStaticModifier(member)) {
        return true;
      }
    }

    return false;
  }

  private static boolean isExcludedInstanceMember(AstNode node) {
    AstNode memberDecl = node.getFirstChild(JavaGrammar.MEMBER_DECL);
    if (memberDecl == null) {
      return true;
    }

    AstNode constructorOrGeneric = memberDecl.getFirstChild(JavaGrammar.CONSTRUCTOR_DECLARATOR_REST, JavaGrammar.GENERIC_METHOD_OR_CONSTRUCTOR_REST);

    return constructorOrGeneric != null &&
      isConstructor(constructorOrGeneric);
  }

  private static List<AstNode> getExplicitConstructors(AstNode node) {
    ImmutableList.Builder<AstNode> builder = ImmutableList.builder();

    AstSelect query = node.select()
        .children(JavaGrammar.CLASS_BODY_DECLARATION)
        .children(JavaGrammar.MEMBER_DECL)
        .children(JavaGrammar.CONSTRUCTOR_DECLARATOR_REST, JavaGrammar.GENERIC_METHOD_OR_CONSTRUCTOR_REST);

    for (AstNode methodOrGeneric : query) {
      if (isConstructor(methodOrGeneric)) {
        builder.add(methodOrGeneric);
      }
    }

    return builder.build();
  }

  private static boolean isConstructor(AstNode node) {
    return node.is(JavaGrammar.CONSTRUCTOR_DECLARATOR_REST) || node.hasDirectChildren(JavaGrammar.CONSTRUCTOR_DECLARATOR_REST);
  }

  private static boolean isPublicConstructor(AstNode node) {
    return hasPublicModifier(node.getFirstAncestor(JavaGrammar.CLASS_BODY_DECLARATION));
  }

  private static boolean hasPublicModifier(AstNode node) {
    for (AstNode modifier : node.getChildren(JavaGrammar.MODIFIER)) {
      if (modifier.hasDirectChildren(JavaKeyword.PUBLIC)) {
        return true;
      }
    }
    return false;
  }

}
