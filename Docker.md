# Docker
```bash
# batch.sh will hold a bunch of commands to run in one go.
touch ~/batch.sh && chmod +x ~/batch.sh
```
## 1. Docker
```bash
# install docker
sudo tee ~/batch.sh<<EOF
#!/bin/bash
set -e
set -x
sudo apt install -y curl gnupg software-properties-common apt-transport-https ca-certificates
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
sudo apt update
sudo apt install -y containerd.io docker-ce docker-ce-cli
sudo usermod -a -G docker $(whoami)
EOF
~/batch.sh

# configure docker
sudo tee ~/batch.sh<<EOF
#!/bin/bash
set -e
set -x
sudo tee /etc/docker/daemon.json <<EOFF
{
  "exec-opts": ["native.cgroupdriver=systemd"],
  "log-driver": "local",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  },
  "storage-driver": "overlay2"
}
EOFF
sudo systemctl daemon-reload
sudo systemctl restart docker
sudo systemctl enable docker
EOF
~/batch.sh
```

## 2. MySQL
```bash
sudo tee ~/batch.sh<<EOF
#!/bin/bash
set -e
set -x
docker pull mysql:8.0.30
docker tag mysql:8.0.30 mysql:latest
mkdir -p ~/docker/mysql/data
docker  run \
        -d \
        -e MYSQL_ROOT_PASSWORD=hello \
        -e MYSQL_DATABASE=sample \
        -e MYSQL_USER=ubuntu \
        -e MYSQL_PASSWORD=hello \
        --name mysql-server \
        --network host \
        --rm \
        --user $(id -u):$(id -g) \
        -v ~/docker/mysql/data:/var/lib/mysql \
        mysql:latest
EOF
~/batch.sh
```
```bash
# run mysql-cli
docker  run \
        -it \
        --name mysql-cli \
        --network host \
         --rm \
         mysql:latest mysql -h127.0.0.1 -uubuntu -phello
```


## 3. Redis
```bash
sudo tee ~/batch.sh<<EOF
#!/bin/bash
set -e
set -x
docker pull redis:7.0.4
docker tag redis:7.0.4 redis:latest
docker  run \
        -d \
        --name redis \
        --rm \
        redis:latest
EOF
~/batch.sh
```
```bash
docker  run \
        -it \
        --name redis-cli \
        --network host \
        --rm \
        redis:latest redis-cli -h 127.0.0.1
```

## 4. Nginx
```bash
sudo tee ~/batch.sh<<EOF
#!/bin/bash
set -e
set -x
docker pull nginx:1.23.1
docker tag nginx:1.23.1 nginx:latest
mkdir -p ~/docker/nginx/www
echo '<html><head><title>index</title></head><body>Hello World!</body></html>' > ~/docker/nginx/www/index.html
docker  run \\
        -d \\
        --name nginx \\
        --network host \\
        --rm \\
        -v ~/docker/nginx/www:/usr/share/nginx/html:ro \\
        nginx:latest
EOF
~/batch.sh
```
```bash
mkdir -p ~/docker/nginx/config
tee ~/docker/nginx/config/default.conf<<EOF
user  nginx;
worker_processes  auto;
error_log  /var/log/nginx/error.log notice;
pid        /var/run/nginx.pid;
events {
    worker_connections  1024;
}
http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    log_format  main  '\$remote_addr - \$remote_user [\$time_local] "\$request" '
                      '\$status \$body_bytes_sent "\$http_referer" '
                      '"\$http_user_agent" "\$http_x_forwarded_for"';
    access_log  /var/log/nginx/access.log  main;
    sendfile        on;
    keepalive_timeout  65;
    server {
        listen       80;
        server_name  localhost;
        location / {
            root   /usr/share/nginx/html;
            index  index.html index.htm;
        }
        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   /usr/share/nginx/html;
        }
    }
}
EOF
tee ~/docker/nginx/config/simple.conf<<EOF
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
tee ~/docker/nginx/run.sh<<EOF
docker  run \\
        -d \\
        --name nginx \\
        --network host \\
        --rm \\
        -v ~/docker/nginx/www:/usr/share/nginx/html:ro \\
        -v ~/docker/nginx/config/default.conf:/etc/nginx/nginx.conf:ro \\
        nginx:latest
EOF
```

## 5. Envoy
```bash
sudo tee ~/batch.sh<<EOF
#!/bin/bash
set -e
set -x
docker pull envoyproxy/envoy:v1.23.1
docker tag envoyproxy/envoy:v1.23.1 envoy:latest
mkdir -p ~/docker/envoy/config
sudo tee ~/docker/envoy/config/envoy.yaml <<EOFF
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
EOFF
docker  run \
        -d \
        --name envoy \
        --network host \
        --rm \
        -v ~/docker/envoy/config/envoy.yaml:/etc/envoy/envoy.yaml:ro \
        envoy:latest
EOF
~/batch.sh
```

