package checks.spring;

import javax.ws.rs.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

public class StatusCodesOnResponseCheckSample {

  class User {
  }

  @Controller
  class UserController {
    public ResponseEntity<User> getUserNoncompliant() {
      try {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new User()); // Noncompliant [[sc=16;ec=71]] {{Use the "ResponseEntity.ok()" method or set the status to
                                                                                         // "HttpStatus.OK".}}
      } catch (NotFoundException e) {
        return ResponseEntity.status(HttpStatus.OK).build(); // Noncompliant [[sc=16;ec=52]] {{Use the "ResponseEntity.badRequest()" or "ResponseEntity.notFound()" methodor set the
                                                             // status to "HttpStatus.INTERNAL_SERVER_ERROR" or "HttpStatus.NOT_FOUND".}}
      } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Compliant
      }
    }

    public ResponseEntity<User> getUserNoncompliant2() {
      try {
        return ResponseEntity.badRequest().build(); // Noncompliant
      } catch (NotFoundException e) {
        return ResponseEntity.ok().build(); // Noncompliant
      } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Compliant
      }
    }

    public ResponseEntity<User> getUserNoncompliant3() {
      try {
        return ResponseEntity.notFound().build(); // Noncompliant
      } catch (NotFoundException e) {
        return ResponseEntity.ok().build(); // Noncompliant
      } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Compliant
      }
    }

    public ResponseEntity<User> getUserCompliant() {
      try {
        return ResponseEntity.ok(new User()); // Compliant
      } catch (NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Compliant
      } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // Compliant
      }
    }
  }

}
