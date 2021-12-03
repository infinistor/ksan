FROM nginx:1.19.7
ARG source=.
WORKDIR /
EXPOSE 443 80
ADD ["pspace.crt", "/etc/ssl/certs/"]
ADD ["pspace.key", "/etc/ssl/certs/"]
ADD ["nginx.conf", "/etc/nginx/"]
ADD ["proxy.conf", "/etc/nginx/"]
CMD ["nginx", "-g", "daemon off;"]
