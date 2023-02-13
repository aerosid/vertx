package sample;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.Response;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class RedisAdapter {

  private Redis client;

  private JsonObject config;

  private boolean fired;
  
  private Handler<Throwable> failure;
  
  private Handler<String> success;

  private Vertx vertx;
  
  private RedisAdapter(Vertx v) {
    super();
    this.config = new JsonObject();
    this.fired = false;
    this.vertx = v;    
  }

  public static RedisAdapter create(Vertx v) {
    RedisAdapter adapter = new RedisAdapter(v);
    //Hint: set default values, if any.
    adapter.config.put("endPoint", "redis://localhost:6379");

    return adapter; 
  }

  private void close() {
    if (this.client != null) {
      this.client.close();
      this.client = null; //prevents close() being called again
    }
    return;
  }

  private Function<Redis, Future<Response>> api() {
    Function<Redis, Future<Response>> commandFuture = (Redis redis) -> {
      this.client = redis;
      String command = this.config.getString("command");
      Promise<Response> promise = Promise.promise();
      String key = null;
      String value = null;
      List<String> list = null;
      switch (command) {
        case "del":
          //See https://redis.io/commands/del/
          key = this.config.getString("key");
          list = List.of(key);
          RedisAPI.api(redis).del(list, promise);
          break;
        case "get":
          //See https://redis.io/commands/get/
          key = this.config.getString("key");
          RedisAPI.api(redis).get(key, promise);          
          break;
        case "set":
          //See https://redis.io/commands/set/
          key = this.config.getString("key");
          value = this.config.getString("value");
          list = List.of(key, value);
          RedisAPI.api(redis).set(list, promise);          
          break;
        default:
          throw new IllegalArgumentException("command=" + command + ", is illegal");
      }
      return promise.future();
    };
    return commandFuture; 
  }

  private Future<Redis> client() {
    Promise<Redis> promise = Promise.promise();
    Redis.createClient(this.vertx, new RedisOptions()).connect(promise);
    return promise.future();
  }

  public Future<String> delete(String key) {
    if (this.fired) {
      throw new IllegalStateException("Instance has already fired.");
    }
    this.fired = true;     
    this.config.put("command", "del");
    this.config.put("key", key);
    Future<String> job = this
        .client()
        .compose(this.api()::apply)
        .compose(this.response()::apply)
        .onComplete(this.onComplete()::accept)
        .onSuccess(this.onSuccess()::accept)
        .onFailure(this.onFailure()::accept);
    return job;
  }

  public RedisAdapter failure(Handler<Throwable> handler) {
    this.failure = handler;
    return this;
  }

  public Future<String> get(String key) {
    if (this.fired) {
      throw new IllegalStateException("Instance has already fired.");
    }
    this.fired = true;     
    this.config.put("command", "get");
    this.config.put("key", key);
    Future<String> job = this
        .client()
        .compose(this.api()::apply)
        .compose(this.response()::apply)
        .onComplete(this.onComplete()::accept)
        .onSuccess(this.onSuccess()::accept)
        .onFailure(this.onFailure()::accept);
    return job;
  }

  private Consumer<AsyncResult<String>> onComplete() {
    Consumer<AsyncResult<String>> onComplete = (AsyncResult<String> i) -> { 
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
  
  private Consumer<String> onSuccess() {
    Consumer<String> onSuccess = (String s) -> { 
      if (this.success != null) {
        this.success.handle(s); 
      }
      return;
    };  
    return onSuccess;
  }

  private Function<Response, Future<String>> response() {
    Function<Response, Future<String>> resultFuture = (Response response) -> {
      String result = (response == null) ? "n/a" : response.toString();
      Promise<String> promise = Promise.promise();
      promise.complete(result);
      return promise.future();
    };
    return resultFuture;
  }

  public Future<String> set(String key, String value) {
    if (this.fired) {
      throw new IllegalStateException("Instance has already fired.");
    }
    this.fired = true;     
    this.config.put("command", "set");
    this.config.put("key", key);
    this.config.put("value", value);
    Future<String> job = this
        .client()
        .compose(this.api()::apply)
        .compose(this.response()::apply)
        .onComplete(this.onComplete()::accept)
        .onSuccess(this.onSuccess()::accept)
        .onFailure(this.onFailure()::accept);
    return job;
  }

  public RedisAdapter success(Handler<String> handler) {
    this.success = handler;
    return this;
  } 
  
}
