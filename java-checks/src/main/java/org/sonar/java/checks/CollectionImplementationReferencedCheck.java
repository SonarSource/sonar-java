/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.analyzer.commons.collections.MapBuilder;
import org.sonarsource.analyzer.commons.collections.SetUtils;

@Rule(key = "S1319")
public class CollectionImplementationReferencedCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String LIST = "java.util.List";
  private static final String DEQUE = "java.util.Deque";
  private static final String QUEUE = "java.util.Queue";
  private static final String SET = "java.util.Set";
  private static final String SORTED_SET = "java.util.SortedSet";
  private static final String MAP = "java.util.Map";
  private static final String CONCURRENT_MAP = "java.util.concurrent.ConcurrentMap";
  private static final String SORTED_MAP = "java.util.SortedMap";

  private static final Map<String, String> SUGGESTED_INTERFACE_BY_COLLECTION_CLASS = MapBuilder.<String, String>newMap()
    .put("java.util.ArrayDeque", DEQUE)
    .put("java.util.concurrent.ConcurrentLinkedDeque", DEQUE)

    .put("java.util.AbstractList", LIST)
    .put("java.util.AbstractSequentialList", LIST)
    .put("java.util.ArrayList", LIST)
    .put("java.util.LinkedList", LIST)
    .put("java.util.concurrent.CopyOnWriteArrayList", LIST)

    .put("java.util.AbstractMap", MAP)
    .put("java.util.EnumMap", MAP)
    .put("java.util.HashMap", MAP)
    .put("java.util.Hashtable", MAP)
    .put("java.util.IdentityHashMap", MAP)
    .put("java.util.LinkedHashMap", MAP)
    .put("java.util.WeakHashMap", MAP)

    .put("java.util.concurrent.ConcurrentHashMap", CONCURRENT_MAP)
    .put("java.util.concurrent.ConcurrentSkipListMap", CONCURRENT_MAP)

    .put("java.util.AbstractQueue", QUEUE)
    .put("java.util.concurrent.ConcurrentLinkedQueue", QUEUE)
    .put("java.util.concurrent.SynchronousQueue", QUEUE)

    .put("java.util.AbstractSet", SET)
    .put("java.util.concurrent.CopyOnWriteArraySet", SET)
    .put("java.util.EnumSet", SET)
    .put("java.util.HashSet", SET)
    .put("java.util.LinkedHashSet", SET)

    .put("java.util.TreeMap", SORTED_MAP)

    .put("java.util.TreeSet", SORTED_SET)
    .build();

  private static final Set<String> COLLECTION_METHODS = Set.of(
    "add",
    "addAll",
    "clear",
    "contains",
    "containsAll",
    "equals",
    "forEach",
    "hashCode",
    "isEmpty",
    "iterator",
    "parallelStream",
    "remove",
    "removeAll",
    "removeIf",
    "retainAll",
    "size",
    "spliterator",
    "stream",
    "toArray"
  );

  @SuppressWarnings("squid:S1192")
  private static final Set<String> LIST_METHODS = SetUtils.concat(COLLECTION_METHODS, Set.of(
    "copyOf",
    "get",
    "indexOf",
    "lastIndexOf",
    "listIterator",
    "replaceAll",
    "set",
    "sort",
    "subList"
  ));

  private static final Set<String> QUEUE_METHODS = SetUtils.concat(COLLECTION_METHODS, Set.of(
    "element",
    "offer",
    "peek",
    "poll"
  ));

  private static final Set<String> DEQUE_METHODS = SetUtils.concat(QUEUE_METHODS, Set.of(
    "addFirst",
    "addLast",
    "descendingIterator",
    "getFirst",
    "getLast",
    "offerFirst",
    "offerLast",
    "peekFirst",
    "peekLast",
    "pollFirst",
    "pollLast",
    "pop",
    "push",
    "removeFirst",
    "removeFirstOccurrence",
    "removeLast",
    "removeLastOccurrence"
  ));

  private static final Set<String> SET_METHODS = SetUtils.concat(COLLECTION_METHODS, Set.of(
    "copyOf"
  ));

  private static final Set<String> SORTED_SET_METHODS = SetUtils.concat(SET_METHODS, Set.of(
    "comparator",
    "first",
    "headSet",
    "last",
    "subSet",
    "tailSet"
  ));

  private static final Set<String> MAP_METHODS = Set.of(
    "clear",
    "compute",
    "computeIfAbsent",
    "computeIfPresent",
    "containsKey",
    "containsValue",
    "copyOf",
    "entry",
    "entrySet",
    "equals",
    "forEach",
    "get",
    "getOrDefault",
    "hashCode",
    "isEmpty",
    "keySet",
    "of",
    "ofEntries",
    "put",
    "putAll",
    "putIfAbsent",
    "remove",
    "replace",
    "replaceAll",
    "size",
    "values"
  );

  private static final Set<String> CONCURRENT_MAP_METHODS = SetUtils.concat(MAP_METHODS, Set.of(
    "merge"
  ));

  private static final Set<String> SORTED_MAP_METHODS = SetUtils.concat(MAP_METHODS, Set.of(
    "comparator",
    "firstKey",
    "headMap",
    "lastKey",
    "subMap",
    "tailMap"
  ));

  private static final Map<String, Set<String>> METHODS_BY_INTERFACE = Map.of(
    LIST, LIST_METHODS,
    QUEUE, QUEUE_METHODS,
    DEQUE, DEQUE_METHODS,
    SET, SET_METHODS,
    SORTED_SET, SORTED_SET_METHODS,
    MAP, MAP_METHODS,
    CONCURRENT_MAP, CONCURRENT_MAP_METHODS,
    SORTED_MAP, SORTED_MAP_METHODS
  );

  private JavaFileScannerContext context;
  private QuickFixHelper.ImportSupplier importSupplier;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    this.importSupplier = null;
    scan(context.getTree());
  }

  @Override
  public void visitVariable(VariableTree tree) {
    super.visitVariable(tree);
    if (isPublic(tree.modifiers())) {
      checkIfAllowed(tree.type(), String.format("The type of \"%s\"", tree.simpleName()));
    }
  }

  @Override
  public void visitMethod(MethodTree tree) {
    super.visitMethod(tree);
    boolean isNotPublic = !isPublic(tree.modifiers());
    boolean isOverridingMethod = !Boolean.FALSE.equals(tree.isOverriding());
    if (isNotPublic || isOverridingMethod) {
      return;
    }

    checkIfAllowed(tree.returnType(), "The return type of this method");
    List<VariableTree> candidateParameters = tree.parameters().stream()
      .filter(it -> getSuggestedInterface(it.type()) != null)
      .collect(Collectors.toList());
    if (candidateParameters.isEmpty()) {
      return;
    }

    var reportParameters = candidateParameters.stream();
    var block = tree.block();
    if (block != null) {
      var visitor = new MethodBodyVisitor(candidateParameters);
      block.accept(visitor);
      reportParameters = reportParameters.filter(it -> !visitor.excludedParameters.contains(it));
    }
    reportParameters.forEach(it -> report(it.type(), String.format("The type of \"%s\"", it.simpleName())));
  }

  private void checkIfAllowed(@Nullable TypeTree tree, String messagePrefix) {
    if (getSuggestedInterface(tree) != null) {
      report(tree, messagePrefix);
    }
  }

  @Nullable
  private static String getSuggestedInterface(@Nullable TypeTree tree) {
    if (tree == null) {
      return null;
    }
    String className = tree.symbolType().erasure().fullyQualifiedName();
    return SUGGESTED_INTERFACE_BY_COLLECTION_CLASS.get(className);
  }

  private void report(TypeTree tree, String messagePrefix) {
    TypeTree reportTree;
    if (tree.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      reportTree = ((ParameterizedTypeTree) tree).type();
    } else {
      reportTree = tree;
    }

    String className = tree.symbolType().erasure().fullyQualifiedName();
    String targetCollection = SUGGESTED_INTERFACE_BY_COLLECTION_CLASS.get(className);
    String usedCollectionSimpleName = toSimpleName(className);
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(reportTree)
      .withMessage("%s should be an interface such as \"%s\" rather than the implementation \"%s\".",
        messagePrefix,
        toSimpleName(targetCollection),
        usedCollectionSimpleName)
      .withQuickFix(() -> quickFix(reportTree, usedCollectionSimpleName, targetCollection))
      .report();
  }

  private JavaQuickFix quickFix(TypeTree typeTree, String usedCollection, String targetedCollection) {
    String targetedCollectionSimpleName = toSimpleName(targetedCollection);
    List<JavaTextEdit> edits = new ArrayList<>();
    edits.add(JavaTextEdit.replaceTree(typeTree, targetedCollectionSimpleName));

    getImportSupplier()
      .newImportEdit(targetedCollection)
      .ifPresent(edits::add);

    return JavaQuickFix.newQuickFix("Replace \"%s\" by \"%s\"", usedCollection, targetedCollectionSimpleName)
      .addTextEdits(edits)
      .build();
  }

  private QuickFixHelper.ImportSupplier getImportSupplier() {
    if (importSupplier == null) {
      importSupplier = QuickFixHelper.newImportSupplier(context);
    }
    return importSupplier;
  }

  private static String toSimpleName(String fullyQualifiedName) {
    return fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf('.') + 1);
  }

  private static boolean isPublic(ModifiersTree modifiers) {
    return ModifiersUtils.hasModifier(modifiers, Modifier.PUBLIC);
  }

  private static class MethodBodyVisitor extends BaseTreeVisitor {

    private final Map<String, VariableTree> candidateParametersByName = new HashMap<>();

    public final Set<VariableTree> excludedParameters = new HashSet<>();

    public MethodBodyVisitor(List<VariableTree> candidateParameters) {
      for (var variableTree : candidateParameters) {
        candidateParametersByName.put(variableTree.simpleName().name(), variableTree);
      }
    }

    @Override
    public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
      super.visitMemberSelectExpression(tree);
      if (!(tree.expression() instanceof IdentifierTree)) {
        return;
      }

      var variableName = ((IdentifierTree) tree.expression()).name();
      var variableTree = candidateParametersByName.get(variableName);
      if (variableTree == null) {
        return;
      }

      var memberName = tree.identifier().name();
      var interfaceName = getSuggestedInterface(variableTree.type());
      if (!METHODS_BY_INTERFACE.get(interfaceName).contains(memberName)) {
        excludedParameters.add(variableTree);
      }
    }
  }
}
