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
package org.sonar.java.bytecode.se.testdata;

import java.io.FileNotFoundException;
import java.io.IOException;

public abstract class ExceptionEnqueue {

  static Object test(ExceptionEnqueue ee) {
    try {
      ee.silentThrow();
      return new Object();
    } catch (MyException e) {
      return null;
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

  abstract void throwIOException() throws IOException;

  static void throwSpecificException() throws IOException {
    throw new FileNotFoundException();
  }

  static boolean testCatchBlockEnqueue(ExceptionEnqueue ee) {
    try {
      ee.throwIOException();
    } catch (FileNotFoundException e) {
      return true;
    } catch (IOException e) {
      return false;
    }
    throw new RuntimeException();
  }

  static boolean testCatchBlockEnqueue2() throws IOException {
    try {
     throwSpecificException();
    } catch (FileNotFoundException e) {
      return true;
    } catch (IOException e) {
      // this return is not reachable, however catch is enqueued and yield is created anyway,
      // because we don't know if we have precise type of exception
      return false;
    }
    throw new RuntimeException();
  }

  static boolean enqueueExitBlock() throws IOException {
    throwSpecificException();
    return false;
  }

  static boolean enqueueExitBlock2(ExceptionEnqueue ee) throws IOException {
    try {
      ee.throwIOException();
    } catch (FileNotFoundException e) {
      return true;
    }
    return false;
  }

  static boolean enqueueExitBlock3() throws IOException {
    try {
      throwSpecificException();
    } finally {
      int x = 0;
    }
    return false;
  }

  static boolean enqueueExitBlock4() {
    try {
      throwSpecificException();
    } catch (Throwable e) {
      return true;
    }
    return false;
  }
}


