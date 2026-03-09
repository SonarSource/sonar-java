/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.serialization;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.AnnotationsHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2055")
public class SerializableSuperConstructorCheck extends IssuableSubscriptionVisitor {

  private static final String LOMBOK_NO_ARGS_CONSTRUCTOR_ANNOTATION = "lombok.NoArgsConstructor";

  private static final MethodMatchers WRITE_REPLACE = MethodMatchers.create()
    .ofAnyType()
    .names("writeReplace")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (classTree.simpleName() == null) {
      // do not cover anonymous classes
      return;
    }
    Symbol.TypeSymbol classSymbol = classTree.symbol();
    Type superclass = classSymbol.superClass();
    if (isSerializable(classSymbol.type()) && isNotSerializableMissingNoArgConstructor(superclass) && !implementsSerializableMethods(classSymbol)) {
      reportIssue(classTree.superClass(), "Add a no-arg constructor to \"" + superclass.name() + "\".");
    }
  }

  private static boolean isNotSerializableMissingNoArgConstructor(@Nullable Type superclass) {
    return superclass != null
      && !superclass.isUnknown()
      && !isSerializable(superclass)
      && !hasNonPrivateNoArgConstructor(superclass)
      && !hasCompliantGeneratedNoArgConstructor(superclass);
  }

  private static boolean isSerializable(Type type) {
    return type.isSubtypeOf("java.io.Serializable");
  }

  private static boolean hasNonPrivateNoArgConstructor(Type type) {
    Collection<Symbol> constructors = type.symbol().lookupSymbols("<init>");
    for (Symbol member : constructors) {
      if (member.isMethodSymbol()) {
        Symbol.MethodSymbol method = (Symbol.MethodSymbol) member;
        if (method.parameterTypes().isEmpty() && !method.isPrivate()) {
          return true;
        }
      }
    }
    return constructors.isEmpty();
  }

  private static boolean hasCompliantGeneratedNoArgConstructor(Type type) {
    return type.symbol()
      .metadata()
      .annotations()
      .stream()
      .anyMatch(annotation -> isLombokNoArgConstructorGenerator(annotation.symbol().type()) && !hasPrivateAccess(annotation));
  }

  private static boolean isLombokNoArgConstructorGenerator(Type symbolType) {
    if (symbolType.isUnknown()) {
      return AnnotationsHelper.annotationTypeIdentifier(LOMBOK_NO_ARGS_CONSTRUCTOR_ANNOTATION).equals(symbolType.name());
    }
    return LOMBOK_NO_ARGS_CONSTRUCTOR_ANNOTATION.equals(symbolType.fullyQualifiedName());
  }

  private static boolean hasPrivateAccess(SymbolMetadata.AnnotationInstance annotation) {
    return annotation.values()
      .stream()
      .anyMatch(v -> "access".equals(v.name()) && "PRIVATE".equals(getAccessLevel(v.value())));
  }

  private static String getAccessLevel(Object value) {
    if (value instanceof Symbol symbol) {
      return symbol.name();
    }
    return null;
  }

  private static boolean implementsSerializableMethods(Symbol.TypeSymbol classSymbol) {
    return classSymbol.memberSymbols().stream().anyMatch(WRITE_REPLACE::matches);
  }
}
