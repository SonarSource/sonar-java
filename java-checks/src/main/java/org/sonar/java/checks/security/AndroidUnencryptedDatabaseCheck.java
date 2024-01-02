/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6291")
public class AndroidUnencryptedDatabaseCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String ANDROID_CONTENT_CONTEXT = "android.content.Context";
  private static final String REALM_CONFIGURATION_BUILDER_TYPE = "io.realm.RealmConfiguration$Builder";

  private static final MethodMatchers UNSAFE_DATABASE_CALL = MethodMatchers.or(
    MethodMatchers.create()
      .ofSubTypes("android.app.Activity")
      .names("getPreferences")
      .addParametersMatcher("int")
      .build(),
    MethodMatchers.create()
      .ofSubTypes("android.preference.PreferenceManager")
      .names("getDefaultSharedPreferences")
      .addParametersMatcher(ANDROID_CONTENT_CONTEXT)
      .build(),
    MethodMatchers.create()
      .ofSubTypes(ANDROID_CONTENT_CONTEXT)
      .names("getSharedPreferences")
      .addParametersMatcher(JAVA_LANG_STRING, "int")
      .addParametersMatcher("java.io.File", "int")
      .build(),
    MethodMatchers.create()
      .ofSubTypes(ANDROID_CONTENT_CONTEXT)
      .names("openOrCreateDatabase")
      .addParametersMatcher(JAVA_LANG_STRING, "int", "android.database.sqlite.SQLiteDatabase$CursorFactory")
      .addParametersMatcher(JAVA_LANG_STRING, "int", "android.database.sqlite.SQLiteDatabase$CursorFactory", "android.database.DatabaseErrorHandler")
      .build()
  );

  private static final MethodMatchers REALM_CONFIGURATION_BUILDER_BUILD = MethodMatchers.create()
    .ofSubTypes(REALM_CONFIGURATION_BUILDER_TYPE)
    .names("build")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers REALM_CONFIGURATION_BUILDER_ENCRYPTION_KEY = MethodMatchers.create()
    .ofSubTypes(REALM_CONFIGURATION_BUILDER_TYPE)
    .names("encryptionKey")
    .withAnyParameters()
    .build();

  private static final MethodMatchers REALM_CONFIGURATION_BUILDER_BUILDER = MethodMatchers.create()
    .ofSubTypes(REALM_CONFIGURATION_BUILDER_TYPE)
    .constructor()
    .withAnyParameters()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (UNSAFE_DATABASE_CALL.matches(mit) || (REALM_CONFIGURATION_BUILDER_BUILD.matches(mit) && !isEncrypted(mit.methodSelect()))) {
      reportIssue(ExpressionUtils.methodName(mit), "Make sure using an unencrypted database is safe here.");
    }
  }

  private static boolean isEncrypted(ExpressionTree expression) {
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      expression = ((MemberSelectExpressionTree) expression).expression();
    }

    if (expression.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) expression;
      if (!REALM_CONFIGURATION_BUILDER_ENCRYPTION_KEY.matches(mit)) {
        return isEncrypted(mit.methodSelect());
      }
    } else if (expression.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) expression).symbol();
      if (symbol.usages().stream().anyMatch(AndroidUnencryptedDatabaseCheck::canEncryptToken)) {
        return true;
      }
      return declarationIsEncrypted(symbol);
    } else if (expression.is(Tree.Kind.NEW_CLASS)) {
      NewClassTree newClassTree = (NewClassTree) expression;
      if (REALM_CONFIGURATION_BUILDER_BUILDER.matches(newClassTree)) {
        return false;
      }
    }
    return true;
  }

  private static boolean canEncryptToken(IdentifierTree tokenIdentifier) {
    Tree parent = tokenIdentifier.parent();
    // When given as argument, we consider it as encrypted to avoid FP.
    return (parent != null && parent.is(Tree.Kind.ARGUMENTS)) ||
      MethodTreeUtils.subsequentMethodInvocation(tokenIdentifier, REALM_CONFIGURATION_BUILDER_ENCRYPTION_KEY).isPresent();
  }

  private static boolean declarationIsEncrypted(Symbol symbol) {
    if (symbol.isLocalVariable()) {
      Tree declaration = symbol.declaration();
      if (declaration instanceof VariableTree) {
        ExpressionTree initializer = ((VariableTree) declaration).initializer();
        return initializer instanceof MethodInvocationTree && isEncrypted(initializer);
      }
    }
    // Can be encrypted anywhere (field, other file), we consider it as encrypted
    return true;
  }
}
