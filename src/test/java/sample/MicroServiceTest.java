package sample;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
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
public class MicroServiceTest {
  
  private Async async;

  private HttpClient httpClient;

  private Vertx vertx;

  @After
  public void destroy() {
    this.httpClient.close();    
    this.vertx.close();
    return;
  }

  @Before
  public void init() {
    this.vertx = Vertx.vertx();
    this.httpClient = this.vertx.createHttpClient();
    this.vertx.deployVerticle(MicroService.class.getName());

    return;
  }

  private void onEnd(AsyncResult<Void> result) {
    if (result.succeeded()) {
      this.async.complete();
    }
 
    return;
  }

  private void onException(Throwable t) {
    t.printStackTrace();
    this.async.complete();
    return;
  }

  private void onResponse(HttpClientResponse response) {
    response.bodyHandler(this::onReponseBody);
    return;
  }

  private void onReponseBody(Buffer buffer) {
    System.out.println(buffer.toString()); 
    this.async.complete();
    return;
  }

  @Test
  public void testMicroService(TestContext testContext) throws Throwable {
    this.async = testContext.async(); 
    RequestOptions requestOptions = new RequestOptions();
    requestOptions.setHost("localhost").setPort(8080).setURI("/").setSsl(false);
    HttpClientRequest request = this.httpClient.request(HttpMethod.GET, requestOptions);
    request.exceptionHandler(this::onException).handler(this::onResponse).end(this::onEnd);
    return;
  }

}
