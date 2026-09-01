# AI_CONTEXT.md — com-API-Hybrid-Framework

Compact knowledge base for AI agents & new contributors. Read fully before editing code.
Last verified: compiles (`mvn test-compile`), BasicAuthTest passes live.

---

## 1. What it is

Hybrid REST API automation framework (keyword + data-driven intent) for testing multiple public APIs.
Layered Java + REST Assured wrapper so tests stay thin.

## 2. Tech Stack (versions matter — Jackson 3, not 2!)

| Tech | Version | Notes |
|---|---|---|
| Java | 17 (`maven.compiler.release`) | |
| Maven | 3.8+ | surefire 3.2.5 auto-discovers `**/*Test.java` |
| REST Assured | 6.0.1 | + `json-schema-validator` 6.0.1 (unused) |
| Jackson | `tools.jackson.core` 3.2.2 + `jackson-dataformat-xml` | **Package `tools.jackson.*`, NOT `com.fasterxml.*`** — import carefully! |
| TestNG | 7.12.0 | Reports → `test-output/index.html` |
| Lombok | 1.18.46 | |
| JSONPath | `com.jayway.jsonpath` 3.0.0 | |

`pom.xml` has **no surefire `<groups>`/suite wiring** — it's include-pattern driven. `testng.xml` at root runs only `DeleteAUserTest`; `src/test/resources/testRunners/*.xml` are empty placeholders.

## 3. Directory map

```
src/main/java/com/qa/
  api/
    constants/    AuthType(enums), AppConstants(API_TIME_OUT=2000), StatusCode(OK_200, NOT_FOUND_404)
    errors/       APIError            — EMPTY stub
    exceptions/   APIException        — RuntimeException subclass
    pojo/         User, Product(+nested Rating), ContactsCredentials — Lombok
    utils/        JsonPathValidatorUtils, JsonUtils, StringUtils — implemented
                  CSVReader, ExcelReader, ObjectMapperUtil, XmlPathUtil — EMPTY stubs
  gorest/
    client/       RestClient          — THE core wrapper (ALL HTTP)
    manager/      ConfigManager       — properties + env/sysprop resolution
src/test/java/com/qa/
  api/base/       BaseTest            — URLs, endpoints, @BeforeMethod restClient
  api/gorest/tests/   CreateAUserTest, GetAUserTest, UpdateAUserTest,
                      DeleteAUserTest, GetAUserWithSerializaitonTest
  api/contacts/tests/ GetContactsAPITests, CreateAContactTest(empty)
  api/products/       ProductAPITest, ProductAPITestWithJsonPath
  api/reqres/tests/   ReqResTest
  api/basicAuth/test/ BasicAuthTest
  api/amadeus/test/   AmadeusAPITest
src/test/resources/
  config/config.properties        # git-ignored secrets (Bearertoken="", clientid, clientseceret, granttype, apikey, basicAuthUsername/password=admin)
  config/config.properties.example
  jsons/User.json                 # file-based POST payload
  schema/createUserSchema.json    # EMPTY
  testRunners/testng_sanity.xml, testng_regression.xml  # EMPTY
```

## 4. Core pattern — RestClient (`com.qa.gorest.client.RestClient`)

One instance per test (BaseTest `@BeforeMethod`). Stateless → thread-safe.

**Pre-built ResponseSpecs** (lines 25–34): `responseSpec200, 201, 204, 400, 404, 200or201, 200or401, 200or404, 200or500, 200or501`. For `get()` the spec is **200or401** — a bad token still passes; for `post/put/patch` **200or201**; `delete` **204**. Applied via `.then().spec(spec).extract().response()`.

**`setupRequest(baseUri, authType, contentType)`**:
- `.log().all()`, sets `baseUri` + `Accept` only (Content-Type is set per body-bearing method — avoids reqres 415).
- Auth switch: BEARER_TOKEN → `Authorization: Bearer <config>`; BASIC_AUTH → Base64 header; API_KEY → **placeholder junk header**; NO_AUTH → skip; else throw `APIException`.
- OAuth2 exists only as the dedicated form-params `post(...)` overload.

