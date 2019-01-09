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

public class Types {

  /**
   * JLS7 4.10. Subtyping
   */
  public boolean isSubtype(JavaType t, JavaType s) {
    boolean result;

    if (t == s) {
      result = true;
    } else {
      switch (t.tag) {
        case JavaType.BYTE:
        case JavaType.CHAR:
          result = t.tag == s.tag || (t.tag + /* skip char for byte and short for char */2 <= s.tag && s.tag <= JavaType.DOUBLE);
          break;
        case JavaType.SHORT:
        case JavaType.INT:
        case JavaType.LONG:
        case JavaType.FLOAT:
        case JavaType.DOUBLE:
          result = t.tag <= s.tag && s.tag <= JavaType.DOUBLE;
          break;
        case JavaType.BOOLEAN:
        case JavaType.VOID:
          result = t.tag == s.tag;
          break;
        case JavaType.ARRAY:
          if(t.tag != s.tag) {
            //t is array, if tags are different then the only way t is subtype of s is s to be object ie: superclass of arrayClass
            result = t.getSymbol().getSuperclass() == s;
            break;
          }
          result = isSubtype(((ArrayJavaType) t).elementType(), ((ArrayJavaType) s).elementType());
          break;
        case JavaType.CLASS:
        case JavaType.PARAMETERIZED:
        case JavaType.WILDCARD:
        case JavaType.TYPEVAR:
          result = t.isSubtypeOf(s);
          break;
        case JavaType.BOT:
          result = s.tag == JavaType.BOT || s.tag == JavaType.CLASS || s.tag == JavaType.ARRAY;
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
