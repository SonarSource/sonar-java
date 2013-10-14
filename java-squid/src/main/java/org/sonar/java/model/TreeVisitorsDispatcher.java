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
package org.sonar.java.model;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TreeVisitorsDispatcher extends BaseTreeVisitor {

  private final List<? extends Object> visitors;
  private final Set<Class> visitorClasses;
  private final Map<Class, Map<Class, Method>> visitMethods = Maps.newHashMap();
  private final Map<Class, Map<Class, Method>> leaveMethods = Maps.newHashMap();

  public TreeVisitorsDispatcher(List<? extends TreeVisitor> visitors) {
    this.visitors = visitors;
    ImmutableSet.Builder<Class> visitorClassesBuilder = ImmutableSet.builder();
    for (Object visitor : visitors) {
      visitorClassesBuilder.add(visitor.getClass());
    }
    this.visitorClasses = visitorClassesBuilder.build();
  }

  public void visit(Object node, Class<?> nodeClass) {
    invoke(visitMethods, false, nodeClass, node);
  }

  public void leave(Object node, Class<?> nodeClass) {
    invoke(leaveMethods, true, nodeClass, node);
  }

  private void invoke(Map<Class, Map<Class, Method>> cache, boolean leave, Class<?> treeClass, Object tree) {
    Map<Class, Method> methods = cache.get(treeClass);
    if (methods == null) {
      methods = lookup(leave, treeClass);
      cache.put(treeClass, methods);
    }
    for (Object visitor : leave ? Lists.reverse(visitors) : visitors) {
      Method method = methods.get(visitor.getClass());
      if (method != null) {
        try {
          method.invoke(visitor, tree);
        } catch (IllegalAccessException e) {
          throw Throwables.propagate(e);
        } catch (InvocationTargetException e) {
          throw Throwables.propagate(e);
        }
      }
    }
  }

  private Map<Class, Method> lookup(boolean leave, Class<?> treeClass) {
    ImmutableMap.Builder<Class, Method> methodsBuilder = ImmutableMap.builder();
    for (Class visitorClass : visitorClasses) {
      Method method = lookup(visitorClass, leave ? "leave" : "visit", treeClass);
      if (method != null) {
        methodsBuilder.put(visitorClass, method);
      }
    }
    return methodsBuilder.build();
  }

  @Nullable
  private static Method lookup(Class<?> visitorClass, String methodName, Class<?> nodeClass) {
    try {
      return visitorClass.getMethod(methodName, nodeClass);
    } catch (NoSuchMethodException e) {
      for (Class base : nodeClass.getInterfaces()) {
        return lookup(visitorClass, methodName, base);
      }
      return null;
    }
  }

  @Override
  protected void scan(Tree tree) {
    if (tree != null) {
      JavaTree javaTree = (JavaTree) tree;
      if (javaTree.getKind() == null) {
        return;
      }
      visit(tree, javaTree.getKind().associatedInterface);
      javaTree.accept(this);
      leave(tree, javaTree.getKind().associatedInterface);
    }
  }

}
