# M5：容器化、Compose 接入与回滚开关

本阶段目标：

- 提供 Spring Boot 后端的 Dockerfile（多阶段构建）
- 保持前端代理目标不变（仍为 `fabric-realty.server:8888`）
- 通过 compose 覆盖文件实现“Spring Boot / Go”一键切换（回滚策略）

## 1. Dockerfile

- `Dockerfile`：Maven 构建 -> JRE 运行
- 工作目录：`/app`
- 端口：`8888`
- 内置 Docker 配置：`config/application.yml` -> `/app/config/application.yml`

## 2. Compose 接入（不改原 compose）

原 compose（Go 镜像）仍在：

- `application/docker-compose.yml`

Spring Boot 覆盖文件：

- `docker-compose.springboot.override.yml`

启动 Spring Boot 后端：

```bash
docker compose -f application/docker-compose.yml -f application/server/springbootserver/docker-compose.springboot.override.yml up -d --build
```

## 3. 回滚策略（一键切回 Go）

提供脚本：

- `switch-backend.sh`

使用：

```bash
# 切到 Spring Boot（build + up）
bash application/server/springbootserver/switch-backend.sh springboot

# 切回 Go（使用 application/docker-compose.yml 的镜像）
bash application/server/springbootserver/switch-backend.sh go
```

## 4. 数据与配置

- 区块数据库（H2 file）：`${app.dataDir}/blocks/blocks`，容器中默认为 `/app/data/blocks/blocks`
- Fabric 证书/私钥路径：Docker 配置已对齐 Go 的 `config-docker.yaml`（使用 `/network/crypto-config`）

