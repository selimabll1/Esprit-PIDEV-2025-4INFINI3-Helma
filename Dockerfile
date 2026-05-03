FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./

RUN npm install --legacy-peer-deps

COPY . .

RUN npm run build -- --configuration production

RUN mkdir -p /app/frontend-dist && \
    if [ -d /app/dist/helma-frontend/browser ]; then \
      cp -r /app/dist/helma-frontend/browser/* /app/frontend-dist/; \
    elif [ -d /app/dist/helma-frontend ]; then \
      cp -r /app/dist/helma-frontend/* /app/frontend-dist/; \
    else \
      echo "Could not find Angular dist folder" && ls -R /app/dist && exit 1; \
    fi

FROM nginx:alpine

COPY --from=build /app/frontend-dist /usr/share/nginx/html

EXPOSE 80