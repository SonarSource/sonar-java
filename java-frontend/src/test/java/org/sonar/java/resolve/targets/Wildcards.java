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
package org.sonar.java.resolve.targets;

class Wildcards {

  ParamType<?> equalityUnboundedWildcard1;
  ParamType<?> equalityUnboundedWildcard2;
  ParamType<? extends String> equalityExtendsWildcard1;
  ParamType<? extends String> equalityExtendsWildcard2;
  ParamType<? super Number> equalitySuperWildcard1;
  ParamType<? super Number> equalitySuperWildcard2;

  public ParamType<?> unboudedItems;
  public ParamType<? extends String> extendsItems;
  public ParamType<? super Number> superItems;

  public ParamType<?> returnsUnboundedItems(ParamType<?> param) {
    return param;
  }

  public ParamType<? extends String> returnsExtendsItems(ParamType<? extends String> param) {
    return param;
  }

  public ParamType<? super Number> returnsSuperItems(ParamType<? super Number> param) {
    return param;
  }
}

class ParamType<T> {
  ParamType<T> foo(ParamType<T> a) {
    return a;
  }
}

class WildcardUnboundedClass<X extends ParamType<?>> {
}

class WildcardExtendsClass<X extends ParamType<? extends String>> {
}

class WildcardSuperClass<X extends ParamType<? super Number>> {
}
