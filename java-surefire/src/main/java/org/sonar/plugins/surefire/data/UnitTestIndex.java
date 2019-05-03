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
package org.sonar.plugins.surefire.data;

import com.google.common.collect.Sets;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @since 2.8
 */
public class UnitTestIndex {

  private Map<String, UnitTestClassReport> indexByClassname;

  public UnitTestIndex() {
    this.indexByClassname = new HashMap<>();
  }

  public UnitTestClassReport index(String classname) {
    return indexByClassname.computeIfAbsent(classname, name -> new UnitTestClassReport());
  }

  public UnitTestClassReport get(String classname) {
    return indexByClassname.get(classname);
  }

  public Set<String> getClassnames() {
    return Sets.newHashSet(indexByClassname.keySet());
  }

  public Map<String, UnitTestClassReport> getIndexByClassname() {
    return indexByClassname;
  }

  public int size() {
    return indexByClassname.size();
  }

  public UnitTestClassReport merge(String classname, String intoClassname) {
    UnitTestClassReport from = indexByClassname.get(classname);
    if (from!=null) {
      UnitTestClassReport to = index(intoClassname);
      to.add(from);
      indexByClassname.remove(classname);
      return to;
    }
    return null;
  }

  public void remove(String classname) {
    indexByClassname.remove(classname);
  }


}
