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
package org.sonar.java.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.collections.SetUtils;

public class JProblem {

  private static final String USE_ENABLE_PREVIEW = " Use --enable-preview to enable";

  private final String message;
  private final Type type;

  JProblem(String message, Type type) {
    this.message = message;
    this.type = type;
  }

  public Type type() {
    return type;
  }

  public String message() {
    return message;
  }

  @Override
  public String toString() {
    if (type == JProblem.Type.PREVIEW_FEATURE_USED) {
      return cleanMessage(message);
    }
    return message;
  }

  private static String cleanMessage(String message) {
    if (message.endsWith(USE_ENABLE_PREVIEW)) {
      // Using --enable-preview is irrelevant in this context.
      return message.substring(0, message.length() - USE_ENABLE_PREVIEW.length());
    }
    return message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof JProblem)) {
      return false;
    }
    JProblem that = (JProblem) o;
    return type == that.type && message.equals(that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, message);
  }

  public enum Type {
    UNDEFINED_TYPE(IProblem.UndefinedType),
    PREVIEW_FEATURE_USED(IProblem.PreviewFeatureUsed),
    UNUSED_IMPORT(IProblem.UnusedImport, JavaCore.COMPILER_PB_UNUSED_IMPORT, Tree.Kind.IMPORT),
    REDUNDANT_CAST(IProblem.UnnecessaryCast, JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, Tree.Kind.TYPE_CAST, Tree.Kind.PARENTHESIZED_EXPRESSION),
    ASSIGNMENT_HAS_NO_EFFECT(IProblem.AssignmentHasNoEffect, JavaCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT, Tree.Kind.ASSIGNMENT),
    MASKED_CATCH(IProblem.MaskedCatch, JavaCore.COMPILER_PB_HIDDEN_CATCH_BLOCK, Tree.Kind.IDENTIFIER, Tree.Kind.MEMBER_SELECT);

    private final int warningID;
    private final String compilerOptionKey;
    private final Set<Tree.Kind> kinds;

    private static final Set<String> COMPILER_OPTIONS = new HashSet<>();

    Type(int warningID) {
      this.warningID = warningID;
      this.compilerOptionKey = null;
      this.kinds = Collections.emptySet();
    }

    Type(int warningID, String compilerOptionKey, Tree.Kind... kinds) {
      this.warningID = warningID;
      this.compilerOptionKey = compilerOptionKey;
      this.kinds = SetUtils.immutableSetOf(kinds);
    }

    boolean matches(IProblem warning) {
      return warning.getID() == warningID;
    }

    public static Set<String> compilerOptions() {
      if (COMPILER_OPTIONS.isEmpty()) {
        Stream.of(Type.values())
          .map(t -> t.compilerOptionKey)
          .forEach(COMPILER_OPTIONS::add);
      }
      return Collections.unmodifiableSet(COMPILER_OPTIONS);
    }

    public Set<Tree.Kind> getKinds() {
      return kinds;
    }
  }

}
