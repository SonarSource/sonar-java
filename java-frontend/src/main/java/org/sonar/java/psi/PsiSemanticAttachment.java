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
package org.sonar.java.psi;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDeconstructionPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFunctionalExpression;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiImportModuleStatement;
import com.intellij.psi.PsiImportStatementBase;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiMethodReferenceExpression;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiQualifiedExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiSuperExpression;
import com.intellij.psi.PsiSwitchExpression;
import com.intellij.psi.PsiThisExpression;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiTypeTestPattern;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.PsiYieldStatement;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.TypeParameterTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.EnumConstantTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.LambdaExpressionTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.MethodReferenceTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.model.pattern.AbstractPatternTree;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

final class PsiSemanticAttachment {

  /**
   * Interns references before registering declarations, then completes context-dependent calls and ordered usages.
   * Each semantic model retains only its compilation unit's declarations and usages, matching the existing frontend contract.
   */
  static void attach(PsiSema sema, JavaTree.CompilationUnitTreeImpl root, Map<PsiElement, List<Tree>> nodes) {
    root.sema = sema;
    Map<Tree, PsiElement> origins = new IdentityHashMap<>();
    nodes.forEach((element, trees) -> trees.forEach(tree -> {
      if (!(element instanceof PsiIdentifier)) {
        origins.put(tree, element);
      } else {
        origins.putIfAbsent(tree, element);
      }
    }));
    Map<PsiLambdaExpression, Boolean> unresolvedLambdas = new IdentityHashMap<>();
    origins.entrySet().removeIf(entry -> insideUnresolvedLambda(entry.getValue(), unresolvedLambdas));
    origins.forEach((tree, element) -> {
      if (tree instanceof IdentifierTreeImpl identifier && identifier.labelSymbol == null) {
        identifier.setSymbol(insideImport(element) ? Symbol.UNKNOWN_SYMBOL : symbol(sema, element));
      }
    });
    origins.forEach((tree, element) -> attachNode(sema, tree, element));
    origins.forEach((tree, element) -> {
      if (tree instanceof VariableTreeImpl variable && element instanceof PsiVariable declaration) {
        if (!sema.type(declaration.getType()).isUnknown()) {
          attachType(sema, variable.type(), declaration.getType());
        }
      } else if (tree instanceof MethodTreeImpl method && element instanceof PsiMethod declaration && method.returnType() != null) {
        if (!sema.type(declaration.getReturnType()).isUnknown()) {
          attachType(sema, method.returnType(), declaration.getReturnType());
        }
      }
    });
    origins.forEach((tree, element) -> {
      if (tree instanceof ArrayTypeTree && !(tree.parent() instanceof ArrayTypeTree) && element instanceof PsiTypeElement type
        && !(tree.parent() instanceof MethodTreeImpl) && !sema.type(type.getType()).isUnknown()) {
        attachType(sema, tree, type.getType());
      }
    });
    origins.forEach((tree, element) -> {
      if (tree instanceof MethodInvocationTreeImpl || tree instanceof NewClassTreeImpl || tree instanceof MethodReferenceTreeImpl
        || tree instanceof EnumConstantTreeImpl) {
        attachNode(sema, tree, element);
      }
    });
    origins.forEach((tree, element) -> {
      if (tree instanceof IdentifierTreeImpl identifier && identifier.labelSymbol == null) {
        var resolved = identifier.symbol();
        var target = resolved instanceof PsiSymbol symbol ? symbol.element : null;
        if (target != null && (!isDeclarationName(element) || element.getParent() instanceof PsiEnumConstant && resolved.isMethodSymbol()) && !insideImport(element)) {
          var usages = sema.usages.computeIfAbsent(sema.key(target), unused -> new ArrayList<>());
          if (!usages.contains(identifier)) {
            usages.add(identifier);
          }
        }
        if (resolved.isMethodSymbol() && element instanceof PsiJavaCodeReferenceElement reference
          && reference.getParent() instanceof PsiNewExpression && reference.resolve() instanceof PsiClass cls) {
          var usages = sema.usages.computeIfAbsent(sema.key(cls), unused -> new ArrayList<>());
          if (!usages.contains(identifier)) {
            usages.add(identifier);
          }
        }
      }
    });
    sema.usages.values().forEach(usages -> usages.sort(Comparator
      .comparingInt((IdentifierTree identifier) -> identifier.identifierToken().range().start().line())
      .thenComparingInt(identifier -> identifier.identifierToken().range().start().columnOffset())));
  }

