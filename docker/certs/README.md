# TLS 证书

生产部署前在此目录放置：

- `server.crt`
- `server.key`

开发自签示例：

```bash
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout server.key -out server.crt \
  -subj "/CN=localhost"
```
