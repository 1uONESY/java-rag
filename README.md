# Java RAG 知识库问答系统

一个基于 **Spring Boot 3 + Java 21** 手写实现的 RAG（检索增强生成）知识库问答系统。上传文档后，大模型能基于文档内容回答，并标注引用来源，而不是凭空编造。

## ✨ 功能特性

- 📄 **文档摄入**：自动读取文档 → 切块 → 向量化 → 存入向量库
- 🔍 **语义检索**：手写余弦相似度，从向量库中检索最相关的 Top-K 块
- 💬 **RAG 生成**：检索结果拼入 Prompt，让大模型基于资料回答并标注来源
- 🚫 **防幻觉**：资料里没有的内容会明确回答「不知道」，不瞎编
- 🖥️ **前端页面**：简洁的问答界面，展示回答与引用来源

## 🛠️ 技术栈

| 模块 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.3 + Java 21 |
| 大模型（生成） | DeepSeek API（OpenAI 兼容接口） |
| 向量化（Embedding） | 智谱 AI `embedding-2`（JWT 签名认证） |
| 向量存储 | 内存版 `VectorStore`（手写，可替换 Redis/pgvector/Milvus） |
| 文档切块 | 手写 `DocumentChunker`（段落切分 + 滑动窗口重叠） |
| HTTP 调用 | JDK 自带 `HttpClient`（零第三方依赖） |

## 🔄 RAG 核心流程

```
文档 → 切块(chunk) → 向量化(embedding) → 存入向量库
                                              ↓
问题 → 向量化 → 余弦相似度检索 Top-K → 拼 Prompt → 大模型生成(带引用)
```

本项目**手写**了切块、余弦相似度、向量检索等核心逻辑（而非直接调用 LangChain），便于深入理解 RAG 原理。

## 🚀 快速开始

### 1. 环境要求

- JDK 21+
- Maven 3.9+
- 两个 API Key：`DEEPSEEK_API_KEY`（生成）、`ZHIPU_API_KEY`（向量化）

### 2. 配置密钥（环境变量）

```bash
# Windows CMD
set DEEPSEEK_API_KEY=你的DeepSeekKey
set ZHIPU_API_KEY=你的智谱Key

# Linux / macOS
export DEEPSEEK_API_KEY=你的DeepSeekKey
export ZHIPU_API_KEY=你的智谱Key
```

### 3. 运行

```bash
mvn spring-boot:run
# 或先打包再运行
mvn -DskipTests package
java -jar target/java-rag-0.0.1-SNAPSHOT.jar
```

启动时会自动加载 `docs/员工手册.txt` 构建知识库。浏览器打开 <http://localhost:8080> 即可提问。

## 📡 API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/health` | 健康检查 |
| POST | `/api/ask` | RAG 问答，`{"message": "年假有几天?"}` |
| POST | `/api/chat` | 直接对话（不带知识库，用于对比） |
| POST | `/api/ingest` | 手动摄入文档，`{"text": "..."}` |

## 📁 项目结构

```
src/main/java/com/example/rag/
├── RagApplication.java          # 启动类
├── controller/ChatController.java  # REST 接口
└── service/
    ├── LlmService.java          # 大模型调用(DeepSeek)
    ├── EmbeddingService.java    # 向量化(智谱,可切换提供商)
    ├── ZhipuAuth.java           # 智谱 JWT 签名
    ├── DocumentChunker.java     # 文档切块
    ├── VectorStore.java         # 向量库 + 余弦相似度检索
    ├── RagService.java          # RAG 编排
    ├── DataLoader.java          # 启动时加载文档
    └── Chunk.java               # 切块实体
docs/员工手册.txt                 # 示例知识库文档
```

## 📝 说明

- 教学项目，核心逻辑手写，适合学习 RAG 原理、面试讲解。
- 向量库为内存实现，重启即清空；进阶可替换为 Redis（RediSearch）、pgvector 或 Milvus。
- Embedding 提供商可切换：修改 `application.yml` 中的 `embedding.provider`（支持 `zhipu` / `openai`）。
