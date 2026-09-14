package com.hkm.pozix.util

/** Shared by template documentation, copied prompts and the in-app assistant. */
object RichContentContract {
    fun guidance(language: String): String = if (language == "vi") """
ĐỊNH DẠNG POZIX HỖ TRỢ (không phải toàn bộ CommonMark):
- Trong lecture/question/explanation: # đến ######, **đậm**, *nghiêng*, ***đậm nghiêng***, ~~gạch ngang~~, `code`, ==tô nổi bật== và các badge highlight màu sắc: ==pink:từ khóa==, ==yellow:tiêu điểm==, ==blue:xem trước==, ==orange:lưu ý==, ==green:đạt chuẩn==, ==purple:công thức==, ==red:cảnh báo== (hoặc cú pháp [pink:nội dung]).
- Bài giảng lý thuyết (tùy chọn): trường "lecture" ở cấp độ quiz để tóm tắt bài giảng trước khi làm quiz, hỗ trợ đầy đủ Markdown, LaTeX, Code blocks, Badges và Tables.
- Trích dẫn: > nội dung, >> trích dẫn lồng nhau; danh sách -, +, *, 1.; checklist - [ ] và - [x] chỉ để hiển thị; bảng Markdown | cột | và --- ngăn đoạn.
- Code nhiều dòng: fence ba backtick kèm tên ngôn ngữ (python, java, kotlin, javascript, cpp, json, html, svg…). Khối mã HTML/SVG hỗ trợ nút bấm xem trước (Live Preview) trực tiếp trong app.
- Toán inline: ${'$'}x^2${'$'} hoặc \(x^2\), hiển thị bằng bộ ký hiệu gọn. Với phân số, tích phân, ma trận hoặc công thức phức tạp, dùng ${'$'}${'$'}\frac{a}{b}${'$'}${'$'} hoặc \[...\] để render bằng KaTeX offline.
- KaTeX hỗ trợ \frac{a}{b}, \sqrt{x}, \sum, \int, \lim, \begin{matrix}...\end{matrix}. Không giả định mọi gói LaTeX đều có; không dùng \usepackage, HTML tùy ý, JavaScript hay hình ảnh nhúng.
- Trong options: ưu tiên text, inline Markdown và toán inline; đặt công thức display/code/bảng dài vào question hoặc explanation.
- Visual trong question dùng mảng tùy chọn "media", không dùng Markdown image/HTML: {"type":"image","uri":"https://...","altText":"...","caption":"..."}; hoặc {"type":"geometry","preset":"cube|cuboid|prism|pyramid|cylinder|cone|sphere|coordinate_axes|triangle|angle"}; hoặc {"type":"diagram"|"mind_map","nodes":[{"id":"...","label":"...","x":0.2,"y":0.5}],"edges":[{"from":"...","to":"...","label":"..."}]}.
- Chỉ thêm media khi câu hỏi thật sự cần hình. Không bịa URL ảnh: nếu không có URL/nguồn ảnh thật, dùng geometry/diagram/mind_map. Tọa độ node nằm trong 0..1 và from/to phải trỏ tới node có thật.
- JSON phải hợp lệ: xuống dòng viết \n, dấu nháy viết \", một dấu \ của LaTeX viết \\ trong chuỗi JSON. Không đặt xuống dòng thô trong chuỗi. Markdown/code fence nằm TRONG chuỗi, không làm hỏng cấu trúc JSON.
    """.trimIndent() else """
POZIX FORMATTING SUPPORT (not full CommonMark):
- lecture/question/explanation: headings # through ######, **bold**, *italic*, ***bold italic***, ~~strikethrough~~, `inline code`, ==highlight== and colored badges: ==pink:keyword==, ==yellow:focus==, ==blue:preview==, ==orange:note==, ==green:passed==, ==purple:formula==, ==red:warning== (or [pink:content] syntax).
- Theory lecture (optional): "lecture" field at the quiz root level for summarizing concepts before testing, supporting full Markdown, LaTeX, Code blocks, Badges, and Tables.
- Blockquotes > and nested >>; lists -, +, *, 1.; display-only task lists - [ ] / - [x]; Markdown pipe tables and --- dividers.
- Fenced code: triple backticks plus a language (python, java, kotlin, javascript, cpp, json, html, svg…). HTML and SVG blocks support interactive in-app Live Preview.
- Inline math ${'$'}x^2${'$'} or \(x^2\) uses compact native notation. Use ${'$'}${'$'}\frac{a}{b}${'$'}${'$'} or \[...\] for complex fractions, sums, integrals and matrices rendered by offline KaTeX.
- KaTeX: \frac{a}{b}, \sqrt{x}, \sum, \int, \lim, \begin{matrix}...\end{matrix}. Do not assume arbitrary LaTeX packages, \usepackage, custom HTML, JavaScript or embedded images are supported.
- options: prefer text, inline Markdown and inline math. Put display equations, long code and tables in question/explanation.
- Visuals in question use an optional typed "media" array, never Markdown images or HTML: {"type":"image","uri":"https://...","altText":"...","caption":"..."}; or {"type":"geometry","preset":"cube|cuboid|prism|pyramid|cylinder|cone|sphere|coordinate_axes|triangle|angle"}; or {"type":"diagram"|"mind_map","nodes":[{"id":"...","label":"...","x":0.2,"y":0.5}],"edges":[{"from":"...","to":"...","label":"..."}]}.
- Add media only when it helps the question. Never invent image URLs: when no real image source exists, use geometry/diagram/mind_map. Node coordinates must stay in 0..1 and edge endpoints must reference real node ids.
- JSON escaping: newline = \n, quote = \", one LaTeX backslash = \\ inside JSON strings. No literal line breaks inside strings. Markdown/code fences belong INSIDE strings; keep the surrounding JSON valid.
    """.trimIndent()

    val example = """{
  "title": "Markdown and mathematics",
  "description": "Valid JSON with formatting inside strings",
  "lecture": "# Tóm tắt bài giảng\n==pink:Kiến thức cốt lõi== về đạo hàm và toán học:\n$$\\frac{d}{dx}x^n = n x^{n-1}$$\n> ==yellow:Ghi nhớ==: Luôn kiểm tra điều kiện xác định.",
  "questions": [
    {
      "type": "single_choice",
      "question": "### Review\n> **Principle:** prefer ==clear code==.\n> - Keep functions short\n> - [x] Use early returns\n\nWhat does this print?\n```python\nprint(2 + 3)\n```",
      "options": ["**5**", "`23`"],
      "correctIndex": 0,
      "explanation": "***Addition*** produces 5, not ~~23~~.",
      "media": [
        {"type": "geometry", "preset": "coordinate_axes", "caption": "A simple coordinate plane"}
      ]
    },
    {
      "type": "true_false",
      "question": "For x > 0:\n$$\\frac{d}{dx}x^2=2x$$",
      "correctAnswer": true,
      "explanation": "Use the power rule. Inline notation: ${'$'}x^2${'$'}."
    }
  ]
}"""
}
