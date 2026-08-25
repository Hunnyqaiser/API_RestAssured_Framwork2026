# 🚀 com-API-Hybrid-Framework

A clean, scalable **REST API automation testing framework** built with **Java**, **REST Assured**, and **TestNG**, following the **Hybrid / Keyword & Data-Driven** approach. It wraps REST Assured into a reusable client, supports multiple authentication strategies, and organizes projects into logical layers (constants, POJOs, utilities, clients, config, and tests).

> **Author:** Hunny Qaiser

---

## ✨ Features

- 🔁 **CRUD support** — ready-to-use `GET`, `POST`, `PUT`, `PATCH`, and `DELETE` methods.
- 🔐 **Multiple authentication types** — `Bearer Token`, `OAuth2`, `Basic Auth`, `API Key`, and `NO_AUTH`.
- 🧱 **Layered architecture** — clean separation between constants, POJOs, utilities, client, config, and tests.
- 🧪 **Data-driven possibilities** — JSON, CSV, and Excel utilities included (extendable).
- 📦 **JSON Schema validation** — built-in schema validator support.
- 📊 **TestNG reports** — HTML reports and XML results generated on every run.
- 🧰 **Lombok** — minimal boilerplate for POJOs.
- 🗂️ **Response specification** — central status-code spec for readable, consistent assertions.

---

## 🧰 Tech Stack

| Technology | Purpose | Version |
|-----------|---------|---------|
| Java | Language | 17 |
| Maven | Build / dependency management | — |
| REST Assured | REST API testing library | 6.0.1 |
| TestNG | Test framework | 7.12.0 |
| Jackson | JSON / XML serialization | 3.2.2 |
| Lombok | POJO boilerplate reduction | 1.18.46 |
| JSONPath | JSON expression evaluation | 3.0.0 |

---

## 📁 Project Structure

```
com-API-Hybrid-Framework/
├── pom.xml
├── src/
│   ├── main/java/com/qa/
│   │   ├── api/
│   │   │   ├── constants/        # AppConstants, AuthType, StatusCode
│   │   │   ├── errors/           # APIError
│   │   │   ├── exceptions/       # APIException custom exception
│   │   │   ├── pojo/             # Request/Response POJOs (e.g. User)
│   │   │   └── utils/            # JsonPath, XmlPath, CSV, Excel utilities
│   │   └── gorest/
│   │       ├── client/           # RestClient (REST Assured wrapper)
│   │       └── manager/          # ConfigManager (properties loader)
│   └── test/java/com/qa/
│       ├── api/base/             # BaseTest (BUILD_URL / endpoints)
│       ├── api/gorest/tests/     # GoRest user tests (CRUD)
│       ├── api/contacts/tests/   # Contact tests
│       └── api/products/         # Product tests
│   └── test/resources/
│       ├── config/config.properties    # tokens / config values
│       ├── jsons/                     # request JSON payloads
│       ├── schema/                     # JSON schema files
│       └── testRunners/                # testng XML suites
└── target/                       # build output (git-ignored)
---

## ✅ Prerequisites

- Install **[JDK 17+](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html)**
- Install **[Maven 3.8+](https://maven.apache.org/download.cgi)** (or use an IDE with Maven support, e.g. Eclipse / IntelliJ)
- (Optional) An API token for authenticated endpoints — add it to `config.properties`

---

## 🚦 Getting Started

### 1. Clone the repository

```bash
git clone git@github.com:Hunnyqaiser/API_RestAssured_Framwork2026.git
cd API_RestAssured_Framwork2026
```

### 2. Open in an IDE
Import the project as a **Maven project** (Eclipse: *File → Import → Existing Maven Projects*, IntelliJ: *File → Open → pom.xml*). The IDE will fetch dependencies automatically.

### 3. Configuration — supplying the API token

The framework reads config values through [`ConfigManager`](src/main/java/com/qa/gorest/manager/ConfigManager.java), which resolves a key in this **priority order** (highest first):

1. **JVM System Property** → `-Dgorest_bearerToken=xyz`
2. **Environment Variable** → `GOREST_BEARER_TOKEN=xyz`
3. **`config.properties`** file (git-ignored local file) → fallback

> 💡 The token key `gorest_bearerToken` maps automatically to the conventional env var name `GOREST_BEARER_TOKEN`, so you can pick whichever method fits your workflow.

> ⚠️ **Security:** never commit real tokens/secrets. `config.properties` is git-ignored — use the env var / system property on CI, or the git-ignored file locally.

---

## ▶️ Running Tests & Passing the Token

### From the command line (Maven)

**Option A — environment variable (recommended):**
```bash
export GOREST_BEARER_TOKEN=YourBearerTokenHere
mvn clean test
```

**Option B — JVM system property:**
```bash
mvn clean test -Dgorest_bearerToken=YourBearerTokenHere
```

**Option C — git-ignored `config.properties`:**
```properties
gorest_bearerToken=YourBearerTokenHere
```

Run a **single test class**:
```bash
GOREST_BEARER_TOKEN=YourBearerTokenHere mvn test -Dtest=CreateAUserTest
```

### From an IDE — Run As TestNG (not Maven)

Here the token goes to the **IDE's JVM**, not Maven, so use one of:

- **Easiest:** put it in `config.properties` (resource on the classpath), then *Run As → TestNG Test*.
- **Run Configuration → Environment** tab: add `GOREST_BEARER_TOKEN` = `YourBearerTokenHere`.
- **Run Configuration → VM arguments** (Arguments tab): add `-Dgorest_bearerToken=YourBearerTokenHere`.

### From Jenkins (CI)

A `Jenkinsfile` is included. It injects the token from a **Secret text** credential named `gorest-bearer-token` as the `GOREST_BEARER_TOKEN` env var, then runs `mvn clean verify`.

### Summary table

| Where you run | How to pass the token |
|---|---|
| Command line / Maven | `GOREST_BEARER_TOKEN=... mvn test` or `mvn test -Dgorest_bearerToken=...` |
| TestNG class in IDE | Put it in `config.properties`, or add an Env Var / VM arg in the Run Config |
| Jenkins | Jenkins credential `gorest-bearer-token` (handled by `Jenkinsfile`) |

> After each run, TestNG writes an HTML report to `test-output/index.html` — open it in your browser.

---

## 🧩 How to Use the RestClient

The `RestClient` wrapper hides REST Assured complexity. Instantiate it (usually once in `BaseTest`) and call the CRUD methods:

```java
RestClient restClient = new RestClient();

