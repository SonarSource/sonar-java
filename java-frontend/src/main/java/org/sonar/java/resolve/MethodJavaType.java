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
package org.sonar.java.resolve;

import javax.annotation.Nullable;

import java.util.List;

public class MethodJavaType extends JavaType {

  List<JavaType> argTypes;
  // Return type of constructor is null.
  @Nullable
  JavaType resultType;
  List<JavaType> thrown;

  public MethodJavaType(List<JavaType> argTypes, @Nullable JavaType resultType, List<JavaType> thrown, JavaSymbol.TypeJavaSymbol symbol) {
    super(METHOD, symbol);
    this.argTypes = argTypes;
    this.resultType = resultType;
    this.thrown = thrown;
  }

  @Override
  public String toString() {
    return resultType == null ? "constructor" : ("returns " + resultType.toString());
  }

  @Nullable
  public JavaType resultType() {
    return resultType;
  }

  public List<JavaType> thrownTypes() {
    return thrown;
  }

  public List<JavaType> argTypes() {
    return argTypes;
  }
}
