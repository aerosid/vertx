# README.md

## 1. Desktop Setup
```
Eclipse, Gradle, Java 11 and 17
```

## 2. Server Setup
```bash
# batch.sh will hold a bunch of commands to run in one go.
touch ~/batch.sh && chmod +x ~/batch.sh

# update and upgrade installed packages.
sudo tee ~/batch.sh<<EOF
#!/bin/bash
set -e
set -x
sudo apt update
sudo apt -y upgrade
sudo systemctl reboot
EOF
~/batch.sh
```

### 2.1. MySql
```bash
# install mysql
sudo tee ~/batch.sh<<EOF
#!/bin/bash
set -e
set -x
sudo apt install -y mysql-server
sudo systemctl enable mysql
sudo systemctl start mysql
sleep 10s
sudo systemctl status mysql
sudo mysql --execute="select version();"
sudo systemctl reboot
EOF
~/batch.sh

# configure database user, ubuntu
sudo tee ~/batch.sh<<EOF
#!/bin/bash
set -e
set -x
sudo mysql --execute="create user 'ubuntu'@'localhost' identified by 'hello';"
sudo mysql --execute="grant all privileges on *.* to 'ubuntu'@'localhost' with grant option;"
sudo mysql --execute="flush privileges;"
EOF
~/batch.sh
mysql -u ubuntu -p
```
### 2.2. Redis
```bash
# install redis
# see https://redis.io/docs/getting-started/installation/install-redis-on-linux/
sudo tee ~/batch.sh<<EOF
#!/bin/bash
set -e
set -x
curl -fsSL https://packages.redis.io/gpg | sudo gpg --dearmor -o /usr/share/keyrings/redis-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/redis-archive-keyring.gpg] https://packages.redis.io/deb $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/redis.list
sudo apt-get update
sudo apt-get install -y redis
sudo systemctl enable redis-server
sudo systemctl start redis-server
sleep 10s
sudo systemctl status redis-server
redis-cli ping
EOF
~/batch.sh
```
### 2.3. Kafka
```bash
# install kafkaa
# see https://tecadmin.net/how-to-install-apache-kafka-on-ubuntu-20-04/

sudo apt install -y openjdk-11-jdk

sudo tee ~/batch.sh<<EOF
#!/bin/bash
set -e
set -x
wget https://archive.apache.org/dist/kafka/2.7.0/kafka_2.13-2.7.0.tgz
tar -xzf kafka_2.13-2.7.0.tgz
sudo mv kafka_2.13-2.7.0 /usr/local/kafka
ll /usr/local/kafka/bin/*.sh | grep -e zookeeper-server -e kafka-server
EOF
~/batch.sh

tee ~/zookeeper.service<<EOF
[Unit]
Description=Apache Zookeeper server
Documentation=http://zookeeper.apache.org
Requires=network.target remote-fs.target
After=network.target remote-fs.target
[Service]
Type=simple
ExecStart=/usr/local/kafka/bin/zookeeper-server-start.sh /usr/local/kafka/config/zookeeper.properties
ExecStop=/usr/local/kafka/bin/zookeeper-server-stop.sh
Restart=on-abnormal
[Install]
WantedBy=multi-user.target
EOF
sudo mv ~/zookeeper.service /etc/systemd/system
sudo chown root:root /etc/systemd/system/zookeeper.service

sudo tee ~/kafka.service<<EOF
[Unit]
Description=Apache Kafka Server
Documentation=http://kafka.apache.org/documentation.html
Requires=zookeeper.service
[Service]
Type=simple
Environment="JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64"
ExecStart=/usr/local/kafka/bin/kafka-server-start.sh /usr/local/kafka/config/server.properties
ExecStop=/usr/local/kafka/bin/kafka-server-stop.sh
[Install]
WantedBy=multi-user.target
EOF
sudo mv ~/kafka.service /etc/systemd/system
sudo chown root:root /etc/systemd/system/kafka.service

sudo systemctl daemon-reload
sudo systemctl enable zookeeper
sudo systemctl start zookeeper
sudo systemctl enable kafka
sudo systemctl start kafka
sudo systemctl status kafka

#see https://stackoverflow.com/questions/54059408/error-replication-factor-1-larger-than-available-brokers-0-when-i-create-a-k
/usr/local/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic testTopic
/usr/local/kafka/bin/kafka-topics.sh --list --zookeeper localhost:2181
/usr/local/kafka/bin/kafka-console-producer.sh --broker-list localhost:9092 --topic testTopic
/usr/local/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic testTopic --from-beginning
```

