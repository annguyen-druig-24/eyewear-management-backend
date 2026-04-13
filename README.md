# Eyewear Management Backend

Backend API for an eyewear management and retail system, built with Spring Boot to support sales operations, product management, cart handling, checkout, orders, payments, shipping, inventory, returns, dashboards, and AI-powered features such as a product recommendation chatbot and OCR for prescription parsing.

This project is more than just a basic REST API. It also integrates several third-party services to handle real-world business requirements:

- Shipping fee calculation and estimated delivery time via GHN
- Online payments through VNPAY Sandbox and PayOS VietQR
- Product recommendation chatbot powered by Gemini
- Prescription image OCR via OCR.Space
- Image and virtual try-on asset uploads via Cloudinary
- HTML order notification emails using Spring Mail and Thymeleaf

## Key Features

- Authentication and authorization with JWT, Spring Security, and role-based access control
- User management with the roles `CUSTOMER`, `ADMIN`, `MANAGER`, `SALES STAFF`, and `OPERATIONS STAFF`
- Management of products, brands, suppliers, and product images
- Cart management, checkout preview, promotion application, deposit calculation, and remaining balance calculation
- Order creation for `DIRECT_ORDER`, `PRE_ORDER`, `PRESCRIPTION_ORDER`, and `MIX_ORDER`
- Online payment support through VNPAY and PayOS, including callback/webhook handling to sync payment statuses
- `shippingFee` and `expectedDeliveryAt` calculation via GHN
- Support for customer order cancellation, returns, refunds, and refund proof/QR uploads
- Inventory and stock receipt management
- Statistical dashboards and top-selling products
- OCR for prescription images to extract optical parameters
- Product recommendation chatbot with a hybrid strategy: Gemini + heuristic fallback
- GLB file configuration for the product virtual try-on feature

## Technology Stack

| Group | Technology |
| --- | --- |
| Language & build tool | Java 25, Maven 3.9+ |
| Backend framework | Spring Boot 4.0.1 |
| REST API | Spring Web, Spring Validation |
| Data REST & Hypermedia | Spring Data REST, Spring HATEOAS |
| Security | Spring Security, OAuth2 Resource Server, JWT, Nimbus JOSE JWT, BCrypt |
| Persistence | Spring Data JPA, Hibernate, Microsoft SQL Server |
| Mapping | MapStruct, Lombok |
| API docs | springdoc-openapi, Swagger UI |
| Performance optimization | Spring Cache |
| Email | Spring Mail, Thymeleaf |
| File/media upload | Cloudinary |
| Payments | VNPAY Sandbox, PayOS VietQR |
| Shipping | GHN API |
| AI / OCR | Gemini API, OCR.Space API |
| Containerization | Docker multi-stage build |
| Testing | JUnit 5, Mockito, Spring Test |

## Architecture and Source Code Organization

The project follows a fairly clear layered architecture:

- `controller`: defines REST endpoints
- `service`: contains business logic
- `integration`: clients/gateways for GHN, VNPAY, Gemini, and other external services
- `repository`: data access via JPA
- `entity`: database mappings
- `dto`: request/response/projection models
- `mapper`: entity <-> DTO conversion using MapStruct
- `config`: security, OpenAPI, payment, GHN, OCR, Gemini, CORS, Cloudinary, and other configuration

```text
backend/eyewear-management-backend
|-- docs
|-- markdown_files
|-- notes
|-- sql_files
|-- src
|   |-- main
|   |   |-- java/com/swp391/eyewear_management_backend
|   |   |   |-- config
|   |   |   |-- controller
|   |   |   |-- dto
|   |   |   |-- entity
|   |   |   |-- exception
|   |   |   |-- integration
|   |   |   |-- mapper
|   |   |   |-- repository
|   |   |   `-- service
|   |   `-- resources
|   `-- test
|-- Dockerfile
`-- pom.xml
```

## Main API Modules

