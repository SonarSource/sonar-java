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
import org.sonar.java.model.BaseTreeVisitor;
import org.sonar.java.model.IdentifierTree;
import org.sonar.java.model.JavaFileScanner;
import org.sonar.java.model.JavaFileScannerContext;
import org.sonar.java.model.MethodTree;
import org.sonar.java.model.ParameterizedTypeTree;
import org.sonar.java.model.Tree;
import org.sonar.java.model.VariableTree;

import java.util.Map;

@Rule(
  key = CollectionImplementationReferencedCheck.KEY,
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class CollectionImplementationReferencedCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String KEY = "S1319";
  private static final RuleKey RULE_KEY = RuleKey.of(CheckList.REPOSITORY_KEY, KEY);

  private static final Map<String, String> MAPPING = ImmutableMap.<String, String> builder()
    .put("ArrayDeque", "Deque")
    .put("ConcurrentLinkedDeque", "Deque")

    .put("AbstractList", "List")
    .put("AbstractSequentialList", "List")
    .put("ArrayList", "List")
    .put("CopyOnWriteArrayList", "List")
    .put("LinkedList", "List")

    .put("AbstractMap", "Map")
    .put("ConcurrentHashMap", "Map")
    .put("EnumMap", "Map")
    .put("HashMap", "Map")
    .put("Hashtable", "Map")
    .put("IdentityHashMap", "Map")
    .put("LinkedHashMap", "Map")
    .put("WeakHashMap", "Map")

    .put("AbstractQueue", "Queue")
    .put("ConcurrentLinkedQueue", "Queue")
    .put("SynchronousQueue", "Queue")

    .put("AbstractSet", "Set")
    .put("CopyOnWriteArraySet", "Set")
    .put("EnumSet", "Set")
    .put("HashSet", "Set")
    .put("LinkedHashSet", "Set")

    .put("TreeMap", "SortedMap")

    .put("TreeSet", "SortedSet")
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
    if (tree.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      tree = ((ParameterizedTypeTree) tree).type();
    }
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) tree).name();
    }
    return null;
  }

  private static String messageRemainder(String collectionImplementation, String collectionInterface) {
    return "should be an interface such as \"" + collectionInterface + "\" rather than the implementation \"" + collectionImplementation + "\".";
  }

}
