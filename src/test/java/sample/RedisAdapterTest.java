package sample;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
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
public class RedisAdapterTest {
  
  private Async async;

  private Vertx vertx;

  @After
  public void destroy() {
    this.vertx.close();
    return;
  }

  @Before
  public  void init() {
    VertxOptions options = new VertxOptions(); 
    options.setMaxEventLoopExecuteTime(Long.MAX_VALUE);  //for debugging unit tests      
    this.vertx = Vertx.vertx(options); 
    return;
  }

  private void onFailure(Throwable t) {
    t.printStackTrace();
    this.async.complete();
    return;
  }

  private void onSuccess(String result) {
    System.out.println(" result: " + result);
    this.async.complete();
    return;
  }

  @Test
  public void testDel(TestContext testContext) throws Throwable {
    this.async = testContext.async();  
    System.out.print("command: del, key: greeting, ...");
    RedisAdapter
        .create(this.vertx)
        .success(this::onSuccess).failure(this::onFailure)
        .delete("greeting");

    return;
  }  

  @Test
  public void testGet(TestContext testContext) throws Throwable {
    this.async = testContext.async();  
    System.out.print("command: get, key: greeting, ...");
    RedisAdapter
        .create(this.vertx)
        .success(this::onSuccess).failure(this::onFailure)
        .get("greeting");

    return;
  }  

  @Test
  public void testSet(TestContext testContext) throws Throwable {
    this.async = testContext.async();  
    System.out.print("command: set, key: greeting, value: hello, ...");
    RedisAdapter
        .create(this.vertx)
        .success(this::onSuccess).failure(this::onFailure)
        .set("greeting", "namaste");

    return;
  }  

}
