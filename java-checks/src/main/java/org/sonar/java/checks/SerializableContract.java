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
import org.sonar.java.bytecode.asm.AsmMethod;

import java.util.Set;

public final class SerializableContract {

  private static final Set<String> SERIALIZABLE_CONTRACT_METHODS = Sets.newHashSet();

  public static final String SERIAL_VERSION_UID_FIELD = "serialVersionUID";

  static {
    SERIALIZABLE_CONTRACT_METHODS.add("writeObject");
    SERIALIZABLE_CONTRACT_METHODS.add("readObject");
    SERIALIZABLE_CONTRACT_METHODS.add("writeReplace");
    SERIALIZABLE_CONTRACT_METHODS.add("readResolve");
    SERIALIZABLE_CONTRACT_METHODS.add("readObjectNoData");
  }

  private SerializableContract() {
  }

  static boolean methodMatch(AsmMethod method) {
    return SERIALIZABLE_CONTRACT_METHODS.contains(method.getName());
  }

}