  private static void attachMethodIdentifier(IdentifierTreeImpl identifier, Symbol.MethodSymbol method) {
    identifier.setSymbol(method.isUnknown() ? Symbol.UNKNOWN_SYMBOL : method);
  }

  private static boolean insideUnresolvedLambda(PsiElement element, Map<PsiLambdaExpression, Boolean> unresolved) {
    for (var current = element; current != null; current = current.getParent()) {
      if (current instanceof PsiLambdaExpression lambda
        && unresolved.computeIfAbsent(lambda, value -> com.intellij.psi.LambdaUtil.getFunctionalInterfaceMethod(value) == null)) {
        return true;
      }
    }
    return false;
  }

  private static void attachUnnamedPattern(PsiSema sema, com.intellij.psi.PsiUnnamedPattern unnamed, VariableTreeImpl variable) {
    var declaration = new com.intellij.psi.impl.light.LightVariableBuilder<>("_", unnamed.getTypeElement().getType(), unnamed) {
      @Override
      public PsiElement getParent() {
        return unnamed;
      }

      @Override
      public com.intellij.psi.PsiFile getContainingFile() {
        return unnamed.getContainingFile();
      }

      @Override
      public com.intellij.openapi.util.TextRange getTextRange() {
        return unnamed.getTextRange();
      }
    };
    variable.setSymbol(sema.symbol(declaration));
    sema.declarations.put(sema.key(declaration), variable);
    attachType(sema, variable.type(), declaration.getType());
  }

  private static void attachNode(PsiSema sema, Tree tree, PsiElement element) {
    if (tree instanceof AbstractTypedTree typed) {
      typed.setSymbolType(insideImport(element) ? Type.UNKNOWN : type(sema, element));
      if (tree instanceof IdentifierTreeImpl identifier && identifier.name().equals("class")) {
        typed.setSymbolType(Type.UNKNOWN);
      }
    }
    if (tree instanceof AbstractPatternTree pattern) {
      pattern.setSymbolType(type(sema, element));
      if (element instanceof com.intellij.psi.PsiUnnamedPattern unnamed && tree instanceof org.sonar.plugins.java.api.tree.TypePatternTree typePattern) {
        attachUnnamedPattern(sema, unnamed, (VariableTreeImpl) typePattern.patternVariable());
      }
    }
    if (tree instanceof ClassTreeImpl cls && element instanceof PsiClass declaration) {
      cls.setSymbol((Symbol.TypeSymbol) sema.symbol(declaration));
      sema.declarations.put(sema.key(declaration), tree);
    } else if (tree instanceof MethodTreeImpl method && element instanceof PsiMethod declaration) {
      method.setSymbol(brokenMethod(declaration) ? Symbol.MethodSymbol.UNKNOWN_METHOD : sema.method(declaration, PsiSubstitutor.EMPTY));
      if (!brokenMethod(declaration)) {
        sema.declarations.put(sema.key(declaration), tree);
      }
    } else if (tree instanceof VariableTreeImpl variable && element instanceof PsiVariable declaration) {
      variable.setSymbol(sema.symbol(declaration));
      sema.declarations.put(sema.key(declaration), tree);
      sema.symbol(declaration);
      if (declaration instanceof PsiEnumConstant constant && variable instanceof EnumConstantTreeImpl enumeration) {
        var creation = (NewClassTreeImpl) enumeration.initializer();
        attachMethodIdentifier((IdentifierTreeImpl) creation.getConstructorIdentifier(), sema.method(constant.resolveMethodGenerics()));
      }
    } else if (tree instanceof LambdaExpressionTreeImpl lambda && element instanceof PsiLambdaExpression declaration) {
      lambda.setSymbol(sema.lambda(declaration));
    } else if (tree instanceof MethodInvocationTreeImpl invocation && element instanceof PsiMethodCallExpression call) {
      invocation.setMethodSymbol(sema.method(call));
      var identifier = invocation.methodSelect() instanceof MemberSelectExpressionTreeImpl select ? select.identifier() : invocation.methodSelect();
      attachMethodIdentifier((IdentifierTreeImpl) identifier, invocation.methodSymbol());
      if (invocation.methodSelect() instanceof AbstractTypedTree selected) {
        selected.setSymbolType(Type.UNKNOWN);
      }
      if (call.getMethodExpression().getReferenceNameElement() != null
        && (call.getMethodExpression().textMatches("this") || call.getMethodExpression().textMatches("super") || call.getMethodExpression().getReferenceName().equals("super"))) {
        var owner = sema.symbol(PsiTreeUtil.getParentOfType(call, PsiClass.class)).type();
        ((AbstractTypedTree) invocation).setSymbolType(call.getMethodExpression().getReferenceName().equals("this") ? owner : owner.symbol().superClass());
      }
    } else if (tree instanceof NewClassTreeImpl creation && element instanceof PsiNewExpression expression) {
      attachMethodIdentifier((IdentifierTreeImpl) creation.getConstructorIdentifier(), sema.constructor(expression));
    } else if (tree instanceof MethodReferenceTreeImpl reference && element instanceof PsiMethodReferenceExpression expression) {
      attachMethodIdentifier((IdentifierTreeImpl) reference.method(), sema.method(expression));
    } else if (tree instanceof JavaTree.ImportTreeImpl imported && element instanceof PsiImportStatementBase statement) {
      imported.setSymbol(statement instanceof PsiImportModuleStatement ? null : sema.symbol(statement.resolve()));
    } else if (tree instanceof TypeParameterTreeImpl parameter && element instanceof PsiTypeParameter declaration) {
      parameter.setSymbol(sema.symbol(declaration));
    }
    if (element instanceof PsiThisExpression || element instanceof PsiSuperExpression) {
      var identifier = tree instanceof MemberSelectExpressionTreeImpl select ? select.identifier() : tree;
      if (identifier instanceof IdentifierTreeImpl id) {
        id.setSymbol(symbol(sema, element));
      }
    }
  }

