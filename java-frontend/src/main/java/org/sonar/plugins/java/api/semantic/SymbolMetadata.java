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
package org.sonar.plugins.java.api.semantic;

import javax.annotation.CheckForNull;
import java.util.List;

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


}
