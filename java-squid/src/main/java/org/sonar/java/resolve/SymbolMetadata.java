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
package org.sonar.java.resolve;

import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.List;

public class SymbolMetadata {

  private List<AnnotationInstance> annotations;

  SymbolMetadata() {
    annotations = Lists.newArrayList();
  }


  public List<AnnotationInstance> annotations() {
    return annotations;
  }

  void addAnnotation(AnnotationInstance annotationInstance) {
    annotations.add(annotationInstance);
  }

  /**
   * Get the annotation values for the specified annotation.
   * @param annotationQualifiedClassName
   * @return null if the annotation is not present, a List otherwise
   */
  @Nullable
  public List<AnnotationValue> getValuesFor(String annotationQualifiedClassName) {
    for (AnnotationInstance annotationInstance : annotations) {
      if(annotationInstance.isTyped(annotationQualifiedClassName)) {
        return annotationInstance.values();
      }
    }
    return null;
  }
}
