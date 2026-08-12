# Enterprise AI Document Assistant

## 1. Project Overview

**Enterprise AI Document Assistant** là hệ thống hỗ trợ quản lý và khai thác tài liệu doanh nghiệp với sự hỗ trợ của AI: upload tài liệu, tự động xử lý (trích xuất, chia nhỏ, tạo embedding), sau đó cho phép tìm kiếm ngữ nghĩa, hỏi đáp trực tiếp trên nội dung tài liệu (RAG), hoặc yêu cầu AI soạn sẵn email, báo cáo, tóm tắt, biểu mẫu dựa trên tài liệu đã upload.

**Stack chính:** Java 17, Spring Boot 4.1, PostgreSQL, MinIO (object storage), Qdrant (vector DB), Google Gemini qua langchain4j (embedding + LLM).

## 2. Thành viên nhóm và phân công

| STT | Họ tên | MSSV | Vai trò |
|---|---|---|---|
| 1 | Lê Võ | N22DCCN097 | Nhóm trưởng |
| 2 | Trần Nhật Nguyên | N22DCCN057 | Thành viên |
| 3 | Nguyễn Ngọc Huy | N22DCCN036 | Thành viên |

### Phần thiết kế hệ thống

| Hạng mục | Phụ trách chính | Đóng góp ý kiến / Review |
|---|---|---|
| Frontend | Lê Võ | Trần Nhật Nguyên, Nguyễn Ngọc Huy |
| Logic nghiệp vụ & kiến trúc hệ thống | Trần Nhật Nguyên, Lê Võ | Nguyễn Ngọc Huy |
| Viết báo cáo | Nguyễn Ngọc Huy (tổng hợp) | Trần Nhật Nguyên, Lê Võ (review) |

## 3. Vấn đề & Giải pháp

**Vấn đề:**
- Nhân viên doanh nghiệp mất nhiều thời gian tìm kiếm thông tin nằm rải rác trong nhiều tài liệu nội bộ (hợp đồng, báo cáo, biên bản...).
- Tìm kiếm theo từ khóa truyền thống không hiểu ngữ nghĩa, dễ bỏ sót thông tin liên quan.
- Soạn thảo thủ công các văn bản lặp lại (email, báo cáo, tóm tắt) tốn thời gian và thiếu nhất quán.

**Giải pháp:**
- Xây dựng pipeline xử lý tài liệu tự động: trích xuất text → chia nhỏ (chunking) → tạo embedding, lưu vào Qdrant để phục vụ tìm kiếm ngữ nghĩa (semantic search).
- Tích hợp RAG (Retrieval-Augmented Generation): người dùng hỏi đáp trực tiếp trên tài liệu, câu trả lời có trích dẫn nguồn (chunk cụ thể) để đảm bảo độ tin cậy.
- Dùng LLM (Gemini) để tự động sinh email, báo cáo, tóm tắt, biểu mẫu dựa trên tài liệu và yêu cầu của người dùng, giảm thời gian soạn thảo thủ công.
- Ghi log mọi lượt gọi AI (token, chi phí, trạng thái) để theo dõi và kiểm soát chi phí vận hành.

## 4. Phân chia công việc theo giai đoạn

> Mốc thời gian là ước lượng, tổng cộng khoảng 1 tháng tính đến hiện tại.

### Giai đoạn 1 — Nền tảng hệ thống (07/07 – 13/07/2026) ✅ Đã xong

| Thành viên | Công việc phụ trách |
|---|---|
| Nguyễn Ngọc Huy | Module Document: xem danh sách & chi tiết tài liệu (phân trang) · Module Processing: xử lý file Excel, điều phối worker xử lý tài liệu |
| Lê Võ | Module Document: upload tài liệu · Module Storage · Module Processing: trích xuất text, xử lý PDF, điều phối worker · Module Chunking |
| Trần Nhật Nguyên | Module Document: tải xuống, xoá tài liệu · Module Storage · Module Processing: chia chunk, xử lý DOCX, điều phối worker · Module Chunking |

### Giai đoạn 2 — Conversation & Message (14/07 – 20/07/2026) ✅ Đã xong

| Thành viên | Công việc phụ trách |
|---|---|
| Lê Võ | Quản lý Conversation: tạo, đổi tiêu đề, xoá; gắn/gỡ tài liệu đính kèm vào conversation |
| Nguyễn Ngọc Huy | Danh sách & chi tiết conversation (phân trang) |
| Trần Nhật Nguyên, Nguyễn Ngọc Huy | Module Message: lịch sử tin nhắn, chi tiết tin nhắn kèm nguồn trích dẫn |

### Giai đoạn 3 — Embedding / Search / Generation nền tảng / Usage Log (21/07 – 27/07/2026) ✅ Đã xong
 
| Thành viên | Công việc phụ trách |
|---|---|
| Lê Võ | Embedding service, Semantic Search |
| Nguyễn Ngọc Huy | AI Usage Log: theo dõi token, chi phí, thống kê sử dụng AI |
| Trần Nhật Nguyên | Các chức năng liên quan đến Generation (entity, CRUD nền tảng) |
 
### Giai đoạn 4 — AI Generation & Hoàn thiện (28/07 – 04/08/2026) ✅ Đã xong
 
| Thành viên | Công việc phụ trách |
|---|---|
| Lê Võ | Triển khai LLM, Document QA (RAG) |
| Nguyễn Ngọc Huy | Report Generation (sinh báo cáo) |
| Trần Nhật Nguyên | Summary Generation (tóm tắt), Email Generation (soạn email) |

### Giai đoạn 5 — Authentication, Permission & Admin (05/08 – 11/08/2026) ✅ Đã xong

| Thành viên | Công việc phụ trách |
| --- | --- |
| Nguyễn Ngọc Huy | Module Admin: quản lý và thống kê hệ thống · Hoàn thiện Module Folder |
| Lê Võ | Module Permission: thiết kế và triển khai cơ chế phân quyền · Quản lý quyền truy cập tài liệu theo User/Department |
| Trần Nhật Nguyên | Module Authentication: đăng nhập, xác thực người dùng, JWT/Refresh Token · Hỗ trợ triển khai Permission |

### Giai đoạn 6 — Hoàn thiện, tích hợp & kiểm thử (12/08 – 18/08/2026) ✅ Đã xong

| Thành viên | Công việc phụ trách |
| --- | --- |
| Nguyễn Ngọc Huy | Hoàn thiện Admin · Hoàn thiện thống kê AI Usage · Kiểm thử các chức năng quản trị |
| Lê Võ | Hoàn thiện Permission & Document Access Control · Tích hợp phân quyền với các module Document, Conversation và Generation |
| Trần Nhật Nguyên | Hoàn thiện Authentication · Tích hợp JWT/Refresh Token với hệ thống · Kiểm thử Authentication & Authorization |
