/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.java.ast.parser;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;

import javax.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.List;

public class AstNodeHacks {

  private static final Field TOKEN_FIELD = getAstNodeField("token");
  private static final Field CHILD_INDEX_FIELD = getAstNodeField("childIndex");
  private static final Field PARENT_FIELD = getAstNodeField("parent");

  public static void setToken(AstNode astNode, @Nullable Token token) {
    setField(TOKEN_FIELD, astNode, token);
  }

  public static int getChildIndex(AstNode astNode) {
    return (Integer) getField(CHILD_INDEX_FIELD, astNode);
  }

  public static void setChildIndex(AstNode astNode, int childIndex) {
    setField(CHILD_INDEX_FIELD, astNode, childIndex);
  }

  public static void setParent(AstNode astNode, @Nullable AstNode parent) {
    setField(PARENT_FIELD, astNode, parent);
  }

  private static Field getAstNodeField(String name) {
    try {
      Field field = AstNode.class.getDeclaredField(name);
      field.setAccessible(true);
      return field;
    } catch (NoSuchFieldException e) {
      throw Throwables.propagate(e);
    }
  }

  private static void setField(Field field, Object instance, Object value) {
    try {
      field.set(instance, value);
    } catch (IllegalAccessException e) {
      throw Throwables.propagate(e);
    }
  }

  private static Object getField(Field field, Object instance) {
    try {
      return field.get(instance);
    } catch (IllegalAccessException e) {
      throw Throwables.propagate(e);
    }
  }

  // SSLR-367
  public static List<AstNode> getDescendants(AstNode astNode) {
    List<AstNode> result = Lists.newArrayList();
    for (AstNode child : astNode.getChildren()) {
      addDescendants(result, child);
    }
    return result;
  }

  private static void addDescendants(List<AstNode> result, AstNode astNode) {
    result.add(astNode);
    for (AstNode child : astNode.getChildren()) {
      addDescendants(result, child);
    }
  }

}
