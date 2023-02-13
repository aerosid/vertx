package sample;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;

import java.util.function.Consumer;
import java.util.function.Function;

public class HttpServerAdapter {

  private JsonObject config;

  private Consumer<Throwable> onException;

  private Consumer<HttpServerRequest> onRequest;

  private HttpServer server;

  private Vertx vertx;

  private HttpServerAdapter(Vertx v) {
    super();
    this.config = new JsonObject();
    this.vertx = v;
  }

  public static HttpServerAdapter create(Vertx v) {
    HttpServerAdapter adapter = new HttpServerAdapter(v);
    return adapter;
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

  public HttpServerAdapter keyStore(String classpath, String password) {
    this.config.put("keyStore", classpath);
    this.config.put("keyStorePassword", password);
    return this;
  }

  public HttpServerAdapter onException(Consumer<Throwable> onException) {
    this.onException = onException;
    return this;
  }

  private Consumer<Throwable> onException() {
    if (this.onException == null) {
      this.onException = (Throwable t) -> {
        t.printStackTrace();
        return;
      };
    }

    return this.onException;
  }

  public Consumer<HttpServerRequest> onRequest() {
    if (this.onRequest == null) {
      this.onRequest = (HttpServerRequest r) -> {
        HttpServerResponse response = r.response();
        response.putHeader("context-type", "text/plain");
        FileAdapter
          .create(this.vertx)
          .read("sample/HttpServerAdapter.txt")
          .onSuccess((Buffer b) -> {
            response.end(b); })
          .onFailure(this.onException()::accept);
      };
    }

    return this.onRequest;
  }

  public HttpServerAdapter onRequest(Consumer<HttpServerRequest> onRequest) {
    this.onRequest = onRequest;
    return this;
  }

  private Function<Buffer, Future<HttpServer>> server() {
    Function<Buffer, Future<HttpServer>> server = (Buffer b) -> {
      HttpServerOptions options = new HttpServerOptions();
      options.setHost(this.config.getString("host"));
      options.setPort(this.config.getInteger("port").intValue());
      if (this.config.containsKey("keyStore") && (this.config.getJsonObject("keyStore") != null)) {
        options.setPfxKeyCertOptions(new PfxOptions(this.config.getJsonObject("keyStore")));
        options.setSsl(true);
      } else {
        options.setSsl(false);
      }
      if (this.config.containsKey("trustStore") && (b != null)) {
        options.setPemTrustOptions(new PemTrustOptions().addCertValue(b));
      }

      this.server = this.vertx
          .createHttpServer(options)
          .exceptionHandler(this.onException()::accept)
          .requestHandler(this.onRequest()::accept)
          .listen();

      Promise<HttpServer> promise = Promise.promise();
      promise.complete(this.server);
      return promise.future();
    };
    return server;
  }

  public HttpServerAdapter start(String host, int port) {
    this.config.put("host", host);
    this.config.put("port", Integer.valueOf(port));
    this
        .keyStore()
        .compose(this.trustStore()::apply)
        .compose(this.server()::apply)
        .onSuccess((HttpServer server) -> {
          System.out.println("Listening on " + port); })
        .onFailure((Throwable t) -> {
          t.printStackTrace(); });
    return this;
  }

  public HttpServerAdapter stop() {
    if (this.server != null) {
      this.server.close();
      this.server = null; //prevents close() being called again
    }
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
        future = FileAdapter.create(this.vertx).read(this.config.getString("keyStore"));
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

  public HttpServerAdapter trustStore(String classpath) {
    this.config.put("method", "trustStore");
    return this;
  }

}
