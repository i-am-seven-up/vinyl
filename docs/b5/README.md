# B5 — Search, Settings và base MVP

PR: [#6 — B5 search/settings/library](https://github.com/i-am-seven-up/vinyl/pull/6) → `main`.

Ngày bàn giao: 26/11/2026. Branch `codex/b5-search-settings`, từ `origin/main` sau B4/PR #5 merge.

## Phạm vi

- Tab Tìm kiếm: tên bài/nghệ sĩ/album trên cột chuẩn hóa Room, không dấu và không phân biệt hoa thường. Debounce 300 ms, hủy query cũ, chặn kết quả cũ ghi đè input mới. Chuỗi rỗng không truy vấn toàn thư viện. `%`, `_`, `!` được escape như ký tự thường; bind tham số.
- Settings: theme Hệ thống/Sáng/Tối lưu bằng DataStore Preferences, áp dụng toàn app và icon system bar; rescan có trạng thái đang quét/thành công/lỗi/thử lại và luồng xin quyền/mở Android Settings.
- Library: Album/Bài hát/Nghệ sĩ, detail nghệ sĩ và sort tên A–Z/Z–A. Nhóm nghệ sĩ theo artistId, bài hát dùng chính Song có sẵn. Permission screen giải thích bước bắt đầu với nhạc local.
- Palette tô nền/text tương phản cho hàng album theo bìa. Decode bitmap khoảng 128 px trên IO; cache LRU 64 entry theo ArtworkRequest có URI/version, gồm fallback bìa thiếu/hỏng. Không tính lại trong body recomposition, không lưu Bitmap vào Room.
- Library ViewModel sống ở AppNavHost, nên đổi tab không tạo lại scan/controller. Settings và Search dùng chung LibraryRepository; B1–B4 giữ nguyên dữ liệu, Room vẫn v3.

Dependencies thêm: DataStore Preferences 1.1.7 và Palette 1.0.0, ghim bản stable đã build với toolchain repo. Không nâng các version hiện có. Tham chiếu [DataStore release](https://developer.android.com/jetpack/androidx/releases/datastore#1.1.7) và [Palette](https://developer.android.com/develop/ui/views/graphics/palette-colors).

## Delivery

- [Handoff B5](../handoff/B5.md)
- [Kiểm tra](TESTING.md)
- [Setup clone/run](../SETUP.md)
- [Library và Palette](screenshots/api36-library-palette.png), [Settings tối](screenshots/api36-settings-dark.png)

APK local: `docs/b5/vinyl-b5-debug.apk` (gitignore). Clone mới build bằng `./gradlew.bat :app:assembleDebug`.

## Phần Khánh làm tiếp / giới hạn

Đã có UI cơ bản cho toàn luồng MVP, chưa phải bản UI hoàn thiện. Khánh tiếp tục trau chuốt Songs/Artists/onboarding, menu, Queue/NowPlaying và drag/drop playlist. Sort đang là lựa chọn của màn, chưa lưu thành setting; Search trả bài hát khớp artist/album, chưa có nhóm kết quả riêng. Palette cache RAM có giới hạn nên có thể tính lại sau eviction hoặc process restart.

Chưa kiểm tra B5 trên API 32, máy thật, font lớn/màn hẹp hoặc profiler thư viện thật 3.000 bài. Bluetooth/gapless/fade máy thật vẫn cần kiểm tra theo B2/B3. Không đánh đồng code B0–B5 đã có với toàn bộ QA sản phẩm đã hoàn tất.
