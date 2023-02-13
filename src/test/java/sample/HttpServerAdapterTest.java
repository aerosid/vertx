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
public class HttpServerAdapterTest {

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
  public void httpGet(TestContext testContext) throws Throwable {
    this.async = testContext.async();
    this.adapter = HttpServerAdapter.create(this.vertx).start("localhost", 8000);
    HttpGetAdapter
        .create(this.vertx)
        .success((Buffer b) -> {
          System.out.println("http://localhost:8000 " + b.toString());
          this.adapter.stop();
          this.async.complete();
          return;
        })
        .failure((Throwable t) -> {
          t.printStackTrace();
          this.async.complete();
          return;
        })
        .get("localhost", 8000, "", false);
    return;
  }

  @Test
  public void httpsGet(TestContext testContext) throws Throwable {
    this.async = testContext.async();
    this.adapter = HttpServerAdapter
        .create(this.vertx)
        .keyStore("sample/localhost.p12", "namaste")
        .trustStore("sample/vertx.crt")
        .start("localhost", 8443);
    HttpGetAdapter
        .create(this.vertx)
        .keyStore("sample/vertx.p12", "namaste")
        .trustStore("sample/localhost.crt")
        .success((Buffer b) -> {
          System.out.println("https://localhost:8443 " + b.toString());
          this.adapter.stop();
          this.async.complete();
          return;
        })
        .failure((Throwable t) -> {
          t.printStackTrace();
          this.async.complete();
          return;
        })
        .get("localhost", 8443, "", true);
    return;
  }

}
