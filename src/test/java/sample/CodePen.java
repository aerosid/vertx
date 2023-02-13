package sample;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.junit5.VertxExtension;

import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
@ExtendWith(VertxExtension.class)
public class CodePen {

  private HttpServerAdapter adapter;

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

  @Test
  public void doSomething(TestContext testContext) throws Throwable {
    this.async = testContext.async();
    Consumer<HttpServerRequest> service = (HttpServerRequest r) -> {
      HttpServerResponse response = r.response();
      response.putHeader("content-type", "text/plain");
      response.end("SSH Remote Forwarding Works!");
      return;
    };
    this.adapter = HttpServerAdapter
        .create(this.vertx)
        .onRequest(service)
        .start("localhost", 8001);
    HttpGetAdapter
        .create(this.vertx)
        .success((Buffer b) -> {
          System.out.println("http://localhost:8001 " + b.toString());
          this.adapter.stop();
          this.async.complete();
          return;
        })
        .failure((Throwable t) -> {
          t.printStackTrace();
          this.async.complete();
          return;
        })
        .get("localhost", 80, "", false);


    return;
  }

}
