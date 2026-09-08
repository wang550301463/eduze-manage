FROM nginx:1.27-alpine
COPY web/dist/ /usr/share/nginx/html/
COPY docker/platform/web.conf /etc/nginx/conf.d/default.conf
