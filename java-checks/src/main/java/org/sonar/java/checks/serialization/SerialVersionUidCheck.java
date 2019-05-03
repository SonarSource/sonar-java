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
package org.sonar.java.checks.serialization;

import com.google.common.base.Joiner;
import org.sonar.check.Rule;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.resolve.JavaSymbol.TypeJavaSymbol;
import org.sonar.java.resolve.ClassJavaType;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Rule(key = "S2057")
public class SerialVersionUidCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      visitClassTree((ClassTree) tree);
    }
  }

  private void visitClassTree(ClassTree classTree) {
    Symbol.TypeSymbol symbol = classTree.symbol();
    IdentifierTree simpleName = classTree.simpleName();
    if (simpleName != null && isSerializable(symbol.type())) {
      Symbol.VariableSymbol serialVersionUidSymbol = findSerialVersionUid(symbol);
      if (serialVersionUidSymbol == null) {
        if (!isExclusion(symbol)) {
          reportIssue(simpleName, "Add a \"static final long serialVersionUID\" field to this class.");
        }
      } else {
        checkModifiers(serialVersionUidSymbol);
      }
    }
  }

  private void checkModifiers(Symbol.VariableSymbol serialVersionUidSymbol) {
    List<String> missingModifiers = new ArrayList<>();
    if (!serialVersionUidSymbol.isStatic()) {
      missingModifiers.add("static");
    }
    if (!serialVersionUidSymbol.isFinal()) {
      missingModifiers.add("final");
    }
    if (!serialVersionUidSymbol.type().is("long")) {
      missingModifiers.add("long");
    }
    VariableTree variableTree = serialVersionUidSymbol.declaration();
    if (variableTree != null && !missingModifiers.isEmpty()) {
      reportIssue(variableTree.simpleName(), "Make this \"serialVersionUID\" field \"" + Joiner.on(' ').join(missingModifiers) + "\".");
    }
  }

  private static Symbol.VariableSymbol findSerialVersionUid(Symbol.TypeSymbol symbol) {
    for (Symbol member : symbol.lookupSymbols("serialVersionUID")) {
      if (member.isVariableSymbol()) {
        return (Symbol.VariableSymbol) member;
      }
    }
    return null;
  }

  private static boolean isSerializable(Type type) {
    return type.isSubtypeOf("java.io.Serializable");
  }

  private static boolean isExclusion(Symbol.TypeSymbol symbol) {
    return symbol.isAbstract()
      || symbol.type().isSubtypeOf("java.lang.Throwable")
      || isGuiClass((TypeJavaSymbol) symbol)
      || hasSuppressWarningAnnotation((TypeJavaSymbol) symbol);
  }

  private static boolean isGuiClass(TypeJavaSymbol symbol) {
    for (ClassJavaType superType : symbol.superTypes()) {
      TypeJavaSymbol superTypeSymbol = superType.getSymbol();
      if (hasGuiPackage(superTypeSymbol)) {
        return true;
      }
    }
    return hasGuiPackage(symbol) || (!symbol.equals(symbol.outermostClass()) && isGuiClass(symbol.outermostClass()));
  }

  private static boolean hasGuiPackage(TypeJavaSymbol superTypeSymbol) {
    String fullyQualifiedName = superTypeSymbol.getFullyQualifiedName();
    return fullyQualifiedName.startsWith("javax.swing.") || fullyQualifiedName.startsWith("java.awt.");
  }

  private static boolean hasSuppressWarningAnnotation(TypeJavaSymbol symbol) {
    List<SymbolMetadata.AnnotationValue> annotations = symbol.metadata().valuesForAnnotation("java.lang.SuppressWarnings");
    if (annotations != null) {
      for (SymbolMetadata.AnnotationValue annotationValue : annotations) {
        if ("serial".equals(stringLiteralValue(annotationValue.value()))) {
          return true;
        }
      }
    }
    return false;
  }

  private static String stringLiteralValue(Object object) {
    if (object instanceof LiteralTree) {
      LiteralTree literal = (LiteralTree) object;
      return LiteralUtils.trimQuotes(literal.value());
    }
    return null;
  }
}
