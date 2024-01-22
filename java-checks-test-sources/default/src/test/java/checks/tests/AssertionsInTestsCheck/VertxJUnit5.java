package checks.tests.AssertionsInTestsCheck;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;

class VertxJUnit5 {

  static class SucceedingVerticle extends AbstractVerticle {
  };

  static class FailingVerticle extends AbstractVerticle {
    @Override
    public void init(Vertx vertx, Context context) {
      throw new RuntimeException();
    }
  };

  @Test
  void noAssertion(Vertx vertx, VertxTestContext vtc) { // Noncompliant
    vertx.deployVerticle(new SucceedingVerticle());
  }

  @Test
  void verifyWithoutAssertion(Vertx vertx, VertxTestContext vtc) { // Noncompliant
    vertx.deployVerticle(new SucceedingVerticle())
    .onComplete(id -> vtc.verify(() -> {}));  // vtc.verify handles exceptions but doesn't assert
  }

  @Test
  void succeeding(Vertx vertx, VertxTestContext vtc) { // Noncompliant
    vertx.deployVerticle(new SucceedingVerticle())
    .onComplete(vtc.succeeding(id -> { // succeeding(Handler) requires assertion in Handler lambda
      vtc.completeNow();
    }));
  }

  @Test
  void succeedingThenComplete(Vertx vertx, VertxTestContext vtc) {
    vertx.deployVerticle(new SucceedingVerticle())
    .onComplete(vtc.succeedingThenComplete()); // compliant, explicitly asserts only Future success
  }

  @Test
  void failing(Vertx vertx, VertxTestContext vtc) { // Noncompliant
    vertx.deployVerticle(new FailingVerticle())
    .onComplete(vtc.failing(e -> { // failing(Handler) requires assertion in Handler lambda
      vtc.completeNow();
    }));
  }

  @Test
  void failingThenComplete(Vertx vertx, VertxTestContext vtc) {
    vertx.deployVerticle(new FailingVerticle())
    .onComplete(vtc.failingThenComplete()); // compliant, explicitly asserts only Future failure
  }

  @Test
  void checkpoint(Vertx vertx, VertxTestContext vtc) {
    Checkpoint checkpoint = vtc.checkpoint(); // compliant, asserts one flag() call
    vertx.deployVerticle(new SucceedingVerticle())
    .onSuccess(res -> checkpoint.flag());
  }

  @Test
  void checkpointWithCount(Vertx vertx, VertxTestContext vtc) {
    Checkpoint checkpoint = vtc.checkpoint(2); // compliant, asserts two flag() calls
    vertx.deployVerticle(new SucceedingVerticle())
    .onSuccess(res -> checkpoint.flag())
    .compose(x -> vertx.deployVerticle(new SucceedingVerticle()))
    .onSuccess(res -> checkpoint.flag());
  }
  @Test
  void laxCheckpoint(Vertx vertx, VertxTestContext vtc) {
    Checkpoint checkpoint = vtc.laxCheckpoint(); // compliant, asserts one flag() call
    vertx.deployVerticle(new SucceedingVerticle())
    .onSuccess(res -> checkpoint.flag());
  }

  @Test
  void laxCheckpointWithCount(Vertx vertx, VertxTestContext vtc) {
    Checkpoint checkpoint = vtc.laxCheckpoint(2); // compliant, asserts two flag() calls
    vertx.deployVerticle(new SucceedingVerticle())
    .onSuccess(res -> checkpoint.flag())
    .compose(x -> vertx.deployVerticle(new SucceedingVerticle()))
    .onSuccess(res -> checkpoint.flag());
  }
}

