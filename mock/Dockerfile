FROM node:22-alpine

WORKDIR /app
RUN npm install -g @mockoon/cli@9.3.0

EXPOSE 3000

# `mockoon-config.json`은 `docker-compose.yaml`에서 볼륨으로 넣어줍니다.
CMD ["mockoon-cli", "start", "--data", "mockoon-config.json", "--watch"]
