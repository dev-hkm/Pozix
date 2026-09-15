package com.hkm.pozix.util

/** Shared by template documentation, copied prompts and the in-app assistant. */
object RichContentContract {
    fun guidance(language: String): String = if (language == "vi") """
CẤU TRÚC 3 DẠNG CÂU HỎI CHUẨN THPTQG (GDPT 2018):
1. 'single_choice' (Phần I - Trắc nghiệm nhiều phương án):
   - Bắt buộc có 'options' (2-6 phương án, chuẩn đề thi là 4 phương án A, B, C, D).
   - Bắt buộc có 'correctIndex' (0, 1, 2, hoặc 3 tương ứng với A, B, C, D).
2. 'true_false' (Phần II - Trắc nghiệm Đúng/Sai):
   - Bắt buộc có 'correctAnswer': true (Đúng) hoặc false (Sai).
3. 'short_answer' (Phần III - Trắc nghiệm trả lời ngắn / Điền đáp án):
   - Bắt buộc có 'correctAnswer': giá trị đáp án dưới dạng số hoặc chuỗi ngắn (ví dụ: "1.5", "8", "-3", "3/4", "Hà Nội").
   - Tùy chọn 'acceptedAnswers': mảng các cách viết tương đương hợp lệ (ví dụ: ["1.5", "1,5", "3/2"]).

ĐỊNH DẠNG POZIX HỖ TRỢ (không phải toàn bộ CommonMark):
- Trong lecture/question/explanation: # đến ######, **đậm**, *nghiêng*, ***đậm nghiêng***, ~~gạch ngang~~, `code`, ==tô nổi bật==.
- Hệ thống nhãn Pastel (Badges) & Lucide Vector Icons (tùy chọn, do AI tự quyết định chọn icon phù hợp hoặc để trống):
  * Màu hỗ trợ: pink, rose, yellow, amber, blue, cyan, teal, emerald, indigo, green, mint, orange, peach, purple, lavender, red, coral, gray.
  * Cú pháp nhãn có icon: ==color:icon:Nội dung==, [color:icon:Nội dung], [icon:Nội dung], ==icon:Nội dung==, hoặc <badge color="..." icon="...">Nội dung</badge>.
  * Cú pháp nhãn trơn (không icon): ==color:Nội dung== hoặc [color:Nội dung].
  * Danh sách icon Lucide chuẩn hỗ trợ (hơn 150 glyph):
    - Chú ý & Cảnh báo: x-circle, circle-x, triangle-alert, alert-triangle, circle-alert, warning, info.
    - Thành công & Hoàn thành: check, check-circle, circle-check, check-check, checklist.
    - Học tập & Khái niệm: scale (cân công lý/định nghĩa), book, book-open, school (mũ cử nhân), landmark (lịch sử/đền đài), quote.
    - Địa lý & Toàn cầu: globe, globe-2, earth, compass.
    - STEM & Khoa học: calculator, percent, atom, flask, biotech, dna, psychology (não bộ/tư duy), trending-up, trending-down.
    - Điểm nhấn & Ý tưởng: lightbulb, sparkles, zap, flame, star, target, award, trophy, clock, shield.
  * CẤM TUYỆT ĐỐI chèn ký tự emoji vào trong nhãn hoặc nội dung (hệ thống tự vẽ vector icon Lucide sắc nét).
- Bài giảng lý thuyết (tùy chọn): trường "lecture" ở cấp độ quiz để tóm tắt bài giảng trước khi làm quiz, hỗ trợ đầy đủ Markdown, LaTeX, Code blocks, Badges và Tables.
- Trích dẫn: > nội dung, >> trích dẫn lồng nhau; danh sách -, +, *, 1.; checklist - [ ] và - [x] chỉ để hiển thị; bảng Markdown | cột | và --- ngăn đoạn.
- Code nhiều dòng: fence ba backtick kèm tên ngôn ngữ (python, java, kotlin, javascript, cpp, json, html, svg…). Khối mã HTML/SVG hỗ trợ nút bấm xem trước (Live Preview) và Trình duyệt Mini toàn màn hình trực tiếp trong app.
- Toán inline: ${'$'}x^2${'$'} hoặc \(x^2\), hiển thị bằng bộ ký hiệu gọn. Với phân số, tích phân, ma trận hoặc công thức phức tạp, dùng ${'$'}${'$'}\frac{a}{b}${'$'}${'$'} hoặc \[...\] để render bằng KaTeX offline.
- KaTeX hỗ trợ \frac{a}{b}, \sqrt{x}, \sum, \int, \lim, \begin{matrix}...\end{matrix}. Không giả định mọi gói LaTeX đều có; không dùng \usepackage, HTML tùy ý, JavaScript hay hình ảnh nhúng.
- Trong options: ưu tiên text, inline Markdown và toán inline; đặt công thức display/code/bảng dài vào question hoặc explanation.
- Visual trong question dùng mảng tùy chọn "media", không dùng Markdown image/HTML: {"type":"image","uri":"https://...","altText":"...","caption":"..."}; hoặc {"type":"geometry","preset":"cube|cuboid|prism|pyramid|cylinder|cone|sphere|coordinate_axes|triangle|angle"}; hoặc {"type":"diagram"|"mind_map","nodes":[{"id":"...","label":"...","x":0.2,"y":0.5}],"edges":[{"from":"...","to":"...","label":"..."}]}.
- Chỉ thêm media khi câu hỏi thật sự cần hình. Không bịa URL ảnh: nếu không có URL/nguồn ảnh thật, dùng geometry/diagram/mind_map. Tọa độ node nằm trong 0..1 và from/to phải trỏ tới node có thật.
- JSON phải hợp lệ: xuống dòng viết \n, dấu nháy viết \", một dấu \ của LaTeX viết \\ trong chuỗi JSON. Không đặt xuống dòng thô trong chuỗi. Markdown/code fence nằm TRONG chuỗi, không làm hỏng cấu trúc JSON.
    """.trimIndent() else """
3 QUESTION TYPES SUPPORTED (Standard Exam Structure):
1. 'single_choice': multiple choice questions with 'options' (2-6 options) and 'correctIndex' (0-indexed).
2. 'true_false': true/false questions with 'correctAnswer' (boolean: true or false).
3. 'short_answer': short answer / fill-in-the-blank with 'correctAnswer' (string/number) and optional 'acceptedAnswers' (list of valid equivalent strings).

POZIX FORMATTING SUPPORT (not full CommonMark):
- lecture/question/explanation: headings # through ######, **bold**, *italic*, ***bold italic***, ~~strikethrough~~, `inline code`, ==highlight==.
- Pastel Badges & Lucide Vector Icons (optional, AI decides appropriate icon or leaves plain):
  * Supported colors: pink, rose, yellow, amber, blue, cyan, teal, emerald, indigo, green, mint, orange, peach, purple, lavender, red, coral, gray.
  * Badge syntax with icon: ==color:icon:Content==, [color:icon:Content], [icon:Content], ==icon:Content==, or <badge color="..." icon="...">Content</badge>.
  * Plain badge syntax: ==color:Content== or [color:Content].
  * Supported Lucide icon names (150+ glyphs):
    - Alerts & Mistakes: x-circle, circle-x, triangle-alert, alert-triangle, circle-alert, warning, info.
    - Verification & Done: check, check-circle, circle-check, check-check, checklist.
    - Education & Concepts: scale (definitions/justice), book, book-open, school (grad cap), landmark (history), quote.
    - Geography & World: globe, globe-2, earth, compass.
    - STEM & Science: calculator, percent, atom, flask, biotech, dna, psychology (brain/logic), trending-up, trending-down.
    - Focus & Highlights: lightbulb, sparkles, zap, flame, star, target, award, trophy, clock, shield.
  * NEVER use raw emojis in badges or text; the system renders crisp Lucide vector icons automatically.
- Theory lecture (optional): "lecture" field at the quiz root level for summarizing concepts before testing, supporting full Markdown, LaTeX, Code blocks, Badges, and Tables.
- Blockquotes > and nested >>; lists -, +, *, 1.; display-only task lists - [ ] / - [x]; Markdown pipe tables and --- dividers.
- Fenced code: triple backticks plus a language (python, java, kotlin, javascript, cpp, json, html, svg…). HTML and SVG blocks support interactive in-app Live Preview & Fullscreen Mini Browser.
- Inline math ${'$'}x^2${'$'} or \(x^2\) uses compact native notation. Use ${'$'}${'$'}\frac{a}{b}${'$'}${'$'} or \[...\] for complex fractions, sums, integrals and matrices rendered by offline KaTeX.
- KaTeX: \frac{a}{b}, \sqrt{x}, \sum, \int, \lim, \begin{matrix}...\end{matrix}. Do not assume arbitrary LaTeX packages, \usepackage, custom HTML, JavaScript or embedded images are supported.
- options: prefer text, inline Markdown and inline math. Put display equations, long code and tables in question/explanation.
- Visuals in question use an optional typed "media" array, never Markdown images or HTML: {"type":"image","uri":"https://...","altText":"...","caption":"..."}; or {"type":"geometry","preset":"cube|cuboid|prism|pyramid|cylinder|cone|sphere|coordinate_axes|triangle|angle"}; or {"type":"diagram"|"mind_map","nodes":[{"id":"...","label":"...","x":0.2,"y":0.5}],"edges":[{"from":"...","to":"...","label":"..."}]}.
- Add media only when it helps the question. Never invent image URLs: when no real image source exists, use geometry/diagram/mind_map. Node coordinates must stay in 0..1 and edge endpoints must reference real node ids.
- JSON escaping: newline = \n, quote = \", one LaTeX backslash = \\ inside JSON strings. No literal line breaks inside strings. Markdown/code fences belong INSIDE strings; keep the surrounding JSON valid.
    """.trimIndent()