## 6. Ubuntu

## 7. Fluentd

## 8. cAdvisor

## 9. Prometheus

## 10. Loki

## 11. Grafana

## 12. Couchbase

See:
- [Multi-node, Single Host Cluster](https://docs.couchbase.com/server/current/install/getting-started-docker.html#multi-node-cluster-one-host)
- [Server Certificates](https://docs.couchbase.com/server/current/manage/manage-security/manage-security-settings.html#root-certificate-security-screen-display)
- [Configure Sever Certificates](https://docs.couchbase.com/server/current/manage/manage-security/configure-server-certificates.html)
- [On-the-Wire Security](https://docs.couchbase.com/server/current/manage/manage-security/manage-tls.html)
```bash
# Install couchbase
docker pull couchbase/server:enterprise-6.6.2
docker pull couchbase/server:enterprise-7.1.1
docker tag couchbase/server:enterprise-6.6.2 couchbase:latest
# Start cluster nodes
docker run -d --network host --name db1 --rm -v ~/docker/couchbase:/home/couchbase couchbase/server:enterprise-6.6.2
docker inspect --format '{{ .NetworkSettings.IPAddress }}' db1
docker run -d --name db2 --rm -v ~/docker/couchbase:/home/couchbase couchbase/server:enterprise-6.6.2
docker inspect --format '{{ .NetworkSettings.IPAddress }}' db2
docker run -d --name db3 --rm -v ~/docker/couchbase:/home/couchbase couchbase/server:enterprise-6.6.2
docker inspect --format '{{ .NetworkSettings.IPAddress }}' db3
# See, Multi-node, Single Host Cluster
services:
  db1:
    container_name: db1
    image: couchbase/server:enterprise-6.6.2
    volumes:
      - /home/ubuntu/docker/couchbase:/home/couchbase
    network_mode: "host"
  db2:
    container_name: db2
    depends_on:
      - db1
    image: couchbase/server:enterprise-6.6.2
    volumes:
      - /home/ubuntu/docker/couchbase:/home/couchbase
  db3:
    container_name: db3
    depends_on:
      - db1
      - db2
    image: couchbase/server:enterprise-6.6.2
    volumes:
      - /home/ubuntu/docker/couchbase:/home/couchbase

# Couchbase Root CA
mkdir -p ~/docker/couchbase
cd ~/docker/couchbase
mkdir -p db1 db2 db3
openssl genrsa -out ca.key 2048
openssl req -new -x509 -days 3650 -sha256 -key ca.key -out ca.pem -subj "/CN=Couchbase Root CA"
openssl x509 -subject -issuer -startdate -enddate -noout -in ca.pem
openssl rsa -check -noout -in ca.key
openssl rsa -modulus -noout -in ca.key | openssl md5
openssl x509 -modulus -noout -in ca.pem | openssl md5
# db1 Certificate; see, Server Certificates
cd ~/docker/couchbase
cat > ./db1/cert.ext <<EOF
basicConstraints=CA:FALSE
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid,issuer:always
extendedKeyUsage=serverAuth
keyUsage=digitalSignature,keyEncipherment
subjectAltName=IP:172.17.0.1
EOF
openssl genrsa -out ./db1/pkey.key 2048
openssl req -new -key ./db1/pkey.key -out ./db1/db1.csr -subj "/CN=Couchbase DB1"
openssl x509 -CA ca.pem -CAkey ca.key -CAcreateserial -days 365 -req \
-in ./db1/db1.csr \
-out ./db1/chain.pem \
-extfile ./db1/cert.ext
openssl x509 -subject -issuer -startdate -enddate -noout -in ./db1/chain.pem
openssl rsa -check -noout -in ./db1/pkey.key
openssl rsa -modulus -noout -in ./db1/pkey.key | openssl md5
openssl x509 -modulus -noout -in ./db1/chain.pem | openssl md5
# db1 SSL Installation
docker exec -it db1 /bin/bash
mkdir -p /opt/couchbase/var/lib/couchbase/inbox
cp /home/couchbase/db1/chain.pem /opt/couchbase/var/lib/couchbase/inbox/chain.pem
cp /home/couchbase/db1/pkey.key /opt/couchbase/var/lib/couchbase/inbox/pkey.key
chown -R couchbase:couchbase /opt/couchbase/var/lib/couchbase/inbox
chmod +x /opt/couchbase/var/lib/couchbase/inbox
chmod 0600 /opt/couchbase/var/lib/couchbase/inbox/*
find /opt/couchbase/var/lib/couchbase/inbox -type f | xargs ls -l
couchbase-cli ssl-manage -c 127.0.0.1 -u Administrator -p password --upload-cluster-ca /home/couchbase/ca.pem
couchbase-cli ssl-manage -c 127.0.0.1 -u Administrator -p password --cluster-cert-info
couchbase-cli ssl-manage -c 127.0.0.1 -u Administrator -p password --set-node-certificate

# db2 Certificate
# db3 Certificate

# Node-to-Node Encryption
couchbase-cli setting-autofailover \
-c http://127.0.0.1:8091 \
-u Administrator \
-p password \
--enable-auto-failover 0
couchbase-cli eventing-function-setup \
-c http://127.0.0.1:8091 \
-u Administrator \
-p password \
--list
couchbase-cli eventing-function-setup \
-c http://127.0.0.1:8091 \
-u Administrator \
-p password \
--pause \
--name ???
couchbase-cli node-to-node-encryption \
-c couchbase://localhost \
-u Administrator \
-p password \
--enable
couchbase-cli setting-security \
-c http://127.0.0.1:8091 \
-u Administrator \
-p password \
--set \
--cluster-encryption-level strict
couchbase-cli eventing-function-setup \
-c http://127.0.0.1:8091 \
-u Administrator \
-p password \
--resume \
--name ???
couchbase-cli setting-autofailover \
-c 127.0.0.1:8091 \
-u Administrator \
-p password \
--enable-auto-failover 1 \
--auto-failover-timeout 120 \
--enable-failover-of-server-groups 0 \
--max-failovers 1 \
--can-abort-rebalance 1
couchbase-cli node-to-node-encryption \
-c http://127.0.0.1:8091 \
-u Administrator \
-p password \
--get
couchbase-cli setting-security \
-c 127.0.0.1:8091 \
-u Administrator \
-p password \
--set \
--disable-http-ui 1 \
--tls-min-version tlsv1.2 \
--hsts-max-age 43200 \
--hsts-preload-enabled 1 \
--hsts-include-sub-domains-enabled 1
couchbase-cli setting-security \
-c 127.0.0.1:8091 \
-u Administrator \
-p password \
--get
```
## 13. SonarQube
See
- [SonarQube Documentation](https://docs.sonarqube.org/latest/)
- [Disable Rules](https://sqa.stackexchange.com/questions/24734/how-to-deactivate-a-rule-in-sonarqube)
```bash
docker pull sonarqube:developer
docker tag sonarqube:developer sonarqube:latest
mkdir -p ~/docker/sonarqube/config ~/docker/sonarqube/data ~/docker/sonarqube/logs ~/docker/sonarqube/extensions

tee ~/docker/sonarqube/config/config.sh <<EOF
#!/bin/bash
set -e
set -x
sudo sysctl -w vm.max_map_count=524288
sudo sysctl -w fs.file-max=131072
ulimit -n 131072
ulimit -u 8192
EOF
chmod +x ~/docker/sonarqube/config/config.sh

tee ~/docker/sonarqube/run.sh<<EOF
#!/bin/bash
set -e
set -x
docker run \\
-d \\
-e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \\
--name sonarqube \\
--publish 9000:9000 \\
--rm \\
--stop-timeout 3600 \\
-v ~/docker/sonarqube/data:/opt/sonarqube/data \\
-v ~/docker/sonarqube/logs:/opt/sonarqube/logs \\
-v ~/docker/sonarqube/extensions:/opt/sonarqube/extensions \\
--user $(id -u):$(id -g) \\
sonarqube:latest
EOF
chmod +x ~/docker/sonarqube/run.sh

# UI
Analyze "TridentCSR": sqp_3d262afd0b532120248e38f0de020c52b182db42

squ_5ee74580b7c2631c7e0363a851bfccf839b546a8

plugins {
  id "org.sonarqube" version "3.4.0.2513"
}

C:\Users\sidharth.sankar\AppData\Local\gradle-5.4.1\bin\gradle.bat sonarqube `
  -Dsonar.projectKey=TridentCSR `
  -Dsonar.host.url=http://localhost:9000 `
  -Dsonar.login=squ_5ee74580b7c2631c7e0363a851bfccf839b546a8 `
  -Dsonar.login=sqp_3d262afd0b532120248e38f0de020c52b182db42
```
## 14. Portainer
See
- [Portainer Deployment](https://docs.portainer.io/v/ce-2.9/start/install/server/docker/linux)

```bash
docker pull portainer/portainer-ce
docker tag portainer/portainer-ce portainer:latest
mkdir -p ~/docker/portainer/data

tee ~/docker/portainer/run.sh <<EOF
#!/bin/bash
docker run \\
  --detach \\
  --name portainer \\
  --publish 9001:9000 \\
  --rm \\
  --volume /var/run/docker.sock:/var/run/docker.sock \\
  --volume portainer_data:/data \\
  portainer:latest
EOF
chmod +x ~/docker/portainer/run.sh
# password: A8(k*2S*muxM
```