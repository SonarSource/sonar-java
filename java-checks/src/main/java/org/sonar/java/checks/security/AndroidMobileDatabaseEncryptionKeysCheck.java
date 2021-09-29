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

import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.methods.AbstractMethodDetection;
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
public class AndroidMobileDatabaseEncryptionKeysCheck extends AbstractMethodDetection {

  private static final String JAVA_LANG_STRING = "java.lang.String";

  private static final MethodMatchers SQLITE_DATABASE_CONSTRUCTOR = MethodMatchers.create()
    .ofTypes("net.sqlcipher.database.SQLiteDatabase")
    .constructor()
    .withAnyParameters()
    .build();

  private static final MethodMatchers SQLITE_DATABASE_METHODS = MethodMatchers.create()
    .ofTypes("net.sqlcipher.database.SQLiteDatabase")
    .names("changePassword", "openDatabase", "openOrCreateDatabase", "create")
    .withAnyParameters()
    .build();

  private static final MethodMatchers REALM_CONFIGURATION_BUILDER_ENCRYPTION_KEY = MethodMatchers.create()
    .ofTypes("io.realm.RealmConfiguration$Builder")
    .names("encryptionKey")
    .addParametersMatcher("byte[]")
    .build();

  private static final MethodMatchers STRING_TO_CHAR_GET_BYTES = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING)
    .names("toCharArray", "getBytes")
    .withAnyParameters()
    .build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      SQLITE_DATABASE_CONSTRUCTOR,
      SQLITE_DATABASE_METHODS,
      REALM_CONFIGURATION_BUILDER_ENCRYPTION_KEY);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    Arguments arguments = mit.arguments();
    String messageArg = REALM_CONFIGURATION_BUILDER_ENCRYPTION_KEY.matches(mit) ? "encryptionKey" : "password";
    ExpressionTree passwordArg = arguments.size() == 1 ? arguments.get(0) : arguments.get(1);
    reportIssueIfHardCoded(passwordArg, messageArg);
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    reportIssueIfHardCoded(newClassTree.arguments().get(1), "password");
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
      final ExpressionTree singleWriteUsage = getSingleWriteUsage(identifier.symbol());
      if (singleWriteUsage != null && singleWriteUsage.is(Tree.Kind.METHOD_INVOCATION)) {
        expression = singleWriteUsage;
      }
    }
    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) expression;
      if (STRING_TO_CHAR_GET_BYTES.matches(mit)) {
        return ((MemberSelectExpressionTree) (mit).methodSelect()).expression();
      }
    }
    return null;
  }

}