| Module | Base path | Description |
| --- | --- | --- |
| Authentication | `/auth/**` | Login, introspection, token refresh, logout |
| Users | `/users/**` | Registration, user profile, user administration |
| Products | `/api/products/**` | Search, detail view, product CRUD |
| Cart | `/api/cart/**` | Add, update, and remove cart items |
| Checkout | `/checkout/preview` | Order preview, promotions, GHN fees, deposit calculation |
| Orders | `/orders/**` | Order creation, order history, order cancellation |
| Staff Orders | `/api/staff/**`, `/api/operation-staff/orders/**` | Order processing for sales staff and operations staff |
| Payments | `/payments/vnpay/**`, `/api/payment/**` | VNPAY callback/IPN, PayOS payment link creation, webhook, status |
| Shipping | `/ghn/**` | Province/district/ward lists and shipping support |
| OCR | `/api/prescriptions/parse-image` | Parse prescription images |
| Chatbot | `/api/chatbot/recommend` | Chatbot-based product recommendations |
| Returns | `/api/return-exchanges/**`, `/api/returns/**` | Returns, refunds, image uploads |
| Inventory | `/api/inventory/**`, `/api/inventory-receipts/**` | Stock management and inventory receipts |
| Dashboard | `/api/v1/dashboard/**` | Statistics and top products |
| Try-on Config | `/api/products/{productId}/try-on-config` | Upload/configure GLB models |
| Suppliers | `/api/suppliers/**` | Supplier and related brand management |

## Security and Authorization

- Uses self-signed JWTs with the `HS512` algorithm
- Passwords are encrypted with `BCrypt`
- Tokens support introspection, refresh, and invalidation on logout
- Authorization is based on `ROLE_*` authorities
- CORS is configured through `app.cors.allowed-origin-patterns`
- Swagger/OpenAPI is publicly available for integration and API testing

Current roles available in the system:

- `CUSTOMER`
- `ADMIN`
- `MANAGER`
- `SALES STAFF`
- `OPERATIONS STAFF`

## Third-Party Integrations

### 1. GHN API

Used for:

- Retrieving province, district, and ward lists
- Calculating `shippingFee`
- Calculating `expectedDeliveryAt`
- Supporting shipping-related steps in the order workflow

The project uses `Spring Cache` to cache GHN location data:

- `ghnProvinces`
- `ghnDistricts`
- `ghnWards`

### 2. VNPAY Sandbox

Used for online payments through a redirect flow.

- Creates VNPAY payment URLs
- Verifies callback signatures
- Processes `return` and `ipn`
- Synchronizes `Payment`, `Order`, and `Invoice` statuses

### 3. PayOS VietQR

Used for the VietQR payment flow:

- Creates payment links
- Receives verified webhooks from PayOS
- Synchronizes payment and order statuses

### 4. Gemini API

Used to analyze user intent in the product recommendation chatbot.

The current chatbot flow is hybrid:

- Gemini is used first for intent analysis
- If Gemini fails or is unavailable, the system falls back to internal heuristics so it can still return useful results

### 5. OCR.Space API

Used to read prescription images and convert them into structured data:

- `SPH`
- `CYL`
- `AXIS`
- `ADD`
- `PD`

The OCR response also includes:

- `confidence`
- `requiresReview`
- `warnings`
- `rawText`

### 6. Cloudinary

Used to upload and store:

- Product images
- Return/refund evidence images
- Refund account QR images
- `.glb` model files for virtual try-on

### 7. SMTP Email

Used to send HTML order-related notification emails with Thymeleaf templates.

## System Requirements

- JDK 25
- Maven 3.9 or later
- Microsoft SQL Server
- Internet access to call external services: GHN, VNPAY, PayOS, Gemini, OCR.Space, Cloudinary

## Environment Configuration

The sample configuration template is located at:

```text
src/main/resources/application.example.yml
```

Recommended setup:

1. Copy the sample file to `src/main/resources/application.yml`
2. Fill in the real values for your local environment
3. Do not commit files that contain real secrets to Git

