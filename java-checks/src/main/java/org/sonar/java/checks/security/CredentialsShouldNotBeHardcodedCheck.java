/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ReassignmentFinder;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6437")
public class CredentialsShouldNotBeHardcodedCheck extends IssuableSubscriptionVisitor {
  private static final Logger LOG = Loggers.get(CredentialsShouldNotBeHardcodedCheck.class);

  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String GET_BYTES = "getBytes";
  private static final MethodMatchers STRING_TO_BYTE_ARRAY_METHODS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names(GET_BYTES)
      .addWithoutParametersMatcher()
      .build(),
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names(GET_BYTES)
      .addParametersMatcher(parameters -> parameters.size() == 1 &&
        (parameters.get(0).is("java.nio.charset.Charset") || parameters.get(0).is(JAVA_LANG_STRING))
      ).build(),
    MethodMatchers.create()
      .ofTypes(JAVA_LANG_STRING)
      .names(GET_BYTES)
      .addParametersMatcher("int", "int", "java.lang.byte[]", "int")
      .build()
  );


  private static Map<String, List<CredentialsMethod>> methodMatchers;

  public CredentialsShouldNotBeHardcodedCheck() {
    loadSignatures();
  }

  private static synchronized void loadSignatures() {
    if (methodMatchers != null) {
      return;
    }
    try {
      methodMatchers = loadMethods(Path.of("..", "credentials-methods.json"));
    } catch (IOException e) {
      LOG.warn(e.getMessage());
    }
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree invocation = (MethodInvocationTree) tree;
    String methodName = invocation.symbol().name();
    List<CredentialsMethod> candidates = methodMatchers.get(methodName);
    if (candidates == null) {
      return;
    }
    for (CredentialsMethod candidate : candidates) {
      MethodMatchers matcher = candidate.methodMatcher;
      if (matcher.matches(invocation)) {
        checkArguments(invocation, candidate.targetArguments).ifPresent(argument -> reportIssue(argument, ""));
      }
    }
  }

  private Optional<Tree> checkArguments(MethodInvocationTree invocation, List<TargetArgument> argumentsToExamine) {
    for (TargetArgument argumentToExamine : argumentsToExamine) {
      int argumentIndex = argumentToExamine.index;
      Arguments arguments = invocation.arguments();
      if (arguments.size() <= argumentIndex) {
        return Optional.empty();
      }
      ExpressionTree argument = arguments.get(argumentIndex);
      if (argument.is(Tree.Kind.STRING_LITERAL)) {
        reportIssue(invocation, "");
      } else if (argument.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) argument;
        Optional<Object> identifierAsConstant = identifier.asConstant();
        if (identifierAsConstant.isPresent()) {
          reportIssue(invocation, "");
        }
        Symbol symbol = identifier.symbol();
        if (!symbol.isVariableSymbol() || JUtils.isParameter(symbol) || isReassigned(symbol)) {
          return Optional.empty();
        }

        VariableTree variableTree = (VariableTree) symbol.declaration();
        org.sonar.plugins.java.api.semantic.Type type = variableTree.symbol().type();

        if (type.is("byte[]") && isByteArrayDerivedFromPlainText(variableTree)) {
          return Optional.of(argument);
        } else if (type.is(JAVA_LANG_STRING) && variableTree.initializer().asConstant().isPresent()) {
          return Optional.of(argument);
        }
      } else if (argument.is(Tree.Kind.METHOD_INVOCATION)) {
        if (isByteArrayDerivedFromPlainText((MethodInvocationTree) argument)) {
          return Optional.of(argument);
        }
      }
    }
    return Optional.empty();
  }

  private static boolean isReassigned(Symbol symbol) {
    return !ReassignmentFinder.getReassignments(symbol.owner().declaration(), symbol.usages()).isEmpty();
  }

  private static boolean isByteArrayDerivedFromPlainText(MethodInvocationTree invocation) {
    if (!STRING_TO_BYTE_ARRAY_METHODS.matches(invocation)) {
      return false;
    }
    ExpressionTree expressionTree = invocation.methodSelect();
    if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree expression = ((MemberSelectExpressionTree) expressionTree).expression();
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) expression;
        Symbol symbol = identifier.symbol();
        if (symbol.isVariableSymbol()) {
          VariableTree variable = (VariableTree) symbol.declaration();
          return variable.symbol().type().is(JAVA_LANG_STRING) && variable.initializer().asConstant().isPresent();
        }
      } else if (expression.is(Tree.Kind.STRING_LITERAL)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isByteArrayDerivedFromPlainText(VariableTree variableTree) {
    ExpressionTree initializer = variableTree.initializer();
    if (!initializer.is(Tree.Kind.METHOD_INVOCATION)) {
      return true;
    }
    MethodInvocationTree initializationCall = (MethodInvocationTree) initializer;
    if (!STRING_TO_BYTE_ARRAY_METHODS.matches(initializationCall)) {
      return true;
    }
    StringConstantFinder visitor = new StringConstantFinder();
    initializationCall.accept(visitor);
    return visitor.finding != null;
  }

  private static class StringConstantFinder extends BaseTreeVisitor {
    VariableTree finding;

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      ExpressionTree expressionTree = tree.methodSelect();
      if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
        expressionTree.accept(this);
      }
    }

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      ExpressionTree expression = tree.expression();
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) expression;
        Symbol symbol = identifier.symbol();
        if (symbol.isVariableSymbol()) {
          symbol.declaration().accept(this);
        }
      }
    }

    @Override
    public void visitVariable(VariableTree tree) {
      if (tree.symbol().type().is(JAVA_LANG_STRING) && tree.initializer().asConstant().isPresent()) {
        finding = tree;
      }
    }
  }

  static Map<String, List<CredentialsMethod>> loadMethods(Path path) throws IOException {
    Gson gson = new Gson();
    Type appSecRecordsCollection = new TypeToken<List<List<String>>>() {
    }.getType();
    String rawData;
    try (InputStream in = new FileInputStream(path.toFile())) {
      rawData = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    List<List<String>> jsonRecords = gson.fromJson(rawData, appSecRecordsCollection);
    Map<String, Integer> argumentTypeCount = new HashMap<>();
    Map<String, List<CredentialsMethod>> methodsGroupedByName = new TreeMap<>();
    for (List<String> jsonRecord : jsonRecords) {
      CredentialsMethod method = new CredentialsMethod(jsonRecord);
      for (TargetArgument argument: method.targetArguments) {
        Integer currentCount = argumentTypeCount.getOrDefault(argument.type, 0);
        argumentTypeCount.put(argument.type, currentCount + 1);
      }
      if (methodsGroupedByName.containsKey(method.methodName)) {
        methodsGroupedByName.get(method.methodName).add(method);
      } else {
        List<CredentialsMethod> methods = new ArrayList<>();
        methods.add(method);
        methodsGroupedByName.put(method.methodName, methods);
      }
    }
    return methodsGroupedByName;
  }

  static class CredentialsMethod {
    public final String groupId;
    public final String artifactId;
    public final String namespace;
    public final String classType;
    public final String methodType;
    public final String methodModifiersAndReturnType;
    public final String methodSignature;
    public final String methodName;
    public final List<TargetArgument> targetArguments;
    public final MethodMatchers methodMatcher;

    public CredentialsMethod(List<String> entry) {
      this.groupId = entry.get(1);
      this.artifactId = entry.get(2);
      this.namespace = entry.get(3);
      this.classType = entry.get(4);
      this.methodType = entry.get(5);
      this.methodModifiersAndReturnType = entry.get(6);
      this.methodSignature = entry.get(7);
      this.methodName = extractMethodName(this.methodSignature);
      List<Integer> argumentIndices = Stream.of(entry.get(8).split(","))
        .map(index -> Integer.valueOf(index.trim()) - 1)
        .collect(Collectors.toList());
      this.targetArguments = extractArguments(this.methodSignature, argumentIndices);
      this.methodMatcher = convertToMatchers(this);
    }

    private static MethodMatchers convertToMatchers(CredentialsMethod credentialsMethod) {
      int argumentListStart = credentialsMethod.methodSignature.indexOf('(');
      int argumentListEnd = credentialsMethod.methodSignature.indexOf(')', argumentListStart);
      String type = credentialsMethod.artifactId + "." + credentialsMethod.classType;
      int numberOfArguments = credentialsMethod.methodSignature.substring(argumentListStart + 1, argumentListEnd).split(",").length;

      if (credentialsMethod.methodType.equals("Constructor")) {
        return MethodMatchers.create()
          .ofTypes(type)
          .constructor()
          .addParametersMatcher(argumentList -> argumentList.size() == numberOfArguments)
          .build();
      }

      return MethodMatchers.create()
        .ofTypes(type)
        .names(credentialsMethod.methodName)
        .addParametersMatcher(argumentList -> argumentList.size() == numberOfArguments)
        .build();
    }

    private static String extractMethodName(String signature) {
      int argumentListStart = signature.indexOf('(');
      return signature.substring(0, argumentListStart);
    }

    private static List<TargetArgument> extractArguments(String signature, List<Integer> indices) {
      List<List<String>> arguments = splitArgumentTypeAndName(signature);
      return indices.stream()
        .filter(index -> 0 <= index && index < arguments.size())
        .map(index -> new TargetArgument(arguments.get(index).get(0), arguments.get(index).get(1), index))
        .collect(Collectors.toList());
    }

    private static List<List<String>> splitArgumentTypeAndName(String signature) {
      return tokenizeArguments(signature).stream()
        .map(argumentString -> {
          int index = argumentString.lastIndexOf(" ");
          return List.of(
            matchType(argumentString.substring(0, index).trim()),
            argumentString.substring(index).trim()
          );
        }).collect(Collectors.toList());
    }

    private static List<String> tokenizeArguments(String signature) {
      int start = signature.indexOf('(');
      int end = signature.indexOf(')');
      String parameters = signature.substring(start + 1, end);
      List<String> types = new ArrayList<>();
      int depth = 0;
      start = 0;
      for (int index = 0; index < parameters.length(); index++) {
        char character = parameters.charAt(index);
        switch (character) {
          case ',':
            if (depth == 0) {
              end = index;
              types.add(parameters.substring(start, end));
              start = end + 1;
            }
            break;
          case '<':
            depth++;
            break;
          case '>':
            depth--;
            break;
          default:
            break;
        }
      }
      types.add(parameters.substring(start));
      return types;
    }

    private static String matchType(String type) {
      switch (type) {
        case "byte[]":
          return "java.lang.byte[]";
        case "char[]":
          return "java.lang.char[]";
        case "String":
          return JAVA_LANG_STRING;
        default:
          return type;
      }
    }
  }

  static class TargetArgument {
    public final String type;
    public final String name;
    public final int index;

    TargetArgument(String type, String name, int index) {
      this.type = type;
      this.name = name;
      this.index = index;
    }
  }
}
