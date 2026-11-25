# B0 — App foundation

Branch: `codex/b0-app-foundation`.

## Đã có

- Một `:app`, giữ nguyên bộ Gradle/AGP/Kotlin/Compose và SDK đã ghim.
- Theme sáng/tối theo hệ thống, bố cục xử lý system bar qua Scaffold.
- Navigation Thư viện → Playlist → Cài đặt; chọn lại tab không tạo thêm bản sao màn hình, Back về Thư viện.
- `VinylApplication` giữ `AppContainer`; container chỉ tạo factory ViewModel, chưa có repository/database để khởi tạo.
- Library dùng Route → ViewModel → StateFlow, collect theo lifecycle. Screen nhận UiState và không tự tạo dependency.
- Preview Library: Empty sáng/tối, Loading, Error. Playlist/Settings ghi rõ đang xây dựng.

## Luồng file để mở rộng

```text
VinylApplication → AppContainer (factory)
MainActivity → VinylTheme → AppNavHost
  library/AlbumsScreen.kt → AlbumsRoute → AlbumsViewModel
  playlist/PlaylistsScreen.kt
  settings/SettingsScreen.kt
```

- Theme: `app/src/main/java/com/vinyl/shared/ui/Theme.kt`.
- Thêm destination tại `AppNavHost.kt`; screen thuộc feature tương ứng.
- B1: thêm repository/scanner tại `library`, Room tại `shared/database`; nối dependency qua AppContainer và constructor AlbumsViewModel.
- `AlbumsUiState` ở cùng file ViewModel; Route, Screen và private Preview ở cùng file Screen. Chỉ mở rộng state khi có dữ liệu thật.
- Text hiển thị ở `app/src/main/res/values/strings.xml`. Component hiện còn ngắn và riêng từng màn, chưa cần abstraction chung.

Tuân thủ mục 4 của [plan](../MVP_SCAFFOLD_PLAN.md): query chỉ đọc, command trả Unit; local/private trước, internal khi cần dùng giữa file. Activity/Application public vì Android khởi tạo từ manifest.

## Chạy thử

1. Mở project, chọn Gradle JDK 17/21 và SDK 36 theo README gốc, Run trên emulator.
2. Mặc định thấy Thư viện và thông báo B0; chuyển Playlist/Cài đặt, chọn lại tab và bấm Back.
3. Đổi dark theme trong Android Settings; kiểm tra chữ/nền và tab. Xoay màn hình khi đang ở Cài đặt; tab hiện tại phải được giữ lại.
4. Mở `AlbumsScreen.kt` → Split/Design để xem bốn Preview. Loading/Error dùng UiState truyền trực tiếp trong Preview, không phải thao tác scan thật.
5. Chạy `assembleDebug`, `testDebugUnitTest`, `lintDebug` theo README gốc.

## Giới hạn và việc tiếp theo

Đây là khung B0, chưa phải thư viện nhạc hoạt động. Runtime hiển thị empty; chưa xin quyền, quét nhạc, lưu DB, playback hoặc lưu cài đặt. Không có bài hát giả và nút bấm giả. B1 sẽ bổ sung loading/error từ thao tác thật và retry.

Khánh có thể làm theme/layout/component và Preview ngay. Quý bắt đầu scanner/Room và nối repository vào ViewModel ở B1. Không cần tài khoản, access token hay API key để chạy app.

## Kết quả kiểm tra

- `:app:assembleDebug`: thành công với JDK 17.
- `:app:lintDebug`: thành công, không có error; còn 6 warning về SDK/version đã ghim và launcher icon chưa có.
- `:app:testDebugUnitTest`: `NO-SOURCE`; B0 chưa có logic nghiệp vụ để thêm unit test.
- Emulator API 36: cài/mở APK, ba tab, chọn lại tab rồi Back, giữ tab khi xoay, đổi theme sáng/tối đều đạt. Đã xem ảnh chụp hai theme, không bị che bởi system bar.
- Các hàm Preview biên dịch cùng debug app; chưa mở Design Preview trong Android Studio hoặc thử clone trên máy teammate.
- Chưa kiểm tra API 26/32 và thiết bị thật. CI tự động chưa cấu hình trong B0.
