enum Level {
  WARN;
}
interface Log {
  void log(Level level, String s, Exception e);
}

class A {
  static final Log LOGGER;

  public void run () {
    try {
      while (true) {
        // do stuff
      }
    }catch (java.io.IOException e) {
      LOGGER.log(Level.WARN, "Interrupted!", e);
    }catch (InterruptedException e) { // Noncompliant [[sc=13;ec=35]] {{Either re-interrupt this method or rethrow the "InterruptedException".}}
        LOGGER.log(Level.WARN, "Interrupted!", e);
    }
  }

  public void runUnknownSymbol () {
    try {
      while (true) {
        // do stuff
      }
    }catch (java.io.IOException e) {
      LOGGER.log(Level.WARN, "Interrupted!", e);
    }catch (InterruptedException e) { // Noncompliant
      unknownField.log(Level.WARN, "Interrupted!", e);
    }
  }

  public void catchUnionType () {
    try {
      while (true) {
        // do stuff
      }
    } catch (InterruptedException | java.io.IOException e) { // Noncompliant [[sc=14;ec=58]] {{Either re-interrupt this method or rethrow the "InterruptedException".}}
      unknownField.log(Level.WARN, "Interrupted!", e);
    }
  }

  public void run () throws InterruptedException{
    try {
      while (true) {
        // do stuff
      }
    } catch (InterruptedException e) {
        LOGGER.log(Level.WARN, "Interrupted!", e);
        // clean up state...
        throw e;
    } catch (InterruptedException e) { // Noncompliant {{Either re-interrupt this method or rethrow the "InterruptedException".}}
      LOGGER.log(Level.WARN, "Interrupted!", e);
      throw new java.io.IOException();
    } catch (InterruptedException e) { // Noncompliant {{Either re-interrupt this method or rethrow the "InterruptedException".}}
      LOGGER.log(Level.WARN, "Interrupted!", e);
      Exception e1 = new Exception();
      throw e1;
    } catch (InterruptedException e) { // Noncompliant {{Either re-interrupt this method or rethrow the "InterruptedException".}}
      LOGGER.log(Level.WARN, "Interrupted!", e);
      throw new IllegalStateException("foo", e);
    } catch (ThreadDeath threadDeath) {
      throw threadDeath;
    } catch (ThreadDeath threadDeath) { // Noncompliant {{Either re-interrupt this method or rethrow the "ThreadDeath".}}
      throw new java.io.IOException();
  }
}

  public void run () {
    try {
      while (true) {
        // do stuff
      }
    } catch (InterruptedException e) {
        LOGGER.log(Level.WARN, "Interrupted!", e);
        // clean up state...
        Thread.currentThread().interrupt();
      }
    try {
      while (true) {
        // do stuff
      }
    } catch (InterruptedException e) { // Noncompliant {{Either re-interrupt this method or rethrow the "InterruptedException".}}
      LOGGER.log(Level.WARN, "Interrupted!", e);
      // clean up state...
      new Foo().interrupt();
    }
    }

  public Task getNextTask(BlockingQueue<Task> queue) {
    boolean interrupted = false;
    try {
      while (true) {
        try {
          return queue.take();
        } catch (InterruptedException e) {
          interrupted = true;
          // fall through and retry
        }
      }
    } finally {
      if (interrupted)
        Thread.currentThread().interrupt();
    }
  }

}

class Foo {
  void interrupt();
}
