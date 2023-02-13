package sample;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;

import java.util.function.Consumer;
import java.util.function.Function;

public class FileAdapter {

  private JsonObject config;

  private Handler<Throwable> failure;

  private boolean fired;

  private Handler<Buffer> success;

  private Vertx vertx;

  private FileAdapter(Vertx v) {
    super();
    this.config = new JsonObject();
    this.fired = false;
    this.vertx = v;
  }

  private void close() {
    return;
  }

  public static FileAdapter create(Vertx v) {
    FileAdapter adapter = new FileAdapter(v);
    return adapter;
  }

  public FileAdapter failure(Handler<Throwable> handler) {
    this.failure = handler;
    return this;
  }

  public Future<FileSystem> fileSystem() {
    Promise<FileSystem> promise = Promise.promise();
    FileSystem fileSystem = this.vertx.fileSystem();
    promise.complete(fileSystem);
    return promise.future();
  }

  private Consumer<AsyncResult<Buffer>> onComplete() {
    Consumer<AsyncResult<Buffer>> onComplete = (AsyncResult<Buffer> i) -> {
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

  private Consumer<Buffer> onSuccess() {
    Consumer<Buffer> onSuccess = (Buffer b) -> {
      if (this.success != null) {
        this.success.handle(b);
      }
      return;
    };
    return onSuccess;
  }

  public Future<Buffer> read(String classpath) {
    if (this.fired) {
      throw new IllegalStateException("Instance has already fired.");
    }
    this.fired = true;
    this.config.put("classpath", classpath);
    Future<Buffer> job = this
        .fileSystem()
        .compose(this.readFile()::apply)
        .onComplete(this.onComplete()::accept)
        .onSuccess(this.onSuccess()::accept)
        .onFailure(this.onFailure()::accept);
    return job;
  }

  private Function<FileSystem, Future<Buffer>> readFile() {
    Function<FileSystem, Future<Buffer>> readFile = (FileSystem fileSystem) -> {
      Promise<Buffer> promise = Promise.promise();
      String classpath = this.config.getString("classpath");
      fileSystem.readFile(classpath, promise);
      return promise.future();
    };
    return readFile;
  }

  public FileAdapter success(Handler<Buffer> handler) {
    this.success = handler;
    return this;
  }
}
