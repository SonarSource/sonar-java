/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Rule(key = "S2068")
public class HardCodedCredentialsCheck extends IssuableSubscriptionVisitor {

  private static final String PWD_TRANSLATION = "password|passwd|pwd|achinsinsi|adgangskode|codice|contrasena|contrasenya|contrasinal|cynfrinair|facal-faire|facalfaire|" +
    "fjaleklaim|focalfaire|geslo|haslo|heslo|iphasiwedi|jelszo|kalmarsirri|katalaluan|katasandi|kennwort|kode|kupuhipa|loluszais|losen|losenord|lozinka|" +
    "lykilorth|mathkau|modpas|motdepasse|olelohuna|oroigbaniwole|parol|parola|parole|parool|pasahitza|pasiwedhi|passord|passwort|" +
    "passwuert|paswoodu|phasewete|salasana|sandi|senha|sifre|sifreya|slaptazois|tenimiafina|upufaalilolilo|wachtwoord|wachtwurd|wagwoord";

  private static final Pattern PASSWORD_LITERAL_PATTERN = Pattern.compile("("+PWD_TRANSLATION+")=\\S.", Pattern.CASE_INSENSITIVE);
  private static final Pattern PASSWORD_VARIABLE_PATTERN = Pattern.compile("("+PWD_TRANSLATION+")", Pattern.CASE_INSENSITIVE);

  private static final MethodMatcher PASSWORD_AUTHENTICATION_CONSTRUCTOR = MethodMatcher.create()
    .typeDefinition("java.net.PasswordAuthentication")
    .name("<init>")
    .addParameter("java.lang.String")
    .addParameter("char[]");

  private static final MethodMatcher STRING_TO_CHAR_ARRAY = MethodMatcher.create()
    .typeDefinition("java.lang.String")
    .name("toCharArray")
    .withoutParameter();

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.STRING_LITERAL, Tree.Kind.VARIABLE, Tree.Kind.ASSIGNMENT, Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.STRING_LITERAL)) {
      handleStringLiteral((LiteralTree) tree);
    } else if (tree.is(Tree.Kind.VARIABLE)) {
      handleVariable((VariableTree) tree);
    } else if (tree.is(Tree.Kind.ASSIGNMENT)) {
      handleAssignement((AssignmentExpressionTree) tree);
    } else if (tree.is(Tree.Kind.NEW_CLASS)) {
      handleConstructor((NewClassTree) tree);
    } else {
      handleMethodInvocation((MethodInvocationTree) tree);
    }
  }

  private static String isSettingPassword(MethodInvocationTree tree) {
    List<ExpressionTree> arguments = tree.arguments();
    if (arguments.size() == 2 && argumentsAreLiterals(arguments)) {
      return isPassword((LiteralTree) arguments.get(0));
    }
    return "";
  }

  private static String isPassword(LiteralTree argument) {
    if (!argument.is(Kind.STRING_LITERAL)) {
      return "";
    }
    Matcher matcher = PASSWORD_VARIABLE_PATTERN.matcher(LiteralUtils.trimQuotes(argument.value()));
    if (matcher.matches()) {
      return matcher.group(1);
    }
    return "";
  }

  private static String isPasswordVariableName(IdentifierTree identifierTree) {
    Matcher matcher = PASSWORD_VARIABLE_PATTERN.matcher(identifierTree.name());
    if (matcher.find()) {
      return matcher.group(1);
    }
    return "";
  }

  private static String isPasswordVariable(ExpressionTree variable) {
    if (variable.is(Tree.Kind.MEMBER_SELECT)) {
      return isPasswordVariableName(((MemberSelectExpressionTree) variable).identifier());
    } else if (variable.is(Tree.Kind.IDENTIFIER)) {
      return isPasswordVariableName((IdentifierTree) variable);
    }
    return "";
  }

  private static boolean isCallOnStringLiteral(ExpressionTree expr) {
    return expr.is(Tree.Kind.MEMBER_SELECT) && ((MemberSelectExpressionTree) expr).expression().is(Tree.Kind.STRING_LITERAL);
  }

  private void handleStringLiteral(LiteralTree tree) {
    Matcher matcher = PASSWORD_LITERAL_PATTERN.matcher(tree.value());
    if (matcher.find()) {
      report(tree, matcher.group(1));
    }
  }

  private void handleVariable(VariableTree tree) {
    IdentifierTree simpleName = tree.simpleName();
    String passwordVariableName = isPasswordVariableName(simpleName);
    if (isStringLiteral(tree.initializer()) && passwordVariableName.length() > 0) {
      report(simpleName, passwordVariableName);
    }
  }

  private void handleAssignement(AssignmentExpressionTree tree) {
    ExpressionTree variable = tree.variable();
    String passwordVariable = isPasswordVariable(variable);
    if (isStringLiteral(tree.expression()) && passwordVariable.length() > 0) {
      report(variable, passwordVariable);
    }
  }

  private static boolean argumentsAreLiterals(List<ExpressionTree> arguments) {
    for (ExpressionTree argument : arguments) {
      if (!argument.is(
        Kind.INT_LITERAL,
        Kind.LONG_LITERAL,
        Kind.FLOAT_LITERAL,
        Kind.DOUBLE_LITERAL,
        Kind.BOOLEAN_LITERAL,
        Kind.CHAR_LITERAL,
        Kind.STRING_LITERAL,
        Kind.NULL_LITERAL)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isStringLiteral(@Nullable ExpressionTree initializer) {
    return initializer != null && initializer.is(Tree.Kind.STRING_LITERAL);
  }

  private void handleConstructor(NewClassTree tree) {
    if (!PASSWORD_AUTHENTICATION_CONSTRUCTOR.matches(tree)) {
      return;
    }
    ExpressionTree secondArg = tree.arguments().get(1);
    if (secondArg.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) secondArg;
      if (isCallOnStringLiteral(mit.methodSelect()) && STRING_TO_CHAR_ARRAY.matches(mit)) {
        report(secondArg, "");
      }
    }
  }

  private void handleMethodInvocation(MethodInvocationTree tree) {
    String settingPassword = isSettingPassword(tree);
    if (settingPassword.length() > 0) {
      report(tree.methodSelect(), settingPassword);
    }
  }

  private void report(Tree tree, String match) {
    String message = "Remove this hard-coded password.";
    if (match.length() > 0) {
      message = "'" + match + "' detected in this expression, review this potentially hardcoded credential.";
    }
    reportIssue(tree, message);
  }

}
