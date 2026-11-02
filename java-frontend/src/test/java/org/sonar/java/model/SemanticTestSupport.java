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

import java.util.List;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.VariableTree;

public final class SemanticTestSupport {
  private SemanticTestSupport() {
  }

  static Type type(Object tree) {
    Type value;
    if (tree instanceof AbstractTypedTree typed) {
      value = typed.symbolType();
    } else if (tree instanceof ClassTree cls) {
      value = cls.symbol().type();
    } else if (tree instanceof TypeParameterTree parameter) {
      value = parameter.symbol().type();
    } else {
      throw new IllegalArgumentException("No type: " + tree.getClass());
    }
    return value == Type.UNKNOWN ? null : value;
  }

  static Symbol symbol(Object tree) {
    Symbol value;
    if (tree instanceof IdentifierTree identifier) {
      value = identifier.symbol();
    } else if (tree instanceof VariableTree variable) {
      value = variable.symbol();
    } else if (tree instanceof ClassTree cls) {
      value = cls.symbol();
    } else if (tree instanceof MethodTree method) {
      value = method.symbol();
    } else if (tree instanceof ImportTree imported) {
      value = imported.symbol();
    } else if (tree instanceof MethodInvocationTree invocation) {
      value = invocation.methodSymbol();
    } else if (tree instanceof LambdaExpressionTree lambda) {
      value = lambda.symbol();
    } else {
      throw new IllegalArgumentException("No symbol: " + tree.getClass());
    }
    return value == Symbol.UNKNOWN_SYMBOL || value == Symbol.MethodSymbol.UNKNOWN_METHOD || value instanceof Symbol.LabelSymbol ? null : value;
  }

  static Symbol.MethodSymbol method(Object tree) {
    return (Symbol.MethodSymbol) symbol(tree);
  }

  static Symbol.VariableSymbol symbol(org.sonar.java.model.declaration.VariableTreeImpl tree) {
    return tree.symbol() instanceof Symbol.VariableSymbol variable ? variable : null;
  }

  static List<IdentifierTree> usages(Object value) {
    Symbol symbol = value instanceof Type type ? type.symbol() : (Symbol) value;
    var usages = symbol.usages();
    return usages.isEmpty() ? null : usages;
  }

  static Tree declaration(Object value) {
    return (value instanceof Type type ? type.symbol() : (Symbol) value).declaration();
  }

  static Symbol specialField(Type type, boolean isSuper) {
    var symbol = (JTypeSymbol) type.symbol();
    return isSuper ? symbol.superSymbol : symbol.thisSymbol;
  }

  static boolean isLambda(Symbol symbol) {
    return symbol instanceof JMethodSymbol psi && psi.isLambda();
  }

  static Symbol.MethodSymbol initializer(Symbol.TypeSymbol owner, boolean isStatic) {
    var psi = (JTypeSymbol) owner;
    return psi.sema.read(() -> psi.sema.initializer(owner, isStatic));
  }

  static int dimensions(Type type) {
    return type instanceof Type.ArrayType array && type.isArray() ? 1 + dimensions(array.elementType()) : 0;
  }
  public static void clearType(JavaTree tree) {
    if (tree instanceof AbstractTypedTree typed) {
      typed.type = SemanticValues.UNKNOWN_TYPE;
    } else if (tree instanceof ClassTreeImpl cls) {
      cls.typeSymbol = SemanticValues.UNKNOWN_TYPE_SYMBOL;
    } else if (tree instanceof TypeParameterTreeImpl parameter) {
      parameter.typeSymbol = SemanticValues.UNKNOWN_SYMBOL;
    } else {
      throw new IllegalArgumentException("No type attachment: " + tree.getClass());
    }
    if (tree instanceof KeywordThis || tree instanceof KeywordSuper) {
      ((IdentifierTreeImpl) tree).symbolSupplier = SemanticValues.UNKNOWN_SYMBOL;
    }
  }

  public static void clearSymbol(JavaTree tree) {
    if (tree instanceof IdentifierTreeImpl identifier) {
      identifier.symbolSupplier = SemanticValues.UNKNOWN_SYMBOL;
    } else if (tree instanceof VariableTreeImpl variable) {
      variable.variableSymbol = SemanticValues.UNKNOWN_SYMBOL;
    } else {
      throw new IllegalArgumentException("No symbol attachment: " + tree.getClass());
    }
  }
}
