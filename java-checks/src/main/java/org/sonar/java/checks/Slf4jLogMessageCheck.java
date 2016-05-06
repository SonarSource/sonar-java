/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.NameCriteria;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

/**
 * This {@link Rule} checks if a slf4j log message is used without the use of {}-placeholders.
 * 
 * <pre>
 * logger.debug("This is how slf4j logging should be done with {}", parameters);
 * </pre>
 */
@Rule(key = "Methods4logmsg")
public class Slf4jLogMessageCheck extends IssuableSubscriptionVisitor {

  private static final String STRING_CLASSNAME = "java.lang.String";
  private static final String SLF4J_LOGGER_CLASSNAME = "org.slf4j.Logger";
  private static final String SLF4J_MARKER_CLASSNAME = "org.slf4j.Marker";

  /**
   * Only looking for Method Invocation.
   */
  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Kind.METHOD_INVOCATION);
  }

  /**
   * Matchers that match for a call on the {@link Logger}.
   * 
   * @return
   */
  private static boolean isLoggingInvocation(MethodInvocationTree tree) {
    return MethodMatcherCollection.create(
      MethodMatcher.create().typeDefinition(SLF4J_LOGGER_CLASSNAME).name(NameCriteria.any()).addParameter(TypeCriteria.anyType()),
      MethodMatcher.create().typeDefinition(SLF4J_LOGGER_CLASSNAME).name(NameCriteria.any()).addParameter(STRING_CLASSNAME).addParameter(TypeCriteria.anyType()),
      MethodMatcher.create().typeDefinition(SLF4J_LOGGER_CLASSNAME).name(NameCriteria.any()).addParameter(SLF4J_MARKER_CLASSNAME).addParameter(TypeCriteria.anyType()),
      MethodMatcher.create().typeDefinition(SLF4J_LOGGER_CLASSNAME).name(NameCriteria.any()).addParameter(SLF4J_MARKER_CLASSNAME).addParameter(STRING_CLASSNAME)
        .addParameter(TypeCriteria.anyType()))
      .anyMatch(tree);
  }

  /**
   * Log invocation that start with the {@link Marker} class. This means the
   * log message is the 2nd argument, instead of the 1st.
   * 
   * @return
   */
  private static boolean logInvocationStartsWithMarker(MethodInvocationTree tree) {
    return MethodMatcherCollection.create(
      MethodMatcher.create().name(NameCriteria.any()).addParameter(SLF4J_MARKER_CLASSNAME).addParameter(TypeCriteria.anyType()),
      MethodMatcher.create().name(NameCriteria.any()).addParameter(SLF4J_MARKER_CLASSNAME).addParameter(STRING_CLASSNAME).addParameter(TypeCriteria.anyType())).anyMatch(tree);
  }

  /**
   * Matchers that match for a call to {@link String#format(String, Object...)}.
   * 
   * @return
   */
  private static boolean isStringFormatInvocation(MethodInvocationTree tree) {
    return MethodMatcherCollection.create(
      MethodMatcher.create().typeDefinition(STRING_CLASSNAME).name(NameCriteria.is("format")).addParameter(STRING_CLASSNAME).addParameter(TypeCriteria.anyType()),
      MethodMatcher.create().typeDefinition(STRING_CLASSNAME).name(NameCriteria.is("format")).addParameter(TypeCriteria.anyType())).anyMatch(tree);
  }

  @Override
  public void visitNode(Tree tree) {
    if (isLoggingInvocation((MethodInvocationTree) tree)) {
      final ExpressionTree logMessageArgument = getLogMessageArgument((MethodInvocationTree) tree);

      switch (logMessageArgument.kind()) {
        case PLUS:
          checkPlus(logMessageArgument);
          break;
        case METHOD_INVOCATION:
        case METHOD_REFERENCE:
          checkMethod(logMessageArgument);
          break;
        case STRING_LITERAL:
        case IDENTIFIER:
        default:
          break;
      }
    }
  }

  /**
   * Retrieve the argument containing the Log message.
   * If a {@link Marker} is also provided, this is the 2nd argument and not the 1st.
   * @param methodInvocationTree The method invocation tree
   * @return The argument expression tree
   */
  private static final ExpressionTree getLogMessageArgument(MethodInvocationTree methodInvocationTree) {
    if (logInvocationStartsWithMarker(methodInvocationTree)) {
      return methodInvocationTree.arguments().get(1);
    }
    return methodInvocationTree.arguments().get(0);
  }

  /**
   * Warn about the concatenating in the log message.
   * @param tree
   */
  private void checkPlus(ExpressionTree tree) {
    BinaryExpressionTree plusArgument = (BinaryExpressionTree) tree;
    if (!isOnlyStringConcatenate(plusArgument)) {
      reportIssue(tree, "Avoid Object concatenating in log messages.");
    }
  }

  /**
   * Check if the {@link BinaryExpressionTree} exists of only String literals.
   * @param tree
   * @return <code>true</code> if only String literals are concatenated. Otherwise <code>false</code>
   */
  private static boolean isOnlyStringConcatenate(BinaryExpressionTree tree) {
    boolean left = tree.leftOperand().is(Kind.STRING_LITERAL)
      || (tree.leftOperand().is(Kind.PLUS) && isOnlyStringConcatenate((BinaryExpressionTree) tree.leftOperand()));

    if (left) {
      return tree.rightOperand().is(Kind.STRING_LITERAL)
        || (tree.rightOperand().is(Kind.PLUS) && isOnlyStringConcatenate((BinaryExpressionTree) tree.rightOperand()));
    }
    return false;
  }

  /**
   * Warn about the method invocation for the log message
   * @param tree
   */
  private void checkMethod(ExpressionTree tree) {
    MethodInvocationTree methodArgument = (MethodInvocationTree) tree;

    if (isStringFormatInvocation(methodArgument)) {
      // error
      reportIssue(tree, "String.format() should not be used as a log message.");
      return;
    }

    reportIssue(tree, "A method call should not be used as a log messages.");
  }

}