  private static void attachType(PsiSema sema, Tree tree, com.intellij.psi.PsiType type) {
    if (tree instanceof AbstractTypedTree typed) {
      typed.setSymbolType(sema.type(type));
    }
    if (tree instanceof ArrayTypeTree array && type instanceof PsiArrayType arrayType) {
      attachType(sema, array.type(), arrayType.getComponentType());
    }
  }

  private static boolean insideImport(PsiElement element) {
    return PsiTreeUtil.getParentOfType(element, PsiImportStatementBase.class) != null;
  }

  private static boolean brokenMethod(PsiMethod method) {
    var owner = method.getContainingClass();
    if (owner != null) {
      for (var previous : owner.getMethods()) {
        if (previous == method) {
          break;
        }
        if (previous.getName().equals(method.getName()) && Arrays.equals(
          Arrays.stream(previous.getParameterList().getParameters()).map(parameter -> com.intellij.psi.util.TypeConversionUtil.erasure(parameter.getType()).getCanonicalText()).toArray(),
          Arrays.stream(method.getParameterList().getParameters()).map(parameter -> com.intellij.psi.util.TypeConversionUtil.erasure(parameter.getType()).getCanonicalText()).toArray())) {
          return true;
        }
      }
    }
    return method.getReturnTypeElement() != null && PsiTreeUtil.findChildrenOfType(method.getReturnTypeElement(), PsiJavaCodeReferenceElement.class).stream()
      .anyMatch(reference -> reference.getQualifier() instanceof PsiJavaCodeReferenceElement qualifier && reference.resolve() == null && qualifier.resolve() == null);
  }

  private static boolean isDeclarationName(PsiElement element) {
    return element instanceof PsiIdentifier && element.getParent() instanceof PsiNameIdentifierOwner;
  }

  private static Symbol symbol(PsiSema sema, PsiElement element) {
    if (element instanceof PsiIdentifier) {
      return symbol(sema, element.getParent());
    }
    if (element instanceof PsiThisExpression || element instanceof PsiSuperExpression) {
      var type = type(sema, element);
      var qualifier = element instanceof PsiQualifiedExpression expression ? expression.getQualifier() : null;
      var owner = qualifier instanceof PsiJavaCodeReferenceElement reference ? sema.symbol(reference.resolve())
        : sema.symbol(PsiTreeUtil.getParentOfType(element, PsiClass.class));
      return owner instanceof PsiTypeSymbol cls ? element instanceof PsiSuperExpression ? cls.superSymbol : cls.thisSymbol : Symbol.UNKNOWN_SYMBOL;
    }
    if (element instanceof PsiReferenceExpression reference && reference.getParent() instanceof PsiMethodCallExpression call) {
      return sema.method(call);
    }
    if (element instanceof PsiReferenceExpression reference) {
      var resolved = reference.advancedResolve(false);
      if (resolved.isValidResult() && resolved.getElement() instanceof PsiField field) {
        return sema.variable(field, resolved.getSubstitutor());
      }
    }
    if (element instanceof PsiTypeElement type) {
      return sema.type(type.getType()).symbol();
    }
    var target = target(sema, element);
    if (target == null && element instanceof PsiJavaCodeReferenceElement reference && !(element instanceof PsiReferenceExpression)) {
      return type(sema, reference).symbol();
    }
    return target == null ? Symbol.UNKNOWN_SYMBOL : sema.symbol(target);
  }