Important configuration groups:

| Configuration group | Representative keys | Purpose |
| --- | --- | --- |
| Database | `spring.datasource.*` | SQL Server connection |
| JWT | `jwt.*` | Access token signing and validation |
| GHN | `ghn.*` | Shipping fee config, shop ID, token, package defaults |
| VNPAY | `vnpay.*` | VNPAY Sandbox integration |
| PayOS | `payos.*` | PayOS VietQR integration |
| Gemini | `app.gemini.*` | Chatbot recommendations |
| OCR | `app.ocr.*` | Prescription image parsing |
| Cloudinary | `cloudinary.*` | Image and try-on model uploads |
| Mail | `spring.mail.*` | HTML email sending |
| Frontend/Backend URL | `app.frontend.*`, `app.backend.*` | Payment callback redirects and frontend links |
| CORS | `app.cors.allowed-origin-patterns` | Allowed API caller domains |

Operational notes:

- `spring.jpa.hibernate.ddl-auto` is currently set to `none`, so the database schema must already exist before running the application
- The application's default time zone is `Asia/Ho_Chi_Minh`
- Multipart upload is currently configured with a maximum of `50MB/file` and `100MB/request`

## Database Setup

Script directory:

```text
sql_files/
```

You can choose one of the following approaches:

### Option 1: Use the all-in-one script

Run:

```text
sql_files/swp391_script.sql
```

This script already includes database creation and the full schema setup.

### Option 2: Separate schema and demo data

Run the following in order:

```text
sql_files/SWP391_Eyewear_DB_Structure.sql
sql_files/SWP391_Eyewear_DB_DemoData.sql
```

Target database:

```text
EyewearManagement
```

## Running the Project Locally

Move to the backend directory:

```bash
cd backend/eyewear-management-backend
```

Build the project:

```bash
mvn clean install
```

Run the application:

```bash
mvn spring-boot:run
```

By default, the application runs at:

```text
http://localhost:8080
```

Note:

- The repository currently includes `mvnw` and `mvnw.cmd`, but the `.mvn/wrapper` directory is missing, so you need to use a locally installed `mvn` instead of Maven Wrapper

## Docker

The project already includes a multi-stage `Dockerfile` for building and running in a container.

Build the image:

```bash
docker build -t eyewear-management-backend .
```

Run the container:

```bash
docker run --rm -p 8080:8080 eyewear-management-backend
```

Notes:

- The current Docker build assumes the runtime configuration file is already present in the project at build time
- In CI/CD, you can inject `application.yml` from secrets as described in `docs/secret-management.md`

## API Documentation

After the application is running, you can access:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Swagger scans the main controllers in the project and is suitable for quick endpoint testing by frontend developers or QA.

## Testing

Run tests:

```bash
mvn test
```

The project already includes tests for several important flows, for example:

- Authentication
- Checkout preview
- Inventory
- Staff order processing
- VNPAY callback

## Related Documentation

- `docs/secret-management.md`: secret management and deployment guide
- `docs/prescription-ocr-flow.md`: detailed prescription OCR workflow
- `sql_files/`: schema and sample data
- `markdown_files/`: extended business documentation
- `notes/`: API notes, business flows, and processing rules

## Security Notes

- Do not commit `application.yml` files that contain real database credentials, mail credentials, payment gateway secrets, or AI keys
- If any secret is exposed, rotate it immediately and update the deployment environment
- For production, prefer environment variables or a secret manager instead of hard-coded configuration

## Additional Backend Stack Summary

In addition to the technologies already listed above, this backend also uses:

- SQL Server
- Spring Data JPA / Hibernate
- Spring Validation
- Spring Cache
- Spring Data REST / HATEOAS
- springdoc-openapi / Swagger UI
- MapStruct
- Lombok
- Cloudinary
- Spring Mail
- Thymeleaf
- Docker
- JUnit 5 + Mockito for testing
