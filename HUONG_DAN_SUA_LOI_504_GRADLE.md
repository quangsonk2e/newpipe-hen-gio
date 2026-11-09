# Sửa lỗi Gradle Configuration Cache + Kotlin HTTP 504 trên GitHub Actions

## Xác định lỗi từ log đã gửi

- `Configuration cache state could not be cached` → task `:app:formatKtlint`.
- `Could not download kotlin-compiler-embeddable-2.2.21.jar` → HTTP 504 Gateway Time-out tại GitHub JetBrains release.
- Các dòng `deprecated` và `BUILD FAILED` không phải nguyên nhân gốc riêng biệt.

## Cách nhanh nhất (không cần tạo repository mới)

1. Mở repository GitHub hiện có → **Code**.
2. Mở `.github/workflows/main.yml` → biểu tượng bút chì.
3. Tìm `- name: 8. Bien dich APK debug`, thay nguyên bước số 8 bằng nội dung trong file
   `.github/workflows/main.yml` của bản FIX-BUILD đính kèm.
4. **Commit changes**, chờ Actions chạy lại (hoặc **Actions → Run workflow**).
5. Xem Artifacts để tải APK khi job có dấu tích xanh.

Để sửa tại chỗ thủ công, lệnh Gradle cốt lõi là:

```bash
./gradlew :app:assembleDebug --no-daemon --no-configuration-cache --stacktrace \
  -DskipFormatKtlint=true \
  -x :app:formatKtlint -x :app:runKtlint -x :app:runCheckstyle
```

* Không sửa mã hẹn giờ hoặc trình phát local.
* Các tác vụ biên dịch, xử lý Android resources và tạo APK vẫn chạy bình thường; chỉ bỏ qua
  kiểm tra định dạng/source style trong CI debug.
* Bản FIX-BUILD còn tự thử tối đa hai lần với độ trễ 20 giây, để giảm khả năng
  hỏng vì HTTP 504 tạm thời.
* Nếu vẫn lỗi, gửi **log mới của bước 8**, nhất là 30–50 dòng bắt đầu từ
  `What went wrong`, `Execution failed for task`, `Caused by` hoặc `error:`.

### Xác minh sửa đúng

Trong log build mới, không còn `Configuration cache entry discarded due to serialization error`
cho `formatKtlint`, và không yêu cầu tải `ktlint` khi xây APK debug. Nếu phát sinh lỗi
khác ở `compileDebugJavaWithJavac` thì đó là vòng chẩn đoán mới, không phải lỗi HTTP 504 cũ.

Nguồn kỹ thuật: https://docs.gradle.org/current/userguide/gradle_command_line.html
https://docs.gradle.org/current/userguide/configuration_cache_enabling.html
