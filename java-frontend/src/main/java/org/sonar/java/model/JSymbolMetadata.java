/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sonar.java.resolve.AnnotationValueResolve;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

final class JSymbolMetadata implements SymbolMetadata {

  private final JSema sema;
  private final IAnnotationBinding[] annotationBindings;

  JSymbolMetadata(JSema sema, IAnnotationBinding[] annotationBindings) {
    this.sema = Objects.requireNonNull(sema);
    this.annotationBindings = annotationBindings;
  }

  JSymbolMetadata(JSema sema, IAnnotationBinding[] typeAnnotationBindings, IAnnotationBinding[] annotationBindings) {
    this.sema = Objects.requireNonNull(sema);
    this.annotationBindings = new IAnnotationBinding[typeAnnotationBindings.length + annotationBindings.length];
    System.arraycopy(typeAnnotationBindings, 0, this.annotationBindings, 0, typeAnnotationBindings.length);
    System.arraycopy(annotationBindings, 0, this.annotationBindings, typeAnnotationBindings.length, annotationBindings.length);
  }

  @Override
  public List<AnnotationInstance> annotations() {
    return Arrays.stream(annotationBindings)
      .map(sema::annotation)
      .collect(Collectors.toList());
  }

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
    private final JSema sema;
    private final IAnnotationBinding annotationBinding;

    JAnnotationInstance(JSema sema, IAnnotationBinding annotationBinding) {
      this.sema = sema;
      this.annotationBinding = annotationBinding;
    }

    @Override
    public Symbol symbol() {
      return sema.typeSymbol(annotationBinding.getAnnotationType());
    }

    @Override
    public List<AnnotationValue> values() {
      List<AnnotationValue> r = new ArrayList<>();
      for (IMemberValuePairBinding pair : annotationBinding.getDeclaredMemberValuePairs()) {
        r.add(new AnnotationValueResolve(pair.getName(), convertAnnotationValue(pair.getValue())));
      }
      return r;
    }

    private Object convertAnnotationValue(Object value) {
      if (value instanceof IVariableBinding) {
        return sema.variableSymbol((IVariableBinding) value);
      } else if (value instanceof ITypeBinding) {
        return sema.typeSymbol((ITypeBinding) value);
      } else if (value instanceof IAnnotationBinding) {
        return sema.annotation((IAnnotationBinding) value);
      } else if (value instanceof Object[]) {
        // Godin: probably better to not modify original array
        Object[] a = (Object[]) value;
        Object[] result = new Object[a.length];
        for (int i = 0; i < a.length; i++) {
          result[i] = convertAnnotationValue(a[i]);
        }
        return result;
      } else {
        return value;
      }
    }
  }

}
