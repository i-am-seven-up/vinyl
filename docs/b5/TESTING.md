# Kiểm tra B5

Ngày: 26/11/2026. Emulator API 36, toolchain đã ghim trong repo.

## Tự động

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest --console=plain
```

BUILD SUCCESSFUL: 10 JVM unit test và 20 instrumentation test, không fail/skip. Sau chỉnh presentation/system bars, assembleDebug và lintDebug chạy lại thành công.

Năm test B5 bổ sung:

- Search query tên/nghệ sĩ/album không dấu và hoa thường; trim; `%`, `_`, `!`, dấu nháy và chuỗi SQL là literal/bound parameter.
- SearchViewModel debounce lấy query cuối, query giống nhau không kẹt loading, blank xóa kết quả, library thay đổi trả empty.
- DataStore SYSTEM mặc định, set DARK, đóng/mở store vẫn DARK, set LIGHT.
- Settings khởi tạo không scan ngầm; scan lỗi hiển thị error, retry thành công và xóa error.
- Palette màu từ bitmap thật, cache theo URI/version, cache missing và fallback bytes hỏng.

B1–B4 regression vẫn pass, gồm snapshot 3.000 record, playlist rescan/migration và playback restore/fade. Không thay thế kiểm tra nhạc thật 3.000 bài bằng test record.

Lint: 0 errors, 19 warnings và 1 hint. Còn cảnh báo version dependency/target được ghim, exported media service, icon app, KTX và gợi ý Palette KTX; không thêm baseline/suppress. `git diff --check` sạch.

## Clone sạch local

Clone commit B5 vào thư mục local riêng trong app/build, không copy local.properties hoặc .idea. Chỉ đặt ANDROID_HOME tới SDK local rồi chạy wrapper `:app:assembleDebug`: BUILD SUCCESSFUL, 38 task executed. Dùng dependency cache chung của máy; đây không phải xác nhận từ một máy teammate mới.

## Smoke nhạc thật API 36

- Cài debug APK, mở hướng dẫn cấp quyền, chọn Allow trong dialog Android. Scan thấy 3 album/6 bài của bộ tone B1.
- Album Đường về → bài 1 → Home → mở lại. Media session sau buffering báo PLAYING, metadata đúng và queue size 2.
- Tạo playlist và thêm Đường về 1 từ picker; sau pause, force-stop/mở lại và rescan từ Settings, playlist và bài vẫn còn.
- Search `duong` trả Đường về 1/2; chạm kết quả để phát. Search query lọc cụ thể cũng được kiểm tra trong instrumentation.
- Chọn Tối, Quét lại: hiện đã quét xong. Mở lại app vẫn Tối, playback paused. Đã kiểm tra ảnh system bar sau sửa tương phản.
- Songs đổi A–Z sang Z–A; Artists có Đặng B1 / Nghệ sĩ B1, mở Đặng B1 ra hai bài đúng.
- [Ảnh Settings tối](screenshots/api36-settings-dark.png), [Library có Palette và fallback](screenshots/api36-library-palette.png) đã xem trực tiếp. Cover xanh có text trắng; bìa thiếu/hỏng giữ màu theme.

## Chưa kiểm tra

API 32 cho B5, máy thật, Bluetooth/gapless/fade bằng tai nghe, font lớn/màn hẹp, profiler thư viện nhạc thật 3.000 bài và xác nhận clone/run trên máy teammate. UI polish còn thuộc Khánh; code base không được xem là toàn bộ QA sản phẩm đã xong.
