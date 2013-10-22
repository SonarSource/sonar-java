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

import com.google.common.collect.ImmutableMap;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Map;

@Rule(
  key = CollectionImplementationReferencedCheck.KEY,
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class CollectionImplementationReferencedCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String KEY = "S1319";
  private static final RuleKey RULE_KEY = RuleKey.of(CheckList.REPOSITORY_KEY, KEY);

  private static final String DEQUE = "Deque";
  private static final String LIST = "List";
  private static final String MAP = "Map";
  private static final String QUEUE = "Queue";
  private static final String SET = "Set";
  private static final String SORTED_MAP = "SortedMap";
  private static final String SORTED_SET = "SortedSet";

  private static final Map<String, String> MAPPING = ImmutableMap.<String, String> builder()
    .put("ArrayDeque", DEQUE)
    .put("ConcurrentLinkedDeque", DEQUE)

    .put("AbstractList", LIST)
    .put("AbstractSequentialList", LIST)
    .put("ArrayList", LIST)
    .put("CopyOnWriteArrayList", LIST)
    .put("LinkedList", LIST)

    .put("AbstractMap", MAP)
    .put("ConcurrentHashMap", MAP)
    .put("EnumMap", MAP)
    .put("HashMap", MAP)
    .put("Hashtable", MAP)
    .put("IdentityHashMap", MAP)
    .put("LinkedHashMap", MAP)
    .put("WeakHashMap", MAP)

    .put("AbstractQueue", QUEUE)
    .put("ConcurrentLinkedQueue", QUEUE)
    .put("SynchronousQueue", QUEUE)

    .put("AbstractSet", SET)
    .put("CopyOnWriteArraySet", SET)
    .put("EnumSet", SET)
    .put("HashSet", SET)
    .put("LinkedHashSet", SET)

    .put("TreeMap", SORTED_MAP)

    .put("TreeSet", SORTED_SET)
    .build();

  private JavaFileScannerContext context;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitVariable(VariableTree tree) {
    super.visitVariable(tree);

    String collectionImplementation = getTypeIdentifierOrNull(tree.type());
    String collectionInterface = MAPPING.get(collectionImplementation);

    if (collectionInterface != null) {
      context.addIssue(
        tree.type(),
        RULE_KEY,
        "The type of the \"" + tree.simpleName() + "\" object " + messageRemainder(collectionImplementation, collectionInterface));
    }
  }

  @Override
  public void visitMethod(MethodTree tree) {
    super.visitMethod(tree);

    String collectionImplementation = getTypeIdentifierOrNull(tree.returnType());
    String collectionInterface = MAPPING.get(collectionImplementation);

    if (collectionInterface != null) {
      context.addIssue(
        tree.returnType(),
        RULE_KEY,
        "The return type of this method " + messageRemainder(collectionImplementation, collectionInterface));
    }
  }

  private static String getTypeIdentifierOrNull(Tree tree) {
    if (tree == null) {
      return null;
    }

    Tree actualTree = tree.is(Tree.Kind.PARAMETERIZED_TYPE) ? ((ParameterizedTypeTree) tree).type() : tree;
    return actualTree.is(Tree.Kind.IDENTIFIER) ? ((IdentifierTree) actualTree).name() : null;
  }

  private static String messageRemainder(String collectionImplementation, String collectionInterface) {
    return "should be an interface such as \"" + collectionInterface + "\" rather than the implementation \"" + collectionImplementation + "\".";
  }

}
