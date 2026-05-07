Hệ Thống Quản Lý Bãi Đỗ Xe Thông Minh (DuAnParking)
1. Mô tả dự án
Dự án là một ứng dụng desktop toàn diện dành cho việc quản lý bãi đỗ xe hiện đại. Hệ thống không chỉ dừng lại ở việc ghi nhận xe vào/ra mà còn tối ưu hóa vận hành thông qua bản đồ bãi đỗ thời gian thực và tự động hóa quy trình tính phí cũng như nhận diện biển số. Điểm đặc biệt của dự án là khả năng chịu tải và dự phòng với kiến trúc hai server đồng bộ song song.

2. Vai trò của bạn
Bạn đóng vai trò là Full-stack Developer & System Architect, chịu trách nhiệm:

Thiết kế kiến trúc hệ thống phân tán sử dụng Java RMI.

Xây dựng cơ sở dữ liệu quan hệ MySQL để quản lý logic nghiệp vụ phức tạp (thuê dài hạn, lịch trình, bảng giá đa điều kiện).

Phát triển giao diện người dùng (UI/UX) và tích hợp các module xử lý ảnh (OCR).

Triển khai cơ chế đồng bộ hóa dữ liệu (Synchronization) giữa các server để đảm bảo tính nhất quán.

3. Công nghệ sử dụng
Ngôn ngữ & Framework: Java 21, JavaFX 21 (Giao diện hiện đại, mượt mà).

Giao tiếp mạng: Java RMI (Remote Method Invocation) cho mô hình Client-Server và Server-Server.

Lưu trữ: MySQL (Quản lý dữ liệu quan hệ).

Thị giác máy tính: Tesseract OCR (Nhận diện biển số), Sarxos Webcam API (Xử lý luồng video trực tiếp).

Thư viện hỗ trợ: ControlsFX (Nâng cao trải nghiệm UI).

4. Những thành tựu đạt được
Tối ưu hóa quy trình: Tự động hóa việc nhận diện biển số và tính phí, giảm thiểu sai sót do thao tác thủ công.

Hệ thống sẵn sàng cao (High Availability): Thiết kế thành công cơ chế đồng bộ hóa giữa hai server (SERVER_A & SERVER_B), giúp hệ thống vẫn hoạt động ổn định ngay cả khi một server gặp sự cố.

Quản lý trực quan: Triển khai bản đồ bãi đỗ dạng lưới cập nhật thời gian thực, giúp quản lý 50 vị trí đỗ (phân chia theo Zone và Loại chỗ) một cách khoa học.

Logic nghiệp vụ linh hoạt: Xử lý thành công các bài toán khó như quản lý hợp đồng thuê dài hạn theo gói tháng và lịch trình sử dụng theo từng ngày trong tuần của khách hàng.
