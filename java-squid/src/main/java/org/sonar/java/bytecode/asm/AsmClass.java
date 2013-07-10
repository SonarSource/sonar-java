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
package org.sonar.java.bytecode.asm;

import org.sonar.java.bytecode.asm.AsmClassProvider.DETAIL_LEVEL;
import org.sonar.squid.api.SourceCodeEdgeUsage;

import java.util.*;

public final class AsmClass extends AsmResource {

  private String internalName;
  private DETAIL_LEVEL level;
  private AsmClass superClass;
  private Set<AsmClass> children;
  private Map<String, AsmMethod> methods = new HashMap<String, AsmMethod>();
  private Map<String, AsmField> fields = new HashMap<String, AsmField>();
  private int noc = 0;

  public AsmClass(String internalName, DETAIL_LEVEL level) {
    this.internalName = internalName;
    setDetailLevel(level);
  }

  public AsmClass(String internalName) {
    this.internalName = internalName;
  }

  void setDetailLevel(DETAIL_LEVEL level) {
    this.level = level;
  }

  DETAIL_LEVEL getDetailLevel() {
    return level;
  }

  public String getInternalName() {
    return internalName;
  }

  void addMethod(AsmMethod asmMethod) {
    methods.put(asmMethod.getKey(), asmMethod);
  }

  public Collection<AsmMethod> getMethods() {
    return methods.values();
  }

  public Collection<AsmField> getFields() {
    return fields.values();
  }

  void addField(AsmField field) {
    fields.put(field.getName(), field);
  }

  public AsmField getField(String fieldName) {
    return fields.get(fieldName);
  }

  AsmField getFieldOrCreateIt(String fieldName) {
    AsmField field = getField(fieldName);
    if (field != null) {
      return field;
    }
    field = new AsmField(this, fieldName);
    addField(field);
    return field;
  }

  public AsmMethod getMethod(String key) {
    return methods.get(key);
  }

  AsmMethod getMethodOrCreateIt(String key) {
    AsmMethod method = getMethod(key);
    if (method != null) {
      return method;
    }
    method = new AsmMethod(this, key);
    method.setBodyLoaded(false);
    addMethod(method);
    return method;
  }

  void setSuperClass(AsmClass superClass) {
    this.superClass = superClass;
    superClass.addChildren(this);
    addEdge(new AsmEdge(this, superClass, SourceCodeEdgeUsage.EXTENDS));
  }

  private void addChildren(AsmClass asmClass) {
    if ("java/lang/Object".equals(getInternalName())) {
      return;
    }
    if (children == null) {
      children = new HashSet<AsmClass>();
    }
    children.add(asmClass);
  }

  public AsmClass getSuperClass() {
    return superClass;
  }

  void addInterface(AsmClass implementedInterface) {
    implementedInterface.addChildren(this);
    addEdge(new AsmEdge(this, implementedInterface, SourceCodeEdgeUsage.IMPLEMENTS));
  }

  Set<AsmClass> getInterfaces() {
    return getImplementedInterfaces();
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object instanceof AsmClass) {
      return internalName.equals(((AsmClass) object).internalName);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return internalName.hashCode();
  }

  public int getNumberOfChildren() {
    if (children != null && noc == 0) {
      for (AsmClass child : children) {
        noc += child.getNumberOfChildren() + 1;
      }
    }
    return noc;
  }

}
