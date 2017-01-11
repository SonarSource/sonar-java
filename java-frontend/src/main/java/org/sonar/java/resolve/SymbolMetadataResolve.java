/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.resolve;

import com.google.common.collect.Lists;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;

import javax.annotation.CheckForNull;
import java.util.List;

public class SymbolMetadataResolve implements SymbolMetadata {

  private List<AnnotationInstance> annotations;

  SymbolMetadataResolve() {
    annotations = Lists.newArrayList();
  }

  @Override
  public List<AnnotationInstance> annotations() {
    return annotations;
  }

  void addAnnotation(AnnotationInstance annotationInstance) {
    annotations.add(annotationInstance);
  }

  @Override
  public boolean isAnnotatedWith(String fullyQualifiedName) {
    for (AnnotationInstance annotationInstance : annotations) {
      if(annotationInstance.symbol().type().is(fullyQualifiedName)) {
        return true;
      }
    }
    return false;
  }

  @Override
  @CheckForNull
  public List<AnnotationValue> valuesForAnnotation(String fullyQualifiedNameOfAnnotation) {
    for (AnnotationInstance annotationInstance : annotations) {
      if(annotationInstance.symbol().type().is(fullyQualifiedNameOfAnnotation)) {
        return annotationInstance.values();
      }
    }
    return null;
  }
}
