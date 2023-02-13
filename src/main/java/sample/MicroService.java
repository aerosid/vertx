package sample;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class MicroService extends AbstractVerticle {

  private HttpServer server;

  private Router getRouter() {
    Router router = Router.router(super.vertx);
    router.route().handler(this::onRequest);
    return router;
  }

  private void onRequest(RoutingContext ctx) {
    HttpServerResponse response = ctx.response();
    response.putHeader("content-type", "text/plain");
    response.end("Hello World!");    
    return;
  }

  public void start() throws Exception {
    this.server = vertx.createHttpServer().requestHandler(this.getRouter()).listen(8080);
    return;
  }

  public void stop() throws Exception  {
    if (this.server != null) {
      this.server.close();
    }
    return;
  }

}
