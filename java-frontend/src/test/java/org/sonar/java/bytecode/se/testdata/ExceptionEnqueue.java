/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.bytecode.se.testdata;

public class ExceptionEnqueue {

  static boolean test(ExceptionEnqueue ee) {
    try {
      ee.silentThrow();
      return false;
    } catch (MyException e) {
      return true;
    } catch (Error e) {
      throw new ErrorCatch();
    } catch (Exception e) {
      throw new ExceptionCatch();
    } catch (Throwable t) {
      throw new ThrowableCatch();
    }
  }

  void silentThrow() {
    throw new MyException();
  }

  public static class MyException extends RuntimeException { }
  public static class ErrorCatch extends RuntimeException {}
  public static class ThrowableCatch extends RuntimeException {}
  public static class ExceptionCatch extends RuntimeException {}

}


