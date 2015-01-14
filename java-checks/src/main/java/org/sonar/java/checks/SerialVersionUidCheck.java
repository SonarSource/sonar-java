/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.resolve.AnnotationValue;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Symbol.TypeSymbol;
import org.sonar.java.resolve.Symbol.VariableSymbol;
import org.sonar.java.resolve.Type;
import org.sonar.java.resolve.Type.ClassType;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.List;

@Rule(
  key = "S2057",
  priority = Priority.MAJOR,
  tags = {"pitfall", "serialization"})
public class SerialVersionUidCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      visitClassTree((ClassTreeImpl) tree);
    }
  }

  private void visitClassTree(ClassTreeImpl classTree) {
    TypeSymbol symbol = classTree.getSymbol();
    if (isSerializable(symbol.getType())) {
      VariableSymbol serialVersionUidSymbol = findSerialVersionUid(symbol);
      if (serialVersionUidSymbol == null) {
        if (!isExclusion(symbol)) {
          addIssue(classTree, "Add a \"static final long serialVersionUID\" field to this class.");
        }
      } else if (!serialVersionUidSymbol.isStatic()) {
        addModifierIssue(serialVersionUidSymbol, "static");
      } else if (!serialVersionUidSymbol.isFinal()) {
        addModifierIssue(serialVersionUidSymbol, "final");
      } else if (!serialVersionUidSymbol.getType().is("long")) {
        addModifierIssue(serialVersionUidSymbol, "long");
      }
    }
  }

  private void addModifierIssue(VariableSymbol serialVersionUidSymbol, String modifier) {
    Tree tree = getSemanticModel().getTree(serialVersionUidSymbol);
    addIssue(tree, "Make this \"serialVersionUID\" field \"" + modifier + "\".");
  }

  private VariableSymbol findSerialVersionUid(TypeSymbol symbol) {
    for (Symbol member : symbol.members().lookup("serialVersionUID")) {
      if (member.isKind(Symbol.VAR)) {
        return (VariableSymbol) member;
      }
    }
    return null;
  }

  private boolean isSerializable(Type type) {
    return type.isSubtypeOf("java.io.Serializable");
  }

  private boolean isExclusion(TypeSymbol symbol) {
    return symbol.isAbstract()
      || symbol.getType().isSubtypeOf("java.lang.Exception")
      || isGuiClass(symbol)
      || hasSuppressWarningAnnotation(symbol);
  }

  private boolean isGuiClass(TypeSymbol symbol) {
    for (ClassType superType : symbol.superTypes()) {
      TypeSymbol superTypeSymbol = superType.getSymbol();
      if (hasGuiPackage(superTypeSymbol)) {
        return true;
      }
    }
    return hasGuiPackage(symbol) || (!symbol.equals(symbol.outermostClass()) && isGuiClass(symbol.outermostClass()));
  }

  private boolean hasGuiPackage(TypeSymbol superTypeSymbol) {
    String fullyQualifiedName = superTypeSymbol.getFullyQualifiedName();
    return fullyQualifiedName.startsWith("javax.swing.") || fullyQualifiedName.startsWith("java.awt.");
  }

  private boolean hasSuppressWarningAnnotation(TypeSymbol symbol) {
    List<AnnotationValue> annotations = symbol.metadata().getValuesFor("java.lang.SuppressWarnings");
    if (annotations != null) {
      for (AnnotationValue annotationValue : annotations) {
        if ("serial".equals(stringLiteralValue(annotationValue.value()))) {
          return true;
        }
      }
    }
    return false;
  }

  private String stringLiteralValue(Object object) {
    if (object instanceof LiteralTree) {
      LiteralTree literal = (LiteralTree) object;
      return LiteralUtils.trimQuotes(literal.value());
    }
    return null;
  }
}
