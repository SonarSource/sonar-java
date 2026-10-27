/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationInstance;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationValue;

final class EcjSymbolMetadata {

  private EcjSymbolMetadata() {
  }

  static JSymbolMetadata declaration(JSema sema, Symbol owner, IAnnotationBinding[] bindings) {
    return metadata(sema, owner, bindings, new JSymbolMetadata[0], false);
  }

  static JSymbolMetadata of(JSema sema, Symbol owner, ITypeBinding type, IAnnotationBinding[] externalAnnotations) {
    var symbolAnnotations = new IAnnotationBinding[externalAnnotations.length + type.getTypeAnnotations().length];
    System.arraycopy(externalAnnotations, 0, symbolAnnotations, 0, externalAnnotations.length);
    System.arraycopy(type.getTypeAnnotations(), 0, symbolAnnotations, externalAnnotations.length, type.getTypeAnnotations().length);

    var parameterMetaDatas = Arrays.stream(type.getTypeArguments())
      .map(param -> ofTypeArg(sema, owner, param))
      .toArray(JSymbolMetadata[]::new);

    return metadata(sema, owner, symbolAnnotations, parameterMetaDatas, false);
  }

  private static JSymbolMetadata ofTypeArg(JSema sema, Symbol owner, ITypeBinding param) {
    var symbolAnnotations = param.getTypeAnnotations();
    var parameterMetaDatas = Arrays.stream(param.getTypeArguments())
      .map(p -> ofTypeArg(sema, owner, p))
      .toArray(JSymbolMetadata[]::new);
    return metadata(sema, owner, symbolAnnotations, parameterMetaDatas, true);
  }

  private static JSymbolMetadata metadata(JSema sema, Symbol owner, IAnnotationBinding[] bindings,
    JSymbolMetadata[] parameters, boolean typeArgument) {
    return new JSymbolMetadata(owner, () -> Arrays.stream(bindings).map(sema::annotation).collect(Collectors.toList()), parameters, typeArgument);
  }

  static final class JAnnotationInstance implements AnnotationInstance {
    private final JSema sema;
    private final IAnnotationBinding annotationBinding;

    /**
     * Cache for {@link #values()}.
     */
    private List<AnnotationValue> values;

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
      if (values == null) {
        values = Arrays.stream(annotationBinding.getDeclaredMemberValuePairs())
          .map(p -> new AnnotationValueImpl(p.getName(), convertAnnotationValue(p.getValue())))
          .collect(Collectors.toList());
      }
      return values;
    }

    private Object convertAnnotationValue(Object value) {
      return EcjSymbolMetadata.convertAnnotationValue(sema, value);
    }
  }

  static Object convertAnnotationValue(JSema sema, Object value) {
    return switch (value) {
      case IVariableBinding iVariableBinding -> sema.variableSymbol(iVariableBinding);
      case ITypeBinding iTypeBinding -> sema.typeSymbol(iTypeBinding);
      case IAnnotationBinding iAnnotationBinding -> sema.annotation(iAnnotationBinding);
      case Object[] a -> {
        Object[] result = new Object[a.length];
        for (int i = 0; i < a.length; i++) {
          result[i] = convertAnnotationValue(sema, a[i]);
        }
        yield result;
      }
      case null, default -> value;
    };
  }
}
