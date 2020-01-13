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
package org.eclipse.jdt.core.dom;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import javax.annotation.Nullable;

public final class ASTUtils {

  private static final Logger LOG = Loggers.get(ASTUtils.class);

  private ASTUtils() {
  }

  public static void mayTolerateMissingType(AST ast) {
    ast.getBindingResolver().lookupEnvironment().mayTolerateMissingType = true;
  }

  @Nullable
  public static ITypeBinding resolveType(AST ast, String name) {
    try {
      BindingResolver bindingResolver = ast.getBindingResolver();
      ReferenceBinding referenceBinding = bindingResolver
        .lookupEnvironment()
        .getType(CharOperation.splitOn('.', name.toCharArray()));
      return bindingResolver.getTypeBinding(referenceBinding);
    } catch (Exception e) {
      // exception on ECJ side when trying to resolve a Type, recover on null type
      LOG.error(String.format("ECJ Unable to resolve type %s", name), e);
      return null;
    }
  }

}
