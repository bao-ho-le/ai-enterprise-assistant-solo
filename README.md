# Enterprise AI Document Assistant

**Enterprise AI Document Assistant** là hệ thống hỗ trợ quản lý và khai thác tài liệu doanh nghiệp với sự hỗ trợ của AI: upload tài liệu, tự động xử lý (trích xuất, chia nhỏ, tạo embedding), sau đó cho phép tìm kiếm ngữ nghĩa, hỏi đáp trực tiếp trên nội dung tài liệu (RAG), hoặc yêu cầu AI soạn sẵn email, báo cáo, tóm tắt, biểu mẫu dựa trên tài liệu đã upload.

🔗 **Demo trực tuyến:** [https://ai-enterprise-assistant-solo.vercel.app](https://ai-enterprise-assistant-solo.vercel.app)

---

## ✨ Tính năng và kỹ thuật nổi bật

- **Quản lý tài liệu theo thư mục** — tạo thư mục, di chuyển, đổi tên; mỗi tài liệu có nhiều phiên bản, xem lại và tải về được từng phiên bản cũ.
- **Xử lý tự động sau khi upload** — hệ thống tự đọc nội dung file PDF, Word, Excel, TXT rồi chuẩn bị sẵn dữ liệu cho AI, người dùng không phải thao tác thêm gì.
- **Tìm kiếm theo ý nghĩa** — mô tả bằng lời thay vì phải đoán đúng từ khoá, hệ thống vẫn tìm ra đoạn tài liệu liên quan.
- **Hỏi đáp trực tiếp trên tài liệu** — chọn tài liệu rồi đặt câu hỏi; câu trả lời kèm trích dẫn đúng đoạn văn nguồn để người dùng tự kiểm chứng.
- **AI nhớ ngữ cảnh hội thoại** — hỏi tiếp "cái đó là gì" mà không cần nhắc lại từ đầu.
- **Soạn văn bản tự động** — AI viết email, báo cáo, biên bản họp, tóm tắt, biểu mẫu dựa trên tài liệu đã có; nội dung sinh ra chỉnh sửa lại được.
- **Phân quyền theo vai trò** — hệ thống có 4 vai trò (Admin, Manager, Supervisor, Employee), mỗi người chỉ xem và thao tác được đúng những gì vai trò của mình cho phép. Phân quyền theo phòng ban và chức năng chia sẻ tài liệu cho đồng nghiệp đang được phát triển tiếp.
- **Thùng rác và khôi phục** — tài liệu, thư mục, hội thoại lỡ xoá đều khôi phục lại được.
- **Trang quản trị** — admin quản lý người dùng, phòng ban, quyền hạn, đồng thời theo dõi lượng dùng AI và chi phí ước tính theo ngày.
- **Chạy được bằng một lệnh** — toàn bộ hệ thống đóng gói sẵn bằng Docker Compose.

---

## 🎯 Vấn đề & Giải pháp

Về mặt nghiệp vụ, nhân viên mất nhiều thời gian tìm thông tin nằm rải rác trong tài liệu nội bộ, tìm kiếm từ khoá không hiểu ngữ nghĩa, và soạn thảo văn bản lặp lại thủ công. Nhưng phần lớn độ khó của dự án nằm ở các bài toán kỹ thuật bên dưới — dưới đây là từng tình huống cụ thể và cách hệ thống đã giải quyết nó trong code.

### 1. Upload nhiều file: nếu một file lỗi thì không để lại dữ liệu dở dang

**Vấn đề:**
Người dùng upload 5 file cùng lúc, đến file thứ 3 thì lỗi. Hai file đầu đã lưu xong không được phép nằm lại trong hệ thống ở trạng thái nửa vời — hoặc lưu trọn vẹn cả 5 file, hoặc không lưu file nào.

Chi tiết hơn, mỗi file khi upload sẽ tạo ra các dữ liệu liên quan gồm:

- File object trên MinIO
- Bản ghi trong `files`
- Bản ghi trong `documents`
- Bản ghi trong `document_versions`
- Sau đó `documents.current_version_id` được cập nhật để trỏ tới version hiện tại.

Toàn bộ các bước trên phải được thực hiện đồng bộ. Nếu file thứ 3 bị lỗi thì cả batch phải được rollback, tránh để lại dữ liệu không hoàn chỉnh trong database, chẳng hạn `document` đã tồn tại nhưng chưa có `version`.

**Giải pháp:**
`DocumentServiceImpl.upload()` chạy toàn bộ quá trình upload của batch bên trong một transaction duy nhất (`@Transactional`). Vì vậy, chỉ cần một file trong batch xảy ra lỗi, toàn bộ thay đổi database của các file trước đó cũng được rollback.

Pipeline AI cũng không được chạy ngay trong transaction. Sau khi toàn bộ batch upload thành công, hệ thống publish `DocumentVersionCreatedBatchEvent`. Worker xử lý event này bằng `@TransactionalEventListener(phase = AFTER_COMMIT)`.

Điều này đảm bảo hai nguyên tắc:

1. Transaction rollback → AI pipeline không chạy.
2. Transaction commit thành công → AI pipeline chỉ chạy sau khi các `version_id` đã thực sự tồn tại trong database.

Nhờ đó, hệ thống không xảy ra tình trạng AI pipeline xử lý một `version_id` chưa commit hoặc xử lý dữ liệu của một batch upload đã bị rollback.

### 2. Xử lý tài liệu chạy nền, nhiều file cùng lúc, tự thử lại khi lỗi

**Vấn đề:**
Tài liệu vừa upload cần được xử lý qua nhiều bước khá nặng. Người dùng không thể ngồi chờ màn hình đứng yên cho tới khi xong, và một lỗi mạng thoáng qua khi gọi dịch vụ bên ngoài cũng không nên làm hỏng luôn cả tài liệu.

Cụ thể, mỗi tài liệu phải đi qua chuỗi: **trích xuất text → chia chunk → tạo embedding → ghi Qdrant**. Các bước gọi Gemini và Qdrant đều qua mạng nên dễ gặp lỗi tạm thời.

**Giải pháp:**

- `AsyncConfig` khai báo `ThreadPoolTaskExecutor` riêng tên `documentProcessingExecutor` — `corePoolSize=3`, `maxPoolSize=5`, `queueCapacity=100`, prefix thread `document-processing-` để dễ trace log.
- `DocumentProcessingBatchWorker` nhận batch event rồi **submit từng `versionId` thành một task độc lập** vào executor → nhiều file trong cùng batch được xử lý song song, một file hỏng không chặn các file còn lại.
- `DocumentProcessingRetryExecutor` bọc lời gọi bằng `@Retryable(maxAttempts = 3, backoff = 3000ms)` và **chỉ retry đúng 4 loại lỗi tạm thời**: `FileStorageException`, `ProcessingException`, `EmbeddingException`, `VectorStoreException`. Lỗi nghiệp vụ (ví dụ file sai định dạng) fail-fast, không phí 3 lượt gọi API.
- `DocumentVersion` mang cột `processing_step` (`TEXT_EXTRACTING` / `CHUNKING` / `EMBEDDING`) được cập nhật trước mỗi bước, nên khi FAILED là biết chính xác hỏng ở khâu nào.

### 3. Xử lý tài liệu thất bại thì phải ghi nhận được là đã thất bại

**Vấn đề:**
Khi một tài liệu xử lý hỏng, hệ thống phải đánh dấu lại để người dùng biết và để có thể xử lý lại. Nhưng ở đây có một nghịch lý: chính lỗi đó lại xoá luôn dòng ghi nhận "đã thất bại", khiến tài liệu kẹt mãi ở trạng thái đang chờ.

Lý do: `DocumentProcessingService.process()` chạy trong `@Transactional`. Khi bước embedding ném exception, transaction rollback — nghĩa là lệnh `setStatus(FAILED)` viết trong cùng transaction cũng bị cuốn theo, và tài liệu kẹt vĩnh viễn ở `PENDING`.

**Giải pháp:**
`ProcessingHelper.handleFailed()` được đánh dấu `@Transactional(propagation = REQUIRES_NEW)` — mở transaction riêng để ghi `FAILED` + `processing_step` + `error_message`, độc lập với transaction chính đang rollback.

### 4. Log chi phí AI được ghi thông qua Event

**Vấn đề:**
Mỗi lượt gọi AI đều phải ghi lại lượng token và chi phí ước tính để theo dõi. Nhưng việc ghi log này không được làm hỏng nghiệp vụ chính, và ngược lại cũng không được biến mất khi nghiệp vụ gặp lỗi.

Cụ thể, usage log có khóa ngoại tham chiếu đến `ai_message` và `ai_conversation`. Nếu ghi log ngay trong transaction tạo message, các bản ghi này có thể chưa commit, dẫn đến lỗi khóa ngoại. Ngoài ra, việc ghi log trực tiếp cũng có thể làm ảnh hưởng đến transaction chính.

**Giải pháp:**
Sau khi xử lý AI, hệ thống **publish `AIUsageLogEvent`** thay vì ghi log trực tiếp. Listener sử dụng `@TransactionalEventListener(AFTER_COMMIT)` để chỉ xử lý event sau khi transaction tạo message commit thành công, sau đó dùng `REQUIRES_NEW` để ghi usage log trong một transaction riêng.

Usage log lưu `model`, `input_tokens`, `output_tokens`, `estimated_cost` và `status` để phục vụ thống kê chi phí AI trên Admin.

### 5. Giữ người dùng đăng nhập lâu mà vẫn an toàn khi token bị lộ

**Vấn đề:**
Người dùng không muốn phải đăng nhập lại mỗi tiếng. Nhưng nếu cấp cho họ một tấm vé sống lâu, tấm vé đó bị lộ thì kẻ tấn công dùng được suốt thời gian còn lại mà hệ thống không hề hay biết.

Cụ thể, access token JWT sống ngắn (1 giờ) nên cần refresh token sống dài (7 ngày) để tự động cấp lại. Chính refresh token dài hạn này là thứ cần được bảo vệ và cần phát hiện được khi bị dùng lại trái phép.

**Giải pháp:**

- Refresh token **không lưu dạng thô** — bảng `refresh_tokens` chỉ giữ `jti` và `token_hash` (SHA-256). So khớp bằng `MessageDigest.isEqual()` (constant-time) để tránh timing attack.
- **Rotation:** mỗi lần `/auth/refresh-token`, token cũ bị `revoked = true` rồi mới cấp cặp token mới (`AuthServiceImpl.refreshToken()`).
- **Phát hiện tái sử dụng:** nếu ai đó gửi lên một refresh token đã `revoked`, hệ thống hiểu token đã bị lộ và gọi `revokeAllByUser()` — thu hồi **toàn bộ** refresh token của user đó, buộc đăng nhập lại.
- Logout thu hồi đúng token đang dùng. Token được trả về qua **httpOnly cookie**, `Secure` + `SameSite=None` bật theo biến `COOKIE_SECURE` để chạy được cross-origin giữa Vercel và Render.
- `SecurityConfig` đặt session `STATELESS`, whitelist đúng `/api/v1/auth/**` và `/api/v1/health`, chặn `/api/v1/admin/**` ở tầng filter bằng `hasRole('ADMIN')` — nhưng **vẫn enforce lại permission ở service layer**, không coi filter là hàng rào duy nhất.

### 6. Một khung chat, nhiều kiểu câu hỏi — định tuyến bằng intent

**Vấn đề:**
Cùng một ô chat, lúc người dùng hỏi nội dung nằm trong tài liệu, lúc lại nhờ tóm tắt, lúc chỉ chào hỏi vu vơ. Nếu xử lý mọi câu theo cùng một cách thì vừa chậm vừa cho câu trả lời sai — chạy tìm kiếm tài liệu cho câu "xin chào" là vô nghĩa.

Vì vậy hệ thống cần nhận ra ý định của từng câu hỏi trước, rồi mới chọn cách xử lý tương ứng.

**Giải pháp:**
`IntentClassifierImpl` gọi LLM với prompt phân loại chuyên biệt, trả về 1 trong 4 giá trị enum `Intent`. `AIMessageServiceImpl.answerByIntent()` dùng `switch` định tuyến sang 4 nhánh xử lý hoàn toàn khác nhau:

| Intent | Dùng khi người dùng muốn gì | Nhánh xử lý | Chạm tới gì |
|---|---|---|---|
| `DOCUMENT_QA` | Hỏi thông tin nằm trong tài liệu đã đính kèm | `DocumentQAService` — RAG đầy đủ | Embedding + Qdrant + tài liệu đính kèm |
| `SUMMARY` | Yêu cầu tóm tắt nội dung một tài liệu | `SummaryChatService` | Full text tài liệu, không retrieval |
| `GENERAL_CHAT` | Hỏi kiến thức chung, không liên quan tài liệu | `GeneralChatService` | Không chạm tài liệu |
| `CONVERSATION_SUMMARY` | Hỏi lại về chính cuộc hội thoại đang diễn ra | `ConversationSummaryChatService` | Chỉ `ConversationMemory` |

Hai chi tiết đáng chú ý:

- **Permission theo intent, không theo conversation:** `requireIntentPermission()` kiểm tra `AI_DOCUMENT_QA` / `AI_DOCUMENT_SUMMARY` / `AI_CHAT` tuỳ nhánh thực sự chạy — vì một conversation type `DOCUMENT_QA` vẫn có thể chứa lượt chat chung.
- **Rewrite câu hỏi trước khi retrieval:** với `DOCUMENT_QA`, `QuestionRewriter` dùng conversation memory để giải các tham chiếu ("nó", "loại thứ hai") thành câu hỏi độc lập trước khi embedding, để vector search nhận đúng ngữ nghĩa. Message của user vẫn lưu nội dung gốc.

### 7. Nhớ ngữ cảnh hội thoại mà không nhồi toàn bộ lịch sử vào prompt

**Vấn đề:**
AI cần nhớ những gì đã trao đổi ở các lượt trước thì người dùng mới hỏi tiếp kiểu "cái đó là gì" được. Nhưng nếu cứ gửi lại toàn bộ lịch sử chat mỗi lượt thì càng chat lâu càng tốn, và đến một lúc sẽ vượt giới hạn độ dài mà model nhận được.

**Giải pháp:**
Bảng `conversation_memories` giữ bộ nhớ **nén hai tầng** — `summarized_context` (đã tóm tắt) + `pending_context` (các lượt mới chưa nén), cùng `context_character_count`. Khi vượt ngưỡng ký tự, `compress()` gọi LLM tóm tắt gộp rồi xoá `pending`. Nhờ vậy prompt gửi đi luôn có ngữ cảnh của các lượt trước nhưng độ dài được giữ trong tầm kiểm soát.

### 8. Phân quyền hai tầng: RBAC cho hành động, ABAC cho từng tài liệu

**Vấn đề:**
Không phải ai trong công ty cũng được đọc mọi tài liệu. Nhưng chỉ dựa vào chức danh thì chưa đủ để trả lời câu hỏi "người này có được xem tài liệu kia không".

Câu trả lời còn phụ thuộc vào ai là người sở hữu tài liệu, tài liệu thuộc phòng ban nào, và tài liệu đó có được chia sẻ riêng cho người này hay không.

**Giải pháp:**

- **RBAC:** bảng `role_permissions` map 4 role (`ADMIN`, `MANAGER`, `SUPERVISOR`, `EMPLOYEE`) tới 35 permission chi tiết (`DOCUMENT_READ`, `AI_SEMANTIC_SEARCH`, `AI_USAGE_READ_DEPARTMENT`...), được seed tự động bởi `RbacInitializer` lúc khởi động. Admin sửa được permission của role qua API mà không cần deploy lại.
- **ABAC:** `DocumentAuthorizationHelper` chứa luật thuần, không chạm repository, quyết định theo thứ tự cố định: **Permission → Ownership → Department scope → Explicit access**. Role `SUPERVISOR` được thiết kế như auditor — đọc được mọi phòng ban nhưng không sửa được gì; explicit share chỉ cấp quyền READ nên mọi thao tác ghi đều bỏ qua nó.
- **Một cổng duy nhất:** `CurrentUserService` là điểm vào duy nhất lấy identity — controller không bao giờ tự truyền `userId` xuống service, chặn hẳn lỗ hổng client tự khai userId.
- **Đẩy filter xuống DB:** `currentAccessScope()` trả về `DocumentAccessScope` để repository lọc bằng query thay vì load hết rồi filter trong bộ nhớ.

### 9. Kết quả tìm kiếm phải đúng quyền và đúng phiên bản hiện hành

**Vấn đề:**
Tìm kiếm không được trả về nội dung của tài liệu mà người dùng không có quyền xem, và cũng không được trích dẫn nội dung từ một bản cũ đã bị thay thế bằng phiên bản mới.

Lý do là Qdrant chỉ biết vector, không biết ai được đọc gì, cũng không biết tài liệu đã bị xoá hay đã có version mới. Trả thẳng kết quả từ Qdrant sẽ rò rỉ nội dung tài liệu của phòng ban khác và trích dẫn nội dung lỗi thời.

**Giải pháp:**
`SemanticSearchServiceImpl` lọc lại **ba lớp** sau khi nhận hits từ Qdrant — (1) `status == ACTIVE`, (2) `filterReadableDocumentIds()` áp đúng luật ABAC ở trên, (3) `isCurrentVersionHit()` đối chiếu `documentVersionId` trong payload với `document.currentVersion.id`, loại bỏ chunk thuộc version cũ. Ở phía RAG, `DocumentQAServiceImpl` cũng **lọc lại quyền ngay trước khi build context** cho LLM, vì quyền có thể đã bị thu hồi sau khi tài liệu được đính kèm vào conversation. Qdrant còn được đặt `score-threshold = 0.65` để loại hits không đủ liên quan thay vì luôn trả đủ top-K.

---

## 🛠️ Công nghệ sử dụng

**Backend**

| Công nghệ | Phiên bản | Vai trò |
|---|---|---|
| Java | 17 | Ngôn ngữ |
| Spring Boot | 4.1.0 | Framework (Web MVC, Data JPA, Validation, Security) |
| Spring Security + JJWT | 0.12.6 | Xác thực JWT, phân quyền |
| Spring Retry | 2.0.13 | Retry lỗi tạm thời trong pipeline xử lý |
| QueryDSL JPA | 5.1.0 | Query động cho filter/phân trang |
| Flyway | Boot BOM | Versioning schema database |
| Lombok | Boot BOM | Giảm boilerplate |

**AI / Vector**

| Công nghệ | Phiên bản | Vai trò |
|---|---|---|
| LangChain4j | 1.4.0 | Lớp trừu tượng gọi LLM và embedding |
| Google Gemini API | — | `gemini-embedding-001` (embedding, 3072 chiều) + Gemini Flash Lite (sinh văn bản) |
| Qdrant | client 1.15.0 | Vector database, collection `document_chunks`, cosine distance |

**Xử lý tài liệu**

| Công nghệ | Phiên bản | Vai trò |
|---|---|---|
| Apache Tika | 3.3.1 | Trích xuất text PDF / DOCX / TXT |
| Apache POI | 5.5.1 | Đọc file Excel (XLSX/XLS) |

**Hạ tầng**

| Công nghệ | Phiên bản | Vai trò |
|---|---|---|
| PostgreSQL | 16 (alpine) | Cơ sở dữ liệu quan hệ |
| MinIO | latest | Object storage tương thích S3, lưu file gốc |
| Docker và Docker Compose | — | Đóng gói và chạy toàn bộ stack |

**Frontend**

| Công nghệ | Phiên bản | Vai trò |
|---|---|---|
| Next.js | 15.5 (App Router) | Framework React |
| React | 19 | UI |
| Tailwind CSS | 4.1 | Styling |
| lucide-react | 0.468 | Icon |

---

## ▶️ Hướng dẫn chạy dự án

### Yêu cầu

- Docker và Docker Compose
- Một **Google Gemini API key** ([lấy tại đây](https://aistudio.google.com/apikey))

### Các bước

**Bước 1.** Clone và tạo file cấu hình từ template:

```bash
cp .env.example .env
cp backend/.env.example backend/.env
```

**Bước 2.** Mở `backend/.env` và điền các giá trị bắt buộc:

- `GEMINI_API_KEY` và `GEMINI_LLM_MODEL_API_KEY` — API key Gemini
- `JWT_ACCESS_SECRET` và `JWT_REFRESH_SECRET` — sinh bằng lệnh:

```bash
openssl rand -base64 64
```

- `POSTGRES_USER` / `POSTGRES_PASSWORD` / `DB_USERNAME` / `DB_PASSWORD` — đặt trùng nhau
- `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` / `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` — đặt trùng nhau
- `ADMIN_USERNAME` / `ADMIN_EMAIL` / `ADMIN_PASSWORD` / `ADMIN_FULL_NAME` — tài khoản admin được tạo tự động ở lần khởi chạy đầu tiên

**Bước 3.** Khởi động:

```bash
docker compose up --build
```

Compose sẽ chờ Postgres, MinIO, Qdrant **healthy** rồi mới khởi động backend, sau đó mới tới frontend. Lần chạy đầu, ứng dụng tự động: chạy Flyway migration tạo schema, tạo bucket MinIO nếu chưa có, tạo collection Qdrant, seed `role_permissions` và tài khoản admin.

**Bước 4.** Truy cập `http://localhost:3000` và đăng nhập bằng tài khoản admin đã cấu hình.

---

## 🚀 Triển khai (Deployment)

Hệ thống đang chạy production trên các dịch vụ cloud free-tier, chi phí vận hành **$0/tháng**.

### Hạ tầng

| Thành phần | Dịch vụ | Ghi chú |
|---|---|---|
| Frontend (Next.js) | **Vercel** (Hobby) | Build từ `frontend/`, biến `NEXT_PUBLIC_API_BASE_URL` nhúng lúc build |
| Backend (Spring Boot) | **Render** (Free, Docker) | Region Singapore, build từ `backend/Dockerfile` |
| Database | **Neon** (PostgreSQL serverless, Free) | Region Singapore — cùng khu vực backend để giảm latency |
| Object storage | **Cloudflare R2** | S3-compatible, thay MinIO ở local, egress miễn phí |
| Vector database | **Qdrant Cloud** (Free) | Kết nối gRPC qua TLS + API key |
| AI models | **Google Gemini API** | Embedding + sinh văn bản |

### URL public

| Service | URL |
|---|---|
| Frontend | https://ai-enterprise-assistant-solo.vercel.app |
| Backend API | https://ai-enterprise-assistant-solo-backend.onrender.com/api/v1 |
| Health check | https://ai-enterprise-assistant-solo-backend.onrender.com/api/v1/health |

> Dự án không tích hợp Swagger/OpenAPI — danh sách endpoint xem ở mục [Danh sách API chính](#-danh-sách-api-chính-endpoints).

### Khác biệt cấu hình so với local

Cùng bộ biến môi trường, chỉ đổi giá trị:

| Biến | Local | Production |
|---|---|---|
| `COOKIE_SECURE` | `false` | `true` — bắt buộc, vì cookie cross-origin Vercel ↔ Render cần `Secure` + `SameSite=None` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | `https://ai-enterprise-assistant-solo.vercel.app` |
| `DB_URL` | `jdbc:postgresql://postgres:5432/...` | Connection string Neon (có `sslmode=require`) |
| `MINIO_ENDPOINT` | `http://minio:9000` | Endpoint S3 API của Cloudflare R2 |
| `QDRANT_HOST` | `qdrant` | Hostname cluster Qdrant Cloud |
| `QDRANT_USE_TLS` | `false` | `true` |
| `QDRANT_API_KEY` | trống | API key của cluster |
| `PORT` | `8080` (mặc định) | Render tự inject |

### Cơ chế duy trì uptime

Free tier có giới hạn idle nên cần giữ dịch vụ "thức":

- **Backend:** cron job (cron-job.org) ping `GET /api/v1/health` mỗi **12 phút** — tránh Render tự sleep sau 15 phút không hoạt động.
- **Qdrant Cloud:** cron job riêng ping REST API mỗi ngày — tránh cluster bị suspend sau 7 ngày idle và bị xoá sau 28 ngày.
- Neon và Vercel không cần cơ chế này (tự bảo toàn dữ liệu khi idle).

---

## 🔌 Các cổng dịch vụ và Truy cập (local)

| Service | Port | URL local |
|---|---|---|
| Frontend (Next.js) | 3000 | `http://localhost:3000` |
| Backend API | 8080 | `http://localhost:8080/api/v1` |
| Health check | 8080 | `http://localhost:8080/api/v1/health` |
| PostgreSQL | 5432 | `localhost:5432` |
| MinIO API | 9000 | `http://localhost:9000` |
| MinIO Console | 9001 | `http://localhost:9001` |
| Qdrant HTTP / Dashboard | 6333 | `http://localhost:6333/dashboard` |
| Qdrant gRPC | 6334 | `localhost:6334` |

Toàn bộ port host đều đổi được qua biến `*_HOST_PORT` trong `.env` ở thư mục gốc mà không cần sửa `docker-compose.yml`.

---

## 🐳 Cấu trúc Services trong docker-compose.yml

| Service | Image / Build | Vai trò | Volume | Healthcheck |
|---|---|---|---|---|
| `postgres` | `postgres:16-alpine` | Lưu toàn bộ dữ liệu quan hệ | `postgres_data` | `pg_isready` |
| `minio` | `minio/minio:latest` | Object storage lưu file gốc + console quản trị | `minio_data` | `/minio/health/live` |
| `qdrant` | `qdrant/qdrant:latest` | Vector database cho semantic search | `qdrant_data` | TCP check cổng 6333 |
| `backend` | Build từ `./backend` | Spring Boot API | — | — |
| `frontend` | Build từ `./frontend` | Next.js UI | — | — |

**Thứ tự khởi động:** `backend` khai báo `depends_on` với `condition: service_healthy` cho cả ba hạ tầng — không khởi động cho tới khi Postgres, MinIO và Qdrant đều sẵn sàng, tránh lỗi kết nối lúc boot. `frontend` phụ thuộc `backend`.

**Biến build của frontend:** `NEXT_PUBLIC_API_BASE_URL` được truyền vào dưới dạng `build.args` chứ không phải biến runtime, vì Next.js nhúng biến `NEXT_PUBLIC_*` vào bundle ngay lúc build.

---

## 📡 Danh sách API chính (Endpoints)

Tất cả endpoint có tiền tố `/api/v1`. Trừ nhóm Authentication và health check, mọi endpoint đều yêu cầu access token hợp lệ.

### 🔐 Authentication

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/auth/register` | Đăng ký tài khoản mới (role mặc định `EMPLOYEE`), tự động đăng nhập |
| `POST` | `/auth/login` | Đăng nhập, trả access token + set refresh token vào httpOnly cookie |
| `POST` | `/auth/refresh-token` | Cấp cặp token mới, thu hồi refresh token cũ (rotation) |
| `POST` | `/auth/logout` | Thu hồi refresh token hiện tại |

### 👤 User và Department

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/users/me` | Thông tin tài khoản đang đăng nhập |
| `PUT` | `/users/me` | Cập nhật hồ sơ cá nhân |
| `GET` | `/departments` | Danh sách phòng ban (phân trang) |
| `GET` | `/departments/me` | Chi tiết phòng ban của mình |
| `GET` | `/departments/{departmentId}` | Chi tiết một phòng ban |

### 📄 Document

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/documents/upload` | Upload nhiều tài liệu cùng lúc (multipart), kích hoạt pipeline xử lý nền |
| `POST` | `/documents/{documentId}/versions` | Upload phiên bản mới cho tài liệu đã có |
| `GET` | `/documents` | Danh sách tài liệu theo phạm vi quyền của user (phân trang, filter) |
| `GET` | `/documents/{documentId}` | Chi tiết tài liệu kèm lịch sử phiên bản |
| `GET` | `/documents/check-title` | Kiểm tra tiêu đề đã tồn tại chưa |
| `PUT` | `/documents/{documentId}` | Cập nhật metadata (tiêu đề, mô tả, loại tài liệu) |
| `PUT` | `/documents/{documentId}/move` | Di chuyển tài liệu sang thư mục khác |
| `GET` | `/documents/{documentId}/{versionId}/download` | Tải xuống một phiên bản cụ thể |
| `DELETE` | `/documents/{documentId}` | Xoá mềm tài liệu |
| `POST` | `/documents/{documentId}/restore` | Khôi phục tài liệu đã xoá mềm |
| `POST` | `/documents/{documentId}/shares` | Chia sẻ tài liệu cho user cụ thể |
| `GET` | `/documents/{documentId}/shares` | Danh sách user đang được chia sẻ |
| `DELETE` | `/documents/{documentId}/shares/{targetUserId}` | Thu hồi quyền chia sẻ |

### 📁 Folder

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/folders` | Tạo thư mục |
| `PUT` | `/folders/{folderId}` | Đổi tên thư mục |
| `PUT` | `/folders/{folderId}/move` | Di chuyển thư mục |
| `GET` | `/folders/{folderId}` | Chi tiết thư mục |
| `GET` | `/folders/{folderId}/contents` | Nội dung thư mục (thư mục con + tài liệu) |
| `GET` | `/folders/root/contents` | Nội dung thư mục gốc |
| `GET` | `/folders/search` | Tìm thư mục theo tên (phân trang) |
| `GET` | `/folders/deleted` | Danh sách thư mục đã xoá mềm |
| `DELETE` | `/folders/{folderId}` | Xoá mềm thư mục |
| `POST` | `/folders/{folderId}/restore` | Khôi phục thư mục |
| `DELETE` | `/folders/{folderId}/hard` | Xoá vĩnh viễn thư mục |

### 💬 Conversation và Message

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/ai-conversations/document-qa/start` | Tạo cuộc hội thoại hỏi đáp tài liệu |
| `POST` | `/ai-conversations/generation/start` | Tạo cuộc hội thoại sinh nội dung |
| `GET` | `/ai-conversations` | Danh sách hội thoại (cuộn vô hạn) |
| `GET` | `/ai-conversations/deleted` | Danh sách hội thoại đã xoá mềm |
| `GET` | `/ai-conversations/{conversationId}` | Chi tiết hội thoại hỏi đáp |
| `GET` | `/ai-conversations/{conversationId}/generation-detail` | Chi tiết hội thoại sinh nội dung |
| `PUT` | `/ai-conversations/{conversationId}` | Đổi tiêu đề hội thoại |
| `DELETE` | `/ai-conversations/{conversationId}` | Xoá mềm hội thoại |
| `POST` | `/ai-conversations/{conversationId}/restore` | Khôi phục hội thoại |
| `DELETE` | `/ai-conversations/{conversationId}/hard` | Xoá vĩnh viễn hội thoại |
| `POST` | `/ai-conversations/{conversationId}/documents` | Đính kèm tài liệu vào hội thoại |
| `GET` | `/ai-conversations/{conversationId}/documents` | Danh sách tài liệu đính kèm |
| `DELETE` | `/ai-conversations/{conversationId}/documents/{documentVersionId}` | Gỡ tài liệu khỏi hội thoại |
| `POST` | `/ai-conversations/{conversationId}/messages` | Gửi tin nhắn — định tuyến theo intent, trả lời kèm nguồn trích dẫn |
| `GET` | `/ai-conversations/{conversationId}/messages` | Lịch sử tin nhắn (phân trang bằng con trỏ `beforeId`) |
| `GET` | `/ai-conversations/{conversationId}/messages/{messageId}` | Chi tiết tin nhắn kèm các chunk nguồn và điểm tương đồng |

### 🔍 Semantic Search

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/search/semantic` | Tìm kiếm ngữ nghĩa, lọc theo quyền + chỉ trả chunk của phiên bản hiện hành |

### ✍️ Generation

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/ai-conversations/{conversationId}/generate` | Sinh email / báo cáo / biên bản / tóm tắt / biểu mẫu |
| `GET` | `/ai-conversations/generations/{generationId}` | Chi tiết một lượt sinh nội dung |
| `GET` | `/ai-conversations/{conversationId}/generations` | Danh sách lượt sinh trong một hội thoại |
| `GET` | `/generated-contents` | Danh sách nội dung đã sinh |
| `GET` | `/generated-contents/{generatedContentId}` | Chi tiết nội dung đã sinh |
| `PUT` | `/generated-contents/{generatedContentId}` | Chỉnh sửa nội dung đã sinh |

### 📊 AI Usage

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/ai-usage` | Nhật ký gọi AI theo phạm vi quyền của user (phân trang, filter) |
| `GET` | `/ai-usage/summary` | Tổng hợp token và chi phí hôm nay và 7 ngày gần nhất |
| `GET` | `/ai-usage/daily` | Thống kê theo ngày (mặc định 7 ngày) |
| `GET` | `/ai-usage/models` | Danh sách model đã được sử dụng |

### ⚙️ Admin

Toàn bộ nhóm này yêu cầu role `ADMIN` (chặn ở cả filter lẫn service layer).

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/admin/users` · `/admin/users/{userId}` | Danh sách / chi tiết người dùng |
| `POST` | `/admin/users` | Tạo người dùng |
| `PUT` | `/admin/users/{userId}` | Cập nhật người dùng |
| `PUT` | `/admin/users/{userId}/enable` · `/disable` | Bật / tắt tài khoản |
| `PUT` | `/admin/users/{userId}/role` | Gán role |
| `PUT` | `/admin/users/{userId}/department` | Chuyển phòng ban |
| `DELETE` | `/admin/users/{userId}` | Xoá người dùng |
| `GET` | `/admin/roles` | Danh sách role kèm permission hiện tại |
| `GET` | `/admin/roles/permissions` | Toàn bộ permission có thể gán |
| `PUT` | `/admin/roles/{role}/permissions` | Cập nhật permission của một role |
| `GET` | `/admin/departments` · `/{departmentId}` | Danh sách / chi tiết phòng ban |
| `POST` `PUT` `DELETE` | `/admin/departments` · `/{departmentId}` | Tạo / sửa / xoá phòng ban |
| `PUT` | `/admin/departments/{departmentId}/manager` | Gán trưởng phòng |
| `POST` `DELETE` | `/admin/departments/{departmentId}/members` · `/members/{userId}` | Thêm / gỡ thành viên |
| `GET` | `/admin/documents` · `/{documentId}` | Danh sách / chi tiết mọi tài liệu trong hệ thống |
| `GET` | `/admin/documents/{documentId}/shares` | Ai đang được chia sẻ tài liệu |
| `GET` | `/admin/documents/{documentId}/{versionId}/download` | Tải xuống bất kỳ phiên bản nào |
| `DELETE` `POST` | `/admin/documents/{documentId}` · `/restore` | Xoá / khôi phục tài liệu |
| `GET` | `/admin/shared-documents` | Toàn bộ quyền chia sẻ trong hệ thống |
| `DELETE` | `/admin/shared-documents/{documentId}/users/{targetUserId}` | Thu hồi quyền chia sẻ |
| `GET` | `/admin/trash` | Thùng rác toàn hệ thống |
| `POST` | `/admin/trash/documents/{documentId}/restore` · `/folders/{folderId}/restore` | Khôi phục |
| `DELETE` | `/admin/trash/folders/{folderId}` | Xoá vĩnh viễn thư mục |
| `GET` | `/admin/ai-usage` · `/overview` · `/models` | Thống kê chi phí AI toàn hệ thống |

---

## 🗄️ Cơ sở dữ liệu (Database Schema)

Schema được quản lý bằng **Flyway** (`V1__init_schema.sql`), Hibernate chạy ở chế độ `ddl-auto=validate` — ứng dụng không tự sửa schema, mọi thay đổi phải qua migration.

### 🔐 Người dùng, xác thực và phân quyền

| Bảng | Cột đáng chú ý | Khóa ngoại | Mô tả |
| :--- | :--- | :--- | :--- |
| `users` | `username` (unique), `email` (unique), `password` (BCrypt hash), `full_name`, `enabled`, `role` (CHECK: `ADMIN`/`MANAGER`/`SUPERVISOR`/`EMPLOYEE`) | → `departments.id` | Tài khoản người dùng hệ thống |
| `refresh_tokens` | `id` kiểu **UUID**, `jti` (unique), `token_hash` (unique, SHA-256), `revoked`, `expires_at`, thời gian dạng `timestamptz` | → `users.id` | Refresh token đang hiệu lực; chỉ lưu hash, không lưu token thô |
| `role_permissions` | `role` (CHECK 4 giá trị), `permission` (CHECK 35 giá trị), unique `(role, permission)` | — | Bảng map RBAC, seed tự động lúc khởi động |
| `departments` | `name` (unique), `description` | → `users.id` (trưởng phòng) | Phòng ban, dùng làm phạm vi ABAC |

### 📁 Tài liệu và lưu trữ

| Bảng | Cột đáng chú ý | Khóa ngoại | Mô tả |
| :--- | :--- | :--- | :--- |
| `documents` | `title`, `description`, `document_type` (CHECK 6 giá trị), `status` (CHECK: `ACTIVE`/`DELETED`), `deleted_at`, `current_version_id` (**unique**) | → `document_versions.id`, → `folders.id`, → `users.id` (owner), → `departments.id`, → `users.id` (người xoá) | Tài liệu logic — bản thân không chứa file, trỏ tới phiên bản hiện hành |
| `document_versions` | `version_number`, `status` (CHECK: `PENDING`/`PROCESSING`/`READY`/`FAILED`), `processing_step` (CHECK: `TEXT_EXTRACTING`/`CHUNKING`/`EMBEDDING`), `error_message`, `change_note`, unique `(document_id, version_number)` | → `documents.id`, → `files.id` (**unique**) | Từng phiên bản tài liệu, mang trạng thái pipeline xử lý |
| `files` | `original_filename`, `stored_filename`, `object_key`, `bucket_name`, `mime_type`, `extension`, `file_size`, `checksum`, `storage_provider` | → `files.id` (tự tham chiếu, file preview) | Metadata file vật lý trên object storage |
| `document_texts` | `content` (text đã trích xuất), `extraction_method` (CHECK: `DIRECT_TEXT`/`OCR`/`MANUAL`/`AI_ENHANCED`), `language` | → `document_versions.id` (**unique**) | Toàn văn trích xuất, quan hệ 1–1 với version |
| `document_chunks` | `chunk_index`, `content`, `token_count`, `page_number`, `start_char`, `end_char`, unique `(document_version_id, chunk_index)` | → `document_versions.id` | Chunk phục vụ embedding; `id` chính là point ID trên Qdrant |
| `folders` | `name`, `status` (CHECK: `ACTIVE`/`DELETED`), `deleted_at` | → `folders.id` (tự tham chiếu, cây thư mục), → `users.id` (owner), → `departments.id`, → `users.id` (người xoá) | Cây thư mục chứa tài liệu |
| `document_accesses` | — | → `documents.id`, → `users.id`, → `users.id` (người cấp quyền) | Bảng nối n–n giữa `documents` và `users`, thể hiện quyền chia sẻ tường minh (chỉ cấp quyền đọc) |

### 💬 Hội thoại và hỏi đáp

| Bảng | Cột đáng chú ý | Khóa ngoại | Mô tả |
| :--- | :--- | :--- | :--- |
| `ai_conversations` | `title`, `conversation_type` (CHECK 8 giá trị), `status` (CHECK: `ACTIVE`/`DELETED`), `deleted_at` | → `users.id` | Cuộc hội thoại với AI |
| `ai_messages` | `content`, `role` (CHECK: `USER`/`ASSISTANT`/`SYSTEM`), `token_count` | → `ai_conversations.id` | Từng tin nhắn trong hội thoại |
| `ai_message_sources` | `chunk_id`, `document_version_id`, `similarity_score`, `score` | → `ai_messages.id`, → `document_chunks.id` | Nguồn trích dẫn của câu trả lời AI — cơ sở để người dùng kiểm chứng |
| `ai_conversation_documents` | — | → `ai_conversations.id`, → `document_versions.id` | Bảng nối n–n giữa `ai_conversations` và `document_versions`, unique `(ai_conversation_id, document_version_id)` |
| `conversation_memories` | `summarized_context` (đã nén), `pending_context` (lượt mới), `context_character_count` | → `ai_conversations.id` (**unique**) | Bộ nhớ hội thoại nén hai tầng, quan hệ 1–1 với hội thoại |

### ✍️ Sinh nội dung

| Bảng | Cột đáng chú ý | Khóa ngoại | Mô tả |
| :--- | :--- | :--- | :--- |
| `generations` | `generated_type` (CHECK 5 giá trị), `status` (CHECK: `PENDING`/`RUNNING`/`COMPLETED`/`CANCELLED`/`FAILED`), `input_data` (**jsonb**), `user_prompt`, `error_message`, `deleted`, `deleted_at` | → `ai_conversations.id`, → `generated_content.id` (**unique**) | Một lượt yêu cầu sinh nội dung và trạng thái của nó |
| `generated_content` | `title`, `content`, `generated_type` (CHECK 5 giá trị) | — | Nội dung AI đã sinh, tách riêng để người dùng sửa lại được |

### 📊 Thống kê chi phí AI

| Bảng | Cột đáng chú ý | Khóa ngoại | Mô tả |
| :--- | :--- | :--- | :--- |
| `ai_usage_logs` | `model`, `input_tokens`, `output_tokens`, `total_tokens`, `estimated_cost` (`numeric(12,6)`), `status` (CHECK: `SUCCESS`/`FAILED`), `conversation_type` (`smallint`, CHECK `0..7`), `error_message`; các cột `user_id`, `department_id`, `generation_id` **không có ràng buộc khóa ngoại** | → `ai_conversations.id`, → `ai_messages.id` | Nhật ký mọi lượt gọi AI (embedding, LLM, phân loại intent) để theo dõi token và chi phí |

### Quyết định thiết kế đáng chú ý

- **Không dùng `ON DELETE CASCADE` ở bất kỳ khóa ngoại nào.** Toàn hệ thống dùng **xoá mềm** (`status` + `deleted_at` + `deleted_by`) thay vì xoá cứng, nên ràng buộc mặc định `NO ACTION` là chủ ý: nó chặn việc xoá cứng làm mất lịch sử hội thoại và trích dẫn đang tham chiếu tới tài liệu.
- **`ai_usage_logs` cố tình không đặt FK cho `user_id` / `department_id` / `generation_id`.** Đây là bảng nhật ký, cần ghi được cả khi tiến trình nền không có user đăng nhập, và cần sống sót độc lập với vòng đời của bản ghi gốc.
- **`documents.current_version_id` là UNIQUE** — một phiên bản chỉ có thể là bản hiện hành của đúng một tài liệu, chặn sai lệch dữ liệu ở tầng DB thay vì tin vào code.
- **`document_chunks` unique `(document_version_id, chunk_index)`** — pipeline có retry tới 3 lần, ràng buộc này đảm bảo chạy lại không sinh chunk trùng.
- **`document_versions` unique `(document_id, version_number)`** và **`file_id` unique** — mỗi file vật lý thuộc đúng một phiên bản, không tái sử dụng chéo.
- **`refresh_tokens` unique cả `jti` lẫn `token_hash`**, dùng `uuid` làm khóa chính thay vì số tăng dần để token định danh không đoán được.
- **Không dùng optimistic locking** (không có cột `version`). Điểm tranh chấp ghi thực sự duy nhất là `conversation_memories`, và nó được xử lý bằng **pessimistic lock** (`@Lock(PESSIMISTIC_WRITE)` trong `ConversationMemoryRepository`).
- **Enum lưu dạng chuỗi kèm `CHECK` constraint** thay vì ordinal, để dữ liệu đọc được trực tiếp trong DB và không vỡ khi thứ tự enum trong code thay đổi. Riêng `ai_usage_logs.conversation_type` là ngoại lệ dùng `smallint` (CHECK `0..7`).

---

## ⚙️ Cấu hình biến môi trường (.env)

Dự án dùng **ba** file cấu hình, mỗi file có `.env.example` đi kèm.

### `backend/.env` — cấu hình backend, Postgres và MinIO

**Kết nối database (Postgres)**

| Biến | Mô tả |
|---|---|
| `POSTGRES_DB` | Tên database container Postgres khởi tạo |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | Tài khoản khởi tạo container Postgres |
| `DB_URL` | JDBC URL backend dùng để kết nối |
| `DB_USERNAME` / `DB_PASSWORD` | Tài khoản backend dùng để kết nối (đặt trùng cặp trên) |

**Object storage (MinIO)**

| Biến | Mô tả |
|---|---|
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | Tài khoản quản trị MinIO, dùng đăng nhập console |
| `MINIO_ENDPOINT` | Địa chỉ MinIO backend gọi tới |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | Khoá truy cập S3 API |
| `MINIO_BUCKET` | Tên bucket lưu file (tự tạo nếu chưa có) |

**Vector database (Qdrant)**

| Biến | Mô tả |
|---|---|
| `QDRANT_HOST` | Hostname Qdrant |
| `QDRANT_GRPC_PORT` / `QDRANT_REST_PORT` | Cổng gRPC (dùng để upsert/search) và REST |
| `QDRANT_API_KEY` | API key — để trống khi chạy Qdrant local |
| `QDRANT_USE_TLS` | Bật TLS, cần `true` khi dùng Qdrant Cloud (mặc định `false`) |

**AI (Google Gemini)**

| Biến | Mô tả |
|---|---|
| `GEMINI_API_KEY` | API key cho embedding |
| `GEMINI_EMBEDDING_MODEL` | Model embedding, mặc định `gemini-embedding-001` |
| `GEMINI_LLM_MODEL_API_KEY` | API key cho LLM sinh văn bản |
| `GEMINI_LLM_MODEL_NAME` | Tên model LLM |

**Bảo mật (JWT và Cookie)**

| Biến | Mô tả |
|---|---|
| `JWT_ACCESS_SECRET` / `JWT_REFRESH_SECRET` | Khoá ký hai loại token — sinh bằng `openssl rand -base64 64` |
| `JWT_ACCESS_EXPIRATION` | Hạn access token, mặc định `3600000` ms (1 giờ) |
| `JWT_REFRESH_EXPIRATION` | Hạn refresh token, mặc định `604800000` ms (7 ngày) |
| `COOKIE_SECURE` | Bật cờ `Secure` cho cookie — **bắt buộc `true` trên HTTPS production** |
| `CORS_ALLOWED_ORIGINS` | Danh sách domain frontend được phép gọi API |

**Tài khoản admin khởi tạo**

| Biến | Mô tả |
|---|---|
| `ADMIN_USERNAME` / `ADMIN_EMAIL` / `ADMIN_PASSWORD` / `ADMIN_FULL_NAME` | Tài khoản `ADMIN` được seed tự động ở lần khởi chạy đầu tiên, tránh tình huống "cần ADMIN để cấp quyền ADMIN" trên database rỗng |

### `.env` (thư mục gốc) — ánh xạ cổng và biến build frontend

| Biến | Mô tả |
|---|---|
| `POSTGRES_HOST_PORT`, `MINIO_API_HOST_PORT`, `MINIO_CONSOLE_HOST_PORT`, `QDRANT_HTTP_HOST_PORT`, `QDRANT_GRPC_HOST_PORT`, `BACKEND_HOST_PORT`, `FRONTEND_HOST_PORT` | Cổng phía host cho từng service — đổi khi bị trùng cổng trên máy |
| `NEXT_PUBLIC_API_BASE_URL` | URL API được **nhúng vào bundle frontend lúc build** |

### `frontend/.env.local` — chỉ khi chạy frontend ngoài Docker

| Biến | Mô tả |
|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | URL backend, mặc định `http://localhost:8080/api/v1` |

> ⚠️ Cả ba file `.env` đều đã được `.gitignore`. Không commit khoá thật lên repository.

---

## 👥 Nhóm thực hiện và Phân công

### Thành viên nhóm

| STT | Họ tên | MSSV | Vai trò |
|---|---|---|---|
| 1 | Lê Võ | N22DCCN097 | Nhóm trưởng |
| 2 | Trần Nhật Nguyên | N22DCCN057 | Thành viên |
| 3 | Nguyễn Ngọc Huy | N22DCCN036 | Thành viên |

### Phần thiết kế hệ thống

| Hạng mục | Phụ trách chính | Đóng góp ý kiến / Review |
|---|---|---|
| Frontend | Lê Võ | Trần Nhật Nguyên, Nguyễn Ngọc Huy |
| Logic nghiệp vụ và kiến trúc hệ thống | Trần Nhật Nguyên, Lê Võ | Nguyễn Ngọc Huy |
| Viết báo cáo | Nguyễn Ngọc Huy (tổng hợp) | Trần Nhật Nguyên, Lê Võ (review) |

### Phân chia công việc theo giai đoạn

> Mốc thời gian là ước lượng, tổng cộng khoảng 1 tháng tính đến hiện tại.

#### Giai đoạn 1 — Nền tảng hệ thống (07/07 – 13/07/2026) ✅ Đã xong

| Thành viên | Công việc phụ trách |
|---|---|
| Nguyễn Ngọc Huy | Module Document: xem danh sách và chi tiết tài liệu (phân trang) · Module Processing: xử lý file Excel, điều phối worker xử lý tài liệu |
| Lê Võ | Module Document: upload tài liệu · Module Storage · Module Processing: trích xuất text, xử lý PDF, điều phối worker · Module Chunking |
| Trần Nhật Nguyên | Module Document: tải xuống, xoá tài liệu · Module Storage · Module Processing: chia chunk, xử lý DOCX, điều phối worker · Module Chunking |

#### Giai đoạn 2 — Conversation và Message (14/07 – 20/07/2026) ✅ Đã xong

| Thành viên | Công việc phụ trách |
|---|---|
| Lê Võ | Quản lý Conversation: tạo, đổi tiêu đề, xoá; gắn/gỡ tài liệu đính kèm vào conversation |
| Nguyễn Ngọc Huy | Danh sách và chi tiết conversation (phân trang) |
| Trần Nhật Nguyên, Nguyễn Ngọc Huy | Module Message: lịch sử tin nhắn, chi tiết tin nhắn kèm nguồn trích dẫn |

#### Giai đoạn 3 — Embedding / Search / Generation nền tảng / Usage Log (21/07 – 27/07/2026) ✅ Đã xong

| Thành viên | Công việc phụ trách |
|---|---|
| Lê Võ | Embedding service, Semantic Search |
| Nguyễn Ngọc Huy | AI Usage Log: theo dõi token, chi phí, thống kê sử dụng AI |
| Trần Nhật Nguyên | Các chức năng liên quan đến Generation (entity, CRUD nền tảng) |

#### Giai đoạn 4 — AI Generation và Hoàn thiện (28/07 – 04/08/2026) ✅ Đã xong

| Thành viên | Công việc phụ trách |
|---|---|
| Lê Võ | Triển khai LLM, Document QA (RAG) |
| Nguyễn Ngọc Huy | Report Generation (sinh báo cáo) · Module Folder |
| Trần Nhật Nguyên | Summary Generation (tóm tắt), Email Generation (soạn email) |

#### Giai đoạn 5 — Authentication, Permission, Admin và Cải thiện module QA (05/08 – 11/08/2026) ✅ Đã xong

| Thành viên | Công việc phụ trách |
| --- | --- |
| Nguyễn Ngọc Huy | Module Admin: quản lý và thống kê hệ thống · Hoàn thiện Module Folder |
| Lê Võ | Cải thiện module QA: Conversation Context (AI có khả năng nhớ context từ các message trước), General Chat · Module Permission: ABAC (Attribute-Based Access Control) |
| Trần Nhật Nguyên | Module Authentication: đăng nhập, xác thực người dùng, JWT, Refresh/Access Token · Module Permission: RBAC (Role-Based Access Control) |

#### Giai đoạn 6 — Hoàn thiện (12/08 – đến nay) 🔄 Đang thực hiện

| Thành viên | Công việc phụ trách |
| --- | --- |
| Nguyễn Ngọc Huy | Hoàn thiện Admin · Hoàn thiện thống kê AI Usage |
| Lê Võ | Hoàn thiện Permission: ABAC |
| Trần Nhật Nguyên | Hoàn thiện Authentication · Hoàn thiện Permission RBAC |

> **Ghi chú:** Module **Department** và **Share** (chức năng cho phép người dùng chia sẻ tài liệu cho nhau) hiện vẫn đang được phát triển tiếp và chưa hoàn thành.
