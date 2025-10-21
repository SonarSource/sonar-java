/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
package org.sonar.java.checks.unused.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.checks.unused.UnusedPrivateFieldCheck;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

/**
 * Finds usages of fields inside annotations.
 * I.e. currently it is used in {@link UnusedPrivateFieldCheck} to determine whether a field that is not used anywhere in the traditional
 * sense may actually be used inside a testing annotation.
 * <p>
 * E.g.
 * <pre>{@code
 * // This field is used in the @FieldSource annotation below.
 * private static final List<Integer> field = List.of(1, 2, 3);
 *
 * @ParameterizedTest
 * @FieldSource("field")
 * void test(int input) {
 *   // ...
 * }
 * </pre>
 */
public class AnnotationFieldReferenceFinder extends BaseTreeVisitor {
  private static final Logger LOG = LoggerFactory.getLogger(AnnotationFieldReferenceFinder.class);

  private final StringLiteralFinder stringLiteralFinder = new StringLiteralFinder();

  // Stores all field names and associated variable trees for which no usages of the symbol have been found so far.
  // Fields are removed from this map, as soon as a string literal is found inside an annotation that matches the name of the field.
  // In other words, all fields in this map that remain after visiting all annotations are unused.
  private final Map<String, VariableTree> fieldNameToVariableTree;
  private boolean hasEnteredClassTree = false;

  // We are intentionally not using a fully qualified name of the annotation class here, as it might be missing in contexts with incomplete
  // semantics.
  private static final String FIELD_SOURCE_METHOD_ANNOTATION = "FieldSource";
  private static final String FIELD_SOURCES_METHOD_ANNOTATION = "FieldSources";

  private AnnotationFieldReferenceFinder(HashMap<String, VariableTree> fieldNameToVariableTree) {
    this.fieldNameToVariableTree = fieldNameToVariableTree;
  }

  /**
   * Constructs an instance of this visitor that looks for references of the given fields inside annotations.
   */
  public static AnnotationFieldReferenceFinder findReferencesTo(Collection<VariableTree> fields) {
    var fieldNameToVariableTree = new HashMap<String, VariableTree>(fields.size());

    for (var variable : fields) {
      var fieldName = variable.simpleName().name();
      if (fieldNameToVariableTree.put(fieldName, variable) != null && LOG.isErrorEnabled()) {
        // We never expect this branch to be reachable since no duplicate field entry will be visible in the AST, even if there is one in
        // a (non-compiling) source.
        // In any case, we have some error reporting here in case this does happen after all
        var owner = variable.symbol().owner();
        var ownerName = owner != null ? owner.name() : "(Unknown Owning Class)";
        LOG.error("""
            Duplicate field name detected: {} in {}.
            This may happen for non-compiling sources and detection of unused variables may be impacted.
          """, fieldName, ownerName);
      }
    }

    return new AnnotationFieldReferenceFinder(fieldNameToVariableTree);
  }

  /**
   * Returns the variable trees of all fields that have not been referenced by an annotation.
   * This method must be called after visiting the class tree of the fields given to {@link AnnotationFieldReferenceFinder#findReferencesTo(Collection)}.
   */
  public Collection<VariableTree> fieldsNotReferencedInAnnotation() {
    return fieldNameToVariableTree.values();
  }

  @Override
  public void visitAnnotation(AnnotationTree annotationTree) {
    var annotatedMethod = findAnnotatedMethod(annotationTree);
    // We only want to handle annotations on (test) methods
    if (annotatedMethod == null) {
      return;
    }

    var annotationName = annotationTree.annotationType().symbolType().name();
    var isFieldSourceAnnotation = FIELD_SOURCE_METHOD_ANNOTATION.equals(annotationName);
    var isFieldSourcesAnnotation = FIELD_SOURCES_METHOD_ANNOTATION.equals(annotationName);
    if (!isFieldSourceAnnotation && !isFieldSourcesAnnotation) {
      return;
    }

    if (isFieldSourceAnnotation) {
      // The @FieldAnnotation annotation can be used without arguments.
      // In that case, the name of the annotated method is also the name of the field that is being referenced as input source.
      fieldNameToVariableTree.remove(annotatedMethod.simpleName().name());
    }

    for (var argument : annotationTree.arguments()) {
      argument.accept(stringLiteralFinder);
    }
  }

  @Override
  public void visitClass(ClassTree tree) {
    // We intentionally do not visit sub-classes - their annotations and fields will be handled separately.
    // Hence, we only visit the first class tree that we see
    if (!hasEnteredClassTree) {
      hasEnteredClassTree = true;
      super.visitClass(tree);
    }
  }

  private static @Nullable MethodTree findAnnotatedMethod(AnnotationTree annotationTree) {
    // For method annotations, the method tree is the parent of the parent of the annotation
    var parent = annotationTree.parent();
    if (parent == null) {
      return null;
    }

    var annotatedElement = parent.parent();
    if (annotatedElement == null) {
      return null;
    }

    if (annotatedElement instanceof MethodTree methodTree) {
      return methodTree;
    }

    return null;
  }

  /**
   * Finds string literals and removes them from {@link AnnotationFieldReferenceFinder#fieldNameToVariableTree}.
   * I.e. it finds usages of field names in string literals.
   */
  private class StringLiteralFinder extends BaseTreeVisitor {
    @Override
    public void visitLiteral(LiteralTree tree) {
      if (!tree.is(Tree.Kind.STRING_LITERAL)) {
        return;
      }

      var literalValue = tree.value();
      if (literalValue.length() < 2) {
        return;
      }

      var literalWithoutQuotes = literalValue.substring(1, literalValue.length() - 1);
      fieldNameToVariableTree.remove(literalWithoutQuotes);
    }
  }
}
