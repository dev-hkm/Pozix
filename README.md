# Pozix

Một app quiz & luyện thi nho nhỏ tôi tự viết để học bài và tự kiểm tra kiến thức trên điện thoại.

Ban đầu tôi chỉ tính làm một cái app đơn giản để ném file JSON câu hỏi vào ôn bài cho đỡ chán. Nhưng làm tới đâu thấy cần tới đó nên tôi đắp thêm giao diện Material 3 bo tròn cho đẹp, hỗ trợ hiển thị công thức Toán Lý Hoá bằng KaTeX, cuộn mượt mà có hiệu ứng lò xo đàn hồi, rồi gắn luôn cả trợ lý AI vào để giải thích câu sai cho tiện.

---

## Có gì trong Pozix?

- **Ném JSON vào là có đề:** Copy text JSON, mở file từ máy hoặc share trực tiếp file/text từ bất kỳ ứng dụng nào khác vào Pozix, app sẽ tự validate và tạo bộ đề ngay lập tức.
- **Chế độ làm bài & thi:** Tính điểm trực tiếp, bấm giờ làm bài, các nút điều hướng nổi nhẹ nhàng, vuốt mượt mà không bị che khuất đáp án.
- **Toán học & Khoa học:** Render công thức LaTeX / KaTeX và Markdown chuẩn chỉ, không bị ngắt quãng hay lỗi font dù biểu thức phức tạp.
- **Trợ lý AI đồng hành:** Sau khi làm xong bài, AI có thể xem lại bài thi và phân tích cặn kẽ tại sao chọn sai. Bạn có thể cắm bất kỳ Provider nào (Gemini, DeepSeek, Groq, OpenAI...) bằng API key của chính mình.
- **Material 3 & Cảm ứng tự nhiên:** Tự động đồng bộ Dark/Light theme, thanh trạng thái status bar sắc nét, hiệu ứng cuộn nảy quán tính (rubber-band spring physics) trên mọi bottom sheet và màn hình preview.
- **14 Font chữ:** Tích hợp sẵn nhiều font chữ đẹp mắt (EB Garamond, Space Mono, Comic Relief, Ubuntu, Alata, Aleo...) để bạn chọn font đọc đề thoải mái nhất.
- **Riêng tư & Offline-first:** Toàn bộ đề thi và lịch sử được lưu cục bộ trong máy qua Room DB, không bắt tạo tài khoản, không quảng cáo.

---

## Mẫu cấu trúc JSON

Một bộ đề JSON đơn giản cho Pozix trông như thế này:

```json
{
  "title": "Ôn tập Sinh học 12",
  "description": "Bộ câu hỏi trắc nghiệm ôn tập nhanh",
  "questions": [
    {
      "type": "single_choice",
      "question": "Sinh vật phân huỷ đóng vai trò quan trọng trong việc trả lại chất dinh dưỡng cho môi trường.",
      "options": ["Đúng", "False"],
      "correctIndex": 0,
      "explanation": "Sinh vật phân huỷ phân giải xác hữu cơ thành chất vô cơ cho thực vật sử dụng."
    }
  ]
}
```

---

## Cài đặt

Tải tệp `.apk` bản phát hành mới nhất tại mục [Releases](https://github.com/dev-hkm/Pozix/releases) rồi cài trực tiếp vào điện thoại Android.

---

https://khanhminh.web.app
