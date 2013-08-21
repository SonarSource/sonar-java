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
package org.sonar.java.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;

import java.util.Iterator;

public class AstNodeTokensMatcher {

  private AstNodeTokensMatcher() {
  }

  public static boolean matches(AstNode node, String string) {
    if (!node.hasToken()) {
      return string.isEmpty();
    }

    String tokenValue = node.getTokenOriginalValue();
    if (!string.startsWith(tokenValue)) {
      return false;
    }
    int offset = tokenValue.length();

    Iterator<Token> it = node.getTokens().iterator();
    it.next();

    while (it.hasNext()) {
      tokenValue = it.next().getOriginalValue();
      if (!string.regionMatches(offset, tokenValue, 0, tokenValue.length())) {
        return false;
      }
      offset = offset + tokenValue.length();
    }

    return offset == string.length();
  }

}
