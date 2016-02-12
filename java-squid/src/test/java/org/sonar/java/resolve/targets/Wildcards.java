/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.resolve.targets;

import java.util.ArrayList;
import java.util.List;

class Wildcards {
  public List<?> unboudedItems;
  public List<? extends String> extendsItems;
  public List<? super Number> superItems;

  public List<?> returnsUnboundedItems(List<?> param) {
    return param;
  }

  public List<? extends String> returnsExtendsItems(List<? extends String> param) {
    return param;
  }

  public List<? super Number> returnsSuperItems(List<? super Number> param) {
    return param;
  }

  List<String> myList = new ArrayList<>();
  public void foo() {
    myList.addAll(new ArrayList<String>());
  }
}

class WildcardUnboundedClass<X extends List<?>> {
}

class WildcardExtendsClass<X extends List<? extends String>> {
}

class WildcardSuperClass<X extends List<? super Number>> {
}
