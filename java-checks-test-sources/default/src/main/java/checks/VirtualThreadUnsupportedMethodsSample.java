/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package checks;

public class VirtualThreadUnsupportedMethodsSample extends VirtualThreadUnsupportedMethodsParentSample{

  void noncompliant(Thread.Builder.OfVirtual builder) {
    var vt = builder.unstarted(() -> {
//                   ^^^^^^^^^>
    });
    vt.setDaemon(true); // Noncompliant
//     ^^^^^^^^^
    vt.setPriority(1); // Noncompliant
//     ^^^^^^^^^^^
    vt.getThreadGroup(); // Noncompliant
//     ^^^^^^^^^^^^^^
    Thread.startVirtualThread(() -> {
//         ^^^^^^^^^^^^^^^^^^>
    }).setDaemon(true); // Noncompliant
//     ^^^^^^^^^

    var newBuilder = Thread.ofVirtual();
    var vt1 = newBuilder.unstarted(() -> {
    });
    vt1.setDaemon(true); // Noncompliant {{Method 'setDaemon' is not supported on virtual threads.}}
    vt1.setPriority(1); // Noncompliant {{Method 'setPriority' is not supported on virtual threads.}}
    vt1.getThreadGroup(); // Noncompliant {{Method 'getThreadGroup' is not supported on virtual threads.}}

    var vt2 = Thread.ofVirtual().unstarted(() -> {
    });
    vt2.setDaemon(true); // Noncompliant
    vt2.setPriority(1); // Noncompliant
    vt2.getThreadGroup(); // Noncompliant

    var vt3 = Thread.ofVirtual().start(() -> {
    });
    vt3.setDaemon(true); // Noncompliant
    vt3.setPriority(1); // Noncompliant
    vt3.getThreadGroup(); // Noncompliant

    var vt4 = Thread.startVirtualThread(() -> {
    });
    vt4.setDaemon(true); // Noncompliant
    vt4.setPriority(1); // Noncompliant
    vt4.getThreadGroup(); // Noncompliant
  }

  void compliant(){
    Thread.startVirtualThread(() -> {
    });
    Thread.ofVirtual().unstarted(() -> {
    });
    Thread.ofVirtual().start(() -> {
    });
    var t = new Thread();
    t.setDaemon(true);
    t.setPriority(1);
    t.getThreadGroup();

    var t2 = Thread.ofPlatform().unstarted(() -> {
    });
    t2.setDaemon(true);
    t2.setPriority(1);
    t2.getThreadGroup();

    Thread.ofPlatform().start(() -> {
    }).setDaemon(true);

    new Thread().setDaemon(true);

    falseNegatives(Thread.startVirtualThread(()-> { }));
  }

  void falseNegatives(Thread virtualAtRuntime){
    virtualAtRuntime.setDaemon(true); // False negative
    outerVirtualThread.setDaemon(true); // False negative
  }

  class IdentifierTreeImplTest {

    SampleThread thread = new SampleThread();

    public void test() {
      thread.start();
    }

    private class SampleThread extends Thread {
      @Override
      public void run() {
        setDaemon(true); // generates => mit.methodSelect() instanceof IdentifierTreeImpl
      }
    }

  }

}
