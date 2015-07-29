/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.java.checks;

import com.google.common.collect.Sets;
import java.util.Set;
import org.sonar.plugins.java.api.tree.MethodTree;

public final class SerializableContractMethodTree {
  private static final Set<String> SERIALIZABLE_CONTRACT_METHODS = Sets.newHashSet();

  static {
    SERIALIZABLE_CONTRACT_METHODS.add("writeObject");
    SERIALIZABLE_CONTRACT_METHODS.add("readObject");
    SERIALIZABLE_CONTRACT_METHODS.add("writeReplace");
    SERIALIZABLE_CONTRACT_METHODS.add("readResolve");
    SERIALIZABLE_CONTRACT_METHODS.add("readObjectNoData");
  }

  private SerializableContractMethodTree() {
  }

  protected static boolean methodMatch(MethodTree tree) {
    return SERIALIZABLE_CONTRACT_METHODS.contains(tree.simpleName().name());
  }
}