    val example = """{
  "title": "Bộ đề thi chuẩn THPTQG 3 Dạng",
  "description": "Ví dụ 3 dạng câu hỏi: Trắc nghiệm 4 lựa chọn, Đúng/Sai, Trả lời ngắn",
  "lecture": "# Tóm tắt bài giảng\n==pink:star:Kiến thức cốt lõi== về đạo hàm và toán học:\n$$\\frac{d}{dx}x^n = n x^{n-1}$$\n> [amber:lightbulb:Ghi nhớ]: Luôn kiểm tra điều kiện xác định.",
  "questions": [
    {
      "type": "single_choice",
      "question": "### Phần I: Câu trắc nghiệm nhiều phương án lựa chọn\nCho hàm số ${'$'}f(x) = x^2 - 4x + 3${'$'}. Tọa độ đỉnh của parabol là:",
      "options": ["(2, -1)", "(1, 0)", "(-2, 15)", "(0, 3)"],
      "correctIndex": 0,
      "explanation": "Đỉnh parabol có hoành độ ${'$'}x = -\\frac{b}{2a} = 2${'$'}, thay vào được ${'$'}y = -1${'$'}.",
      "media": [
        {"type": "geometry", "preset": "coordinate_axes", "caption": "Hệ trục tọa độ Oxy"}
      ]
    },
    {
      "type": "true_false",
      "question": "### Phần II: Câu trắc nghiệm Đúng/Sai\nCho hàm số ${'$'}f(x) = x^3 - 3x${'$'}. Mệnh đề sau đúng hay sai?\n$$\\text{Hàm số đồng biến trên khoảng } (1; +\\infty)$$",
      "correctAnswer": true,
      "explanation": "Ta có ${'$'}f'(x) = 3x^2 - 3 = 3(x-1)(x+1) > 0${'$'} với mọi ${'$'}x > 1${'$'}, do đó hàm số đồng biến."
    },
    {
      "type": "short_answer",
      "question": "### Phần III: Câu trắc nghiệm trả lời ngắn\nTìm giá trị cực tiểu của hàm số ${'$'}y = x^2 - 4x + 5${'$'}.",
      "correctAnswer": "1",
      "acceptedAnswers": ["1.0", "1,0", "1"],
      "explanation": "Ta có ${'$'}y = (x-2)^2 + 1 \\ge 1${'$'}. Giá trị cực tiểu bằng 1 tại ${'$'}x = 2${'$'}."
    }
  ]
}"""
}
