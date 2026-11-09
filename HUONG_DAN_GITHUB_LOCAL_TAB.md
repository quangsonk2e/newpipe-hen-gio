# HƯỚNG DẪN NEWPIPE LOCAL TAB – TỪ MÃ NGUỒN ĐẾN APK

## 1. Những gì đã đổi

Khi mở NewPipe, trên **thanh tab ngang** sẽ có tab **Local** với biểu tượng nốt nhạc. Đây là một tab thật trong màn hình chính (có thể vuốt qua), không phải lối tắt nằm trong menu bên.

Trong tab Local, chọn **Âm thanh** hoặc **Video**; có ô tìm theo tên tệp, nghệ sĩ hoặc thư mục; chọn thư mục từ danh sách; đổi thứ tự Mới nhất/Tên A–Z/Dài nhất; nút Mở tệp dùng bộ chọn Android cho tệp chưa xuất hiện trong thư viện. Khi có nhạc đang phát, cuối thư viện có thanh điều khiển nhanh.

Mở bài hát/video vào trình phát: phát/tạm dừng, chuyển bài, tua, phát ngẫu nhiên, lặp một bài/lặp tất cả, hẹn giờ, và video cửa sổ nhỏ PiP (Android 8+ nếu thiết bị hỗ trợ). Phát nền qua một foreground service. Hẹn giờ NewPipe trực tuyến trước đó vẫn được giữ nguyên.

## 2. Cách đơn giản nhất: GitHub Desktop (Windows)

1. Tải tệp `NewPipe-HenGio-LocalTab-GitHub.zip` rồi **giải nén**.
2. Mở https://github.com/ và đăng nhập. Chọn **New repository** hoặc dùng repository `newpipe-hen-gio-local` đã hoạt động.
3. Mở **GitHub Desktop**, chọn `File → Clone repository`, chọn đúng repository của bạn, bấm **Clone**. Nếu chưa cài: https://desktop.github.com/.
4. Mở thư mục repository trên ổ đĩa bằng `Repository → Show in Explorer`.
5. **Sao chép nội dung bên trong** thư mục ZIP đã giải nén (không sao chép thừa một lớp thư mục) vào thư mục repository vừa mở, chọn ghi đè các tệp trùng tên. Đặc biệt kiểm tra có đúng các đường dẫn:
   - `.github/workflows/main.yml`
   - `apply_sleep_timer.py`
   - `apply_local_media.py`
   - `features/local_media/LocalMediaFragment.java`
   - `scripts/verify_local_tab.py`
6. Trở lại GitHub Desktop: đặt Summary `Add Local tab and redesigned offline player`, bấm **Commit to main**, rồi **Push origin**.
7. Trong trang GitHub repository, mở tab **Actions**. Nếu GitHub hỏi bật workflow, chọn **I understand my workflows, go ahead and enable them** (sau khi bạn đã kiểm tra mã).
8. Chọn workflow **Build NewPipe - Local Tab + Hen Gio**. Nếu workflow chưa tự chạy sau khi push, nhấn **Run workflow → Run workflow**.
9. Xem job **build**. Khi có dấu ✅ màu xanh, mở lượt chạy; ở gần cuối trang tìm **Artifacts → NewPipe-v0.29.1-HenGio-LocalTab-APK** và bấm tải.
10. Giải nén artifact ZIP từ GitHub, lấy `app-debug.apk`, chép sang điện thoại và mở để cài đặt. Android có thể yêu cầu cấp quyền **Cho phép cài ứng dụng từ nguồn này** cho ứng dụng quản lý tệp.

**Không nén lại file `app-debug.apk`**. File cần cài trên điện thoại phải có đuôi `.apk`.

## 3. Cách dùng Git command line thay GitHub Desktop

Mở PowerShell trong thư mục làm việc, thay `TAIKHOAN` và tên repo cho đúng:

```powershell
git clone https://github.com/TAIKHOAN/newpipe-hen-gio-local.git
cd newpipe-hen-gio-local
```

Dùng Windows Explorer sao chép **toàn bộ** nội dung đã giải nén vào thư mục hiện tại (chọn ghi đè, giữ nguyên `.github`), rồi chạy:

```powershell
git add -A
git commit -m "Add Local tab, media library and player controls"
git push origin main
```

Nếu nhánh chính của bạn tên `master`, thay `main` bằng `master`. GitHub workflow cũng có thể chạy bằng nút `Run workflow` mà không cần dùng terminal.

## 4. Sửa giao diện và chức năng về sau

