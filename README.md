# Substacker APIs

This repository hosts two implementations of the **Substacker API**—one written in **Python** and another in **Java**. Both services parse public Substack RSS feeds to retrieve publication information and newsletter posts in structured JSON format, stripping HTML to provide clean plain text.

Example: [newniyas substack posts](https://substacker-umber.vercel.app/substack/newniyas)

<img width="1501" height="428" alt="Screenshot 2026-04-26 at 6 44 03 PM" src="https://github.com/user-attachments/assets/72c8d128-0946-457b-8c89-6665fb9560ee" />

---

## Directory Structure

```
├── substacker_python/      # Python microservice (FastAPI + Vercel)
│   ├── app.py              # FastAPI application
│   ├── mcp_server.py       # Python FastMCP server
│   ├── requirements.txt    # Python dependencies
│   └── vercel.json         # Vercel deployment configuration
│
└── substacker_java/        # Java microservice (Spring Boot + Render)
    ├── src/                # Spring Boot source code
    ├── McpServer.java      # Java MCP server (STDIO)
    ├── pom.xml             # Maven dependencies configuration
    └── Dockerfile          # Multi-stage Docker configuration for Render
```

---

## 1. Python Service (`substacker_python`)

A lightweight API built with **FastAPI** and **feedparser**.

### Local Setup

1. **Navigate to directory & create environment:**
   ```bash
   cd substacker_python
   python -m venv venv
   source venv/bin/activate
   pip install -r requirements.txt
   ```

2. **Start the API:**
   ```bash
   uvicorn app:app --reload
   ```
   The API will be available at `http://127.0.0.1:8000`.

3. **Start the Python MCP Server:**
   ```bash
   python mcp_server.py
   ```

---

## 2. Java Service (`substacker_java`)

A robust API built with **Spring Boot** and **JSoup**.

### Local Setup

1. **Build the application:**
   ```bash
   cd substacker_java
   mvn clean package
   ```

2. **Start the Spring Boot API:**
   ```bash
   java -jar target/substacker-java-1.0.0.jar
   ```
   The API will be available at `http://localhost:8080`.

3. **Start the Java MCP Server:**
   The Java MCP server runs over STDIO. You can launch it directly as a single-file program (Java 11+ required):
   ```bash
   java substacker_java/McpServer.java
   ```

---

## API Endpoints (Shared Spec)

Both APIs expose the same endpoints:

* **`GET /`**
  - Returns a status message indicating the API is running.

* **`GET /substack/{username}/info`**
  - Retrieves metadata about a publication (title, subtitle, link, description).

* **`GET /substack/{username}`**
  - Fetches newsletter posts.
  - **Query Parameters**:
    - `limit` (default: 10) - Maximum number of posts to return.
    - `search` (optional) - Filter posts by matching keywords in the title or content.

---

## Deployment

### Python Service
Deployed directly to **Vercel** using the `@vercel/python` builder defined in `substacker_python/vercel.json`.

### Java Service
To deploy the Java service (`substacker_java`) to **Render**, you have two options:

#### Option A: Using the Render Blueprint (Recommended)
This repository includes a `render.yaml` Blueprint file inside `substacker_java/`:
1. Log in to the [Render Dashboard](https://dashboard.render.com/).
2. Click **New** -> **Blueprint**.
3. Connect your GitHub repository.
4. In the settings, change the **Blueprint Path** to `substacker_java/render.yaml` and click **Apply** to deploy.

#### Option B: Manual Setup via Render Dashboard
1. Log in to the [Render Dashboard](https://dashboard.render.com/) and click **New** -> **Web Service**.
2. Connect your Git repository.
3. Configure the following settings:
   - **Name**: `substacker-java`
   - **Environment**: `Docker`
   - **Root Directory**: `substacker_java` (This is important since it is a monorepo)
   - **Dockerfile Path**: `Dockerfile`
4. Click **Create Web Service** to trigger the build and deployment.
