/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.java.se.checks;

import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2689")
public class ObjectOutputStreamCheck extends SECheck {

  private static final MethodMatcher FILES_NEW_OUTPUT_STREAM = MethodMatcher.create()
    .typeDefinition("java.nio.file.Files")
    .name("newOutputStream")
    .withAnyParameters();

  public enum FileOutputStreamConstraint implements Constraint {
    APPEND
  }

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    ProgramState programState = context.getState();
    if (syntaxNode.is(Tree.Kind.NEW_CLASS)) {
      NewClassTree newClassTree = (NewClassTree) syntaxNode;
      if (newClassTree.symbolType().is("java.io.FileOutputStream") && newClassTree.arguments().size() == 2) {
        ProgramState psBeforeInvocation = context.getNode().programState;
        BooleanConstraint argConstraint = psBeforeInvocation.getConstraint(psBeforeInvocation.peekValue(), BooleanConstraint.class);
        if (argConstraint == BooleanConstraint.TRUE) {
          programState = programState.addConstraint(programState.peekValue(), FileOutputStreamConstraint.APPEND);
        }
      }
    } else if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) syntaxNode;
      if (FILES_NEW_OUTPUT_STREAM.matches(mit)) {
        ProgramState psBeforeInvocation = context.getNode().programState;
        int optionsNumber = mit.arguments().size() - 1/*skip Path argument*/;
        for (int i = 0; i < optionsNumber; i++) {
          FileOutputStreamConstraint argConstraint = psBeforeInvocation.getConstraint(psBeforeInvocation.peekValue(i), FileOutputStreamConstraint.class);
          if (argConstraint == FileOutputStreamConstraint.APPEND) {
            programState = programState.addConstraint(programState.peekValue(), FileOutputStreamConstraint.APPEND);
          }
        }
      }
    } else if (syntaxNode.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree member = (MemberSelectExpressionTree) syntaxNode;
      if (member.symbolType().is("java.nio.file.StandardOpenOption") && "APPEND".equals(member.identifier().name())) {
        programState = programState.addConstraint(programState.peekValue(), FileOutputStreamConstraint.APPEND);
      }
    }
    return programState;
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    if (syntaxNode.is(Tree.Kind.NEW_CLASS)) {
      NewClassTree newClassTree = (NewClassTree) syntaxNode;
      if (newClassTree.symbolType().is("java.io.ObjectOutputStream")) {
        ProgramState programState = context.getState();
        FileOutputStreamConstraint constraint = programState.getConstraint(programState.peekValue(), FileOutputStreamConstraint.class);
        if (constraint == FileOutputStreamConstraint.APPEND) {
          context.reportIssue(syntaxNode, this, "This file was opened in append mode.");
        }
      }
    }
    return super.checkPreStatement(context, syntaxNode);
  }

}
