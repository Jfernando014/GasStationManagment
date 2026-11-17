FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration=production

FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist/frontend-tmp/browser /usr/share/nginx/html

COPY config.template.json /etc/nginx/templates/config.template.json
COPY docker-entrypoint.d/40-generate-config.sh /docker-entrypoint.d/40-generate-config.sh
RUN chmod +x /docker-entrypoint.d/40-generate-config.sh

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]