**`applyParams(request, queryParams, pathParams)`** — attaches maps if non-null.

**CRUD methods** (all `prettyPrint()` the response):
- `get(baseUrl, endpoint, queryParams, pathParams, authType, contentType)` → `responseSpec200or401`
- `<T> post(baseUrl, endpoint, T body, ...)` → generic body (POJO/string) → `responseSpec200or201`
- `post(baseUrl, endpoint, File file, ...)` → JSON file body → `responseSpec200or201`
- `post(BaseUrl, endpoint, clientid, clientSecret, grantType, contentType)` → **OAuth2 token**: urlencoded formParams `grant_type/client_id/client_secret`, returns raw (no spec)
- `<T> put(..., T body, ...)` / `<T> patch(..., T body, ...)` → `responseSpec200or201`
- `delete(baseUrl, endpoint, ...)` → `responseSpec204`

## 5. Config resolution — ConfigManager

- `static{}` block loads `config/config.properties` from classpath.
- `Get(key)`: system prop `-Dkey=...` → env var (`camelCaseToScreamingSnake`: `gorest_bearerToken` → `GOREST_BEARER_TOKEN`) → properties file.
- `getProperty(key)`: direct file read.
- **GOTCHA:** `RestClient` uses `ConfigManager.getProperty("Bearertoken")` (file-only). The env/sysprop fallback (`Get()`) is NOT on the bearer-auth path. When editing config, keep this in mind.
- Tests override token via `ConfigManager.setProperty("Bearertoken", <token>)` in `@BeforeMethod`.

## 6. POJOs & serialization

- **User** (`id,name,email,gender,status`): `@Data @NoArgsConstructor @AllArgsConstructor @Builder @JsonInclude(Include.NON_NULL)` — NON_NULL lets one POJO do request serialization (no id) + response deserialization.
- **Product**: nested static `Rating{rate:Double, count:Integer}` — Lombok on both.
- **ContactsCredentials** (`email,password`) — login payload.
- `JsonUtils.deserialize(response, Class<T>)` wraps Jackson `ObjectMapper.readValue(bodyAsString, cls)`; throws RuntimeException on failure.
- `JsonPathValidatorUtils`: `read`, `readList`, `readListOfMaps` via Jayway `JsonPath.parse(body).read(path)` — supports filters (`$[?(@.price > 50)]`), projections (`$.[*].['id','title']`), aggregations (`min($[*].price)`).
- `StringUtils.getRandomEmailId()` → `"testing"+System.currentTimeMillis()+"@dummy.com"`.

## 7. BaseTest — shared constants

Base URLs: GOREST `https://gorest.co.in` · CONTACTS `https://thinking-tester-contact-list.herokuapp.com/` · REQRES `https://reqres.in` · BASIC_AUTH `https://the-internet.herokuapp.com` · PRODUCTS `https://fakestoreapi.com` · OAUTH2 (Amadeus test, repointed to free public OAuth2 demo) `https://demo.duendesoftware.com`

Endpoints: `/public/v2/users` · `/users/login` · `/users/contacts` · `/api/users` · `/basic_auth` · `/products` · OAuth2: `/connect/token` + `/api/test` (Duende demo: `client_id=m2m`, `client_secret=secret`)

## 8. Test map (what each does)

