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
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.List;

@Rule(
  key = "ModifiersOrderCheck",
  priority = Priority.MINOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MINOR)
public class ModifiersOrderCheck extends SquidCheck<LexerlessGrammar> {

  private static final AstNodeType[] EXPECTED_ORDER = new AstNodeType[] {
    JavaGrammar.ANNOTATION,
    JavaKeyword.PUBLIC,
    JavaKeyword.PROTECTED,
    JavaKeyword.PRIVATE,
    JavaKeyword.ABSTRACT,
    JavaKeyword.STATIC,
    JavaKeyword.FINAL,
    JavaKeyword.TRANSIENT,
    JavaKeyword.VOLATILE,
    JavaKeyword.SYNCHRONIZED,
    JavaKeyword.NATIVE,
    JavaKeyword.STRICTFP
  };

  @Override
  public void init() {
    subscribeTo(JavaGrammar.MODIFIER);
  }

  @Override
  public void visitNode(AstNode node) {
    if (isFirstModifer(node)) {
      List<AstNode> modifiers = getModifiers(node);

      if (isBadlyOrdered(modifiers)) {
        getContext().createLineViolation(this, "Reorder the modifiers to comply with the Java Language Specification.", node);
      }
    }
  }

  private static boolean isFirstModifer(AstNode node) {
    return node.getPreviousSibling() == null;
  }

  private static List<AstNode> getModifiers(AstNode node) {
    ImmutableList.Builder<AstNode> builder = ImmutableList.builder();
    builder.add(node);

    for (AstNode nextSibling = node.getNextSibling(); nextSibling != null && nextSibling.is(JavaGrammar.MODIFIER); nextSibling = nextSibling.getNextSibling()) {
      builder.add(nextSibling);
    }

    return builder.build();
  }

  private static boolean isBadlyOrdered(List<AstNode> modifiers) {
    int i = 0;

    for (AstNode modifier : modifiers) {
      for (; i < EXPECTED_ORDER.length && !modifier.getFirstChild().is(EXPECTED_ORDER[i]); i++) {
        // We're just interested in the final value of 'i'
      }
    }

    return i == EXPECTED_ORDER.length;
  }

}
