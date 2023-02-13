package sample;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
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
public class HttpGetAdapterTest {
  
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

  private void onSuccess(Buffer result) {
    String response = result.toString().replaceAll("\\s+", " ");
    System.out.println("http://httpbin.org/ip: " + response);
    this.async.complete();
    return;
  }

  @Test
  public void testGet(TestContext testContext) throws Throwable {
    this.async = testContext.async();
    HttpGetAdapter
        .create(this.vertx)
        .success(this::onSuccess).failure(this::onFailure)
        .get("httpbin.org", 80, "/ip", false);
    return;
  }  

}
