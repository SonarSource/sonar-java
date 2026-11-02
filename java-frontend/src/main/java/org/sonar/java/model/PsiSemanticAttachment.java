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
package org.sonar.java.model;

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
import java.util.Objects;
import java.util.function.Supplier;
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

final class PsiSemanticAttachment<T> implements Supplier<T> {
  private final JSema sema;
  private final Supplier<T> factory;
  private T value;

  PsiSemanticAttachment(JSema sema, Supplier<T> factory) {
    this.sema = sema;
    this.factory = factory;
  }

  @Override
  public T get() {
    return sema.read(() -> {
      if (value == null) {
        value = Objects.requireNonNull(factory.get());
      }
      return value;
    });
  }

  static void attach(JSema sema, JavaTree.CompilationUnitTreeImpl root, Map<PsiElement, List<Tree>> nodes) {
    root.sema = sema;
    Map<Tree, PsiElement> origins = new IdentityHashMap<>();
    nodes.forEach((element, trees) -> trees.forEach(tree -> {
      if (!(element instanceof PsiIdentifier)) {
        origins.put(tree, element);
      } else {
        origins.putIfAbsent(tree, element);
      }
    }));
    origins.forEach((tree, element) -> {
      if (tree instanceof IdentifierTreeImpl identifier && identifier.labelSymbol == null) {
        identifier.symbolSupplier = new PsiSemanticAttachment<>(sema, () -> insideImport(element) ? Symbol.UNKNOWN_SYMBOL : symbol(sema, element));
      }
    });
    origins.forEach((tree, element) -> attachNode(sema, tree, element));
    origins.forEach((tree, element) -> {
      if (tree instanceof VariableTreeImpl variable && element instanceof PsiVariable declaration) {
        attachType(sema, variable.type(), declaration.getType());
      }
    });
    origins.forEach((tree, element) -> {
      if (tree instanceof ArrayTypeTree && !(tree.parent() instanceof ArrayTypeTree) && element instanceof PsiTypeElement type) {
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
        var target = resolved instanceof JSymbol symbol ? symbol.element : null;
        if (target != null && (!isDeclarationName(element) || element.getParent() instanceof PsiEnumConstant && resolved.isMethodSymbol()) && !insideImport(element)) {
          var usages = sema.usages.computeIfAbsent(sema.key(target), unused -> new ArrayList<>());
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

  private static void attachUnnamedPattern(JSema sema, com.intellij.psi.PsiUnnamedPattern unnamed, VariableTreeImpl variable) {
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
    variable.variableSymbol = new PsiSemanticAttachment<>(sema, () -> sema.symbol(declaration));
    sema.declarations.put(sema.key(declaration), variable);
    attachType(sema, variable.type(), declaration.getType());
  }

  private static void attachNode(JSema sema, Tree tree, PsiElement element) {
    if (tree instanceof AbstractTypedTree typed) {
      typed.type = insideImport(element) ? SemanticValues.UNKNOWN_TYPE : new PsiSemanticAttachment<>(sema, () -> type(sema, element));
      if (tree instanceof IdentifierTreeImpl identifier && identifier.name().equals("class")) {
        typed.type = SemanticValues.UNKNOWN_TYPE;
      }
    }
    if (tree instanceof AbstractPatternTree pattern) {
      pattern.type = new PsiSemanticAttachment<>(sema, () -> type(sema, element));
      if (element instanceof com.intellij.psi.PsiUnnamedPattern unnamed && tree instanceof org.sonar.plugins.java.api.tree.TypePatternTree typePattern) {
        attachUnnamedPattern(sema, unnamed, (VariableTreeImpl) typePattern.patternVariable());
      }
    }
    if (tree instanceof ClassTreeImpl cls && element instanceof PsiClass declaration) {
      cls.typeSymbol = new PsiSemanticAttachment<>(sema, () -> (Symbol.TypeSymbol) sema.symbol(declaration));
      sema.declarations.put(sema.key(declaration), tree);
    } else if (tree instanceof MethodTreeImpl method && element instanceof PsiMethod declaration) {
      method.methodSymbol = new PsiSemanticAttachment<>(sema, () -> brokenMethod(declaration) ? Symbol.MethodSymbol.UNKNOWN_METHOD : sema.method(declaration, PsiSubstitutor.EMPTY));
      if (!brokenMethod(declaration)) {
        sema.declarations.put(sema.key(declaration), tree);
      }
    } else if (tree instanceof VariableTreeImpl variable && element instanceof PsiVariable declaration) {
      variable.variableSymbol = new PsiSemanticAttachment<>(sema, () -> sema.symbol(declaration));
      sema.declarations.put(sema.key(declaration), tree);
      sema.symbol(declaration);
      if (declaration instanceof PsiEnumConstant constant && variable instanceof EnumConstantTreeImpl enumeration) {
        var creation = (NewClassTreeImpl) enumeration.initializer();
        ((IdentifierTreeImpl) creation.getConstructorIdentifier()).symbolSupplier = new PsiSemanticAttachment<>(sema, () -> sema.method(constant.resolveMethodGenerics()));
      }
    } else if (tree instanceof LambdaExpressionTreeImpl lambda && element instanceof PsiLambdaExpression declaration) {
      lambda.methodSymbol = new PsiSemanticAttachment<>(sema, () -> sema.lambda(declaration));
    } else if (tree instanceof MethodInvocationTreeImpl invocation && element instanceof PsiMethodCallExpression call) {
      invocation.methodSymbolSupplier = new PsiSemanticAttachment<>(sema, () -> sema.method(call));
      var identifier = invocation.methodSelect() instanceof MemberSelectExpressionTreeImpl select ? select.identifier() : invocation.methodSelect();
      ((IdentifierTreeImpl) identifier).symbolSupplier = invocation.methodSymbolSupplier::get;
      if (invocation.methodSelect() instanceof AbstractTypedTree selected) {
        selected.type = SemanticValues.UNKNOWN_TYPE;
      }
      if (call.getMethodExpression().getReferenceNameElement() != null
        && (call.getMethodExpression().textMatches("this") || call.getMethodExpression().textMatches("super") || call.getMethodExpression().getReferenceName().equals("super"))) {
        ((AbstractTypedTree) invocation).type = new PsiSemanticAttachment<>(sema, () -> {
          var owner = sema.symbol(PsiTreeUtil.getParentOfType(call, PsiClass.class)).type();
          return call.getMethodExpression().getReferenceName().equals("this") ? owner : owner.symbol().superClass();
        });
      }
    } else if (tree instanceof NewClassTreeImpl creation && element instanceof PsiNewExpression expression) {
      ((IdentifierTreeImpl) creation.getConstructorIdentifier()).symbolSupplier = new PsiSemanticAttachment<>(sema, () -> sema.constructor(expression));
    } else if (tree instanceof MethodReferenceTreeImpl reference && element instanceof PsiMethodReferenceExpression expression) {
      ((IdentifierTreeImpl) reference.method()).symbolSupplier = new PsiSemanticAttachment<>(sema, () -> sema.method(expression.advancedResolve(false)));
    } else if (tree instanceof JavaTree.ImportTreeImpl imported && element instanceof PsiImportStatementBase statement) {
      imported.symbolSupplier = statement instanceof PsiImportModuleStatement ? () -> null
        : new PsiSemanticAttachment<>(sema, () -> sema.symbol(statement.resolve()));
    } else if (tree instanceof TypeParameterTreeImpl parameter && element instanceof PsiTypeParameter declaration) {
      parameter.typeSymbol = new PsiSemanticAttachment<>(sema, () -> sema.symbol(declaration));
    }
    if (element instanceof PsiThisExpression || element instanceof PsiSuperExpression) {
      var identifier = tree instanceof MemberSelectExpressionTreeImpl select ? select.identifier() : tree;
      if (identifier instanceof IdentifierTreeImpl id) {
        id.symbolSupplier = new PsiSemanticAttachment<>(sema, () -> symbol(sema, element));
      }
    }
  }

  private static void attachType(JSema sema, Tree tree, com.intellij.psi.PsiType type) {
    if (tree instanceof AbstractTypedTree typed) {
      typed.type = new PsiSemanticAttachment<>(sema, () -> sema.type(type));
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

  private static Symbol symbol(JSema sema, PsiElement element) {
    if (element instanceof PsiIdentifier) {
      return symbol(sema, element.getParent());
    }
    if (element instanceof PsiThisExpression || element instanceof PsiSuperExpression) {
      var type = type(sema, element);
      var qualifier = element instanceof PsiQualifiedExpression expression ? expression.getQualifier() : null;
      var owner = qualifier instanceof PsiJavaCodeReferenceElement reference ? sema.symbol(reference.resolve())
        : sema.symbol(PsiTreeUtil.getParentOfType(element, PsiClass.class));
      return owner instanceof JTypeSymbol cls ? element instanceof PsiSuperExpression ? cls.superSymbol : cls.thisSymbol : Symbol.UNKNOWN_SYMBOL;
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

  private static PsiElement target(JSema sema, PsiElement element) {
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
    return null;
  }

  private static Type type(JSema sema, PsiElement element) {
    if (element instanceof PsiIdentifier) {
      return type(sema, element.getParent());
    }
    if (element instanceof PsiExpression expression) {
      if (expression instanceof PsiSwitchExpression && PsiTreeUtil.findChildrenOfType(expression, PsiYieldStatement.class).isEmpty()
        && !PsiTreeUtil.findChildrenOfType(expression, PsiReturnStatement.class).isEmpty()) {
        return Type.UNKNOWN;
      }
      if (expression instanceof PsiFunctionalExpression functional) {
        return sema.type(functional.getFunctionalInterfaceType());
      }
      var value = expression.getType();
      if (value == null && expression instanceof PsiReferenceExpression reference && reference.resolve() instanceof PsiClass cls) {
        return sema.symbol(cls).type();
      }
      return sema.type(value);
    }
    if (element instanceof PsiAnnotation annotation) {
      return sema.symbol(annotation.resolveAnnotationType()).type();
    }
    if (element instanceof PsiTypeElement type) {
      return sema.type(type.getType());
    }
    if (element instanceof PsiVariable variable) {
      return sema.type(variable.getType());
    }
    if (element instanceof PsiClass cls) {
      return sema.symbol(cls).type();
    }
    if (element instanceof PsiJavaCodeReferenceElement reference) {
      return sema.type(JavaPsiFacade.getElementFactory(sema.project).createType(reference));
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
}
