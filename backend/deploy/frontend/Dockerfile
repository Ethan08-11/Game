FROM node:20-alpine AS build
WORKDIR /app
COPY Game_Frontend/package*.json ./
RUN npm install
COPY Game_Frontend/ ./
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY deploy/frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
