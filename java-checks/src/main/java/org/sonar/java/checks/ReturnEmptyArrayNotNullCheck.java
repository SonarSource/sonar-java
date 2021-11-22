/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel;

@Rule(key = "S1168")
public class ReturnEmptyArrayNotNullCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers ITEM_PROCESSOR_PROCESS_METHOD = MethodMatchers.create()
    .ofSubTypes("org.springframework.batch.item.ItemProcessor").names("process").withAnyParameters().build();

  private final Deque<ReturnKind> returnKinds = new LinkedList<>();
  private QuickFixHelper.ImportSupplier importSupplier;

  private enum Returns {
    ARRAY, COLLECTION, MAP, OTHER;
  }

  private static class ReturnKind {
    private static final ReturnKind OTHER = new ReturnKind(Returns.OTHER, null);

    private final Returns kind;
    @Nullable
    private final Type type;

    private ReturnKind(Returns kind, @Nullable Type type) {
      this.kind = kind;
      this.type = type;
    }

    public static ReturnKind forType(Type type) {
      if (type.isUnknown()) {
        return OTHER;
      }
      if (type.isArray()) {
        return new ReturnKind(Returns.ARRAY, type);
      }
      if (type.isSubtypeOf("java.util.Collection")) {
        return new ReturnKind(Returns.COLLECTION, type);
      }
      if (type.isSubtypeOf("java.util.Map")) {
        return new ReturnKind(Returns.MAP, type);
      }
      return OTHER;
    }
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    super.setContext(context);
    reset();
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    reset();
  }

  private void reset() {
    returnKinds.clear();
    importSupplier = null;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR, Tree.Kind.RETURN_STATEMENT, Tree.Kind.LAMBDA_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      MethodTree methodTree = (MethodTree) tree;
      SymbolMetadata metadata = methodTree.symbol().metadata();
      if (metadata.nullabilityData().isNullable(NullabilityLevel.PACKAGE, false, true) || requiresReturnNull(methodTree)) {
        returnKinds.push(ReturnKind.OTHER);
      } else {
        returnKinds.push(ReturnKind.forType(methodTree.returnType().symbolType()));
      }
    } else if (tree.is(Tree.Kind.CONSTRUCTOR, Tree.Kind.LAMBDA_EXPRESSION)) {
      returnKinds.push(ReturnKind.OTHER);
    } else {
      checkForIssue((ReturnStatementTree) tree);
    }
  }

  private void checkForIssue(ReturnStatementTree returnStatement) {
    if (!isReturningNull(returnStatement)) {
      return;
    }
    ReturnKind returnKind = returnKinds.peek();
    if (returnKind.kind == Returns.OTHER) {
      return;
    }
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(returnStatement.expression())
      .withMessage("Return an empty %s instead of null.", returnKind.kind.name().toLowerCase(Locale.ROOT))
      .withQuickFixes(() -> quickFix(returnStatement))
      .report();
  }

  @Override
  public void leaveNode(Tree tree) {
    if (!tree.is(Tree.Kind.RETURN_STATEMENT)) {
      returnKinds.pop();
    }
  }

  private static boolean isReturningNull(ReturnStatementTree tree) {
    ExpressionTree expression = tree.expression();
    return expression != null && expression.is(Tree.Kind.NULL_LITERAL);
  }

  private static boolean requiresReturnNull(MethodTree methodTree) {
    Symbol owner = methodTree.symbol().owner();
    if (owner == null || !owner.isTypeSymbol()) {
      // Unknown hierarchy, consider it as requires null to avoid FP
      // At this point, owner should never be null, defensive programming
      return true;
    }
    List<Type> interfaces = ((Symbol.TypeSymbol) owner).interfaces();
    return isOverriding(methodTree)
      && (interfaces.stream().anyMatch(Type::isUnknown) || ITEM_PROCESSOR_PROCESS_METHOD.matches(methodTree));
  }

  private static boolean isOverriding(MethodTree tree) {
    return Boolean.TRUE.equals(tree.isOverriding());
  }

  private List<JavaQuickFix> quickFix(ReturnStatementTree returnStatement) {
    ReturnKind returnKind = returnKinds.peek();
    // can only be ARRAY or COLLECTION
    if (returnKind.kind == Returns.ARRAY) {
      return Collections.singletonList(JavaQuickFix.newQuickFix("Replace \"null\" with an empty array")
        .addTextEdit(JavaTextEdit.replaceTree(returnStatement.expression(), emptyArrayString((Type.ArrayType) returnKind.type)))
        .build());
    }
    Optional<CollectionType> candidate = CollectionType.forType(returnKind.type);
    if (!candidate.isPresent()) {
      return Collections.emptyList();
    }
    CollectionType collectionType = candidate.get();

    JavaQuickFix.Builder builder = JavaQuickFix.newQuickFix("Replace \"null\" with an empty %s", collectionType.typeName)
      .addTextEdit(JavaTextEdit.replaceTree(returnStatement.expression(), collectionType.replacement));

    if (importSupplier == null) {
      importSupplier = QuickFixHelper.newImportSupplier(context);
    }
    importSupplier.newImportEdit(collectionType.requiredType)
      .ifPresent(builder::addTextEdit);

    return Collections.singletonList(builder.build());
  }

  private static String emptyArrayString(Type.ArrayType arrayType) {
    return String.format("new %s", arrayType.name()
      .replace("[]", "[0]")
      .replaceAll("<.+>", ""));
  }

  private enum CollectionType {
    COLLECTION("Collection", "Collections.emptyList()"),
    LIST("List", "Collections.emptyList()"),
    ARRAY_LIST("ArrayList"),
    LINKED_LIST("LinkedList"),
    SET("Set", "Collections.emptySet()"),
    HASH_SET("HashSet"),
    TREE_SET("TreeSet"),
    SORTED_SET("SortedSet", "Collections.emptySortedSet()"),
    NAVIGABLE_SET("NavigableSet", "Collections.emptyNavigableSet()"),
    MAP("Map", "Collections.emptyMap()"),
    HASH_MAP("HashMap"),
    TREE_MAP("TreeMap"),
    SORTED_MAP("SortedMap", "Collections.emptySortedMap()"),
    NAVIGABLE_MAP("NavigableMap", "Collections.emptyNavigableMap()");

    private final String fullyQualifiedName;
    private final String replacement;
    private final String typeName;
    private final String requiredType;

    CollectionType(String typeName) {
      this.typeName = typeName;
      this.replacement = String.format("new %s<>()", typeName);
      this.fullyQualifiedName = String.format("java.util.%s", typeName);
      this.requiredType = fullyQualifiedName;
    }

    CollectionType(String typeName, String replacement) {
      this.typeName = typeName;
      this.replacement = replacement;
      this.fullyQualifiedName = String.format("java.util.%s", typeName);
      this.requiredType = "java.util.Collections";
    }

    private static Optional<CollectionType> forType(Type type) {
      Type erasure = type.erasure();
      for (CollectionType collectionType : CollectionType.values()) {
        if (erasure.is(collectionType.fullyQualifiedName)) {
          return Optional.of(collectionType);
        }
      }
      return Optional.empty();
    }
  }
}
