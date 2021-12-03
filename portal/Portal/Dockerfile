# Stage 1
FROM node:14.15.4-stretch-slim as build-step
RUN mkdir -p /app
WORKDIR /app
COPY . /app
RUN npm install
RUN npm run build --prod

# Stage 2
FROM nginx:1.19.3
COPY --from=build-step /app/pspace.crt /etc/ssl/certs/
COPY --from=build-step /app/pspace.key /etc/ssl/certs/
COPY --from=build-step /app/default.conf /etc/nginx/conf.d/
COPY --from=build-step /app/dist/Portal /usr/share/nginx/html
