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
package org.sonar.java.semsitter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

/**
 * One line per tree node with its kind, its position and what the semantic model says about it, so that two frontends
 * can be compared node by node.
 */
final class TreeDescription {

  private TreeDescription() {
  }

  static List<String> describe(Tree tree) {
    List<String> lines = new ArrayList<>();
    describe(tree, lines, true);
    lines.addAll(warnings(tree));
    return lines;
  }

  static List<String> warnings(Tree tree) {
    List<String> lines = new ArrayList<>();
    if (tree instanceof JavaTree.CompilationUnitTreeImpl unit) {
      unit.supportedWarningKinds().stream().sorted().forEach(kind -> unit.warnings(kind).forEach(warning -> lines.add("WARNING " + kind + " "
        + warning.syntaxTree().kind() + position(warning.syntaxTree()) + " " + warning.message())));
    }
    return lines;
  }

  private static String position(Tree tree) {
    SyntaxToken first = tree.firstToken();
    return first == null ? "" : " @" + first.range().start().line() + ":" + first.range().start().column();
  }

  /** Only the kind and the position of each node. */
  static List<String> shape(Tree tree) {
    List<String> lines = new ArrayList<>();
    describe(tree, lines, false);
    return lines;
  }

  private static void describe(Tree tree, List<String> lines, boolean semantics) {
    StringBuilder line = new StringBuilder(tree.kind().name());
    SyntaxToken first = tree.firstToken();
    if (first != null) {
      line.append(" @").append(first.range().start().line()).append(':').append(first.range().start().column());
    }
    if (semantics) {
      describeSemantics(tree, line);
    }
    lines.add(line.toString());
    if (semantics) {
      Symbol declared = declaredOrCalled(tree);
      if (declared != null && !declared.isUnknown()) {
        lines.add("  " + SymbolDetails.of(declared));
      }
    }
    JavaTree javaTree = (JavaTree) tree;
    if (javaTree.isLeaf()) {
      return;
    }
    for (Tree child : javaTree.getChildren()) {
      if (child != null) {
        describe(child, lines, semantics);
      }
    }
  }

  @Nullable
  private static Symbol declaredOrCalled(Tree tree) {
    if (tree instanceof ClassTree type) {
      return type.symbol();
    } else if (tree instanceof MethodTree method) {
      return method.symbol();
    } else if (tree instanceof VariableTree variable) {
      return variable.symbol();
    } else if (tree instanceof MethodInvocationTree invocation) {
      return invocation.methodSymbol();
    } else if (tree instanceof NewClassTree creation) {
      return creation.methodSymbol();
    }
    return null;
  }

  private static void describeSemantics(Tree tree, StringBuilder line) {
    if (tree instanceof IdentifierTree identifier) {
      line.append(" id ").append(identifier.name()).append(" ").append(symbol(identifier.symbol()));
    } else if (tree instanceof MethodInvocationTree invocation) {
      line.append(" call ").append(symbol(invocation.methodSymbol()));
    } else if (tree instanceof NewClassTree creation) {
      line.append(" new ").append(symbol(creation.methodSymbol()));
    } else if (tree instanceof VariableTree variable) {
      line.append(" var ").append(symbol(variable.symbol()));
    } else if (tree instanceof MethodTree method) {
      line.append(" method ").append(symbol(method.symbol())).append(" overrides ").append(method.isOverriding());
    } else if (tree instanceof ClassTree type && type.symbol() != null) {
      line.append(" class ").append(symbol(type.symbol())).append(" super ").append(type(type.symbol().superClass()))
        .append(" interfaces ").append(type.symbol().interfaces().stream().map(TreeDescription::type).toList());
    }
    if (tree instanceof ExpressionTree expression) {
      line.append(" : ").append(type(expression.symbolType()));
      expression.asConstant().ifPresent(constant -> line.append(" = ").append(constant.toString().replace("\n", "\\n")));
    }
  }

  private static String symbol(Symbol symbol) {
    if (symbol.isUnknown()) {
      return "unknown";
    }
    StringBuilder out = new StringBuilder();
    Symbol owner = symbol.owner();
    out.append(owner == null ? "" : owner.name() + ".").append(symbol.name());
    out.append(" [").append(flags(symbol)).append("]");
    out.append(" : ").append(type(symbol.type()));
    String annotations = symbol.metadata().annotations().stream().map(annotation -> annotation.symbol().type().fullyQualifiedName()).sorted().collect(Collectors.joining(","));
    if (!annotations.isEmpty()) {
      out.append(" @").append(annotations);
    }
    if (symbol instanceof Symbol.MethodSymbol method) {
      out.append(" (").append(method.parameterTypes().stream().map(TreeDescription::type).collect(Collectors.joining(","))).append(")");
      out.append(" returns ").append(method.returnType() == null ? "-" : type(method.returnType().type()));
      out.append(" throws ").append(method.thrownTypes().stream().map(TreeDescription::type).toList());
      out.append(" sig ").append(method.signature());
    }
    return out.toString();
  }

  private static String flags(Symbol symbol) {
    StringBuilder flags = new StringBuilder();
    append(flags, symbol.isMethodSymbol(), "m");
    append(flags, symbol.isVariableSymbol(), "v");
    append(flags, symbol.isTypeSymbol(), "t");
    append(flags, symbol.isPackageSymbol(), "p");
    append(flags, symbol.isStatic(), "static");
    append(flags, symbol.isFinal(), "final");
    append(flags, symbol.isAbstract(), "abstract");
    append(flags, symbol.isPublic(), "public");
    append(flags, symbol.isPrivate(), "private");
    append(flags, symbol.isProtected(), "protected");
    append(flags, symbol.isPackageVisibility(), "package");
    append(flags, symbol.isEnum(), "enum");
    append(flags, symbol.isInterface(), "interface");
    append(flags, symbol.isDeprecated(), "deprecated");
    append(flags, symbol.isVolatile(), "volatile");
    return flags.toString();
  }

  private static void append(StringBuilder flags, boolean set, String name) {
    if (set) {
      flags.append(flags.isEmpty() ? "" : " ").append(name);
    }
  }

  static String type(Type type) {
    if (type == null) {
      return "null";
    }
    if (type.isUnknown()) {
      return "?";
    }
    StringBuilder out = new StringBuilder(type.fullyQualifiedName());
    if (type.isParameterized()) {
      out.append(type.typeArguments().stream().map(TreeDescription::type).collect(Collectors.joining(",", "<", ">")));
    }
    if (!type.erasure().fullyQualifiedName().equals(type.fullyQualifiedName())) {
      out.append(" erasure ").append(type.erasure().fullyQualifiedName());
    }
    return out.toString();
  }
}
