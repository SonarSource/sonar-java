/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.plugins.java.api.semantic;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Holds the metadata information (annotations) of a symbol.
 *
 */
public interface SymbolMetadata {

  /**
   * Check if the symbol is annotated with the specified annotation.
   * @param fullyQualifiedName fully Qualified Name of the annotation
   * @return true if the symbol is annotated with the annotation
   */
  boolean isAnnotatedWith(String fullyQualifiedName);

  /**
   * Get the annotation values for the specified annotation.
   * @param fullyQualifiedNameOfAnnotation fully Qualified Name of the annotation
   * @return null if the annotation is not present, a List otherwise
   */
  @CheckForNull
  List<AnnotationValue> valuesForAnnotation(String fullyQualifiedNameOfAnnotation);

  /**
   * The list of annotations found on this symbol.
   * @return A list of {@link AnnotationInstance}
   */
  List<AnnotationInstance> annotations();

  /**
   * @return the nullability definition for metadata() of:
   * - fields
   * - method parameters
   * - methods (related to method return values)
   * - local variables
   * And currently always return UNKNOWN_NULLABILITY for unsupported metadata() of:
   * - lambda parameters
   */
  NullabilityData nullabilityData();

  NullabilityData nullabilityData(NullabilityTarget level);

  @Nullable
  AnnotationTree findAnnotationTree(AnnotationInstance annotationInstance);

  /**
   * Occurrence of an annotation on a symbol.
   */
  interface AnnotationInstance {

    /**
     * Type symbol of this annotation. Can be unknown if bytecode for this annotation is not provided.
     * @return the symbol declaring this annotation.
     */
    Symbol symbol();

    /**
     * Annotation values for this annotation.
     * @return immutable list of annotation values.
     */
    List<AnnotationValue> values();

  }


  /**
   * Value of a property of an annotation.
   */
  interface AnnotationValue {

    /**
     * Name of the annotation property.
     * @return the name of the property.
     */
    String name();

    /**
     * Stored value of the annotation property.
     * @return the value of the annotation that has been found.
     */
    Object value();

  }

  enum NullabilityType {
    NO_ANNOTATION,
    UNKNOWN,
    NON_NULL,
    WEAK_NULLABLE,
    STRONG_NULLABLE
  }

  enum NullabilityLevel {
    UNKNOWN,
    PACKAGE,
    CLASS,
    METHOD,
    VARIABLE
  }

  enum NullabilityTarget {
    METHOD,
    PARAMETER,
    FIELD,
    LOCAL_VARIABLE
  }

  interface NullabilityData {
    @Nullable
    AnnotationInstance annotation();

    @Nullable
    Tree declaration();

    NullabilityType type();

    NullabilityLevel level();

    boolean metaAnnotation();

    boolean isNonNull(NullabilityLevel minLevel, boolean ignoreMetaAnnotation, boolean defaultValue);

    boolean isNullable(NullabilityLevel minLevel, boolean ignoreMetaAnnotation, boolean defaultValue);

    boolean isStrongNullable(NullabilityLevel minLevel, boolean ignoreMetaAnnotation, boolean defaultValue);
  }

}
