FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration=production

FROM nginx:alpine

ARG FRONTEND_PROD_PORT=80
ENV PORT=$FRONTEND_PROD_PORT

COPY nginx.conf /etc/nginx/conf.d/default.conf

RUN sed -i "s/listen 80;/listen ${PORT};/g" /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist/frontend-tmp/browser /usr/share/nginx/html

EXPOSE $PORT

CMD ["nginx", "-g", "daemon off;"]