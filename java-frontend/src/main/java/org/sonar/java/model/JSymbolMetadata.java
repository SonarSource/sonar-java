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
package org.sonar.java.model;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.sonar.java.resolve.AnnotationValueResolve;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

abstract class JSymbolMetadata implements SymbolMetadata {

  @Override
  public abstract List<AnnotationInstance> annotations();

  @Override
  public final boolean isAnnotatedWith(String fullyQualifiedName) {
    for (AnnotationInstance a : annotations()) {
      if (a.symbol().type().is(fullyQualifiedName)) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  @Override
  public final List<AnnotationValue> valuesForAnnotation(String fullyQualifiedNameOfAnnotation) {
    for (AnnotationInstance a : annotations()) {
      if (a.symbol().type().is(fullyQualifiedNameOfAnnotation)) {
        // TODO what about repeating annotations?
        return a.values();
      }
    }
    return null;
  }

  static final class JAnnotationInstance implements AnnotationInstance {
    private final JSema ast;
    private final IAnnotationBinding annotationBinding;

    JAnnotationInstance(JSema ast, IAnnotationBinding annotationBinding) {
      this.ast = ast;
      this.annotationBinding = annotationBinding;
    }

    @Override
    public Symbol symbol() {
      return ast.typeSymbol(annotationBinding.getAnnotationType());
    }

    @Override
    public List<AnnotationValue> values() {
      List<AnnotationValue> r = new ArrayList<>();
      for (IMemberValuePairBinding pair : annotationBinding.getDeclaredMemberValuePairs()) {
        r.add(new AnnotationValueResolve(pair.getName(), pair.getValue()));
      }
      return r;
    }
  }

}