- `features/local_media/activity_local_media.xml`: giao diện thư viện, tìm kiếm, bộ lọc, tab Âm thanh/Video, thanh mini-player.
- `features/local_media/row_local_media.xml`: mỗi dòng tệp nhạc/video.
- `features/local_media/activity_local_media_player.xml`: màn hình phát.
- `features/local_media/LocalMediaFragment.java`: hành vi của tab Local, quyền truy cập, bộ chọn tệp, lọc và sắp xếp.
- `features/local_media/LocalMediaRepository.java`: danh sách tệp Android MediaStore.
- `features/local_media/LocalMediaPlaybackService.java`: phát nền, danh sách phát, hẹn giờ, phát ngẫu nhiên, lặp.
- `features/local_media/LocalMediaPlayerActivity.java`: tua, PiP, các nút phát.
- `features/local_media/strings-vi.xml.part` và `strings-en.xml.part`: các nhãn giao diện.
- `apply_local_media.py`: mã thêm Local vào NewPipe (có xử lý các tệp Java/Manifest/resources của NewPipe).
- `apply_sleep_timer.py`: chức năng hẹn giờ NewPipe đã có, hiện không sửa.
- `.github/workflows/main.yml`: tự tải NewPipe v0.29.1, chèn hai chức năng, biên dịch, ký debug và tải lên Artifact.

Dùng nút bút chì trên GitHub để sửa tệp, rồi chọn `Commit changes`; workflow tự khởi chạy nếu bạn sửa các đường dẫn trong phần `push.paths`.

## 5. Cài đặt trên điện thoại

1. Nên **xuất bản sao lưu dữ liệu NewPipe** (đăng ký kênh, danh sách phát và cài đặt) trong ứng dụng cũ trước khi thử bản mới.
2. Nếu đây là APK đầu tiên được ký bằng khóa debug mới, bạn có thể gặp lỗi `App not installed` hoặc `INSTALL_FAILED_UPDATE_INCOMPATIBLE` do khóa ký khác. Khi đó phải dùng đúng khóa ký cũ, hoặc gỡ bản cũ (sau khi đã sao lưu) rồi mới cài.
3. Mở ứng dụng → trên thanh tab ngang vuốt tìm **Local**. Có thể chuyển thanh tab xuống dưới nếu bản NewPipe cho phép tại phần cài đặt giao diện.
4. Cấp quyền âm thanh hoặc video theo yêu cầu của Android. Android 13+ yêu cầu quyền riêng cho hai loại media; trên Android thấp hơn dùng quyền bộ nhớ đọc.
5. Chọn bài để mở trình phát; vuốt sang ứng dụng khác hoặc tắt màn hình để nghe nền. Nút PiP chỉ hiện với video trên thiết bị hỗ trợ.
6. Khi muốn chọn tệp chưa được Android lập chỉ mục, bấm **Mở tệp** và chọn trực tiếp từ trình quản lý tệp Android.

## 6. Duy trì chữ ký APK để cập nhật không cần gỡ bản cũ

GitHub runner thông thường tạo khóa debug tạm thời, và APK ở lần build sau có thể mang chữ ký khác. Để cập nhật được, bạn cần dùng lại **cùng file `debug.keystore` đã ký phiên bản đang cài**. Workflow có hỗ trợ GitHub Repository Secret `DEBUG_KEYSTORE_BASE64`.

Trên Windows, có thể tạo keystore mới một lần bằng `keytool` (đi kèm JDK):

```powershell
keytool -genkeypair -v -keystore debug.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US"
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path .\debug.keystore))) | Set-Content debug-keystore-base64.txt
```

Trên GitHub: repository → **Settings → Secrets and variables → Actions → New repository secret**; tạo secret tên `DEBUG_KEYSTORE_BASE64`, dán nội dung 1 dòng từ `debug-keystore-base64.txt`. Giữ kín keystore và khóa riêng. **Khóa mới không thể cập nhật bản APK cũ đã ký bằng khóa khác**; hãy sao lưu và gỡ bản cũ lần đầu nếu cần.

## 7. Khi Actions báo lỗi

- `504 Gateway Time-out` khi tải Kotlin hoặc Gradle: thường do kết nối máy chủ thư viện; workflow tự thử lại 1 lần. Có thể rerun job khi dịch vụ hồi phục.
- `Configuration cache` / `:app:formatKtlint`: workflow dùng `--no-configuration-cache` và loại trừ tác vụ định dạng trong bước tạo APK.
- `cannot find symbol` / `resource ... not found`: kiểm tra **toàn bộ** `features/local_media` và `apply_local_media.py` đã được push. Xem phần `Caused by` đầu tiên trong Actions.
- `GitHub Actions không chạy`: kiểm tra tên/đường dẫn `.github/workflows/main.yml` và trạng thái workflow đã được bật.
- `Không tìm thấy Artifact APK`: chỉ có Artifact APK khi `assembleDebug` hoàn thành. Artifact FULL-SOURCE không phải APK.

## 8. Giới hạn đã xác định

Đây là mã nguồn và cấu hình build đã được rà soát tĩnh, **chưa biên dịch thành APK ngay trong môi trường soạn thảo** vì thiếu Android SDK/Gradle có thể tải được. Chỉ khi GitHub Actions chạy thành công mới có APK. Thư viện phụ thuộc vào Android MediaStore để liệt kê các tệp đã lập chỉ mục, hoặc Android file picker để chọn tệp riêng. Không hỗ trợ đầy đủ tính năng quản lý danh sách phát thủ công, đồng bộ cloud, Chromecast hoặc mọi tính năng độc quyền của Pure Tuber.
