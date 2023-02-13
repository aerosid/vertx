package sample;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
public class VertxTest {  

  private Async async;

  private Vertx vertx;

  @Test
  public void complexHandler(TestContext testContext) throws Throwable {
    this.async = testContext.async();   
    int amount = 5;
    TimerEvent event = (Long timeout) -> {
      this.increment(amount, timeout);
    };
    this.vertx.setTimer(3000, event::onTimeout);
    return;
  }

  @After
  public void destroy() {
    this.vertx.close();
    return;
  }

  private void increment(int amount, Long timeout) {
    long incrementedTimeout = Integer.valueOf(amount).longValue() + timeout.longValue();
    this.onTimer(Long.valueOf(incrementedTimeout));
    return;
  }

  @Before
  public  void init() {
    this.vertx = Vertx.vertx();
    return;
  }

  @Test
  public void simpleHandler(TestContext testContext) throws Throwable {
    this.async = testContext.async();   
    this.vertx.setTimer(3000, this::onTimer);
 
    return;
  }

  private void onTimer(Long timeout) {
    System.out.println("timeout: " + timeout + ", fired!");
    this.async.complete();
    return;
  }
}