  private static PsiElement target(PsiSema sema, PsiElement element) {
    if (element instanceof PsiIdentifier) {
      var parent = element.getParent();
      if (parent instanceof PsiNameIdentifierOwner owner && owner.getNameIdentifier() == element) {
        return parent;
      }
      return target(sema, parent);
    }
    if (element instanceof PsiJavaCodeReferenceElement reference) {
      var result = reference.advancedResolve(false);
      return result.isValidResult() || result.getElement() instanceof PsiPackage ? result.getElement() : null;
    }
    if (element instanceof PsiClass || element instanceof PsiVariable || element instanceof PsiMethod) {
      return element;
    }
    if (element instanceof PsiNameValuePair attribute) {
      var annotation = PsiTreeUtil.getParentOfType(attribute, PsiAnnotation.class);
      var declaration = annotation == null ? null : annotation.resolveAnnotationType();
      var methods = declaration == null ? new PsiMethod[0] : declaration.findMethodsByName(attribute.getName(), false);
      return methods.length == 1 ? methods[0] : null;
    }
    return null;
  }

  private static Type type(PsiSema sema, PsiElement element) {
    if (element instanceof PsiIdentifier) {
      return type(sema, element.getParent());
    }
    if (element instanceof PsiExpression expression) {
      if (expression instanceof PsiMethodCallExpression call && sema.method(call).isUnknown()) {
        return Type.UNKNOWN;
      }
      if (expression instanceof PsiSwitchExpression && PsiTreeUtil.findChildrenOfType(expression, PsiYieldStatement.class).isEmpty()
        && !PsiTreeUtil.findChildrenOfType(expression, PsiReturnStatement.class).isEmpty()) {
        return Type.UNKNOWN;
      }
      if (expression instanceof PsiFunctionalExpression functional) {
        return sema.type(functional.getFunctionalInterfaceType());
      }
      var value = expression.getType();
      if (expression instanceof PsiMethodCallExpression call && sema.method(call) instanceof PsiMethodSymbol method && method.method.hasTypeParameters()) {
        var expected = com.intellij.psi.util.PsiTypesUtil.getExpectedTypeByParent(call);
        if (value != null && expected != null && !sema.type(expected).isUnknown()
          && !com.intellij.psi.util.TypeConversionUtil.isAssignable(expected, value)) {
          return Type.UNKNOWN;
        }
      }
      if (value == null && expression instanceof PsiReferenceExpression reference && reference.resolve() instanceof PsiClass cls) {
        return sema.symbol(cls).type();
      }
      return sema.type(value);
    }
    if (element instanceof PsiAnnotation annotation) {
      return annotation.getQualifiedName() == null ? Type.UNKNOWN : sema.type(JavaPsiFacade.getElementFactory(sema.project)
        .createTypeByFQClassName(annotation.getQualifiedName(), annotation.getResolveScope()));
    }
    if (element instanceof PsiTypeElement type) {
      return syntaxType(sema, type);
    }
    if (element instanceof PsiVariable variable) {
      return sema.type(variable.getType());
    }
    if (element instanceof PsiClass cls) {
      return sema.symbol(cls).type();
    }
    if (element instanceof PsiJavaCodeReferenceElement reference) {
      return syntaxType(sema, reference, JavaPsiFacade.getElementFactory(sema.project).createType(reference));
    }
    if (element instanceof PsiTypeTestPattern pattern) {
      return sema.type(pattern.getCheckType().getType());
    }
    if (element instanceof PsiDeconstructionPattern pattern) {
      return sema.type(pattern.getTypeElement().getType());
    }
    if (element instanceof com.intellij.psi.PsiUnnamedPattern pattern) {
      return sema.type(pattern.getTypeElement().getType());
    }
    return Type.UNKNOWN;
  }

  private static Type syntaxType(PsiSema sema, PsiTypeElement type) {
    var reference = type.getInnermostComponentReferenceElement();
    return syntaxType(sema, reference, type.getType());
  }

  private static Type syntaxType(PsiSema sema, PsiJavaCodeReferenceElement reference, com.intellij.psi.PsiType type) {
    if (reference != null && reference.resolve() == null
      && reference.getQualifier() instanceof PsiJavaCodeReferenceElement qualifier && qualifier.resolve() == null) {
      while (qualifier.getQualifier() instanceof PsiJavaCodeReferenceElement parent && parent.resolve() == null) {
        qualifier = parent;
      }
      return sema.recoveredType(type, qualifier.getQualifiedName());
    }
    return sema.type(type);
  }
}
