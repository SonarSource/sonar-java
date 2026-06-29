package checks;

import io.quarkus.security.Authenticated;
import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnPongMessage;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import jakarta.annotation.security.RolesAllowed;

class QuarkusWebSocketSecurityAnnotationCheckSample {

  @WebSocket(path = "/secure")
  class AuthenticatedOnMethod {
    @Authenticated // Noncompliant {{Move this security annotation to the WebSocket endpoint class to secure the HTTP upgrade handshake.}}
    @OnTextMessage
    String echo(String msg) {
      return msg;
    }
  }

  @WebSocket(path = "/admin")
  class RolesAllowedOnMethod {
    @RolesAllowed("admin") // Noncompliant {{Move this security annotation to the WebSocket endpoint class to secure the HTTP upgrade handshake.}}
    @OnTextMessage
    String handleMessage(String msg) {
      return msg;
    }
  }

  @WebSocket(path = "/multi")
  class MultipleCallbacksWithSecurity {
    @Authenticated // Noncompliant {{Move this security annotation to the WebSocket endpoint class to secure the HTTP upgrade handshake.}}
    @OnOpen
    void onOpen() {
    }

    @RolesAllowed("admin") // Noncompliant {{Move this security annotation to the WebSocket endpoint class to secure the HTTP upgrade handshake.}}
    @OnTextMessage
    String onMessage(String msg) {
      return msg;
    }

    @Authenticated // Noncompliant {{Move this security annotation to the WebSocket endpoint class to secure the HTTP upgrade handshake.}}
    @OnClose
    void onClose() {
    }
  }

  @WebSocket(path = "/callbacks")
  class DifferentCallbackTypes {
    @Authenticated // Noncompliant {{Move this security annotation to the WebSocket endpoint class to secure the HTTP upgrade handshake.}}
    @OnBinaryMessage
    void onBinary(byte[] data) {
    }

    @RolesAllowed("user") // Noncompliant {{Move this security annotation to the WebSocket endpoint class to secure the HTTP upgrade handshake.}}
    @OnError
    void onError(Throwable t) {
    }

    @Authenticated // Noncompliant {{Move this security annotation to the WebSocket endpoint class to secure the HTTP upgrade handshake.}}
    @OnPongMessage
    void onPong() {
    }
  }

  @WebSocket(path = "/secure-class")
  @Authenticated
  class ClassLevelAuthenticated {
    @OnTextMessage
    String echo(String msg) {
      return msg;
    }
  }

  @WebSocket(path = "/admin-class")
  @RolesAllowed("admin")
  class ClassLevelRolesAllowed {
    @OnTextMessage
    String handleMessage(String msg) {
      return msg;
    }
  }

  @WebSocket(path = "/hybrid")
  @Authenticated
  class HybridSecurity {
    @OnTextMessage
    String normalMessage(String msg) {
      return msg;
    }

    @RolesAllowed("admin")
    @OnTextMessage
    String adminMessage(String msg) {
      return msg;
    }
  }

  @WebSocket(path = "/multi-secure")
  @RolesAllowed("user")
  class SecureClassMultipleMethods {
    @OnOpen
    void onOpen() {
    }

    @OnTextMessage
    String onMessage(String msg) {
      return msg;
    }

    @OnClose
    void onClose() {
    }
  }

  @WebSocket(path = "/public")
  class PublicEndpoint {
    @OnTextMessage
    String echo(String msg) {
      return msg;
    }
  }

  class NotAWebSocket {
    @Authenticated
    void someMethod() {
    }
  }

  @WebSocket(path = "/helper")
  class SecurityOnNonCallback {
    @OnTextMessage
    String echo(String msg) {
      return process(msg);
    }

    @Authenticated
    String process(String msg) {
      return msg.toUpperCase();
    }
  }
}
