# NewPipe – tab Local, hẹn giờ và trình phát trên máy

Bản tùy biến NewPipe v0.29.1. Đây là **repository để GitHub Actions tạo APK**, không phải APK và không phải bản sao giao diện/copyright Pure Tuber.

## Thay đổi chính

- Tab **Local** nằm trên thanh tab ngang NewPipe, sau hai tab đầu tiên; vẫn hoạt động khi đặt thanh tab ở phía dưới trong cài đặt NewPipe.
- Trong Local: **Âm thanh** / **Video**, tìm kiếm, chọn thư mục, sắp xếp, mở tệp riêng qua bộ chọn Android.
- Danh sách tệp có tiêu đề, thông tin nghệ sĩ/thư mục, thời lượng; thanh phát nhanh khi có nội dung đang phát.
- Trình phát nội bộ: phát nền, chuyển bài, phát/tạm dừng, tua, ngẫu nhiên, lặp lại, PiP cho video và hẹn giờ tắt.
- Giữ nguyên `apply_sleep_timer.py` của bản đã chạy ổn; Local có hẹn giờ riêng trong dịch vụ phát cục bộ.
- Quyền riêng tư: sử dụng MediaStore + bộ chọn tệp Android; không xin quyền toàn bộ bộ nhớ.

## Mã nguồn và cách build

`apply_sleep_timer.py` + `apply_local_media.py` sẽ được áp dụng theo thứ tự lên mã NewPipe v0.29.1 do workflow tự tải. Mã bổ sung đặt trong `features/local_media/`.

**Trên GitHub:** Actions → Build NewPipe - Local Tab + Hen Gio → Run workflow → tải Artifact `NewPipe-v0.29.1-HenGio-LocalTab-APK` → giải nén và cài `app-debug.apk`.

Chi tiết xem `HUONG_DAN_GITHUB_LOCAL_TAB.md`.

**Tình trạng kiểm chứng:** các script patch, cấu trúc Java, liên kết ID/resources và XML đã qua kiểm tra tĩnh trên bộ mã nguồn kiểm thử. Môi trường thực hiện không có Android SDK/Gradle phân phối và bị chặn DNS tới dịch vụ tải Gradle, nên không thể chạy `assembleDebug` tại đây. Chỉ coi APK thành công nếu job GitHub Actions báo `success` và tạo file APK hợp lệ.

## Lưu ý phiên bản và chữ ký

Không đổi tag gốc nếu chưa kiểm tra lại vị trí chèn. Nếu cần cập nhật ứng dụng không gỡ bản cũ, các APK phải được ký bởi **cùng keystore**; xem `HUONG_DAN_GITHUB_LOCAL_TAB.md`. Dự án gốc NewPipe và phần tùy biến được phân phối theo GPL-3.0-or-later.
