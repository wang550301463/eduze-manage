# TLS 证书

生产部署前在此目录放置：

- `server.crt`
- `server.key`

## 本机 Docker 验证

访问 `https://localhost:18443/login`。`18443` 是 HTTPS 端口，不能使用 `http://`。

本地证书必须包含 Subject Alternative Name（SAN）；仅设置 `CN=localhost`
无法满足现代浏览器的主机名校验。以下命令从仓库根目录执行；续期前先备份现有证书与私钥。

```bash
openssl req -x509 -nodes -sha256 -days 365 -newkey rsa:2048 \
  -keyout docker/certs/server.key -out docker/certs/server.crt \
  -subj "/CN=localhost/O=EduZE Local Development" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1,IP:::1" \
  -addext "basicConstraints=critical,CA:FALSE" \
  -addext "keyUsage=critical,digitalSignature,keyEncipherment" \
  -addext "extendedKeyUsage=serverAuth"
chmod 600 docker/certs/server.key

# 仅在本机 macOS 用户钥匙串中信任这张本地服务器证书的 SSL 用途。
# macOS 可能要求用户亲自授权。不要添加 -s localhost：Chromium 会忽略这种信任设置。
security add-trusted-cert -r trustRoot -p ssl \
  -k "$HOME/Library/Keychains/login.keychain-db" docker/certs/server.crt

docker exec eduze-nginx nginx -t
docker exec eduze-nginx nginx -s reload

# 通过 macOS 原生信任校验，不跳过 TLS 校验。
CURL_SSL_BACKEND=secure-transport /usr/bin/curl -I https://localhost:18443/login
```

浏览器已有证书错误页面时重新加载。证书和私钥已被 Git 忽略；本地信任不自动传播到其他电脑。
公网部署仍须使用正式证书。

撤销本机信任（更换证书前需指向要撤销的旧证书）：

```bash
security remove-trusted-cert docker/certs/server.crt
```
