# 🚀 tBits AI RAG System — Complete Server Architecture Guide

## 🌍 Live Application

**URL:** [http://annotate.mytbits.com:8080](http://annotate.mytbits.com:8080)

---

## 🧠 Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Backend Framework | Spring Boot | 4.0.6 |
| Programming Language | Java | 17 |
| Build Tool | Maven | 3.x |
| AI Framework | LangChain4j | 0.31.0 |
| Vector Database | Milvus | 2.4.15 |
| LLM Provider | OpenAI | GPT-4o |
| Embedding Model | OpenAI text-embedding-3-large | 3072 dimensions |
| Deployment OS | Ubuntu | 24.x |
| Process Manager | systemd | Linux Service |
| Container Runtime | Docker | Latest |
| Web Server | Embedded Tomcat | Spring Boot |

---

## 📁 Project Structure (Local)

```
tbits-ai/
│
├── src/main/java/com/tbits/ai
│   ├── controller/
│   │   ├── ChatController.java
│   │   └── HealthController.java
│   ├── service/
│   │   └── RAGService.java
│   └── TBitsAiApplication.java
│
├── src/main/resources/
│   ├── application.properties
│   └── static/
│       ├── index.html
│       ├── style.css
│       ├── script.js
│       └── images/
│           └── logo.png
│
├── pom.xml
└── target/
    └── tbits-ai-0.0.1-SNAPSHOT.jar
```

---

## ☁️ Server Structure (Ubuntu)

```
/root/
├── tbits-ai-0.0.1-SNAPSHOT.jar
├── app.log
└── docker-compose.yml
```

---

## ⚙️ systemd Service File

**Location:** `/etc/systemd/system/tbits-ai.service`

**Purpose:**
- Auto start on boot
- Auto restart on failure
- Permanent deployment

---

## 🔑 OpenAI API Key Location

Configured inside the systemd service file:

```ini
Environment="OPENAI_API_KEY=sk-proj-xxxxx"
```

---

## 🐳 Milvus Docker Containers

| Container | Purpose |
|-----------|---------|
| `milvus-standalone` | Main vector database |
| `milvus-etcd` | Metadata storage |
| `milvus-minio` | Object storage |

---

## 🔌 Ports Used

| Port | Purpose |
|------|---------|
| 8080 | Spring Boot Application |
| 19530 | Milvus API |
| 9091 | Milvus Health |
| 9000 | MinIO |
| 4848 | Old GlassFish Admin |

---

## 🧠 How the System Works

```
User Question
      ↓
Spring Boot API
      ↓
RAGService
      ↓
OpenAI Embedding
      ↓
Milvus Vector Search
      ↓
Relevant Chunks Retrieved
      ↓
GPT-4o Generates Final Answer
      ↓
Response to User
```

---

## 📄 Indexing Flow

```
Standalone Java Indexer
        ↓
Document Chunking
        ↓
OpenAI Embeddings
        ↓
Milvus Storage
```

> The chatbot uses the same Milvus collection as the indexer.

---

## 📦 Important Dependencies (pom.xml)

| Dependency | Version |
|------------|---------|
| spring-boot-starter-web | 4.0.6 |
| langchain4j | 0.31.0 |
| langchain4j-open-ai | 0.31.0 |
| langchain4j-milvus | 0.31.0 |
| langchain4j-document-parser-apache-pdfbox | 0.31.0 |
| slf4j-simple | 2.0.13 |

---

## 🖥️ Server Commands

### Application Control

| Command | Description |
|---------|-------------|
| `systemctl start tbits-ai` | Start the application |
| `systemctl stop tbits-ai` | Stop the application |
| `systemctl restart tbits-ai` | Restart the application |
| `systemctl status tbits-ai` | Check application status |
| `journalctl -u tbits-ai -f` | View live logs |

### Process & Port Inspection

```bash
# Check Java processes
ps -ef | grep java

# Check port 8080
sudo ss -tulpn | grep 8080
```

### Milvus Management

```bash
# Check running containers
docker ps | grep milvus

# Start Milvus (in order)
docker start milvus-etcd
docker start milvus-minio
docker start milvus-standalone

# View Milvus logs
docker logs -f milvus-standalone

# Check Milvus port
sudo ss -tulpn | grep 19530
```

### Build & Deploy

```bash
# Check uploaded JAR
ls -lh /root/*.jar

# Build project
mvn clean package -DskipTests

# Run JAR manually
java -jar tbits-ai-0.0.1-SNAPSHOT.jar

# Firewall
sudo ufw allow 8080/tcp
sudo ufw reload
```

---

## 🧠 Embedding Configuration

| Setting | Value |
|---------|-------|
| Embedding Model | `text-embedding-3-large` |
| Dimensions | 3072 |
| Collection Name | `openai_data_collection` |

---

## 📂 Static Files Location

| File | Location |
|------|----------|
| index.html | `static/index.html` |
| CSS | `static/style.css` |
| JS | `static/script.js` |
| Logo | `static/images/logo.png` |

---

## 🔄 Deployment Flow

```
Code Changes
     ↓
mvn clean package
     ↓
Generate JAR
     ↓
Upload JAR via WinSCP
     ↓
Restart systemd service
     ↓
Application Live ✅
```

---

## 🧩 Current Architecture

```
Frontend UI
     ↓
Spring Boot REST API
     ↓
RAG Service
     ↓
OpenAI + Milvus
     ↓
Enterprise AI Response
```

---

*tBits AI RAG System — Built with Spring Boot, LangChain4j, Milvus & OpenAI GPT-4o*
