# HƯỚNG DẪN CỤ THỂ: THÊM THƯ VIỆN NHẠC/VIDEO LOCAL, SỬA MÃ, BIÊN DỊCH NEWPIPE THÀNH APK TRÊN GITHUB

Cập nhật 09/11/2026 — dành cho Windows 10/11 và điện thoại Android.

> **Quan trọng:** ZIP `newpipe-hen-gio-main.zip` bạn đưa **không chứa toàn bộ NewPipe**: chỉ có GitHub Actions và `apply_sleep_timer.py`. Tôi giữ nguyên mô hình đó, thêm `apply_local_media.py` và thư mục `features/local_media`. Khi chạy Actions, GitHub tự tải **NewPipe v0.29.1**, chạy hai script rồi tạo APK.
>
> Chưa có APK biên dịch sẵn; GitHub Actions phải chạy thành công mới xác nhận có APK dùng được.

## A. CHỨC NĂNG MỚI VÀ CÁCH DÙNG TRÊN ĐIỆN THOẠI

1. Mở NewPipe phiên bản tự build.
2. Chạm menu `☰` (menu bên trái) → **Nhạc và video trong máy**.
3. Chọn **Âm thanh** để nghe nhạc, ghi âm, MP3/AAC/FLAC…; chọn **Video** để xem những tệp được Android lập chỉ mục, ví dụ MP4/WebM.
4. Lần đầu, cấp quyền truy cập **Âm thanh** hoặc **Video** khi Android yêu cầu. Nếu không cấp, danh sách không thể hiện tệp.
5. Dùng ô **Tìm tệp trong máy**, chọn bài/video, dùng nút play/pause của trình phát, Bài trước/Bài tiếp, hoặc nút trên thanh thông báo.
6. Chạm **Hẹn giờ tắt** trong cửa sổ trình phát local. Đây là bộ hẹn giờ của trình phát local, độc lập với hẹn giờ đang dùng cho nội dung trực tuyến.
7. Chạm **Video cửa sổ nhỏ** (Android 8.0 trở lên, tùy máy hỗ trợ) để xem video dạng Picture-in-Picture. Có thể chuyển sang ứng dụng khác khi tiếp tục nghe nhạc.
8. Khi muốn **dừng hoàn toàn**, chọn **Dừng** trong trình phát local, hoặc hành động dừng trên thông báo.

**Lưu ý:** Chỉ tìm thấy những tệp MediaStore đã lập chỉ mục và bạn đã cấp quyền. Bản này không có trình duyệt file tự do, bộ cân bằng âm thanh, tổ chức playlist theo album riêng, hay trình chỉnh sửa video như một số phiên bản Pure Tuber. Không thể bảo đảm mọi codec/định dạng hoạt động trên mọi máy.

## B. TẠO REPOSITORY TRÊN GITHUB BẰNG GIAO DIỆN WEB

**Bước 1 — tải ZIP:** Tải file `NewPipe-HenGio-LocalMedia-GitHub.zip` đi kèm, giải nén trên Windows bằng **Extract All**. Trong thư mục bạn thấy:

```text
.github/workflows/main.yml
apply_sleep_timer.py
apply_local_media.py
features/local_media/
README.md
HUONG_DAN_GITHUB_CHI_TIET.md
```

**Bước 2 — tạo repository:** Vào https://github.com → đăng nhập → nút `+` ở trên → **New repository**. Nhập tên ví dụ `newpipe-hen-gio-local`. Chọn **Private** nếu chỉ tự dùng, bỏ tick `Add a README file` (vì ZIP đã có README) rồi bấm **Create repository**.

**Bước 3 — tải mã lên qua web:** Trong repository mới, chọn **Add file → Upload files**. Kéo **toàn bộ nội dung bên trong** thư mục đã giải nén vào khu vực upload (đừng chỉ upload tệp ZIP). Kiểm tra Github giữ đủ thư mục `.github/workflows/` và `features/local_media/`. Nhập commit message `Add local audio and video player` rồi chọn **Commit changes**. Nếu trình duyệt không giữ cấu trúc thư mục, dùng phương pháp Git Bash/PowerShell ở mục C.

**Bước 4 — chạy build:** Vào **Actions**. Nếu GitHub hỏi kích hoạt Actions, chọn **I understand my workflows, go ahead and enable them**. Ở danh sách workflow bên trái, chọn **Build NewPipe - Hen Gio + Nhac Video Trong May**. Nhấn **Run workflow → Run workflow**. Workflow cũng tự chạy khi bạn cập nhật script hay tệp trong `features/local_media`.

**Bước 5 — kiểm tra:** Vào workflow run mới. Màu xanh ✅ ở job `build` nghĩa là tất cả bước đã thành công. Nếu có màu đỏ ❌, mở bước đỏ và xem vài dòng cuối (phần G bên dưới).

