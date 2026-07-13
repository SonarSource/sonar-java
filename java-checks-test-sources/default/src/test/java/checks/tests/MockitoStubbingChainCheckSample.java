package checks.tests;

import org.junit.jupiter.api.Test;
import org.mockito.stubbing.OngoingStubbing;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MockitoStubbingChainCheckSample {

  interface UserRepository {
    User findUser(long id);
    void deleteUser(long id);
    User saveUser(User user);
  }

  record User(String name) {}

  @Test
  void incompleteWhen() {
    UserRepository repo = mock(UserRepository.class);
    when(repo.findUser(42)); // Noncompliant {{Complete this stubbing by adding "thenReturn()", "thenThrow()", "thenAnswer()", or "thenCallRealMethod()".}}
  //^^^^^^^^^^^^^^^^^^^^^^^
  }

  @Test
  void completeWhen() {
    UserRepository repo = mock(UserRepository.class);
    when(repo.findUser(42)).thenReturn(new User("Alice")); // Compliant
    when(repo.findUser(43)).thenThrow(new RuntimeException()); // Compliant
    when(repo.findUser(44)).thenAnswer(inv -> new User("Bob")); // Compliant
    when(repo.findUser(45)).thenCallRealMethod(); // Compliant
  }

  @Test
  void incompleteDoMethods() {
    UserRepository repo = mock(UserRepository.class);
    doReturn(new User("Alice")); // Noncompliant {{Complete this stubbing by adding ".when(mock).method()".}}
  //^^^^^^^^^^^^^^^^^^^^^^^^^^^
    doThrow(new RuntimeException()); // Noncompliant {{Complete this stubbing by adding ".when(mock).method()".}} [[sc=5;ec=36]]
    doAnswer(inv -> null); // Noncompliant {{Complete this stubbing by adding ".when(mock).method()".}}
    doNothing(); // Noncompliant {{Complete this stubbing by adding ".when(mock).method()".}}
    doCallRealMethod(); // Noncompliant {{Complete this stubbing by adding ".when(mock).method()".}}
  }

  @Test
  void incompleteDoChainMissingMethodCall() {
    UserRepository repo = mock(UserRepository.class);
    doThrow(new RuntimeException()).when(repo); // Noncompliant {{Complete this stubbing by adding the method to stub.}}
  //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  @Test
  void completeDoMethods() {
    UserRepository repo = mock(UserRepository.class);
    doReturn(new User("Alice")).when(repo).findUser(42); // Compliant
    doThrow(new RuntimeException()).when(repo).deleteUser(99); // Compliant
    doAnswer(inv -> new User("Bob")).when(repo).saveUser(new User("Bob")); // Compliant
    doNothing().when(repo).deleteUser(1); // Compliant
    doCallRealMethod().when(repo).findUser(1); // Compliant
  }

  @Test
  void whenStoredInVariable() {
    UserRepository repo = mock(UserRepository.class);
    OngoingStubbing<User> stub = when(repo.findUser(42)); // Compliant - variable declaration skipped
    stub.thenReturn(new User("Alice"));
  }

  @Test
  void whenStoredViaAssignment() {
    UserRepository repo = mock(UserRepository.class);
    OngoingStubbing<User> stub;
    stub = when(repo.findUser(42)); // Compliant - assignment expression skipped
    stub.thenReturn(new User("Alice"));
  }

  @Test
  void incompleteWhenInsideLambda() {
    UserRepository repo = mock(UserRepository.class);
    org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
      when(repo.findUser(1)); // Noncompliant {{Complete this stubbing by adding "thenReturn()", "thenThrow()", "thenAnswer()", or "thenCallRealMethod()".}}
    });
  }

  @Test
  void completeWhenInsideLambda() {
    UserRepository repo = mock(UserRepository.class);
    org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
      when(repo.findUser(1)).thenReturn(new User("Alice")); // Compliant
    });
  }

  OngoingStubbing<User> helperMethod(UserRepository repo) {
    return when(repo.findUser(42)); // Compliant - return statement skipped
  }
}

