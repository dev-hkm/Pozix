package com.hkm.pozix.util

/** Shared by template documentation, copied prompts and the in-app assistant. */
object RichContentContract {
    fun guidance(language: String): String = if (language == "vi") """
CẤU TRÚC 3 DẠNG CÂU HỎI CHUẨN THPTQG (GDPT 2018):
1. 'single_choice' (Phần I - Trắc nghiệm nhiều phương án lựa chọn):
   - Bắt buộc có 'options' (2-6 phương án, chuẩn đề thi là 4 phương án A, B, C, D).
   - Bắt buộc có 'correctIndex' (0, 1, 2, hoặc 3 tương ứng với A, B, C, D).
2. 'true_false' (Phần II - Trắc nghiệm Đúng/Sai):
   - Bắt buộc có 'correctAnswer': true (Đúng) hoặc false (Sai).
3. 'short_answer' (Phần III - Trắc nghiệm trả lời ngắn / Điền đáp án):
   - Bắt buộc có 'correctAnswer': giá trị đáp án dưới dạng số hoặc chuỗi ngắn (ví dụ: "1.5", "8", "-3", "3/4", "Hà Nội").
   - Tùy chọn 'acceptedAnswers': mảng các cách viết tương đương hợp lệ (ví dụ: ["1.5", "1,5", "3/2"]).

BÀI GIẢNG LÝ THUYẾT & TÓM TẮT TRỌNG TÂM ("lecture"):
- Tùy chọn trường "lecture" ở cấp độ root của quiz để tóm tắt bài giảng trước khi làm bài thi.
- Hỗ trợ đầy đủ Markdown, LaTeX, nhãn Badges, Lucide Vector Icons, bảng Markdown Tables, Code blocks và sơ đồ Flow giải bài.

HỆ THỐNG NHÃN PASTEL & LUCIDE VECTOR ICONS:
- 18 màu sắc Pastel chuyên nghiệp:
  pink, rose, yellow, amber, blue, cyan, teal, emerald, indigo, green, mint, orange, peach, purple, lavender, red, coral, gray.
- Các định dạng cú pháp hỗ trợ:
  * Nhãn có icon và màu: ==color:icon:Nội dung== hoặc [color:icon:Nội dung] (Ví dụ: ==yellow:lightbulb:Ý tưởng==, [teal:scan:Đọc dữ kiện]).
  * Nhãn icon tự nhận diện màu: ==icon:Nội dung== hoặc [icon:Nội dung] (Ví dụ: [check-circle:Chính xác], [alert-triangle:Lưu ý]).
  * Nhãn trơn không icon: ==color:Nội dung== hoặc [color:Nội dung] (Ví dụ: ==blue:Công thức==, [purple:Định lý]).
  * Thẻ HTML tương đương: <badge color="..." icon="...">Nội dung</badge> hoặc <mark color="..." icon="...">Nội dung</mark>.
- Danh mục hơn 160 Lucide Vector Icons hỗ trợ theo chuyên mục:
  * Quy trình & Flow giải bài: play-circle, circle-play, scan, brain, lightbulb, calculator, check-circle, flag, arrow-right, target, crosshair, settings, wrench, timer, clock, history.
  * Cảnh báo & Bẫy sai lầm: x-circle, circle-x, triangle-alert, alert-triangle, circle-alert, warning, info, help-circle, shield-alert.
  * Thành công & Kiểm tra: check, check-circle, circle-check, check-check, list-checks, clipboard-check, shield-check, award, trophy.
  * Khoa học & STEM: atom, flask-conical, flask, beaker, dna, biotech, function-square, binary, percent, sigma, radical, compass, gauge, trending-up, trending-down.
  * Học thuật & Khái niệm: scale, book, book-open, school, graduation-cap, library, bookmark, quote, file-text, notebook, landmark.
  * Tự nhiên & Không gian: globe, globe-2, earth, map-pin, sun, moon, wind.
  * Điểm nhấn & Ý tưởng: sparkles, zap, flame, star, pin, heart, thumbs-up, tag.
- CẤM TUYỆT ĐỐI dùng emoji thô trong nhãn hoặc nội dung văn bản (hệ thống tự render vector icon Lucide sắc nét, liền khối không thủng nền).
- Khuyến khích mô tả quy trình tư duy giải bài dạng sơ đồ Flow:
  [blue:play-circle:Bắt đầu]
  ↓
  [teal:scan:Đọc dữ kiện]
  ↓
  [purple:brain:Phân tích bản chất]
  ↓
  [yellow:lightbulb:Chọn phương pháp]
  ↓
  [orange:calculator:Tính toán]
  ↓
  [green:check-circle:Kiểm tra & Kết luận]

ĐỊNH DẠNG MARKDOWN HỖ TRỢ:
- Tiêu đề: # đến ######.
- Định dạng chữ: **đậm**, *nghiêng*, ***đậm nghiêng***, ~~gạch ngang~~, __đậm__, _nghiêng_, ___đậm nghiêng___.
- Trích dẫn & Hộp ghi nhớ (Callouts):
  > Nội dung trích dẫn
  >> Trích dẫn lồng nhau
  > [amber:lightbulb:Ghi nhớ quan trọng]: Luôn tìm điều kiện xác định trước khi giải.
- Danh sách & Checklists:
  * Danh sách không thứ tự: -, +, *
  * Danh sách có thứ tự: 1., 2., 3.
  * Danh sách kiểm tra (Task list): - [ ] Chưa hoàn thành, - [x] Đã hoàn thành
- Bảng Markdown (Markdown Pipe Tables):
  | Cột 1 | Cột 2 | Cột 3 |
  | :--- | :---: | ---: |
  | Căn trái | Căn giữa | Căn phải |
- Khối mã lập trình (Code blocks): Ba dấu backtick kèm tên ngôn ngữ (python, java, kotlin, javascript, typescript, cpp, json, html, svg, sql, xml...).
  * Khối mã HTML và SVG tự động hỗ trợ nút xem trước (Live Preview) và Trình duyệt Mini tương tác trực tiếp trong app.

CÔNG THỨC TOÁN HỌC & KHOA HỌC (LaTeX / KaTeX):
- Toán inline: ${'$'}công thức${'$'} hoặc \(công thức\) (Ví dụ: ${'$'}E = mc^2${'$'}, ${'$'}f'(x) = 3x^2 - 1${'$'}).
- Toán display / khối: ${'$'}${'$'}công thức${'$'}${'$'} hoặc \[công thức\] trên dòng riêng.
- BẮT BUỘC dùng cặp ngoặc nhọn {} cho phân số và căn thức:
  * Phân số: \frac{a}{b} hoặc \dfrac{a}{b} (Ví dụ: ${'$'}\frac{-b \pm \sqrt{\Delta}}{2a}${'$'}). TUYỆT ĐỐI KHÔNG viết thiếu ngoặc như dfrac12.
  * Căn thức: \sqrt{x}, \sqrt[3]{x}, \sqrt[n]{x}.
  * Tích phân, tổng, giới hạn: \int_{a}^{b} f(x)dx, \sum_{i=1}^{n} a_i, \lim_{x \to 0} \frac{\sin x}{x} = 1.
  * Ký hiệu: \alpha, \beta, \gamma, \Delta, \pi, \in, \subset, \cup, \cap, \le, \ge, \neq, \approx, \pm, \cdot, \times.
  * Hệ phương trình & Ma trận: \begin{cases} x + y = 5 \\ 2x - y = 1 \end{cases}, \begin{pmatrix} a & b \\ c & d \end{pmatrix}.
  * Hóa học: 2\text{H}_2 + \text{O}_2 \to 2\text{H}_2\text{O}.
- QUY TẮC ESCAPE JSON: Trong chuỗi JSON, mỗi dấu \ của LaTeX BẮT BUỘC viết thành \\ (ví dụ: "${'$'}${'$'}\\frac{a}{b}${'$'}${'$'}").

HÌNH ẢNH & ĐỒ HỌA TRONG CÂU HỎI ("media"):
- Mảng tùy chọn "media" trong từng câu hỏi:
  * Hình học không gian / giải tích tạo sẵn: {"type":"geometry","preset":"coordinate_axes|cube|cuboid|prism|pyramid|cylinder|cone|sphere|triangle|angle","caption":"..."}
  * Sơ đồ tư duy / đồ thị mạng: {"type":"diagram"|"mind_map","nodes":[{"id":"a","label":"...","x":0.2,"y":0.5}],"edges":[{"from":"a","to":"b","label":"..."}]}
  * Hình ảnh thực: {"type":"image","uri":"https://...","altText":"...","caption":"..."} (chỉ dùng URL thật, không bịa URL).
    """.trimIndent() else """
3 QUESTION TYPES SUPPORTED (Standard Exam Structure):
1. 'single_choice' (Part I - Multiple Choice):
   - Requires 'options' (2-6 options, standard 4 choices A, B, C, D).
   - Requires 'correctIndex' (0-indexed integer corresponding to correct choice).
2. 'true_false' (Part II - True/False):
   - Requires 'correctAnswer': boolean (true or false).
3. 'short_answer' (Part III - Short Answer / Fill-in-the-Blank):
   - Requires 'correctAnswer': concise number or short string (e.g. "1.5", "8", "-3", "3/4").
   - Optional 'acceptedAnswers': array of equivalent accepted representations (e.g. ["1.5", "1,5", "3/2"]).

THEORETICAL STUDY NOTES ("lecture"):
- Optional "lecture" field at quiz root to summarize theory, formulas, and concepts before taking the quiz.
- Fully supports Markdown, LaTeX, Badges, Lucide Vector Icons, Markdown Tables, Code blocks, and Flow step diagrams.

PASTEL BADGES & LUCIDE VECTOR ICONS:
- 18 professional pastel colors:
  pink, rose, yellow, amber, blue, cyan, teal, emerald, indigo, green, mint, orange, peach, purple, lavender, red, coral, gray.
- Syntax options:
  * Badge with icon and color: ==color:icon:Text== or [color:icon:Text] (e.g. ==yellow:lightbulb:Idea==, [teal:scan:Given Data]).
  * Auto-colored icon badge: ==icon:Text== or [icon:Text] (e.g. [check-circle:Correct], [alert-triangle:Notice]).
  * Plain badge without icon: ==color:Text== or [color:Text] (e.g. ==blue:Formula==, [purple:Theorem]).
  * HTML tags: <badge color="..." icon="...">Text</badge> or <mark color="..." icon="...">Text</mark>.
- Over 160 supported Lucide icons:
  * Flow & Problem Solving: play-circle, circle-play, scan, brain, lightbulb, calculator, check-circle, flag, arrow-right, target, crosshair, settings, wrench, timer, clock, history.
  * Alerts & Warnings: x-circle, circle-x, triangle-alert, alert-triangle, circle-alert, warning, info, help-circle, shield-alert.
  * Success & Verification: check, check-circle, circle-check, check-check, list-checks, clipboard-check, shield-check, award, trophy.
  * Science & STEM: atom, flask-conical, flask, beaker, dna, biotech, function-square, binary, percent, sigma, radical, compass, gauge, trending-up, trending-down.
  * Education & Theory: scale, book, book-open, school, graduation-cap, library, bookmark, quote, file-text, notebook, landmark.
  * Geography & Nature: globe, globe-2, earth, map-pin, sun, moon, wind.
  * Focus & Highlights: sparkles, zap, flame, star, pin, heart, thumbs-up, tag.
- NEVER use raw emojis in badges or text; the system renders crisp vector Lucide icons automatically without gaps.
- Encourage problem-solving flow diagrams:
  [blue:play-circle:Start]
  ↓
  [teal:scan:Given Data]
  ↓
  [purple:brain:Analysis]
  ↓
  [yellow:lightbulb:Select Method]
  ↓
  [orange:calculator:Calculation]
  ↓
  [green:check-circle:Check & Conclude]

MARKDOWN FORMATTING SUPPORT:
- Headings: # through ######.
- Typography: **bold**, *italic*, ***bold italic***, ~~strikethrough~~, __bold__, _italic_, ___bold italic___.
- Blockquotes & Callouts:
  > Quote
  >> Nested quote
  > [amber:lightbulb:Important Note]: Always verify domain constraints before solving.
- Lists & Checklists: -, +, *, 1., 2., - [ ] Incomplete, - [x] Completed.
- Markdown Pipe Tables:
  | Header 1 | Header 2 | Header 3 |
  | :--- | :---: | ---: |
  | Cell 1 | Cell 2 | Cell 3 |
- Code Blocks: Triple backticks with language specifier (python, java, kotlin, javascript, typescript, cpp, json, html, svg, sql, xml...).
  * HTML and SVG code blocks support interactive in-app Live Preview & Fullscreen Mini Browser.

MATHEMATICS & SCIENCE (LaTeX / KaTeX):
- Inline math: ${'$'}formula${'$'} or \(formula\) (e.g. ${'$'}E = mc^2${'$'}).
- Display math: ${'$'}${'$'}formula${'$'}${'$'} or \[formula\] on dedicated lines.
- ALWAYS use curly braces {} for fractions and roots:
  * Fractions: \frac{a}{b} or \dfrac{a}{b} (e.g. ${'$'}\frac{-b \pm \sqrt{\Delta}}{2a}${'$'}). NEVER write unbraced macros like dfrac12.
  * Roots: \sqrt{x}, \sqrt[3]{x}, \sqrt[n]{x}.
  * Calculus: \int_{a}^{b} f(x)dx, \sum_{i=1}^{n} a_i, \lim_{x \to 0} \frac{\sin x}{x} = 1.
  * Symbols: \alpha, \beta, \gamma, \Delta, \pi, \in, \subset, \cup, \cap, \le, \ge, \neq, \approx, \pm, \cdot, \times.
  * Systems & Matrices: \begin{cases} x + y = 5 \\ 2x - y = 1 \end{cases}, \begin{pmatrix} a & b \\ c & d \end{pmatrix}.
  * Chemistry: 2\text{H}_2 + \text{O}_2 \to 2\text{H}_2\text{O}.
- JSON ESCAPING RULE: In JSON strings, every LaTeX backslash \ MUST be escaped as double backslash \\ (e.g. "${'$'}${'$'}\\frac{a}{b}${'$'}${'$'}").

MEDIA GRAPHICS IN QUESTIONS ("media"):
- Optional "media" array:
  * Built-in geometry: {"type":"geometry","preset":"coordinate_axes|cube|cuboid|prism|pyramid|cylinder|cone|sphere|triangle|angle","caption":"..."}
  * Diagrams & mind maps: {"type":"diagram"|"mind_map","nodes":[{"id":"a","label":"...","x":0.2,"y":0.5}],"edges":[{"from":"a","to":"b","label":"..."}]}
  * Real image: {"type":"image","uri":"https://...","altText":"...","caption":"..."} (real URLs only).
    """.trimIndent()

