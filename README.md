# Hướng dẫn Cài đặt và Chạy dự án

Hướng dẫn để cài đặt môi trường và chạy dự án trên IntelliJ IDEA.

## Yêu cầu
-   [Oracle OpenJDK 23.0.1](https://www.oracle.com/java/technologies/downloads/#jdk23) (không cần nếu có ở Intellij)
-   [JavaFX SDK 23.0.1](https://gluonhq.com/products/javafx/) (ở thư mục lib trong project)
-   [IntelliJ IDEA](https://www.jetbrains.com/idea/download/)
-   Công cụ dòng lệnh [SQLite](https://www.sqlite.org/download.html) (tùy chọn)

Các thư viện JUnit 5 junit-jupiter-5.9.0, SQLite-JDBC đã được bao gồm trong thư mục `/lib` của dự án.

## Cài đặt

### 1. Lấy mã nguồn
Clone repository:
```bash
git clone https://github.com/phongviet/ITSS-Project-He-Thong-Soi-Day-Gan-Ket
```

### 2. Cài đặt công cụ dòng lệnh SQLite (Tùy chọn)
Để có thể xem và thao tác với cơ sở dữ liệu từ terminal:
-   Tải `sqlite-tools` từ trang chủ SQLite và thêm vào `PATH` của hệ thống.
-   Chạy lệnh hiển thị csdl có format (tùy chọn):
    ```bash
    echo .header on>.sqliterc & echo .mode column>>.sqliterc & move .sqliterc %USERPROFILE%
    ```

## Cấu hình dự án trong IntelliJ IDEA

### 1. Mở dự án
-   Mở IntelliJ IDEA.
-   Chọn `File -> Open...` và trỏ đến thư mục của dự án đã clone về.

### 2. Cấu hình Java JDK
-   Vào `File -> Project Structure...` (phím tắt: `Ctrl+Alt+Shift+S`).
-   Trong tab `Project`, ở mục `SDK`, chọn `Add SDK -> JDK...` và trỏ đến thư mục cài đặt OpenJDK 23.0.1 hoặc chọn có sẵn.
-   Bấm `Apply`.

### 3. Thêm các thư viện (JavaFX, JUnit, SQLite)

-   (Nếu chưa tự động nhận diện từ folder dự án)
-   Trong cửa sổ `Project Structure`, chuyển qua tab `Libraries`.
-   Bấm vào dấu `+` (Add) và chọn `Java`.
-   **Thêm thư viện có sẵn trong dự án (JUnit, SQLite):**
    -   Trỏ đến thư mục `lib` trong project.
    -   Chọn **tất cả** các file có đuôi `.jar` bên trong đó.
    -   Bấm `OK`. IntelliJ sẽ hỏi bạn muốn thêm vào module nào, hãy chọn module chính của project (thường có tên là tên project).
-   **Thêm thư viện JavaFX SDK:**
    -   Làm tương tự, bấm `+ -> Java`.
    -   Trỏ đến thư mục `lib` của **JavaFX SDK** mà bạn đã tải về (ví dụ: `C:\path\to\javafx-sdk-23.0.1\lib`).
    -   Bấm `OK` và thêm vào module.
-   Sau khi hoàn tất, bấm `Apply` và `OK` để đóng cửa sổ `Project Structure`.

### 4. Cấu hình để chạy ứng dụng

-   Vào `Run -> Edit Configurations...`.
-   Bấm vào dấu `+` (Add New Configuration) và chọn `Application`.
-   Đặt tên cho cấu hình (ví dụ: `Run App`).
-   Ở mục `Main class`, bấm vào biểu tượng `...` và chọn class Main của ứng dụng.
-   **Quan trọng:** Ở mục `Modify options`, chọn `Add VM options`.
-   Trong ô `VM options` vừa hiện ra, dán đoạn mã sau:
    ```
    --module-path "ĐƯỜNG_DẪN_TỚI_THƯ_MỤC_LIB_CỦA_JAVAFX_SDK" --add-modules javafx.controls,javafx.fxml
    ```
    **Lưu ý:** Thay thế `ĐƯỜNG_DẪN_TỚI_THƯ_MỤC_LIB_CỦA_JAVAFX_SDK` bằng đường dẫn thực tế hoặc trong folder lib của dự án (ví dụ: `D:\javafx-sdk-23.0.1\lib`). **Phải có dấu ngoặc kép `"` nếu đường dẫn chứa khoảng trắng.**
-   Bấm `Apply` và `OK`.

Chạy ứng dụng bằng cách bấm vào nút Run (hình tam giác màu xanh) trên thanh công cụ.