// GET
Response response = restClient.get(
    BASE_URL, ENDPOINT,
    null, null,                 // queryParams, pathParams
    AuthType.BEARER_TOKEN,      // auth type
    ContentType.JSON);          // content type

// POST with a POJO body
User user = new User("Yasir", StringUtils.getRandomEmailId(), "male", "active");
Response response = restClient.post(
    BASE_URL, ENDPOINT, user,
    null, null, AuthType.BEARER_TOKEN, ContentType.JSON);
```

### ResponseSpec (status-code validation)
The client pre-configures `ResponseSpecification`s for common status codes (200, 201, 204, 400, 404, etc.) and auto-validates responses as they are returned, keeping test assertions clean.

---

## 🧪 Test Coverage

The test suites cover the full **CRUD** lifecycle against the **gorest.co.in** public API:

- `CreateAUserTest` — create users (POJO, string, and file bodies)
- `GetAUserTest` — retrieve users
- `UpdateAUserTest` — update users
- `DeleteAUserTest` — delete users
- Plus templates for **contacts** and **products** modules.

---

## 📚 Sample Test

```java
public class CreateAUserTest extends BaseTest {

    @Test
    public void createAUserTest() {
        User user = new User("Yasir", StringUtils.getRandomEmailId(), "male", "active");
        Response response = restClient.post(
            BASE_URL_GOREST, GOREST_USERS_ENDPOINT, user,
            null, null, AuthType.BEARER_TOKEN, ContentType.JSON);

        Assert.assertEquals(response.jsonPath().getString("name"), "Yasir");
        Assert.assertNotNull(response.jsonPath().getString("id"));
    }
}
```

---

## 🔭 Roadmap

- [ ] Populate the empty `testng_regression.xml` and `testng_sanity.xml` suites
- [ ] Add data-driven tests with CSV / Excel readers
- [ ] Add schema-validation examples for `CreateAProductTest`
- [ ] Add Allure reporting integration

---

## 📄 License / Info

This is a personal learning and practice framework. No formal license — reach out for any questions.
```