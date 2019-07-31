/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ConstantUtils;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S3457")
public class PrintfMisuseCheck extends AbstractPrintfChecker {

  private static final MethodMatcher TO_STRING = MethodMatcher.create().typeDefinition(TypeCriteria.anyType()).name("toString").withoutParameter();
  private static final MethodMatcherCollection GET_LOGGER = MethodMatcherCollection.create(
    MethodMatcher.create().typeDefinition(JAVA_UTIL_LOGGING_LOGGER).name("getLogger").parameters(JAVA_LANG_STRING, JAVA_LANG_STRING),
    MethodMatcher.create().typeDefinition(JAVA_UTIL_LOGGING_LOGGER).name("getAnonymousLogger").parameters(JAVA_LANG_STRING)
    );

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    boolean isMessageFormat = MESSAGE_FORMAT.matches(mit);
    if (isMessageFormat && !mit.symbol().isStatic()) {
      // only consider the static method
      return;
    }
    if (!isMessageFormat) {
      isMessageFormat = JAVA_UTIL_LOGGER.matches(mit);
      if (isMessageFormat && (mit.arguments().get(mit.arguments().size() - 1).symbolType().isSubtypeOf("java.lang.Throwable") || hasResourceBundle(mit))) {
        // ignore formatting issues when last argument is a throwable
        return;
      }
    }
    if(!isMessageFormat) {
      isMessageFormat = isLoggingMethod(mit);
      if (isMessageFormat && mit.arguments().get(mit.arguments().size() - 1).symbolType().isSubtypeOf("java.lang.Throwable")) {
        // ignore formatting issues when last argument is a throwable
        return;
      }
    }
    super.checkFormatting(mit, isMessageFormat);
  }

  private static boolean hasResourceBundle(MethodInvocationTree mit) {
    Tree id;
    if (mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      id = ((MemberSelectExpressionTree) mit.methodSelect()).expression();
    } else {
      // defensive programming : cannot be reached : log methods are not static
      return false;
    }
    if (id.is(Tree.Kind.MEMBER_SELECT)) {
      id = ((MemberSelectExpressionTree) id).identifier();
    }
    if (id.is(Tree.Kind.IDENTIFIER)) {
      Tree decl = ((IdentifierTree) id).symbol().declaration();
      if (decl != null && decl.is(Tree.Kind.VARIABLE)) {
        VariableTree var = ((VariableTree) decl);
        ExpressionTree init = var.initializer();
        if (init != null && init.is(Tree.Kind.METHOD_INVOCATION)) {
          return GET_LOGGER.anyMatch((MethodInvocationTree) init);
        }
      }
    }
    return false;
  }

  @Override
  protected void handlePrintfFormat(MethodInvocationTree mit, String formatString, List<ExpressionTree> args) {
    List<String> params = getParameters(formatString, mit);
    if (usesMessageFormat(formatString, params)) {
      reportIssue(mit, "Looks like there is a confusion with the use of java.text.MessageFormat, parameters will be simply ignored here");
      return;
    }
    checkLineFeed(formatString, mit);
    if (params.isEmpty() && (!args.isEmpty() || !isLoggingMethod(mit))) {
      reportIssue(mit, "String contains no format specifiers.");
      return;
    }
    cleanupLineSeparator(params);
    if (!params.isEmpty() && argIndexes(params).size() <= args.size()) {
      verifyParameters(mit, args, params);
    }
  }

  private static boolean isLoggingMethod(MethodInvocationTree mit) {
    String methodName = mit.symbol().name();
    return "log".equals(methodName) || LEVELS.contains(methodName);
  }

  private void verifyParameters(MethodInvocationTree mit, List<ExpressionTree> args, List<String> params) {
    int index = 0;
    List<ExpressionTree> unusedArgs = new ArrayList<>(args);
    for (String rawParam : params) {
      String param = rawParam;
      int argIndex = index;
      if (param.contains("$")) {
        argIndex = getIndex(param) - 1;
        if (argIndex == -1) {
          return;
        }
        param = param.substring(param.indexOf('$') + 1);
      } else if (param.charAt(0) == '<') {
        //refers to previous argument
        argIndex = Math.max(0, argIndex - 1);
      }else {
        index++;
      }
      if (argIndex >= args.size()) {
        // indexes are obviously wrong - will be caught by S2275 (PrintfFailCheck)
        return;
      }
      ExpressionTree argExpressionTree = args.get(argIndex);
      unusedArgs.remove(argExpressionTree);
      Type argType = argExpressionTree.symbolType();
      checkBoolean(mit, param, argType);
    }
    reportUnusedArgs(mit, args, unusedArgs);
  }

  @Override
  protected void handleMessageFormat(MethodInvocationTree mit, String formatString, List<ExpressionTree> args) {
    String newFormatString = cleanupDoubleQuote(formatString);
    Set<Integer> indexes = getMessageFormatIndexes(newFormatString, mit);
    if (indexes.isEmpty() && formatArgumentsCount(mit) > 1) {
      reportIssue(mit, "String contains no format specifiers.");
      return;
    }
    List<ExpressionTree> newArgs = args;
    if (newArgs.size() == 1) {
      ExpressionTree firstArg = newArgs.get(0);
      if (firstArg.symbolType().isArray()) {
        if (isNewArrayWithInitializers(firstArg)) {
          newArgs = ((NewArrayTree) firstArg).initializers();
        } else {
          // size is unknown
          return;
        }
      }
    }
    checkToStringInvocation(newArgs);
    verifyParameters(mit, newArgs, indexes);
  }

  private static long formatArgumentsCount(MethodInvocationTree mit) {
    return mit.arguments().stream().filter(arg -> !arg.symbolType().is("org.slf4j.Marker")).count();
  }

  private void checkToStringInvocation(List<ExpressionTree> args) {
    args.stream()
      .filter(arg -> arg.is(Tree.Kind.METHOD_INVOCATION) && TO_STRING.matches((MethodInvocationTree) arg))
      .forEach(arg -> reportIssue(arg, getToStringMessage(arg)));
  }

  private static String getToStringMessage(ExpressionTree arg) {
    if (isInStringArrayInitializer(arg)) {
      return "No need to call \"toString()\" method since an array of Objects can be used here.";
    }
    return "No need to call \"toString()\" method as formatting and string conversion is done by the Formatter.";
  }

  private static boolean isInStringArrayInitializer(ExpressionTree arg) {
    return Optional.of(arg)
      .map(Tree::parent)
      .filter(tree -> tree.is(Tree.Kind.LIST))
      .map(Tree::parent)
      .filter(tree -> tree.is(Tree.Kind.NEW_ARRAY))
      .map(NewArrayTree.class::cast)
      .map(ExpressionTree::symbolType)
      .filter(Type::isArray)
      .map(Type.ArrayType.class::cast)
      .map(Type.ArrayType::elementType)
      .filter(type -> type.is(JAVA_LANG_STRING))
      .isPresent();
  }

  private void verifyParameters(MethodInvocationTree mit, List<ExpressionTree> args, Set<Integer> indexes) {
    List<ExpressionTree> unusedArgs = new ArrayList<>(args);
    for (int index : indexes) {
      if (index >= args.size()) {
        return;
      }
      unusedArgs.remove(args.get(index));
    }
    reportUnusedArgs(mit, args, unusedArgs);
  }

  private void reportUnusedArgs(MethodInvocationTree mit, List<ExpressionTree> args, List<ExpressionTree> unusedArgs) {
    for (ExpressionTree unusedArg : unusedArgs) {
      int i = args.indexOf(unusedArg);
      String stringArgIndex = "first";
      if (i == 1) {
        stringArgIndex = "2nd";
      } else if (i == 2) {
        stringArgIndex = "3rd";
      } else if (i >= 3) {
        stringArgIndex = (i + 1) + "th";
      }
      reportIssue(mit, stringArgIndex + " argument is not used.");
    }
  }


  private void checkBoolean(MethodInvocationTree mit, String param, Type argType) {
    if (param.charAt(0) == 'b' && !(argType.is("boolean") || argType.is("java.lang.Boolean"))) {
      reportIssue(mit, "Directly inject the boolean value.");
    }
  }

  private void checkLineFeed(String formatString, MethodInvocationTree mit) {
    if (formatString.contains("\\n")) {
      reportIssue(mit, "%n should be used in place of \\n to produce the platform-specific line separator.");
    }
  }

  private static boolean usesMessageFormat(String formatString, List<String> params) {
    return params.isEmpty() && (formatString.contains("{0") || formatString.contains("{1"));
  }

  @Override
  protected void handleOtherFormatTree(MethodInvocationTree mit, ExpressionTree formatTree) {
    if (isConcatenationOnSameLine(formatTree)) {
      reportIssue(mit, "Format specifiers should be used instead of string concatenation.");
    }
  }

  private static boolean isConcatenationOnSameLine(ExpressionTree formatStringTree) {
    return formatStringTree.is(Tree.Kind.PLUS)
      && operandsAreOnSameLine((BinaryExpressionTree) formatStringTree)
      && ConstantUtils.resolveAsConstant(formatStringTree) == null;
  }

  private static boolean operandsAreOnSameLine(BinaryExpressionTree formatStringTree) {
    return formatStringTree.leftOperand().firstToken().line() == formatStringTree.rightOperand().firstToken().line();
  }
}
