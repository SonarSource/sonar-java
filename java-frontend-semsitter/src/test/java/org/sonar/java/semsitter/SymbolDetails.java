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
package org.sonar.java.semsitter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;

/**
 * What the checks most often ask of a symbol beyond its name and type: annotation values, nullability, overridden
 * methods, members and subtyping.
 */
final class SymbolDetails {

  private static final List<String> SUPERTYPES = List.of("java.lang.Object", "java.io.Serializable", "java.lang.Comparable", "java.lang.Number",
    "java.lang.Runnable", "java.util.Collection", "java.lang.Throwable", "java.lang.annotation.Annotation");

  private SymbolDetails() {
  }

  static String of(Symbol symbol) {
    StringBuilder out = new StringBuilder("details");
    out.append(" declared ").append(symbol.declaration() != null).append(" usages ").append(symbol.usages().size());
    out.append(" metadata ").append(metadata(symbol.metadata()));
    out.append(" typeArguments ").append(Arrays.stream(symbol.metadata().parametersMetadata()).map(SymbolDetails::lookups).toList());
    out.append(" enclosing ").append(name(symbol.enclosingClass()));
    out.append(" flags ").append(flags(symbol));
    if (symbol instanceof Symbol.MethodSymbol method) {
      out.append(method(method));
    } else if (symbol instanceof Symbol.TypeSymbol type) {
      out.append(type(type));
    } else if (symbol instanceof Symbol.VariableSymbol variable) {
      out.append(" local ").append(variable.isLocalVariable()).append(" parameter ").append(variable.isParameter())
        .append(" effectivelyFinal ").append(variable.isEffectivelyFinal()).append(" constant ").append(variable.constantValue().orElse(null));
      if (variable.isLocalVariable() && variable.owner() instanceof Symbol.MethodSymbol owner) {
        out.append(" owner ").append(owner(owner));
      }
    }
    return out.toString();
  }

  private static List<String> flags(Symbol symbol) {
    List<String> flags = new java.util.ArrayList<>();
    addIf(flags, symbol.isStatic(), "static");
    addIf(flags, symbol.isFinal(), "final");
    addIf(flags, symbol.isAbstract(), "abstract");
    addIf(flags, symbol.isPublic(), "public");
    addIf(flags, symbol.isPrivate(), "private");
    addIf(flags, symbol.isProtected(), "protected");
    addIf(flags, symbol.isPackageVisibility(), "package");
    addIf(flags, symbol.isDeprecated(), "deprecated");
    addIf(flags, symbol.isVolatile(), "volatile");
    addIf(flags, symbol.isEnum(), "enum");
    addIf(flags, symbol.isInterface(), "interface");
    addIf(flags, symbol.isUnknown(), "unknown");
    return flags;
  }

  private static void addIf(List<String> flags, boolean condition, String flag) {
    if (condition) {
      flags.add(flag);
    }
  }

  private static String owner(Symbol.MethodSymbol owner) {
    return owner.name() + " " + owner.signature() + " " + flags(owner) + " returns " + TreeDescription.type(owner.returnType().type())
      + " parameters " + owner.parameterTypes().size() + " thrown " + owner.thrownTypes().size() + " overridden " + owner.overriddenSymbols().size()
      + " declared " + (owner.declaration() != null) + " usages " + owner.usages().size() + " metadata " + owner.metadata().annotations().size()
      + " enclosing " + name(owner.enclosingClass()) + " type " + owner.type().isUnknown() + " kinds " + owner.isMethodSymbol() + owner.isTypeSymbol() + owner.isVariableSymbol()
      + " other " + owner.isDefaultMethod() + owner.isSynchronizedMethod() + owner.isVarArgsMethod() + owner.isNativeMethod() + owner.isOverridable()
      + owner.isParametrizedMethod() + owner.declarationParameters().size() + owner.isLocalVariable() + owner.isParameter();
  }

