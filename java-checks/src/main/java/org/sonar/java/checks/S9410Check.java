/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9410")
public class S9410Check extends IssuableSubscriptionVisitor {

  private static final MethodMatchers LOOKUP_METHODS = MethodMatchers.create()
    .ofTypes("java.lang.invoke.MethodHandles.Lookup")
    .names("findVirtual", "findStatic", "findSpecial", "findConstructor")
    .withAnyParameters()
    .build();
  private static final MethodMatchers VAR_HANDLE_METHODS = MethodMatchers.create()
    .ofTypes("java.lang.invoke.MethodHandles")
    .names("findVarHandle", "findStaticVarHandle")
    .withAnyParameters()
    .build();
  private static final MethodMatchers METHOD_TYPE_FACTORY = MethodMatchers.create()
    .ofTypes("java.lang.invoke.MethodType")
    .names("methodType")
    .withAnyParameters()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree invocation = (MethodInvocationTree) tree;
    if (context.getSemanticModel() == null) {
      return;
    }
    if (hasKnownReceiver(invocation) && (LOOKUP_METHODS.matches(invocation) || isLookupInvocation(invocation))) {
      checkMethodLookup(invocation);
    } else if (hasKnownReceiver(invocation) && (VAR_HANDLE_METHODS.matches(invocation) || isVarHandleInvocation(invocation))) {
      checkFieldLookup(invocation);
    }
  }

  private static boolean hasKnownReceiver(MethodInvocationTree invocation) {
    return invocation.methodSelect() instanceof MemberSelectExpressionTree select && !select.expression().symbolType().isUnknown();
  }

  private static String invocationName(MethodInvocationTree invocation) {
    return invocation.methodSelect() instanceof MemberSelectExpressionTree select ? select.identifier().name() : "";
  }

  private static boolean isLookupInvocation(MethodInvocationTree invocation) {
    if (!(invocation.methodSelect() instanceof MemberSelectExpressionTree select)) {
      return false;
    }
    String methodName = invocationName(invocation);
    return select.expression().symbolType().name().equals("Lookup")
      && ("findVirtual".equals(methodName) || "findStatic".equals(methodName) || "findSpecial".equals(methodName) || "findConstructor".equals(methodName));
  }

  private static boolean isVarHandleInvocation(MethodInvocationTree invocation) {
    if (!(invocation.methodSelect() instanceof MemberSelectExpressionTree select)) {
      return false;
    }
    String methodName = invocationName(invocation);
    return (select.expression().symbolType().is("java.lang.invoke.MethodHandles") || select.expression().symbolType().name().equals("Lookup"))
      && ("findVarHandle".equals(methodName) || "findStaticVarHandle".equals(methodName));
  }

  private void checkMethodLookup(MethodInvocationTree invocation) {
    String name = invocationName(invocation);
    List<ExpressionTree> arguments = invocation.arguments();
    int methodTypeIndex;
    Type targetType;
    String memberName = null;
    boolean staticLookup = "findStatic".equals(name);
    if ("findConstructor".equals(name)) {
      if (arguments.size() != 2) {
        return;
      }
      targetType = classLiteralType(arguments.get(0));
      methodTypeIndex = 1;
    } else {
      if (arguments.size() != ("findSpecial".equals(name) ? 4 : 3)) {
        return;
      }
      targetType = classLiteralType(arguments.get(0));
      memberName = constantString(arguments.get(1));
      methodTypeIndex = 2;
    }
    if (targetType == null || (memberName == null && !"findConstructor".equals(name))) {
      return;
    }
    MethodSignature signature = methodSignature(arguments.get(methodTypeIndex));
    if (signature == null || !targetType.isClass()) {
      return;
    }
    boolean found = findMethod(targetType, memberName, signature, staticLookup, "findConstructor".equals(name));
    if (!found) {
      reportIssue(arguments.get(methodTypeIndex), "Use a type signature matching the target method.");
    }
  }

  private void checkFieldLookup(MethodInvocationTree invocation) {
    List<ExpressionTree> arguments = invocation.arguments();
    if (arguments.size() != 3) {
      return;
    }
    Type targetType = classLiteralType(arguments.get(0));
    String memberName = constantString(arguments.get(1));
    Type requestedType = classLiteralType(arguments.get(2));
    if (targetType == null || memberName == null || requestedType == null || !targetType.isClass()) {
      return;
    }
    boolean staticLookup = invocationName(invocation).equals("findStaticVarHandle");
    if (!findField(targetType, memberName, requestedType, staticLookup)) {
      reportIssue(arguments.get(2), "Use the declared type of the target field.");
    }
  }

  private static Type classLiteralType(ExpressionTree tree) {
    if (!(tree instanceof MemberSelectExpressionTree select) || !select.identifier().name().equals("class")) {
      return null;
    }
    Type type = select.expression().symbolType();
    return type.isUnknown() ? null : type;
  }

  private static String constantString(ExpressionTree tree) {
    return tree.asConstant(String.class).orElse(null);
  }

  private static MethodSignature methodSignature(ExpressionTree tree) {
    if (!(tree instanceof MethodInvocationTree invocation) || !METHOD_TYPE_FACTORY.matches(invocation)) {
      return null;
    }
    List<ExpressionTree> arguments = invocation.arguments();
    if (arguments.isEmpty()) {
      return null;
    }
    Type returnType = classLiteralType(arguments.get(0));
    if (returnType == null) {
      return null;
    }
    List<Type> parameters = new java.util.ArrayList<>();
    for (int i = 1; i < arguments.size(); i++) {
      Type parameter = classLiteralType(arguments.get(i));
      if (parameter == null) {
        return null;
      }
      parameters.add(parameter);
    }
    return new MethodSignature(returnType, parameters);
  }

  private static boolean findMethod(Type targetType, String name, MethodSignature signature, boolean staticLookup, boolean constructor) {
    String symbolName = constructor ? "<init>" : name;
    for (Symbol symbol : targetType.symbol().lookupSymbols(symbolName)) {
      if (!(symbol instanceof Symbol.MethodSymbol method) || method.isUnknown()) {
        continue;
      }
      if (method.isStatic() != staticLookup || !sameTypes(method.parameterTypes(), signature.parameters)) {
        continue;
      }
      Type returnType = method.returnType().type();
      if (constructor ? signature.returnType.isVoid() : returnType.equals(signature.returnType)) {
        return true;
      }
    }
    return false;
  }

  private static boolean findField(Type targetType, String name, Type requestedType, boolean staticLookup) {
    for (Symbol symbol : targetType.symbol().lookupSymbols(name)) {
      if (symbol.isVariableSymbol() && symbol.isStatic() == staticLookup && symbol.type().equals(requestedType)) {
        return true;
      }
    }
    return false;
  }

  private static boolean sameTypes(List<Type> actual, List<Type> expected) {
    if (actual.size() != expected.size()) {
      return false;
    }
    for (int i = 0; i < actual.size(); i++) {
      if (!actual.get(i).equals(expected.get(i))) {
        return false;
      }
    }
    return true;
  }

  private record MethodSignature(Type returnType, List<Type> parameters) {
  }
}