    val example = """{
  "title": "Bộ đề thi chuẩn THPTQG 3 Dạng",
  "description": "Ví dụ mẫu tích hợp bài giảng lý thuyết, Lucide icons, bảng Markdown và công thức toán LaTeX",
  "language": "vi",
  "lecture": "# Tóm tắt bài giảng & Phương pháp giải\n\n==pink:sparkles:Khái niệm cốt lõi== về đạo hàm và ứng dụng khảo sát hàm số:\n\n$$\\frac{d}{dx} x^n = n x^{n-1} \\quad (n \\in \\mathbb{R})$$\n\n### Bảng công thức đạo hàm cơ bản:\n\n| Dạng hàm | Đạo hàm ${'$'}f'(x)${'$'} | Điều kiện xác định |\n| :--- | :--- | :--- |\n| ${'$'}y = x^n${'$'} | ${'$'}y' = n x^{n-1}${'$'} | ${'$'}x > 0${'$'} nếu ${'$'}n \\notin \\mathbb{Z}${'$'} |\n| ${'$'}y = \\sqrt{x}${'$'} | ${'$'}y' = \\frac{1}{2\\sqrt{x}}${'$'} | ${'$'}x > 0${'$'} |\n| ${'$'}y = \\ln x${'$'} | ${'$'}y' = \\frac{1}{x}${'$'} | ${'$'}x > 0${'$'} |\n| ${'$'}y = e^x${'$'} | ${'$'}y' = e^x${'$'} | ${'$'}x \\in \\mathbb{R}${'$'} |\n\n### Quy trình 5 bước khảo sát hàm số:\n[blue:play-circle:Bắt đầu]\n↓\n[teal:scan:Đọc dữ kiện]\n↓\n[purple:brain:Tính đạo hàm f'(x)]\n↓\n[orange:calculator:Tìm nghiệm f'(x) = 0]\n↓\n[green:check-circle:Lập bảng biến thiên & Kết luận]\n\n> [amber:lightbulb:Ghi nhớ quan trọng]:\n> - Điểm ${'$'}x_0${'$'} là điểm cực trị khi và chỉ khi đạo hàm ${'$'}f'(x)${'$'} đổi dấu qua ${'$'}x_0${'$'}.\n> - Luôn tìm tập xác định trước khi tính toán.",
  "questions": [
    {
      "type": "single_choice",
      "question": "### Phần I: Câu trắc nghiệm nhiều phương án lựa chọn\n[blue:book:Khảo sát hàm số]\nCho hàm số ${'$'}f(x) = x^2 - 4x + 3${'$'}. Tọa độ đỉnh của parabol là:",
      "options": ["(2, -1)", "(1, 0)", "(-2, 15)", "(0, 3)"],
      "correctIndex": 0,
      "explanation": "Ta có tọa độ đỉnh parabol ${'$'}I(x_0, y_0)${'$'}:\n- Hoành độ: ${'$'}x_0 = -\\frac{b}{2a} = -\\frac{-4}{2 \\cdot 1} = 2${'$'}.\n- Tung độ: ${'$'}y_0 = f(2) = 2^2 - 4(2) + 3 = -1${'$'}.\n[green:check-circle:Chính xác]: Tọa độ đỉnh là (2, -1).",
      "media": [
        {"type": "geometry", "preset": "coordinate_axes", "caption": "Hệ trục tọa độ Oxy và parabol"}
      ]
    },
    {
      "type": "true_false",
      "question": "### Phần II: Câu trắc nghiệm Đúng/Sai\n[purple:brain:Xét tính đơn điệu]\nCho hàm số ${'$'}f(x) = x^3 - 3x${'$'}. Xét tính đúng/sai của mệnh đề sau:\n$$\\text{Mệnh đề: Hàm số } f(x) \\text{ đồng biến trên khoảng } (1; +\\infty)$$",
      "correctAnswer": true,
      "explanation": "Đạo hàm: ${'$'}f'(x) = 3x^2 - 3 = 3(x - 1)(x + 1)${'$'}.\nVới mọi ${'$'}x > 1${'$'}, ta có ${'$'}x - 1 > 0${'$'} và ${'$'}x + 1 > 0${'$'}, suy ra ${'$'}f'(x) > 0${'$'}.\n[green:check-circle:Đúng]: Do ${'$'}f'(x) > 0${'$'} trên $(1; +\\infty)$ nên hàm số đồng biến trên khoảng này."
    },
    {
      "type": "short_answer",
      "question": "### Phần III: Câu trắc nghiệm trả lời ngắn\n[orange:calculator:Tìm giá trị cực trị]\nCho hàm số ${'$'}y = x^2 - 4x + 5${'$'}. Tìm giá trị nhỏ nhất của hàm số trên ${'$'}\\mathbb{R}${'$'}.",
      "correctAnswer": "1",
      "acceptedAnswers": ["1", "1.0", "1,0"],
      "explanation": "Biến đổi tam thức bậc hai:\n$${'$'}y = x^2 - 4x + 4 + 1 = (x - 2)^2 + 1${'$'}$$\nVì ${'$'}(x - 2)^2 \\ge 0${'$'} với mọi ${'$'}x \\in \\mathbb{R}${'$'} nên ${'$'}y \\ge 1${'$'}.\nDấu bằng xảy ra khi ${'$'}x = 2${'$'}.\n[green:check-circle:Kết quả]: Giá trị nhỏ nhất bằng 1."
    }
  ]
}"""
}
