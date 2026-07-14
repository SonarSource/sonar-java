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
package org.sonar.java.checks.security;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2092")
public class SecureCookieCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Make sure creating this cookie without the \"secure\" flag is safe here.";

  private static final String JAX_RS_COOKIE = "javax.ws.rs.core.Cookie";
  private static final String JAX_RS_COOKIE_JAKARTA = "jakarta.ws.rs.core.Cookie";
  private static final String JAX_RS_NEW_COOKIE = "javax.ws.rs.core.NewCookie";
  private static final String JAX_RS_NEW_COOKIE_JAKARTA = "jakarta.ws.rs.core.NewCookie";
  private static final String SPRING_SAVED_COOKIE = "org.springframework.security.web.savedrequest.SavedCookie";
  private static final String PLAY_COOKIE = "play.mvc.Http$Cookie";
  private static final String SPRING_HTTP_COOKIE_BUILDER = "org.springframework.http.ResponseCookie$ResponseCookieBuilder";
  private static final List<String> COOKIES = Arrays.asList(
    "javax.servlet.http.Cookie",
    "jakarta.servlet.http.Cookie",
    "java.net.HttpCookie",
    JAX_RS_COOKIE,
    JAX_RS_COOKIE_JAKARTA,
    JAX_RS_NEW_COOKIE,
    JAX_RS_NEW_COOKIE_JAKARTA,
    "org.apache.shiro.web.servlet.SimpleCookie",
    SPRING_SAVED_COOKIE,
    PLAY_COOKIE,
    "play.mvc.Http$CookieBuilder",
    "org.springframework.boot.web.server.Cookie",
    SPRING_HTTP_COOKIE_BUILDER);

  private static final List<String> SETTER_NAMES = Arrays.asList("setSecure", "withSecure", "secure");

  /**
   * Some constructors have the 'secure' parameter and do not need a 'setSecure' call afterwards.
   */
  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String JAVA_UTIL_DATE = "java.util.Date";
  private static final String INT = "int";
  private static final String BOOLEAN = "boolean";

  private static final MethodMatchers CONSTRUCTORS_WITH_SECURE_PARAM_LAST = MethodMatchers.create()
    .ofTypes(JAX_RS_NEW_COOKIE, JAX_RS_NEW_COOKIE_JAKARTA)
    .constructor()
    .addParametersMatcher(JAX_RS_COOKIE, JAVA_LANG_STRING, INT, BOOLEAN)
    .addParametersMatcher(JAX_RS_COOKIE_JAKARTA, JAVA_LANG_STRING, INT, BOOLEAN)
    .addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, INT, JAVA_LANG_STRING, INT, BOOLEAN)
    .addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, INT, BOOLEAN)
    .build();

  private static final MethodMatchers CONSTRUCTORS_WITH_SECURE_PARAM_BEFORE_LAST = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes(JAX_RS_NEW_COOKIE, JAX_RS_NEW_COOKIE_JAKARTA)
      .constructor()
      .addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, INT, JAVA_LANG_STRING, INT, JAVA_UTIL_DATE, BOOLEAN, BOOLEAN)
      .addParametersMatcher(JAX_RS_COOKIE, JAVA_LANG_STRING, INT, JAVA_UTIL_DATE, BOOLEAN, BOOLEAN)
      .addParametersMatcher(JAX_RS_COOKIE_JAKARTA, JAVA_LANG_STRING, INT, JAVA_UTIL_DATE, BOOLEAN, BOOLEAN)
      .addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, INT, BOOLEAN, BOOLEAN)
      .build(),
    MethodMatchers.create()
      .ofTypes(SPRING_SAVED_COOKIE)
      .constructor()
      .addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, JAVA_LANG_STRING, INT, JAVA_LANG_STRING, BOOLEAN, INT)
      .build(),
    MethodMatchers.create()
      .ofTypes(PLAY_COOKIE)
      .constructor()
      .addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING, "java.lang.Integer", JAVA_LANG_STRING, JAVA_LANG_STRING, BOOLEAN, BOOLEAN)
      .build());

  private static final MethodMatchers CONSTRUCTORS_WITH_SECURE_PARAM_BEFORE_BEFORE_LAST = MethodMatchers.create()
    .ofTypes(PLAY_COOKIE)
    .constructor()
    .addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING, "java.lang.Integer", JAVA_LANG_STRING, JAVA_LANG_STRING, BOOLEAN, BOOLEAN, "play.mvc.Http$Cookie$SameSite")
    .build();

  private static final MethodMatchers RESPONSE_COOKIE_FROM = MethodMatchers.create()
    .ofTypes("org.springframework.http.ResponseCookie")
    .names("from")
    .addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING)
    .build();

  private static final MethodMatchers RESPONSE_COOKIE_MAX_AGE_ZERO = MethodMatchers.create()
    .ofTypes(SPRING_HTTP_COOKIE_BUILDER)
    .names("maxAge")
    .addParametersMatcher("long")
    .build();

  private final Map<Symbol.VariableSymbol, NewClassTree> unsecuredCookies = new HashMap<>();
  private final Map<Symbol.VariableSymbol, NewClassTree> deletionCandidateCookies = new HashMap<>();
  private final Set<NewClassTree> cookieConstructors = new HashSet<>();
  private final Deque<Symbol.TypeSymbol> enclosingClass = new ArrayDeque<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(
      Tree.Kind.VARIABLE,
      Tree.Kind.ASSIGNMENT,
      Tree.Kind.METHOD_INVOCATION,
      Tree.Kind.NEW_CLASS,
      Tree.Kind.CLASS);
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    unsecuredCookies.clear();
    deletionCandidateCookies.clear();
    cookieConstructors.clear();
    enclosingClass.clear();
    super.setContext(context);
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    cookieConstructors.forEach(r -> reportIssue(r.identifier(), MESSAGE));
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.VARIABLE)) {
      addToUnsecuredCookies((VariableTree) tree);
      addToDeletionCandidates((VariableTree) tree);
    } else if (tree.is(Tree.Kind.ASSIGNMENT)) {
      addToUnsecuredCookies((AssignmentExpressionTree) tree);
      addToDeletionCandidates((AssignmentExpressionTree) tree);
    } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      checkSecureCall((MethodInvocationTree) tree);
      checkMaxAgeCall((MethodInvocationTree) tree);
    } else if (tree.is(Tree.Kind.CLASS)) {
      enclosingClass.push(((ClassTree) tree).symbol());
    } else {
      checkConstructor((NewClassTree) tree);
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.CLASS)) {
      enclosingClass.pop();
    }
  }

  private void addToUnsecuredCookies(VariableTree variableTree) {
    ExpressionTree initializer = variableTree.initializer();
    Symbol variableTreeSymbol = variableTree.symbol();

    if (initializer != null && variableTreeSymbol.isVariableSymbol()) {
      boolean isInitializedWithConstructor = initializer.is(Tree.Kind.NEW_CLASS);
      boolean isMatchedType = isCookieClass(variableTreeSymbol.type()) || isCookieClass(initializer.symbolType());
      if (isInitializedWithConstructor && isMatchedType && isSecureParamFalse((NewClassTree) initializer)) {
        unsecuredCookies.put((Symbol.VariableSymbol) variableTreeSymbol, (NewClassTree) initializer);
      }
    }
  }

  private void addToUnsecuredCookies(AssignmentExpressionTree assignment) {
    if (assignment.expression().is(Tree.Kind.NEW_CLASS) && assignment.variable().is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree assignmentVariable = (IdentifierTree) assignment.variable();
      Symbol assignmentVariableSymbol = assignmentVariable.symbol();
      boolean isMatchedType = isCookieClass(assignmentVariable.symbolType()) || isCookieClass(assignment.expression().symbolType());
      if (isMatchedType && isSecureParamFalse((NewClassTree) assignment.expression()) && !assignmentVariableSymbol.isUnknown()) {
        unsecuredCookies.put((Symbol.VariableSymbol) assignmentVariableSymbol, (NewClassTree) assignment.expression());
      }
    }
  }

  private void checkSecureCall(MethodInvocationTree mit) {
    if (isSetSecureCall(mit) && mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionsHelper.ValueResolution<Boolean> valueResolution = ExpressionsHelper.getConstantValueAsBoolean(mit.arguments().get(0));
      Boolean secureArgument = valueResolution.value();
      boolean isFalse = secureArgument != null && !secureArgument;
      if (isFalse && !isDeletionCookieChain(mit)) {
        reportIssue(mit.arguments(), MESSAGE, valueResolution.valuePath(), null);
      }
      ExpressionTree methodObject = ((MemberSelectExpressionTree) mit.methodSelect()).expression();
      if (methodObject.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifierTree = (IdentifierTree) methodObject;
        NewClassTree newClassTree = unsecuredCookies.remove(identifierTree.symbol());
        cookieConstructors.remove(newClassTree);
      }
    }
  }

  private void checkConstructor(NewClassTree tree) {
    if (isCookieClass(tree.symbolType()) && isSecureParamFalse(tree) && !isSelfInstantiation(tree)) {
      cookieConstructors.add(tree);
    }
  }

  /**
   * Tracked separately from {@link #unsecuredCookies}: whether a cookie's value is null/empty is independent of
   * whether it's secure, so a deletion candidate isn't necessarily a pending "unsecured" issue (e.g. secure=true).
   */
  private void addToDeletionCandidates(VariableTree variableTree) {
    ExpressionTree initializer = variableTree.initializer();
    Symbol variableTreeSymbol = variableTree.symbol();

    if (initializer != null && initializer.is(Tree.Kind.NEW_CLASS) && variableTreeSymbol.isVariableSymbol()
      && isCookieClass(variableTreeSymbol.type()) && isDeletionValue((NewClassTree) initializer)) {
      deletionCandidateCookies.put((Symbol.VariableSymbol) variableTreeSymbol, (NewClassTree) initializer);
    }
  }

  private void addToDeletionCandidates(AssignmentExpressionTree assignment) {
    if (assignment.expression().is(Tree.Kind.NEW_CLASS) && assignment.variable().is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree assignmentVariable = (IdentifierTree) assignment.variable();
      Symbol assignmentVariableSymbol = assignmentVariable.symbol();
      if (isCookieClass(assignmentVariable.symbolType()) && isDeletionValue((NewClassTree) assignment.expression()) && !assignmentVariableSymbol.isUnknown()) {
        deletionCandidateCookies.put((Symbol.VariableSymbol) assignmentVariableSymbol, (NewClassTree) assignment.expression());
      }
    }
  }

  /**
   * Value is always the 2nd constructor argument across the cookie types tracked here (e.g. {@code Cookie(name, value)});
   * the {@code >= 2} guard excludes no-arg/name-only overloads rather than acting as a general bounds check.
   */
  private static boolean isDeletionValue(NewClassTree newClassTree) {
    Arguments arguments = newClassTree.arguments();
    return arguments.size() >= 2 && isNullOrEmptyLiteral(arguments.get(1));
  }

  private void checkMaxAgeCall(MethodInvocationTree mit) {
    if (isSetMaxAgeZeroCall(mit) && mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree methodObject = ((MemberSelectExpressionTree) mit.methodSelect()).expression();
      if (methodObject.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifierTree = (IdentifierTree) methodObject;
        NewClassTree newClassTree = deletionCandidateCookies.remove(identifierTree.symbol());
        cookieConstructors.remove(newClassTree);
      }
    }
  }

  private static boolean isSetMaxAgeZeroCall(MethodInvocationTree mit) {
    return mit.arguments().size() == 1
      && !mit.methodSymbol().isUnknown()
      && !mit.methodSymbol().owner().isUnknown()
      && isCookieClass(mit.methodSymbol().owner().type())
      && "setMaxAge".equals(getIdentifier(mit).name())
      && LiteralUtils.isZero(mit.arguments().get(0));
  }

  /**
   * Only suppresses the Spring builder path: the servlet/JAX-RS Cookie constructor path is handled separately
   * by {@link #deletionCandidateCookies}/{@link #checkMaxAgeCall}, since it never reaches this method.
   * Walks the whole fluent chain rather than just {@code mit}'s receivers, since {@code .from(...)}
   * and {@code .maxAge(0)} can appear in either order relative to the {@code secure(false)} call being checked.
   */
  private static boolean isDeletionCookieChain(MethodInvocationTree mit) {
    boolean hasEmptyOrNullValue = false;
    boolean hasMaxAgeZero = false;
    ExpressionTree current = chainRoot(mit);
    while (current != null && current.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree currentMit = (MethodInvocationTree) current;
      if (RESPONSE_COOKIE_FROM.matches(currentMit)) {
        hasEmptyOrNullValue = isNullOrEmptyLiteral(currentMit.arguments().get(1));
      } else if (RESPONSE_COOKIE_MAX_AGE_ZERO.matches(currentMit) && LiteralUtils.isZero(currentMit.arguments().get(0))) {
        hasMaxAgeZero = true;
      }
      ExpressionTree methodSelect = currentMit.methodSelect();
      current = methodSelect.is(Tree.Kind.MEMBER_SELECT) ? ((MemberSelectExpressionTree) methodSelect).expression() : null;
    }
    return hasEmptyOrNullValue && hasMaxAgeZero;
  }

  /**
   * Climbs to the outermost method call of the fluent chain {@code mit} belongs to, so the whole chain
   * (including calls made after {@code mit}, e.g. {@code secure(false)} followed by {@code maxAge(0)}) gets inspected.
   */
  private static ExpressionTree chainRoot(MethodInvocationTree mit) {
    ExpressionTree root = mit;
    Tree parent = mit.parent();
    while (parent != null && parent.is(Tree.Kind.MEMBER_SELECT) && parent.parent() != null && parent.parent().is(Tree.Kind.METHOD_INVOCATION)) {
      root = (ExpressionTree) parent.parent();
      parent = root.parent();
    }
    return root;
  }

  private static boolean isNullOrEmptyLiteral(ExpressionTree expression) {
    return expression.is(Tree.Kind.NULL_LITERAL) || LiteralUtils.isEmptyString(expression);
  }

  private boolean isSelfInstantiation(NewClassTree tree) {
    Symbol.TypeSymbol enclosing = enclosingClass.peek();
    return enclosing != null && !tree.symbolType().isUnknown() && tree.symbolType().equals(enclosing.type());
  }

  private static boolean isSecureParamFalse(NewClassTree newClassTree) {
    ExpressionTree secureArgument = null;
    Arguments arguments = newClassTree.arguments();
    if (CONSTRUCTORS_WITH_SECURE_PARAM_LAST.matches(newClassTree)) {
      secureArgument = arguments.get(arguments.size() - 1);
    } else if (CONSTRUCTORS_WITH_SECURE_PARAM_BEFORE_LAST.matches(newClassTree)) {
      secureArgument = arguments.get(arguments.size() - 2);
    } else if (CONSTRUCTORS_WITH_SECURE_PARAM_BEFORE_BEFORE_LAST.matches(newClassTree)) {
      secureArgument = arguments.get(arguments.size() - 3);
    }
    if (secureArgument != null) {
      return LiteralUtils.isFalse(secureArgument);
    }
    return true;
  }

  private static boolean isSetSecureCall(MethodInvocationTree mit) {
    return mit.arguments().size() == 1
      && !mit.methodSymbol().isUnknown()
      && !mit.methodSymbol().owner().isUnknown()
      && isCookieClass(mit.methodSymbol().owner().type())
      && SETTER_NAMES.stream().anyMatch(getIdentifier(mit).name()::equals);
  }

  private static boolean isCookieClass(Type type) {
    return COOKIES.stream().anyMatch(type::isSubtypeOf);
  }

  private static IdentifierTree getIdentifier(MethodInvocationTree mit) {
    IdentifierTree id;
    if (mit.methodSelect().is(Tree.Kind.IDENTIFIER)) {
      id = (IdentifierTree) mit.methodSelect();
    } else {
      id = ((MemberSelectExpressionTree) mit.methodSelect()).identifier();
    }
    return id;
  }

}
