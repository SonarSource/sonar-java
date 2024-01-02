/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S3077")
public class VolatileNonPrimitiveFieldCheck extends IssuableSubscriptionVisitor {

  private static final String REF_MESSAGE = "Use a thread-safe type; adding \"volatile\" is not enough to make this field thread-safe.";
  private static final String ARRAY_MESSAGE = "Use an \"Atomic%sArray\" instead.";

  private static final List<String> STANDARD_IMMUTABLE_TYPES = Arrays.asList(
    "java.awt.Color",
    "java.awt.Cursor",
    "java.awt.Font",
    "java.io.File",
    "java.lang.Boolean",
    "java.lang.Byte",
    "java.lang.Character",
    "java.lang.Double",
    "java.lang.Float",
    "java.lang.Integer",
    "java.lang.Long",
    "java.lang.Short",
    "java.lang.String",
    "java.math.BigDecimal",
    "java.math.BigInteger",
    "java.net.Inet4Address",
    "java.net.Inet6Address",
    "java.net.URL",
    "java.time.Clock",
    "java.time.Instant",
    "java.time.LocalDate",
    "java.time.LocalDateTime",
    "java.time.LocalTime",
    "java.time.MonthDay",
    "java.time.OffsetDateTime",
    "java.time.OffsetTime",
    "java.time.Year",
    "java.time.YearMonth",
    "java.time.ZoneId",
    "java.time.ZoneOffset",
    "java.time.ZonedDateTime",
    "java.time.Duration",
    "java.time.Period",
    "java.util.Locale",
    "java.util.UUID");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    ((ClassTree) tree).members()
      .stream()
      .filter(m -> m.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .filter(v -> ModifiersUtils.hasModifier(v.modifiers(), Modifier.VOLATILE))
      .filter(v -> isUnsafeVolatile(v.type().symbolType()))
      .forEach(v -> reportIssue(ModifiersUtils.getModifier(v.modifiers(), Modifier.VOLATILE), v.type(), getMessage(v)));
  }

  private static boolean isUnsafeVolatile(Type symbolType) {
    return !symbolType.isUnknown() &&
      !(symbolType.isPrimitive()
      || isImmutableType(symbolType)
      || isSafelyAnnotated(symbolType.symbol().metadata()));
  }

  private static boolean isSafelyAnnotated(SymbolMetadata metadata) {
    return metadata.annotations().stream()
      .map(SymbolMetadata.AnnotationInstance::symbol)
      .map(Symbol::type)
      .anyMatch(type -> type.is("javax.annotation.concurrent.Immutable")
        || type.is("javax.annotation.concurrent.ThreadSafe"));
  }

  private static boolean isImmutableType(Type type) {
    return type.symbol().isEnum() || STANDARD_IMMUTABLE_TYPES.stream().anyMatch(type::is);
  }

  private static String getMessage(VariableTree variableTree) {
    Type varType = variableTree.type().symbolType();
    if (varType.isArray()) {
      String atomicType = "Reference";
      Type elementType = ((Type.ArrayType) varType).elementType();
      if (elementType.isPrimitive(Type.Primitives.LONG)) {
        atomicType = "Long";
      } else if (elementType.isPrimitive(Type.Primitives.INT)) {
        atomicType = "Integer";
      }
      return String.format(ARRAY_MESSAGE, atomicType);
    }
    return REF_MESSAGE;
  }
}
