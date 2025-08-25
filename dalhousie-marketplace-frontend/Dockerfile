FROM node:18-alpine

WORKDIR /app

COPY package.json .

RUN npm install

COPY . .

# Allow setting dynamic environment variables
ARG PORT=3000
ARG REACT_APP_BACKEND_BASE_URL

ENV PORT=$PORT
ENV REACT_APP_BACKEND_BASE_URL=$REACT_APP_BACKEND_BASE_URL

EXPOSE $PORT

RUN npm run build

# Ensure the app uses the dynamically assigned PORT and BACKEND_BASE_URL
CMD ["sh", "-c", "PORT=$PORT REACT_APP_BACKEND_BASE_URL=$REACT_APP_BACKEND_BASE_URL npm start"]
