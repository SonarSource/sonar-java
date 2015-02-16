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
package org.sonar.java.resolve;

import com.google.common.collect.Sets;

import java.util.Set;

public class Types {

  /**
   * JLS7 4.10. Subtyping
   */
  public boolean isSubtype(Type t, Type s) {
    boolean result;

    if (t == s) {
      result = true;
    } else {
      switch (t.tag) {
        case Type.BYTE:
        case Type.CHAR:
          result = t.tag == s.tag || t.tag + /* skip char for byte and short for char */2 <= s.tag && s.tag <= Type.DOUBLE;
          break;
        case Type.SHORT:
        case Type.INT:
        case Type.LONG:
        case Type.FLOAT:
        case Type.DOUBLE:
          result = t.tag <= s.tag && s.tag <= Type.DOUBLE;
          break;
        case Type.BOOLEAN:
        case Type.VOID:
          result = t.tag == s.tag;
          break;
        case Type.ARRAY:
          if(t.tag != s.tag) {
            //t is array, if tags are different then the only way t is subtype of s is s to be object ie: superclass of arrayClass
            result = t.getSymbol().getSuperclass() == s;
            break;
          }
          result = isSubtype(((Type.ArrayType) t).elementType(), ((Type.ArrayType) s).elementType());
          break;
        case Type.CLASS:
          if(t.tag != s.tag) {
            result = false;
            break;
          }

          //FIXME work on erased types while generics method is not implemented/read from bytecode.
          Set<Type> erasedTypes = Sets.newHashSet();
          for (Type.ClassType classType : t.getSymbol().superTypes()) {
            erasedTypes.add(classType.erasure());
          }

          result = erasedTypes.contains(s);
          break;
        case Type.BOT:
          result = s.tag == Type.BOT || s.tag == Type.CLASS || s.tag == Type.ARRAY;
          break;
        default:
          // TODO error recovery, but should be rewritten to not happen at all
          result = false;
          break;
      }
    }

    return result;
  }

}
