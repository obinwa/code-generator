# ⚙️ AI-Powered Code Generator (Spring Boot + OpenAPI)

An experimental **AI-driven backend code generator** that leverages OpenAPI specifications to scaffold RESTful APIs using Spring Boot. Designed to automate boilerplate generation for faster, cleaner project bootstrapping.

> **Status**: 🚧 In Progress — MVP underway  
> **Tech Stack**: Spring Boot · OpenAPI Generator · Mustache Templates · OpenAI API · Java · Docker

---

## ✨ Features

- 🔁 Parse and consume OpenAPI (YAML/JSON) specifications
- 🧠 Use AI to recommend best practices, annotations, and structure
- 🛠️ Generate `Controller`, `Service`, and `Model` classes
- 📁 Export scaffolded Spring Boot project as a ZIP or Git-ready folder
- 🔌 Pluggable template system (Mustache/Freemarker)
- ⚙️ Configurable naming conventions (camelCase, snake_case, etc.)

---

## 📦 Sample Usage (REST API)

```bash
POST /api/generate

Request:
{
  "openApiUrl": "https://raw.githubusercontent.com/OAI/OpenAPI-Specification/main/examples/v3.0/petstore.yaml",
  "template": "springboot-basic"
}

Response:
✅ Generated project with 8 files
├── PetController.java
├── PetService.java
├── Pet.java
...