**Bước 6 — tải APK:** Trong trang run, kéo xuống **Artifacts** → tải **NewPipe-v0.29.1-HenGio-LocalMedia-APK**. Đây là tệp ZIP chứa `app-debug.apk`. Giải nén trước khi chép APK sang điện thoại. Artifact mã nguồn (không phải APK) có tên **NewPipe-v0.29.1-HenGio-LocalMedia-FULL-SOURCE**.

**Bước 7 — cài đặt:** Chép `app-debug.apk` sang Android và mở. Nếu máy chặn cài đặt, cho phép trình **Files / Trình quản lý tệp** cài ứng dụng từ nguồn này tại mục **Cài đặt → Bảo mật → Cài đặt ứng dụng không rõ nguồn gốc** (tên menu khác tùy hãng). Chỉ cấp quyền cho ứng dụng nguồn mà bạn tin cậy. Sau đó cấp quyền âm thanh/video khi mở tính năng local.

## C. CÁCH ĐƯA MÃ LÊN GITHUB BẰNG GIT (KHUYẾN NGHỊ NẾU WEB KHÔNG GIỮ THƯ MỤC)

Cài [Git for Windows](https://git-scm.com/downloads/win), giải nén ZIP, mở **Git Bash** trong thư mục mã nguồn. Nếu repository GitHub đã tạo trống, dùng:

```bash
git init
git branch -M main
git add .
git commit -m "Add NewPipe local media and sleep timer"
git remote add origin https://github.com/TEN_TAI_KHOAN/newpipe-hen-gio-local.git
git push -u origin main
```

Thay `TEN_TAI_KHOAN` bằng username GitHub thực tế của bạn. Với giao thức HTTPS, GitHub thường yêu cầu xác thực qua trình quản lý đăng nhập hoặc token; không nhập mật khẩu tài khoản như mật khẩu Git cũ. Nếu repository đã có các file khác, hãy `git clone` repository hiện có, chép mã vào, sau đó `git add .`, `git commit` và `git push` (đừng khởi tạo đè).

## D. CÁCH BIÊN TẬP MÃ NGAY TRÊN GITHUB

1. Mở repository → tab **Code** → chọn tệp cần sửa.
2. Nhấn biểu tượng **bút chì (Edit this file)**, chỉnh Java/XML/Python.
3. Bấm **Commit changes** để lưu. Sau khi commit lên nhánh chính, workflow sẽ tự chạy nếu thay đổi nằm trong các đường dẫn đã cấu hình.
4. Nếu muốn thử thủ công sau khi sửa README hoặc tệp khác, vào **Actions → Run workflow**.

**Tệp cần sửa theo chức năng:**

| Muốn sửa | Tệp tương ứng | Ghi chú |
|---|---|---|
| Đổi tên menu | `features/local_media/strings-vi.xml.part` | Dòng `local_media_title` |
| Đổi màu/giao diện danh sách | `features/local_media/activity_local_media.xml` | XML màn hình thư viện |
| Thêm/sửa giao diện phát | `features/local_media/activity_local_media_player.xml` | Nút và PlayerView |
| Sắp xếp/lọc thư viện | `features/local_media/LocalMediaRepository.java` | Truy vấn MediaStore |
| Cách tìm kiếm | `features/local_media/LocalMediaActivity.java` | Bộ lọc tên, nghệ sĩ |
| Phát nền, bài tiếp, hẹn giờ local | `features/local_media/LocalMediaPlaybackService.java` | ExoPlayer/notification |
| Video PiP, giao diện phát | `features/local_media/LocalMediaPlayerActivity.java` | Hoạt động trên Android |
| Vị trí mục trong menu NewPipe | `apply_local_media.py` | Đoạn `patch_main_activity` |
| Chức năng hẹn giờ gốc | `apply_sleep_timer.py` | Không chỉnh nếu đã chạy ổn |
| Cách tạo APK | `.github/workflows/main.yml` | Phiên bản gốc và lệnh Gradle |

**Không sửa các tệp ZIP mà Github Actions tạo trong Artifacts:** đó là sản phẩm build. Muốn sửa ứng dụng, thay đổi **script/tệp mẫu trong repository**, sau đó biên dịch lại.

## E. HIỂU TỪNG BƯỚC BIÊN DỊCH

Workflow thực hiện:

1. `actions/checkout@v4` tải các script của bạn.
2. `git clone --branch v0.29.1 --depth 1 ...` lấy NewPipe chính thức đúng phiên bản.
3. Cài Java 21 và Android SDK.
4. `python3 apply_sleep_timer.py NewPipe`: áp dụng chức năng hẹn giờ đã ổn.
5. `python3 apply_local_media.py NewPipe`: thêm 5 tệp Java, 2 layout, khai báo Activity/Service, quyền MediaStore và chuỗi tiếng Việt/Anh.
6. Kiểm tra XML và `git diff --check`.
7. `./gradlew :app:assembleDebug --no-daemon --no-configuration-cache -DskipFormatKtlint=true -x :app:formatKtlint -x :app:runKtlint -x :app:runCheckstyle --stacktrace`: biên dịch ứng dụng, bỏ qua tác vụ kiểm tra kiểu mã để tránh lỗi trong log HTTP 504/Configuration Cache.
8. Tải APK và toàn bộ mã nguồn đã sửa qua **Artifacts**.

Nếu bước `git clone ...` kiểm tra commit `00acf2f` bị lỗi, xem có thay đổi upstream tag hoặc mạng GitHub không. **Không tự ý đổi sang branch master/dev** vì script hẹn giờ phụ thuộc v0.29.1.

## F. BUILD TRÊN MÁY TÍNH QUA ANDROID STUDIO (TÙY CHỌN)

Có thể biên dịch ngoài GitHub bằng Windows/Android Studio và JDK 21:

```bash
git clone --branch v0.29.1 --depth 1 https://github.com/TeamNewPipe/NewPipe.git NewPipe
python apply_sleep_timer.py NewPipe
python apply_local_media.py NewPipe
cd NewPipe
gradlew.bat :app:assembleDebug --no-configuration-cache -DskipFormatKtlint=true -x :app:formatKtlint -x :app:runKtlint -x :app:runCheckstyle --stacktrace
```

Mở thư mục `NewPipe` bằng Android Studio → chờ Gradle Sync (cần Internet lần đầu) → **Build → Build Bundle(s) / APK(s) → Build APK(s)**. Tệp kết quả thường ở `NewPipe/app/build/outputs/apk/debug/app-debug.apk`.

> Trên GitHub, **không cần cài Java hay Android Studio trên Windows**. Toàn bộ diễn ra trong máy ảo của Actions.

## G. CÁCH ĐỌC VÀ SỬA LỖI THƯỜNG GẶP

| Triệu chứng | Nguyên nhân khả dĩ | Cách làm |
|---|---|---|
| Actions không thấy workflow | `.github/workflows/main.yml` chưa được upload đúng thư mục / Actions bị tắt | Kiểm tra tab Code và kích hoạt Actions |
| Bước clone NewPipe đỏ | Không tải được GitHub hoặc commit tag khác dự kiến | Mở log bước 2; không cập nhật sang bản mới tùy tiện |
| Bước thêm hẹn giờ đỏ | Script cũ không tìm thấy vị trí trong NewPipe đúng phiên bản | Bảo đảm giữ đúng v0.29.1 và không sửa script hẹn giờ |
| Bước thêm local media đỏ | MainActivity/Manifest bị thay đổi so với mã gốc | Xem log `expected 1 anchor; found 0`, điều chỉnh anchor trong `apply_local_media.py` |
| Lỗi `symbol not found`, `R.string...` | Sai tên Resource Java/XML hoặc thiếu upload 1 tệp | Đối chiếu `features/local_media` và tên tệp/ID |
| `Execution failed for task ':app:runCheckstyle'` | Phong cách Java không đạt kiểm tra nguồn | Đọc tên tệp và dòng lỗi ở GitHub log, sửa Java và commit lại |
| `Could not resolve...`, `services.gradle.org` | Không tải được Gradle/dependencies | Chạy lại khi mạng GitHub ổn định; không xóa kiểm tra bảo mật |
| Android không hiện nhạc/video | Chưa cấp quyền hoặc Android chưa quét file vào MediaStore | Vào cài đặt ứng dụng cấp quyền Âm thanh/Video, mở lại và nhấn Tải lại |
| APK bị lỗi `App not installed` | Trùng package name nhưng khác khóa ký APK | Sao lưu dữ liệu, sau đó gỡ bản Debug cũ trước khi cài; hoặc áp dụng khóa ký cố định mục H |
| Chạy nền bị hệ thống dừng | Máy đặt tiết kiệm pin quá chặt | Cho phép ứng dụng chạy nền trong cài đặt pin (tùy hãng) |
| Video có tiếng nhưng không có hình | Codec video không được Android/ExoPlayer hỗ trợ | Thử MP4 H.264/AAC để đối chiếu, không kết luận mọi định dạng đều hỗ trợ |

## H. TÙY CHỌN: CÀI ĐẶT NHIỀU LẦN KHÔNG PHẢI GỠ APP (KHÓA KÝ DEBUG ỔN ĐỊNH)

Github Actions mặc định tạo **debug keystore mới theo máy chạy**, nên build lần sau có thể khác chứng chỉ; Android sẽ từ chối cập nhật cùng package. Muốn giữ khóa cố định cho các lần build tiếp theo:

1. Trên máy có **JDK 21**, mở PowerShell trong thư mục cá nhân và tạo file ký debug (chỉ thực hiện một lần):

```powershell
keytool -genkeypair -keystore newpipe-debug.keystore -alias androiddebugkey -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
```

2. Chuyển tệp này thành Base64 bằng PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$PWD\newpipe-debug.keystore")) | Set-Content "$PWD\keystore-b64.txt" -NoNewline
```

3. Tại repository GitHub → **Settings → Secrets and variables → Actions → New repository secret**. Name: `DEBUG_KEYSTORE_BASE64`. Value: sao chép **toàn bộ nội dung** file `keystore-b64.txt`. Lưu secret. Đừng commit keystore hoặc chuỗi Base64 vào repository.
4. Chạy lại Actions. Bước `7b. Su dung khoa ky debug co dinh...` sẽ khôi phục khóa trước khi Gradle build. Các lần build về sau dùng cùng keystore với điều kiện bạn không thay secret và không đổi package name.
5. Sao lưu `newpipe-debug.keystore` ở nơi an toàn. Nếu mất khóa sẽ không cập nhật được APK cùng package mà vẫn giữ dữ liệu app. Các bản build trước khi thêm secret có thể cần gỡ cài đặt một lần.

**Lưu ý:** Khóa debug nói trên chỉ phù hợp mục đích sử dụng/cài thử riêng, không phải phương pháp phát hành APK chính thức. Không đưa keystore, password hoặc mã nguồn nhạy cảm lên repository công khai.

## I. KIỂM TRA SAU KHI CÀI

- [ ] App chạy và hẹn giờ hiện có (phát nội dung online) vẫn hoạt động.
- [ ] Mở menu `☰` thấy **Nhạc và video trong máy**.
- [ ] Cấp quyền âm thanh, danh sách hiện tệp MP3/ghi âm của máy.
- [ ] Cấp quyền video, danh sách hiện MP4 của máy.
- [ ] Chọn một tệp: nghe/xem được ngay, dùng nút Bài trước/Bài tiếp.
- [ ] Tắt màn hình: âm thanh tiếp tục phát, hiển thị điều khiển trong thông báo.
- [ ] Hẹn giờ tắt local hoạt động, có thể hủy.
- [ ] Video cửa sổ nhỏ hoạt động trên máy hỗ trợ PiP.
- [ ] Không kết nối Internet vẫn có thể mở media trong máy.

## J. TÀI LIỆU THAM KHẢO

- NewPipe chính thức: https://github.com/TeamNewPipe/NewPipe
- Quyền truy cập media của Android 13+: https://developer.android.com/about/versions/13/behavior-changes-13
- MediaStore: https://developer.android.com/reference/android/provider/MediaStore
- GitHub Actions: https://docs.github.com/en/actions
- GitHub Secrets: https://docs.github.com/en/actions/security-guides/using-secrets-in-github-actions

**Giấy phép:** Phần mã nguồn sửa đổi tuân theo GPL-3.0-or-later như NewPipe. Không bao gồm mã nguồn Pure Tuber.


## J. SỬA LỖI BUILD NGÀY 09/11/2026

Log lỗi cho biết hai nguyên nhân: `formatKtlint` không ghi được Configuration Cache;
và tải `kotlin-compiler-embeddable-2.2.21.jar` bị HTTP 504 do redirect
qua GitHub/JetBrains. Để sửa mà không tác động tính năng đang hoạt động:

1. Trên GitHub vào **Code → .github/workflows/main.yml → Edit (bút chì)**.
2. Thay bước số 8 bằng bước số 8 trong bản ZIP FIX-BUILD, hoặc tải file
   `.github/workflows/main.yml` từ ZIP FIX-BUILD để ghi đè.
3. Chọn **Commit changes**. Truy cập **Actions**, mở lượt chạy mới.
4. Nếu xanh, vào **Artifacts** tải APK. Nếu đỏ, lấy log cuối của bước
   **8. Bien dich APK debug**; lỗi tiếp theo có thể là lỗi biên dịch Java/XML
   chưa xuất hiện trong log trước.

Các cờ build: `--no-configuration-cache` vô hiệu cơ chế cache không tương thích;
`-DskipFormatKtlint=true` để NewPipe không thêm bước format vào preDebugBuild;
`-x` bỏ chạy ba tác vụ kiểm tra phong cách mã. Các tác vụ biên dịch thật
(`compileDebugJavaWithJavac`, `mergeDebugResources`...) không bị bỏ qua.
Workflow thử tối đa 2 lần để chống lỗi HTTP 504 nhất thời. Khi đã có APK,
nên kiểm tra kiểu mã riêng nếu tiếp tục phát triển tính năng.
