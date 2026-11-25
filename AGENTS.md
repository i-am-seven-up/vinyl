# Quy tắc làm việc trong Vinyl

- Trả lời ngắn gọn, dễ hiểu. K.I.S.S.
- Khi thêm/sửa code, đọc `docs/MVP_SCAFFOLD_PLAN.md`, đặc biệt mục 3 và 4 về cấu trúc, vị trí file và convention Kotlin. Đây là cấu trúc đích; chỉ tạo phần task hiện tại cần.
- Một module `:app`; package tính năng nằm ngang hàng với `shared`. Shared chỉ chứa phần dùng chung có trách nhiệm rõ ràng và không phụ thuộc package tính năng.
- Giữ logic đơn giản, ít abstraction; không tự thêm Base class, UseCase, cặp interface/implementation hoặc lớp model/mapper trùng lặp.
- Main class theo tên file PascalCase; khai báo nhỏ liên quan có thể cùng file. UiState top-level cùng ViewModel; Route/Screen/private component/Preview cùng file Screen. Chi tiết và ngoại lệ ở mục 4 của plan.
- Nếu PLANNING.md gốc khác kế hoạch scaffold, áp dụng quyết định cấu trúc và độ đơn giản trong MVP_SCAFFOLD_PLAN.md; giữ phạm vi tính năng theo yêu cầu đang thực hiện.
- Tuân thủ hợp đồng hàm tại mục 4.4: get/find/observe chỉ đọc; command set/create/update/delete/play/scan trả Unit, không kết hợp đọc và sửa ngầm. Lỗi command được báo rõ, không đổi thành Boolean hoặc bị nuốt.
- Tuân thủ visibility tại mục 4.5: local/private trước, internal khi cần giữa file; chỉ mở rộng theo caller thật hoặc framework. Giữ mutable state và dependency fields private, expose state read-only.