| Class | Auth | Covers |
|---|---|---|
| CreateAUserTest | Bearer | POST with POJO (active); string-body & File-body variants disabled; +11 edge/negative cases (missing/invalid fields, unicode, long names, duplicate emails, Content-Type, NFR) |
| GetAUserTest | Bearer | list, list+queryParams(name,status), single by ID; +9 cases (gender/status/name filters, pagination edges, per_page boundary, non-numeric id, NFR) |
| UpdateAUserTest | Bearer | create→GET verify→PUT (name/status via setters); +8 cases (PATCH single-field preserves, idempotent PUT, invalid email/status enum, no-auth, empty body, duplicate email) |
| DeleteAUserTest | Bearer | create→verify→DELETE(204)→GET expects 404+"Resource not found"; +3 cases (invalid auth, non-numeric id, list-integrity check) |
| GetAUserWithSerializaitonTest | Bearer | POST→GET→deserialize body→User POJO; +3 cases (all-fields-present single, all-fields-present list, post-update round-trip) |
| ProductAPITest | None | GET /products → `Product[]` deserialization + print; +10 cases (per-field validation, category coverage, limit/sort, edge ids, data integrity, NFR) |
| ProductAPITestWithJsonPath | None | JSONPath filters/min aggregation; +6 cases (price range, projection, category filter, sum/avg consistency, title length, rating range) |
| ReqResTest | None | GET /api/users?page=2 (415-avoidance case); +7 cases (schema, email regex, pagination edges, empty body/name, non-existing DELETE, NFR) |
| BasicAuthTest | Basic | GET /basic_auth → 200 + body text; +6 cases (malformed base64, empty/missing parts, body marker, NFR) |
| AmadeusAPITest | OAuth2 | form-param token POST (`connect/token`) → Bearer GET (`/api/test`); +8 cases (token schema, reusability, expiry 3600, invalid secret, missing client_id, back-to-back, NFR, audience claim) |
| GetContactsAPITests | None→Bearer | login→token→GET contacts; +5 cases (array shape, single-id GET, non-existing 404, token persistence, lowercase-bearer 401) |
| CreateAContactTest | Bearer (login @BeforeMethod) | create + E2E lifecycle; +10 cases (empty/missing fields, malformed JSON, unknown fields, optional fields, unicode, long email, owner field) |

**Live count:** 142 tests total. Run `mvn test` to execute.

## 9. Conventions to follow

- Tabs, not spaces; javadoc on public RestClient methods.
- All API tests `extends BaseTest`; use `restClient` field; assert with TestNG `Assert`.
- Packages: `com.qa.api.*` (generic) vs `com.qa.gorest.*` (client/manager) — historical, keep.
- Body-bearing calls set `ContentType` explicitly; bodyless GET/DELETE may use `ContentType.ANY`.
- Secrets only in `config.properties` (git-ignored) / env / sysprop — NEVER hardcode.

## 10. Running commands (from project root)

```bash
mvn test-compile                      # compile only
mvn test -Dtest=BasicAuthTest         # one class (surefire include **/*Test.java)
mvn clean test                        # all tests (GoRest ones need a token)
mvn verify                            # Jenkins entry point
GOREST_BEARER_TOKEN=... mvn test      # intended token path (see §5 gotcha)
# IDE: Run As → TestNG Test; token from config.properties or Run Config env/VM-arg
# Reports: test-output/index.html · surefire XMLs in target/surefire-reports
```

## 11. Known issues & sharp edges (do NOT "fix" silently)

1. `generateTheBasicAuth()` line 82 uses `basicAuthUsername` for BOTH sides: `user+":"+getProperty("basicAuthUsername")`. Works only because both =admin. Bug: should be `basicAuthPassword`.
2. Line 20: `import groovyjarjarantlr4.v4.parse.ANTLRParser.id_return;` is a stray accidental import.
3. GoRest test classes hardcode `tokenid` which LOOKS like a leaked secret (history has `backup-before-token-purge` tag) — intentional practice for now.
4. `CreateAUserTest.createUserTestWithStringBody` (disabled) posts `inactive` but asserts `active`.
5. StatusCode enum, APIError, APIException unused in main flow; API_KEY auth is a placeholder.
6. `get()` accepts 200 or 401 → weak auth failure detection.
7. CSV/Excel data-driven, JSON-schema validation, Allure — roadmap, not built.
8. ~~`AmadeusAPITest` no assert after flights call.~~ **Fixed** — now asserts 200 + claim echo against Duende demo.

---
_Use this file as ground truth; always re-run `mvn test-compile` after edits to main code._