### 2.4. Envoy
```bash
# install envoy
# see https://nextgentips.com/2021/12/14/how-to-install-envoy-proxy-server-on-ubuntu-20-04/
# see https://songrgg.github.io/architecture/deeper-understanding-to-envoy/
sudo tee ~/batch.sh<<EOF
#!/bin/bash
set -e
set -x
sudo apt install -y apt-transport-https gnupg2 curl lsb-release
curl -sL 'https://deb.dl.getenvoy.io/public/gpg.8115BA8E629CC074.key' | sudo gpg --dearmor -o /usr/share/keyrings/getenvoy-keyring.gpg
echo a077cb587a1b622e03aa4bf2f3689de14658a9497a9af2c427bba5f4cc3c4723 /usr/share/keyrings/getenvoy-keyring.gpg | sha256sum --check
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/getenvoy-keyring.gpg] https://deb.dl.getenvoy.io/public/deb/ubuntu $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/getenvoy.list
sudo apt update
sudo apt install -y getenvoy-envoy
envoy --version
EOF
~/batch.sh

tee ~/envoy.yaml<<EOF
static_resources:
  listeners:
  - name: listener_0
    address:
      socket_address:
        address: 0.0.0.0
        port_value: 10000
    filter_chains:
    - filters:
      - name: envoy.filters.network.http_connection_manager
        typed_config:
          "@type": type.googleapis.com/envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager
          stat_prefix: edge
          http_filters:
          - name: envoy.filters.http.router
            typed_config:
              "@type": type.googleapis.com/envoy.extensions.filters.http.router.v3.Router
          route_config:
            virtual_hosts:
            - name: direct_response_service
              domains: ["*"]
              routes:
              - match:
                  prefix: "/"
                route:
                  cluster: nginx
  clusters:
  - name: nginx
    connect_timeout: 5s
    load_assignment:
      cluster_name: nginx
      endpoints:
      - lb_endpoints:
        - endpoint:
            address:
              socket_address:
                address: 127.0.0.1
                port_value: 80
EOF
# See https://www.envoyproxy.io/docs/envoy/latest/start/quick-start/index.html
# See https://www.tetrate.io/blog/get-started-with-envoy-in-5-minutes/
envoy --mode validate -c envoy.yaml
envoy -c envoy.yaml
curl localhost:10000
```
### 2.5. Nginx
See 
- (Nginx docs)[https://docs.nginx.com/nginx/admin-guide/web-server/web-server/]
- (SSH Port Forwarding)[https://unix.stackexchange.com/questions/115897/whats-ssh-port-forwarding-and-whats-the-difference-between-ssh-local-and-remot]
```bash
# install nginx
sudo tee ~/batch.sh<<EOF
#!/bin/bash
set -e
set -x
sudo apt update
sudo apt install -y nginx
systemctl status nginx
sudo systemctl enable nginx
sudo systemctl restart nginx
sleep 5s
curl -I 127.0.0.1
EOF
~/batch.sh
```
`Directives` are single-line configuration parameters.  `Blocks` are containers holding multiple, related directives.  `Contexts` are top-level blocks that relate to the following traffic-types: `events` (general connections), `http` (http traffic), `mail` (mail traffic), and `stream` (TCP/UDP traffic).

Simple index page:
```bash
tee ~/index.html<<EOF
<html><head><title>Kaveri</title></head><body>Hello World!</body></html>
EOF
tee ~/simple.conf<<EOF
server {
  listen 80;
  root /var/www/localhost/html;
  index index.html;
  server_name localhost 127.0.0.1;
  location / {
    try_files \$uri \$uri/ =404;
  }
}
EOF
sudo tee ~/batch.sh<<EOF
#!/bin/bash
set -e
set -x
sudo mkdir -p /var/www/localhost/html
sudo cp ~/index.html /var/www/localhost/html/
sudo chown -R $USER:$USER /var/www/localhost/html
sudo chmod -R 755 /var/www/localhost
sudo cp ~/simple.conf /etc/nginx/sites-available/localhost
sudo chown root:root /etc/nginx/sites-available/localhost
sudo ln -s -f /etc/nginx/sites-available/localhost /etc/nginx/sites-enabled/
ls -l /etc/nginx/sites-enabled/
sudo systemctl reload nginx
sleep 5s
curl localhost
EOF
~/batch.sh
```
Validate:
```ps1
ssh -F .\ssh-config -i .\id_rsa -L 80:localhost:80 ivs-kaveri
```

Simple reverse-proxy:
```bash
tee ~/simple-reverse-proxy.conf<<EOF
upstream backend {
  server localhost:8001;
}
server {
  listen 80;
  server_name localhost;
  location / {
    proxy_pass http://backend;
  }
}
EOF
sudo tee ~/batch.sh<<EOF
#!/bin/bash
set -e
set -x
sudo cp ~/simple-reverse-proxy.conf /etc/nginx/sites-available/localhost
sudo chown root:root /etc/nginx/sites-available/localhost
sudo ln -s -f /etc/nginx/sites-available/localhost /etc/nginx/sites-enabled/
sudo systemctl reload nginx
EOF
~/batch.sh
```

## 3. KeyStore and TrustStore

Key Pair = public key (shared) + private key (not shared)

SSL Cert. = public key encrypted by private key of Certificate Authority (i.e. trusted party) + public key

Self-signed SSL Cert. = public key encrypted by associated private key + public key

Key Store = Client/Self certificate + private key

Trust Store = Server/Other certificate

DER file = certificate in binary format

PEM file = certificate in base64 format

PFX/PKCS#12 file = certificate + private key

See [SSL Converter](https://www.sslshopper.com/ssl-converter.html)

Create self-signed certificate
```bash
keytool -genkey -alias domain -keyalg RSA -validity 365 -keystore keystore.jks
keytool -list -v -keystore keystore.jks
```

Export certificate as DER file
```bash
keytool -exportcert -alias domain -keystore keystore.jks -file domain.der
```

Export certificate + private key as PKCS12 file
```bash
keytool -importkeystore -srckeystore keystore.jks -destkeystore keystore.p12 -deststoretype PKCS12
```

## 4. Encyrption/Decryption
```bash
plain text to/from cipher text
    Symmetric Key
        RC4, AES, DES, 3DES
    Asymmetric Key
        RSA, ECC, Diffie-Hellman, El Gamal, and DSA

Hashing (a.k.a signature)
plain/cipher text to hash/signature
MD5, SHA-1, SHA-2
```

## 5. Vertx

### 5.1. Handler<AsyncResult<T>>

Given:
```java
void ClassA.asyncOp1(Handler<AsyncResult<Class1>> handler)  //1st async op
void ClassB.asyncOp2(Handler<AsyncResult<Class2>> handler)  //2nd async op
Class2A Class2.toClass2A()  //sync op; final result required in Class2A, not Class2
```
We want:
```java
Future<Class2A> Adapter.asClass2A() //execute asyncOp1, then asyncOp2 and convert Class2 to Class2A
```
Solution:
```java
public class Adapter {
	private Future<Class1> asynOp1() {
		Promise<Class1> promise = Promise.promise();
		ClassA.asyncOp1(promise);
		return promise.future();
	}
	private Function<Class1, Future<Class2>> asyncOp2() {
		Function<Class1, Future<Class2>> asyncOp2 = (Class1 c1) -> {
			Promise<Class2> promise = Promise.promise();
			ClassA.asyncOp1(promise);
			return promise.future();
		};

		return asyncOp2;
	}
	private Function<Class2, Future<Class2A>> toClass2A() {
		Function<Class1, Future<Class2>> asyncOp2 = (Class2 c2) -> {
			Promise<Class2A> promise = Promise.promise();
			Class2A c2a = c2.toClass2A();
			return promise.future(c2a);
		};

		return toClass2A;
	}
	public Future<Class2A> asClass2A() {
		Future<Class2A> job = this
			.asyncOp1()
			.compose(this.asyncOp2()::apply).
			.compose(this.toClass2A()::apply);
		return job;
	}
}
```
#### 5.1.1. DbQueryAdapter

Given:
```java
SQLClient client = JDBCClient.create(Vertx v, JsonObject config)
SQLClient.getConnection(Handler<AsyncResult<SQLConnection>> handler)
SQLConnection.query(String sql, Handler<AsyncResult<ResultSet>> resultHandler)
List<JsonObject> list = ResultSet.getRows()
```
We Want:
```java
Future<List<JsonObject>> rows = DbQueryAdapter.create(Vertx v)...query(String sql);
```
Solution:
```java
public class DbQueryAdapter {
  private Future<SQLClient> client() { ... }
  private Function<SQLClient, Future<SQLConnection>> connection() { ... }
  private Function<SQLConnection, Future<ResultSet>> resultSet() { ... }
  private Function<ResultSet, Future<List<JsonObject>>> rows() { ... }
  public Future<List<JsonObject>> query(String sql) {
    ...
    Future<List<JsonObject>> query = this
        .client()
        .compose(this.connection()::apply)
        .compose(this.resultSet()::apply)
        .compose(this.rows()::apply);
    return query;
  }
}
```
### 5.2. Future<T>
Given:
```java
Future<Class1> AdapterA.opA()
Future<Class2> AdapterB.opB()
```
We want:
```java
Future<Class2> AdapterC.opC()
```
Solution:
```java
public class AdapterC {
	private Future<Class1> opA() {
		return AdapterA.opA();
	}
	private Function<Class1, Future<Class2>> opB() {
		Function<Class1, Future<Class2>> asClass2 = (Class1 c1) -> {
			return AdapterB.opB();
		};
		return asClass2;
	}
	public Future<Class2> opC() {
		Future<Class2> opC = this
			.opA()
			.compose(this.opB()::apply);
		return opC;
	}
}
```

## Design Principles
1. Design principles should be consistently followed, even at the expense of making code verbose.  This is because consistency makes code more readable and maintainable.
2. Design principles are formulated such that, a Java programmer, fresh out of college, can understand them.
3. `Adapters` are the primary building blocks of the application.  `Adapters` are built using Vertx API and provide the means to implement business functionality.  `Adapters` implement the `method chaining`/`Fluent API` design pattern. Adapter methods fall under three categories: `bean`, `future`, `api`, and `business`.
4. `Adapters` implement a private construtor and also provide a `create()` method to enable `method chaining`.  Ideally, `create(Vertx v)`.  See, `FileAdapter.create()`.
5. `Adapters` store configuration in a the `config` attribute of type, `JsonObject`.  See, `FileAdapter.config`.
6. `Adapters` are used only once.  That is because the `Handlers`,  `failure` and `success`, and `JsonObject`, `config`, should not be simultaneously set/executed from multiple threads/contexts.  The attribute, `fired` ensures single-use.  See, `FileAdapter.read()`.
7. `Adapters` should implement the `close()` method.  If there are no resources to release, this method does nothing.  See, `FileAdapter.close()`.
8. `business` methods provide some functional value to the end-user.  They always return `Future<T>`.  See, `RedisAdapter.set()`.
9. `Api` (category) methods make use of Vertx API.
10. Synchronous `api` methods complete `Promise<T>`, to return `Future<T>`.  See, `FileAdapter.fileSystem()`.
11. Asynchronous `api` methods, that require `Handler<AsyncResult<T>>`, take `Promise<T>` to return `Future<T>`.  See. `FileAdapter.readFile()`.
12. Callback `api` methods, that require `Handler<T>`, make use of `Consumer<T>`, to complete `Promise<T>`, that returns `Future<T>`.  See, `HttpGetAdapter.response()`.
13. `Api` (category) methods either return `Function<T, Future<U>>`, `Consumer<T>`, or `Future<T>`.
13. Multiple `Adapters`, all returning `Future<T>`, are sequentially composed to provide business functionality.  See, `HttpGetAdapter.get()`.
14. `Adapters` implement the following `Future` (category) methods: `onComplete()`, `onSuccess()`, `onFailure()`.
15.  Resources are closed by calling `close()` in `onComplete()`.  See, `DbQueryAdapter.onComplete()`.
16.  Exceptions are handled (or not) in `onFailure()`.  Note that a Future can have multiple `onFailure()` exception handlers.  Each handler will then be individually called in the event of an exception.
17. `Adapters` should not catch exceptions.  Only `Future.onFailure(Hander<Throwable> h)` should catch and handle (if required) exceptions.
18.  Exceptions, ideally, should only be handled by `AbstractVerticle`.


## Note(s)

```bash
mvn archetype:generate -DgroupId=sample -DartifactId=sample -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false

git init
git remote add origin https://git.enstage-sas.com/accosa2/acs2-devops/acs2-cloud.git
git pull origin develop
git branch
git status
```
```
# install ansible
# see https://www.digitalocean.com/community/tutorials/how-to-install-and-configure-ansible-on-ubuntu-20-04





Nginx
Nginx
https://docs.nginx.com/nginx/admin-guide/web-server/web-server/
file, tutorial, under /etc/nginx/sites-enabled
context > block > directive
server {
       listen 81;
       server_name example.ubuntu.com;
       root /var/www/tutorial;
       index index.html;
       location / {
               try_files $uri $uri/ =404;
       }
}
SSH Port Forwarding
laptop:80 > AWS:80
laptop:8001 < AWS:8001
laptop:8002 < AWS:8002
Reverse Proxy
localhost:80/1 > localhost:8001
localhost:80/2 > localhost:8002

laptop         AWS
               nginx
service1:8001
service2:8002

localhost:80/1
              nginx:80
service1:8001
localhost:80/2
              nginx:80
service2:8002
Envoy

```
