/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.checks.security;

import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.ExpressionsHelper.getSingleWriteUsage;

@Rule(key = "S6301")
public class AndroidMobileDatabaseEncryptionKeysCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_LANG_STRING = "java.lang.String";

  private static final MethodMatchers SQLITE_DATABASE_CONSTRUCTOR = MethodMatchers.create()
    .ofTypes("net.sqlcipher.database.SQLiteDatabase")
    .constructor()
    .addParametersMatcher(args -> !args.isEmpty())
    .build();

  private static final MethodMatchers SQLITE_DATABASE_METHODS = MethodMatchers.create()
    .ofTypes("net.sqlcipher.database.SQLiteDatabase")
    .names("changePassword", "openDatabase", "openOrCreateDatabase", "create")
    .addParametersMatcher(args -> !args.isEmpty())
    .build();

  private static final MethodMatchers REALM_CONFIGURATION_BUILDER_ENCRYPTION_KEY = MethodMatchers.create()
    .ofTypes("io.realm.RealmConfiguration$Builder")
    .names("encryptionKey")
    .addParametersMatcher("byte[]")
    .build();

  private static final MethodMatchers JAVA_LANG_STRING_TO_CHAR_GET_BYTES = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names("toCharArray")
      .addWithoutParametersMatcher()
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names("getBytes")
      .addParametersMatcher(MethodMatchers.ANY)
      .build());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.NEW_CLASS)) {
      final NewClassTree newClassTree = (NewClassTree) tree;
      if (SQLITE_DATABASE_CONSTRUCTOR.matches(newClassTree)) {
        reportIssueIfHardCoded(newClassTree.arguments().get(1), "password");
      }
    } else {
      final MethodInvocationTree mit = (MethodInvocationTree) tree;
      if (REALM_CONFIGURATION_BUILDER_ENCRYPTION_KEY.matches(mit)) {
        reportIssueIfHardCoded(mit, "encryptionKey");
      } else if (SQLITE_DATABASE_METHODS.matches(mit)) {
        reportIssueIfHardCoded(mit, "password");
      }
    }
  }

  private void reportIssueIfHardCoded(MethodInvocationTree mit, String argName) {
    Arguments arguments = mit.arguments();
    ExpressionTree passwordArg = arguments.size() == 1 ? arguments.get(0) : arguments.get(1);
    reportIssueIfHardCoded(passwordArg, argName);
  }

  private void reportIssueIfHardCoded(ExpressionTree expressionTree, String messageArg) {
    ExpressionTree stringExpression = expressionTree;
    if (!expressionTree.symbolType().is(JAVA_LANG_STRING)) {
      // byte[] or char[]
      stringExpression = tryGetOriginStringFromByteOrCharArray(expressionTree);
      if (stringExpression == null) {
        return;
      }
    }
    ExpressionsHelper.ValueResolution<String> constantValueAsString = ExpressionsHelper.getConstantValueAsString(stringExpression);
    if (constantValueAsString.value() != null) {
      reportIssue(expressionTree, "The \"" + messageArg + "\" parameter should not be hardcoded.", constantValueAsString.valuePath(), null);
    }
  }

  @CheckForNull
  private static ExpressionTree tryGetOriginStringFromByteOrCharArray(ExpressionTree givenExpression) {
    ExpressionTree expression = givenExpression;
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) expression;
      ExpressionTree singleWriteUsage = getSingleWriteUsage(identifier.symbol());
      if (singleWriteUsage != null) {
        expression = singleWriteUsage;
      }
    }
    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) expression;
      if (JAVA_LANG_STRING_TO_CHAR_GET_BYTES.matches(mit)) {
        return ((MemberSelectExpressionTree) (mit).methodSelect()).expression();
      }
    }
    return null;
  }

}
