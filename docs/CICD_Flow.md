# CI/CD Flow - Eyewear Management Backend

## 1. Mục tiêu
Khi developer push code lên GitHub, hệ thống GitHub Actions sẽ tự động chạy pipeline trên máy ảo (self-hosted runner), build image mới và deploy lại container backend.

Pipeline này dựa trên 2 file chính:
- [.github/workflows/auto-build-deploy-java.yaml](../.github/workflows/auto-build-deploy-java.yaml)
- [Dockerfile](../Dockerfile)

---

## 2. Vai trò của từng file

### 2.1 File workflow: auto-build-deploy-java.yaml
File này đóng vai trò "điều phối" toàn bộ quy trình CI/CD.

Nó quy định:
- Khi nào pipeline chạy: khic nhá push hoặc pull request vào cánh `main` và `feature/API_Synthesis`.
- Chạy ở đâu: trên `self-hosted` runner (chính là máy ảo của bạn).
- Chạy cái gì và theo thứ tự nào: checkout code, khôi phục `application.yml` từ secret, build Docker image, dừng/xóa container cũ, chạy container mới, kiểm tra trạng thái, rồi cleanup.

### 2.2 File Dockerfile
File này mô tả cách đóng gói backend thành Docker image theo mô hình multi-stage:

- **Build stage**:
  - Dùng image Maven.
  - Copy `pom.xml` và tải dependency trước (`mvn dependency:go-offline`) để tận dụng cache.
  - Copy source code và build JAR (`mvn clean package -Dmaven.test.skip=true`).

- **Runtime stage**:
  - Dùng image JRE nhẹ hơn (Amazon Corretto).
  - Copy file JAR từ stage build sang.
  - Expose cổng `8080`.
  - Chạy app bằng `java -jar app.jar`.

Nói ngắn gọn: workflow quyết định **quy trình deploy**, Dockerfile quyết định **cách build và chạy app trong container**.

---

## 3. Flow CI/CD chi tiết khi push code

### Bước 1: Developer push code
Developer push code lên GitHub vào nhánh `main` hoặc `feature/API_Synthesis`.

### Bước 2: GitHub Actions trigger workflow
GitHub phát hiện event phù hợp và khởi chạy job `build-and-deploy` trong file workflow.

### Bước 3: Job chạy trên máy ảo self-hosted
Runner trên máy ảo nhận job và thực thi lần lượt các step sau:

1. **Checkout code**
	- Lấy source mới nhất từ repository về máy ảo.

2. **Restore `application.yml` từ GitHub Secret**
	- Đọc secret `BACKEND_APPLICATION_YML`.
	- Ghi nội dung vào `src/main/resources/application.yml`.
	- Mục đích: inject cấu hình nhạy cảm (DB, JWT, mail...) mà không lưu cứng trong repo.

3. **Build Docker image mới**
	- Chạy lệnh `docker build -t "$IMAGE_NAME:latest" .`
	- Docker sẽ đọc `Dockerfile` để build image backend.

4. **Stop + remove container cũ (nếu có)**
	- `docker stop "$IMAGE_NAME" || true`
	- `docker rm "$IMAGE_NAME" || true`
	- Mục đích: tránh trùng tên container và đảm bảo bản cũ được thay thế sạch.

5. **Dọn image rác (optional)**
	- `docker image prune -f || true`
	- Mục đích: giải phóng dung lượng trên máy ảo.

6. **Run container mới**
	- Chạy container từ image vừa build:
	  - `--name "$IMAGE_NAME"`
	  - `-p "$DOCKER_HOST_PORT:$APP_INTERNAL_PORT"`
	- Mục đích: publish backend ra cổng host để truy cập từ bên ngoài.

7. **Check trạng thái container**
	- `docker ps -f "name=${IMAGE_NAME}"`
	- Mục đích: xác nhận container mới đang chạy.

8. **Cleanup file `application.yml` đã tạo tạm**
	- Step này chạy với `if: always()`.
	- Xóa `src/main/resources/application.yml` khỏi workspace của runner để giảm rủi ro lộ thông tin.

---

## 4. Sơ đồ luồng tổng quát

1. Developer push code -> GitHub
2. GitHub Actions trigger workflow
3. Self-hosted runner (VM) nhận job
4. Runner build image từ Dockerfile
5. Runner dừng container cũ và chạy container mới
6. Backend phiên bản mới hoạt động trên VM

---

## 5. Ghi chú quan trọng

- Pipeline hiện tại đang bỏ qua test khi build JAR (`-Dmaven.test.skip=true`), nên tốc độ nhanh hơn nhưng có rủi ro nếu code lỗi logic.
- Tính sẵn sàng của hệ thống phụ thuộc vào self-hosted runner: nếu VM tắt hoặc runner offline thì pipeline không chạy.
- Cấu hình cổng chạy container đang lấy từ secret/env:
  - `DOCKER_HOST_PORT`
  - `APP_INTERNAL_PORT`

