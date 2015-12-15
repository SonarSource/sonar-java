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
package org.sonar.java.checks.methods;

public abstract class NameCriteria {

  public abstract boolean matches(String name);
  public static NameCriteria any() {
    return new AnyNameCriteria();
  }
  public static NameCriteria is(String exactName) {
    return new ExactNameCriteria(exactName);
  }

  public static NameCriteria startsWith(String prefix) {
    return new PrefixNameCriteria(prefix);
  }

  private static class ExactNameCriteria extends NameCriteria {
    private String exactName;

    public ExactNameCriteria(String exactName) {
      this.exactName = exactName;
    }

    @Override
    public boolean matches(String name) {
      return exactName.equals(name);
    }
  }

  private static class PrefixNameCriteria extends NameCriteria {
    private String prefix;

    public PrefixNameCriteria(String prefix) {
      this.prefix = prefix;
    }

    @Override
    public boolean matches(String name) {
      return name.startsWith(prefix);
    }

  }

  private static class AnyNameCriteria extends NameCriteria {
    @Override
    public boolean matches(String name) {
      return true;
    }
  }
}