  private static String method(Symbol.MethodSymbol method) {
    return " overridden " + method.overriddenSymbols().stream().map(overridden -> qualified(overridden) + lookups(overridden.metadata())).toList()
      + " overridable " + method.isOverridable() + " generic " + method.isParametrizedMethod() + " default " + method.isDefaultMethod()
      + " synchronized " + method.isSynchronizedMethod() + " varargs " + method.isVarArgsMethod() + " native " + method.isNativeMethod()
      + " parameters " + method.declarationParameters().stream().map(Symbol::name).toList()
      + " parameterMetadata " + Arrays.stream(method.metadata().parametersMetadata()).map(SymbolDetails::metadata).toList();
  }

  private static String type(Symbol.TypeSymbol type) {
    Type self = type.type();
    return " members " + type.memberSymbols().stream().map(Symbol::name).sorted().toList()
      + " lookupToString " + type.lookupSymbols("toString").size()
      + " outermost " + name(type.outermostClass())
      + " annotation " + type.isAnnotation()
      + " subtypeOf " + SUPERTYPES.stream().filter(self::isSubtypeOf).toList()
      + " superTypes " + type.superTypes().stream().map(TreeDescription::type).sorted().toList()
      + " numerical " + self.isNumerical() + " wrapper " + self.isPrimitiveWrapper() + " raw " + self.isRawType()
      + " declaring " + name(self.declaringType().symbol());
  }

  private static String metadata(SymbolMetadata metadata) {
    String annotations = metadata.annotations().stream()
      .map(annotation -> annotation.symbol().type().fullyQualifiedName() + annotation.values().stream()
        .map(value -> value.name() + "=" + value(value.value())).collect(Collectors.joining(",", "(", ")")))
      .sorted().collect(Collectors.joining(" "));
    SymbolMetadata.NullabilityData nullability = metadata.nullabilityData();
    return "[" + annotations + "] nullability " + nullability.type() + "/" + nullability.level()
      + " nonNull " + nullability.isNonNull(SymbolMetadata.NullabilityLevel.PACKAGE, false, false)
      + " nullable " + nullability.isNullable(SymbolMetadata.NullabilityLevel.PACKAGE, false, false)
      + " strongNullable " + nullability.isStrongNullable(SymbolMetadata.NullabilityLevel.PACKAGE, false, false)
      + " strict " + nullability.isNonNull(SymbolMetadata.NullabilityLevel.VARIABLE, true, true)
      + nullability.isNullable(SymbolMetadata.NullabilityLevel.VARIABLE, true, true)
      + nullability.isStrongNullable(SymbolMetadata.NullabilityLevel.VARIABLE, true, true)
      + " meta " + nullability.metaAnnotation()
      + " source " + (nullability.annotation() == null ? null : nullability.annotation().symbol().name())
      + " tree " + (nullability.declaration() != null)
      + " lookups " + lookups(metadata);
  }

  /** How each annotation is found again: its tree, its values by name, and whether the symbol counts as annotated with it. */
  private static String lookups(SymbolMetadata metadata) {
    return metadata.annotations().stream()
      .map(annotation -> {
        String name = annotation.symbol().type().fullyQualifiedName();
        List<SymbolMetadata.AnnotationValue> values = metadata.valuesForAnnotation(name);
        return name + " tree " + (metadata.findAnnotationTree(annotation) != null) + " values " + (values == null ? null : values.size())
          + " annotated " + metadata.isAnnotatedWith(name);
      })
      .sorted().collect(Collectors.joining(", ", "[", "]"));
  }

  private static String value(Object value) {
    if (value instanceof Object[] array) {
      return Arrays.stream(array).map(SymbolDetails::value).collect(Collectors.joining(",", "{", "}"));
    }
    if (value instanceof Symbol symbol) {
      return qualified(symbol);
    }
    return String.valueOf(value);
  }

  private static String qualified(Symbol symbol) {
    return name(symbol.owner()) + "." + symbol.name();
  }

  private static String name(Symbol symbol) {
    return symbol == null ? "null" : Objects.toString(symbol.name());
  }
}
