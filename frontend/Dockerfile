# Multi-stage build: compile the static bundle with Node, serve it with
# nginx — no Node runtime ships in the final image.

FROM node:20-alpine AS build
WORKDIR /build

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Vite bakes VITE_-prefixed variables into the bundle at build time (they
# don't exist at container-start time like a server-side app's env vars
# would) — passed as a build arg so docker-compose's .env value reaches it.
ARG VITE_API_BASE_URL=http://localhost:8080/api/v1
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=5s --start-period=10s --retries=5 \
  CMD wget -qO- http://localhost/healthz || exit 1
