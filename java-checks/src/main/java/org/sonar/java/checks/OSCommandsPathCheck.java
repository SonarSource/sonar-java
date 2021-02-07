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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S4036")
public class OSCommandsPathCheck extends AbstractMethodDetection {
  private static final String LIST_TYPE = "java.util.List";
  private static final String STRING_ARRAY_TYPE = "java.lang.String[]";
  private static final String STRING_TYPE = "java.lang.String";

  private static final MethodMatchers RUNTIME_EXEC_MATCHER = MethodMatchers.create()
    .ofTypes("java.lang.Runtime")
    .names("exec")
    .addParametersMatcher(STRING_TYPE)
    .addParametersMatcher(STRING_TYPE, STRING_ARRAY_TYPE)
    .addParametersMatcher(STRING_TYPE, STRING_ARRAY_TYPE, "java.io.File")
    .addParametersMatcher(STRING_ARRAY_TYPE)
    .addParametersMatcher(STRING_ARRAY_TYPE, STRING_ARRAY_TYPE)
    .addParametersMatcher(STRING_ARRAY_TYPE, STRING_ARRAY_TYPE, "java.io.File")
    .build();

  private static final MethodMatchers PROCESS_BUILDER_MATCHER = MethodMatchers.create()
    .ofTypes("java.lang.ProcessBuilder")
    .constructor()
    .withAnyParameters()
    .build();

  private static final MethodMatchers PROCESS_BUILDER_COMMAND_MATCHER = MethodMatchers.create()
    .ofTypes("java.lang.ProcessBuilder")
    .names("command")
    .withAnyParameters()
    .build();

  private static final MethodMatchers LIST_CREATION_MATCHER = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.util.Arrays")
      .names("asList")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("java.util.Collections")
      .names("singletonList")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes(LIST_TYPE)
      .names("of")
      .withAnyParameters()
      .build()
  );

  private static final List<String> STARTS = Arrays.asList(
    "/",
    "./",
    "../",
    "~/",
    "\\",
    ".\\",
    "..\\"
  );

  private static final Pattern WINDOWS_DISK_PATTERN = Pattern.compile("^[A-Z]:\\\\.*");

  private static final String MESSAGE = "Make sure the \"PATH\" used to find this command includes only what you intend.";

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      RUNTIME_EXEC_MATCHER,
      PROCESS_BUILDER_MATCHER,
      PROCESS_BUILDER_COMMAND_MATCHER
    );
  }


  private static boolean isCompliant(String command) {
    return STARTS.stream().anyMatch(command::startsWith) ||
      WINDOWS_DISK_PATTERN.matcher(command).matches();
  }

  @Override
  protected void onConstructorFound(NewClassTree tree) {
    Arguments arguments = tree.arguments();
    if (arguments.isEmpty()) {
      return;
    }
    ExpressionTree argument = arguments.get(0);
    processArgument(argument);
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree tree) {
    Arguments arguments = tree.arguments();
    if (arguments.isEmpty()) {
      return;
    }
    ExpressionTree argument = arguments.get(0);
    processArgument(argument);
  }

  private void processArgument(ExpressionTree tree) {
    ExpressionTree argument = ExpressionUtils.skipParentheses(tree);
    if (argument.is(Tree.Kind.STRING_LITERAL)) {
      if (isStringLiteralCommandValid(argument)) {
        return;
      }
      reportIssue(argument, MESSAGE);
    } else if (argument.is(Tree.Kind.NEW_ARRAY)) {
      if (isNewArrayCommandValid(argument)) {
        return;
      }
      reportIssue(argument, MESSAGE);
    } else if (argument.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) argument;
      if (isIdentifierCommandValid(identifier)) {
        return;
      }
      reportIssue(argument, MESSAGE);
    } else if (argument.is(Tree.Kind.METHOD_INVOCATION)) {
      if (isListCommandValid(argument)) {
        return;
      }
      reportIssue(argument, MESSAGE);
    }
  }

  private static boolean isStringLiteralCommandValid(ExpressionTree stringLitteral) {
    Optional<String> command = stringLitteral.asConstant(String.class);
    return !command.isPresent() || isCompliant(command.get());
  }

  private static boolean isIdentifierCommandValid(IdentifierTree identifier) {
    Symbol symbol = identifier.symbol();
    Type type = symbol.type();
    if (type.is(STRING_TYPE)) {
      return isStringLiteralCommandValid(identifier);
    }
    //The identifier must be an array or a list
    Tree declaration = symbol.declaration();
    if (declaration == null || !declaration.is(Tree.Kind.VARIABLE)) {
      return true;
    }
    VariableTree variable = (VariableTree) declaration;
    ExpressionTree initializer = variable.initializer();
    if (initializer == null) {
      return true;
    }
    Tree.Kind initializerKind = initializer.kind();
    if (initializerKind.equals(Tree.Kind.NEW_ARRAY)) {
      return isNewArrayCommandValid(initializer);
    }
    if (initializerKind.equals(Tree.Kind.METHOD_INVOCATION)) {
      return isListCommandValid(initializer);
    }
    return true;
  }

  private static boolean isListCommandValid(ExpressionTree listInitializer) {
    MethodInvocationTree invocation = (MethodInvocationTree) listInitializer;
    if (!LIST_CREATION_MATCHER.matches(invocation)) {
      return true;
    }
    Arguments listArguments = invocation.arguments();
    if (listArguments.isEmpty()) {
      return true;
    }
    ExpressionTree firstArgument = ExpressionUtils.skipParentheses(listArguments.get(0));
    if (firstArgument.is(Tree.Kind.STRING_LITERAL)) {
      return isStringLiteralCommandValid(firstArgument);
    }
    if (firstArgument.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) firstArgument;
      return isIdentifierCommandValid(identifier);
    }
    return true;
  }

  private static boolean isNewArrayCommandValid(ExpressionTree arrayInitializer) {
    NewArrayTree newArray = (NewArrayTree) arrayInitializer;
    ListTree<ExpressionTree> initializers = newArray.initializers();
    if (initializers.isEmpty()) {
      return true;
    }
    ExpressionTree firstArgument = ExpressionUtils.skipParentheses(initializers.get(0));
    if (firstArgument.is(Tree.Kind.STRING_LITERAL)) {
      return isStringLiteralCommandValid(firstArgument);
    }
    return true;
  }
}
