package sample;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;

import java.util.function.Consumer;
import java.util.function.Function;

public class HttpGetAdapter {

  private HttpClient client;

  private JsonObject config;

  private Handler<Throwable> failure;

  private boolean fired;

  private Handler<Buffer> success;

  private Vertx vertx;

  private HttpGetAdapter(Vertx v) {
    super();
    this.config = new JsonObject();
    this.fired = false;
    this.vertx = v;
  }

  private Function<Buffer, Future<HttpClient>> client() {
    Function<Buffer, Future<HttpClient>> client = (Buffer b) -> {
      HttpClientOptions options = new HttpClientOptions();
      options.setDefaultHost(this.config.getString("host"));
      options.setDefaultPort(this.config.getInteger("port").intValue());
      if (this.config.containsKey("ssl") && this.config.getBoolean("ssl")) {
        options.setSsl(true);
      } else {
        options.setSsl(false);
      }
      if (this.config.containsKey("keyStore") && (this.config.getJsonObject("keyStore") != null)) {
        options.setPfxKeyCertOptions(new PfxOptions(this.config.getJsonObject("keyStore")));
      }
      if (this.config.containsKey("trustStore") && (b != null)) {
        options.setPemTrustOptions(new PemTrustOptions().addCertValue(b));
      }

      this.client = this.vertx.createHttpClient(options);

      Promise<HttpClient> promise = Promise.promise();
      promise.complete(this.client);
      return promise.future();
    };

    return client;
  }

  private void close() {
    if (this.client != null) {
      this.client.close();
      this.client = null; //prevents close() being called again
    }
    return;
  }

  public static HttpGetAdapter create(Vertx v) {
    HttpGetAdapter adapter = new HttpGetAdapter(v);
    return adapter;
  }

  public HttpGetAdapter failure(Handler<Throwable> handler) {
    this.failure = handler;
    return this;
  }

  public Future<Buffer> get(String host, int port, String path, boolean ssl) {
    if (this.fired) {
      throw new IllegalStateException("Instance has already fired.");
    }
    this.fired = true;
    this.config
        .put("host", host)
        .put("port", Integer.valueOf(port))
        .put("path", path)
        .put("ssl", ssl);
    Future<Buffer> job = this
        .keyStore()
        .compose(this.trustStore()::apply)
        .compose(this.client()::apply)
        .compose(this.request()::apply)
        .compose(this.response()::apply)
        .compose(this.responseBody()::apply)
        .onComplete(this.onComplete()::accept)
        .onSuccess(this.onSuccess()::accept)
        .onFailure(this.onFailure()::accept);
    return job;
  }

  private Future<Buffer> keyStore() {
    Future<Buffer> future;
    if (this.config.containsKey("keyStore")) {
      future = FileAdapter.create(this.vertx).read(this.config.getString("keyStore"));
    } else {
      Buffer b = null;
      Promise<Buffer> promise = Promise.promise();
      promise.complete(b);
      future = promise.future();
    }
    return future;
  }

  public HttpGetAdapter keyStore(String classpath, String password) {
    this.config.put("keyStore", classpath);
    this.config.put("keyStorePassword", password);
    return this;
  }

  private Consumer<AsyncResult<Buffer>> onComplete() {
    Consumer<AsyncResult<Buffer>> onComplete = (AsyncResult<Buffer> b) -> {
      this.close();
      return;
    };
    return onComplete;
  }

  private Consumer<Throwable> onFailure() {
    Consumer<Throwable> onFailure = (Throwable t) -> {
      if (this.failure != null) {
        this.failure.handle(t);
      }
      return;
    };
    return onFailure;
  }

  private Consumer<HttpClientResponse> onResponse(Promise<HttpClientResponse> promise) {
    Consumer<HttpClientResponse> handler = (HttpClientResponse r) -> {
      promise.complete(r);
      return;
    };
    return handler;
  }

  private Consumer<Buffer> onResponseBody(Promise<Buffer> promise) {
    Consumer<Buffer> handler = (Buffer b) -> {
      promise.complete(b);
      return;
    };
    return handler;
  }

  private Consumer<Buffer> onSuccess() {
    Consumer<Buffer> onSuccess = (Buffer b) -> {
      if (this.success != null) {
        this.success.handle(b);
      }
      return;
    };
    return onSuccess;
  }

  private Function<HttpClient, Future<HttpClientRequest>> request() {
    Function<HttpClient, Future<HttpClientRequest>>  request = (HttpClient client) -> {
      Promise<HttpClientRequest> promise = Promise.promise();
      promise.complete(client.get(this.config.getString("path")));
      return promise.future();
    };
    return request;
  }

  private Function<HttpClientRequest, Future<HttpClientResponse>> response() {
    Function<HttpClientRequest, Future<HttpClientResponse>> response = (HttpClientRequest req) -> {
      Promise<HttpClientResponse> promise = Promise.promise();
      req
          .exceptionHandler(this.onFailure()::accept)
          .handler(this.onResponse(promise)::accept)
          .end();
      return promise.future();
    };
    return response;
  }

  private Function<HttpClientResponse, Future<Buffer>> responseBody() {
    Function<HttpClientResponse, Future<Buffer>> responseBody = (HttpClientResponse response) -> {
      Promise<Buffer> promise = Promise.promise();
      response.bodyHandler(this.onResponseBody(promise)::accept);
      return promise.future();
    };
    return responseBody;
  }

  public HttpGetAdapter success(Handler<Buffer> handler) {
    this.success = handler;
    return this;
  }

  private Function<Buffer, Future<Buffer>> trustStore() {
    Function<Buffer, Future<Buffer>> trustStore = (Buffer b) -> {
      if (this.config.containsKey("keyStore") && b != null) {
        JsonObject object = new PfxOptions()
            .setPassword(this.config.getString("keyStorePassword"))
            .setValue(b)
            .toJson();
        this.config.put("keyStore", object);
      }
      Future<Buffer> future;
      if (this.config.containsKey("trustStore")) {
        future = FileAdapter.create(this.vertx).read(this.config.getString("trustStore"));
      } else {
        Buffer buffer = null;
        Promise<Buffer> promise = Promise.promise();
        promise.complete(buffer);
        future = promise.future();
      }
      return future;
    };
    return trustStore;
  }

  public HttpGetAdapter trustStore(String classpath) {
    this.config.put("trustStore", classpath);
    return this;
  }
}
