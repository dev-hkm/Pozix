package com.hkm.pozix.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hkm.pozix.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PromptItem(
    val id: String,
    val title: String,
    val content: String,
    val isExpanded: Boolean = false
)

data class PromptTemplatesUiState(
    val presetPrompts: List<PromptItem> = emptyList(),
    val customTopic: String = "",
    val generatedPrompt: String = "",
    val currentLanguage: String = "en"
)

class PromptTemplatesViewModel(application: Application) : AndroidViewModel(application) {
    
    private val settingsRepository = SettingsRepository(application)
    
    private val _uiState = MutableStateFlow(PromptTemplatesUiState())
    val uiState: StateFlow<PromptTemplatesUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            settingsRepository.getLanguage().collect { language ->
                _uiState.value = _uiState.value.copy(
                    currentLanguage = language,
                    presetPrompts = getPresetPrompts(language)
                )
            }
        }
    }
    
    fun toggleExpand(promptId: String) {
        val updatedPrompts = _uiState.value.presetPrompts.map { prompt ->
            if (prompt.id == promptId) {
                prompt.copy(isExpanded = !prompt.isExpanded)
            } else {
                prompt
            }
        }
        _uiState.value = _uiState.value.copy(presetPrompts = updatedPrompts)
    }
    
    fun updateCustomTopic(topic: String) {
        _uiState.value = _uiState.value.copy(customTopic = topic)
    }
    
    fun generateCustomPrompt() {
        val topic = _uiState.value.customTopic
        if (topic.isBlank()) return
        
        val prompt = buildCustomPrompt(topic, _uiState.value.currentLanguage)
        _uiState.value = _uiState.value.copy(generatedPrompt = prompt)
    }
    
    private fun buildCustomPrompt(topic: String, language: String): String {
        return if (language == "vi") {
            """
Tạo một bộ quiz về "$topic" ở định dạng JSON. Chỉ trả về JSON hợp lệ, không có markdown fences hay bình luận.

Schema:
{
  "title": "Tiêu đề quiz",
  "description": "Mô tả tùy chọn",
  "language": "vi",
  "questions": [
    {
      "type": "single_choice",
      "question": "Nội dung câu hỏi",
      "options": ["Lựa chọn 1", "Lựa chọn 2", "Lựa chọn 3", "Lựa chọn 4"],
      "correctIndex": 0,
      "explanation": "Giải thích tùy chọn"
    },
    {
      "type": "true_false",
      "question": "Nội dung câu hỏi",
      "correctAnswer": true,
      "explanation": "Giải thích tùy chọn"
    }
  ]
}

Yêu cầu:
- Tạo 10-15 câu hỏi
- Kết hợp cả hai loại single_choice và true_false
- single_choice phải có 2-6 lựa chọn
- correctIndex phải hợp lệ (bắt đầu từ 0)
- Câu hỏi phải rõ ràng và có thể trả lời được
- Với môn Toán, Lý, Hóa: BẮT BUỘC dùng cú pháp LaTeX chuẩn và đóng gói công thức trong dấu đô la ${'$'}...${'$'} (inline) hoặc ${'$'}${'$'}...${'$'}${'$'} (display).
  * Phân số: Phải có ngoặc nhọn đầy đủ: \\frac{a}{b} hoặc \\dfrac{a}{b} (Ví dụ: "${'$'}f'(x) = \\frac{1}{x}${'$'}", KHÔNG ĐƯỢC viết thiếu ngoặc như dfrac1x).
  * Ký hiệu toán học: \\cdot (nhân), \\sqrt{x} (căn), \\int_{a}^{b} (tích phân), \\lim_{x \\to 0} (giới hạn), \\ln x, \\sin x, x^2, x_1.
  * Trong chuỗi JSON, ký tự gạch chéo ngược phải escape thành 2 gạch: \\\\frac, \\\\sqrt, \\\\cdot.
- Với môn Tin học / Lập trình: Dùng markdown code block (```python, ```cpp, ```java...) cho đoạn code và `code` cho mã inline.
- Tránh trùng lặp
- Giữ cho nội dung chuẩn xác và có tính giáo dục
            """.trimIndent() + "\n\n" + com.hkm.pozix.util.RichContentContract.guidance("vi")
        } else {
            """
Generate a quiz about "$topic" in JSON format. Return ONLY valid JSON without markdown fences or any commentary.

Schema:
{
  "title": "Quiz title",
  "description": "Optional description",
  "language": "en",
  "questions": [
    {
      "type": "single_choice",
      "question": "Question text",
      "options": ["Option 1", "Option 2", "Option 3", "Option 4"],
      "correctIndex": 0,
      "explanation": "Optional explanation"
    },
    {
      "type": "true_false",
      "question": "Question text",
      "correctAnswer": true,
      "explanation": "Optional explanation"
    }
  ]
}

Requirements:
- Generate 10-15 questions
- Mix single_choice and true_false types
- single_choice must have 2-6 options
- correctIndex must be valid (0-based)
- Questions should be clear and answerable
- For STEM (Math, Physics, Chem): MUST use standard LaTeX syntax enclosed in ${'$'}...${'$'} (inline) or ${'$'}${'$'}...${'$'}${'$'} (display).
  * Fractions: Always wrap in curly braces: \\frac{a}{b} or \\dfrac{a}{b} (e.g. "${'$'}f'(x) = \\frac{1}{x}${'$'}", NEVER omit braces like dfrac1x).
  * Symbols: \\cdot (dot), \\sqrt{x} (root), \\int_{a}^{b} (integral), \\lim_{x \\to 0} (limit), \\ln x, \\sin x, x^2, x_1.
  * In JSON, escape all backslashes as double backslashes: \\\\frac, \\\\sqrt, \\\\cdot.
- For Computer Science: Use markdown code blocks (```python, ```cpp...) for multi-line code and `code` for inline code.
- Avoid duplicates
- Keep it educational and accurate
            """.trimIndent() + "\n\n" + com.hkm.pozix.util.RichContentContract.guidance("en")
        }
    }
    
    private fun getPresetPrompts(language: String): List<PromptItem> {
        return if (language == "vi") {
            getVietnamesePrompts()
        } else {
            getEnglishPrompts()
        }
    }
    
    private fun getEnglishPrompts(): List<PromptItem> {
        return listOf(
            PromptItem(
                id = "general_trivia",
                title = "General Trivia",
                content = """
Generate a general trivia quiz in JSON format. Return ONLY valid JSON without markdown fences or any commentary.

Schema:
{
  "title": "General Trivia Challenge",
  "description": "Test your general knowledge",
  "language": "en",
  "questions": [
    {
      "type": "single_choice",
      "question": "Question text",
      "options": ["Option 1", "Option 2", "Option 3", "Option 4"],
      "correctIndex": 0,
      "explanation": "Optional explanation"
    },
    {
      "type": "true_false",
      "question": "Question text",
      "correctAnswer": true,
      "explanation": "Optional explanation"
    }
  ]
}

Requirements:
- Generate 12-15 questions
- Mix single_choice and true_false types
- Cover various topics: science, history, geography, culture
- single_choice must have 2-6 options
- correctIndex must be valid (0-based)
- Questions should be clear and answerable
                """.trimIndent()
            ),
            PromptItem(
                id = "riddles",
                title = "Riddles & Logic",
                content = """
Generate a riddles and logic puzzle quiz in JSON format. Return ONLY valid JSON without markdown fences or any commentary.

Schema:
{
  "title": "Riddles & Logic Challenge",
  "description": "Test your logical thinking",
  "language": "en",
  "questions": [
    {
      "type": "single_choice",
      "question": "Riddle or logic question",
      "options": ["Option 1", "Option 2", "Option 3", "Option 4"],
      "correctIndex": 0,
      "explanation": "Explanation of the answer"
    }
  ]
}

Requirements:
- Generate 10-12 riddles and logic questions
- Use mostly single_choice type
- Options should be 3-4 per question
- Include clever riddles, lateral thinking, and logic puzzles
- Provide clear explanations
                """.trimIndent()
            ),
            PromptItem(
                id = "school",
                title = "School Knowledge",
                content = """
Generate a school knowledge quiz in JSON format. Return ONLY valid JSON without markdown fences or any commentary.

Schema:
{
  "title": "School Knowledge Quiz",
  "description": "Test your academic knowledge",
  "language": "en",
  "questions": [
    {
      "type": "single_choice",
      "question": "Question text",
      "options": ["Option 1", "Option 2", "Option 3", "Option 4"],
      "correctIndex": 0,
      "explanation": "Optional explanation"
    },
    {
      "type": "true_false",
      "question": "Question text",
      "correctAnswer": true,
      "explanation": "Optional explanation"
    }
  ]
}

Requirements:
- Generate 15 questions
- Cover math, science, literature, history
- Mix single_choice and true_false types
- Suitable for high school level
- Provide helpful explanations
                """.trimIndent()
            ),
            PromptItem(
                id = "fun_mixed",
                title = "Fun Mixed Quiz",
                content = """
Generate a fun mixed quiz in JSON format. Return ONLY valid JSON without markdown fences or any commentary.

Schema:
{
  "title": "Fun Mixed Quiz",
  "description": "A fun variety quiz",
  "language": "en",
  "questions": [
    {
      "type": "single_choice",
      "question": "Question text",
      "options": ["Option 1", "Option 2", "Option 3"],
      "correctIndex": 0,
      "explanation": "Optional explanation"
    },
    {
      "type": "true_false",
      "question": "Question text",
      "correctAnswer": true,
      "explanation": "Optional explanation"
    }
  ]
}

Requirements:
- Generate 12 questions
- Mix topics: pop culture, food, animals, sports, technology
- Mix single_choice and true_false types
- Keep it light and entertaining
- Vary option counts (2-5 options)
                """.trimIndent()
            ),
            PromptItem(
                id = "true_false",
                title = "True/False Focused",
                content = """
Generate a true/false focused quiz in JSON format. Return ONLY valid JSON without markdown fences or any commentary.

Schema:
{
  "title": "True or False Challenge",
  "description": "Can you spot the truth?",
  "language": "en",
  "questions": [
    {
      "type": "true_false",
      "question": "Statement to evaluate",
      "correctAnswer": true,
      "explanation": "Explanation"
    }
  ]
}

Requirements:
- Generate 15-20 true/false questions
- Use ONLY true_false type
- Cover various interesting facts
- Make some tricky but fair
- Provide clear explanations
                """.trimIndent()
            )
        )
    }
    
    private fun getVietnamesePrompts(): List<PromptItem> {
        return listOf(
            PromptItem(
                id = "general_trivia",
                title = "Kiến thức tổng hợp",
                content = """
Tạo một bộ quiz kiến thức tổng hợp ở định dạng JSON. Chỉ trả về JSON hợp lệ, không có markdown fences hay bình luận.

Schema:
{
  "title": "Thử thách Kiến thức Tổng hợp",
  "description": "Kiểm tra kiến thức tổng quát của bạn",
  "language": "vi",
  "questions": [
    {
      "type": "single_choice",
      "question": "Nội dung câu hỏi",
      "options": ["Lựa chọn 1", "Lựa chọn 2", "Lựa chọn 3", "Lựa chọn 4"],
      "correctIndex": 0,
      "explanation": "Giải thích tùy chọn"
    },
    {
      "type": "true_false",
      "question": "Nội dung câu hỏi",
      "correctAnswer": true,
      "explanation": "Giải thích tùy chọn"
    }
  ]
}

Yêu cầu:
- Tạo 12-15 câu hỏi
- Kết hợp cả hai loại single_choice và true_false
- Bao gồm nhiều chủ đề: khoa học, lịch sử, địa lý, văn hóa
- single_choice phải có 2-6 lựa chọn
- correctIndex phải hợp lệ (bắt đầu từ 0)
- Câu hỏi phải rõ ràng và có thể trả lời được
                """.trimIndent()
            ),
            PromptItem(
                id = "riddles",
                title = "Câu đố & Logic",
                content = """
Tạo một bộ quiz câu đố và logic ở định dạng JSON. Chỉ trả về JSON hợp lệ, không có markdown fences hay bình luận.

Schema:
{
  "title": "Thử thách Câu đố & Logic",
  "description": "Kiểm tra tư duy logic của bạn",
  "language": "vi",
  "questions": [
    {
      "type": "single_choice",
      "question": "Câu đố hoặc câu hỏi logic",
      "options": ["Lựa chọn 1", "Lựa chọn 2", "Lựa chọn 3", "Lựa chọn 4"],
      "correctIndex": 0,
      "explanation": "Giải thích đáp án"
    }
  ]
}

Yêu cầu:
- Tạo 10-12 câu đố và câu hỏi logic
- Chủ yếu sử dụng loại single_choice
- Mỗi câu nên có 3-4 lựa chọn
- Bao gồm câu đố thông minh, tư duy ngang và câu đố logic
- Cung cấp giải thích rõ ràng
                """.trimIndent()
            ),
            PromptItem(
                id = "school",
                title = "Kiến thức học đường",
                content = """
Tạo một bộ quiz kiến thức học đường ở định dạng JSON. Chỉ trả về JSON hợp lệ, không có markdown fences hay bình luận.

Schema:
{
  "title": "Quiz Kiến thức Học đường",
  "description": "Kiểm tra kiến thức học thuật của bạn",
  "language": "vi",
  "questions": [
    {
      "type": "single_choice",
      "question": "Nội dung câu hỏi",
      "options": ["Lựa chọn 1", "Lựa chọn 2", "Lựa chọn 3", "Lựa chọn 4"],
      "correctIndex": 0,
      "explanation": "Giải thích tùy chọn"
    },
    {
      "type": "true_false",
      "question": "Nội dung câu hỏi",
      "correctAnswer": true,
      "explanation": "Giải thích tùy chọn"
    }
  ]
}

Yêu cầu:
- Tạo 15 câu hỏi
- Bao gồm toán, khoa học, văn học, lịch sử
- Kết hợp cả hai loại single_choice và true_false
- Phù hợp với trình độ trung học phổ thông
- Cung cấp giải thích hữu ích
                """.trimIndent()
            ),
            PromptItem(
                id = "fun_mixed",
                title = "Quiz vui nhộn",
                content = """
Tạo một bộ quiz vui nhộn đa dạng ở định dạng JSON. Chỉ trả về JSON hợp lệ, không có markdown fences hay bình luận.

Schema:
{
  "title": "Quiz Vui Nhộn",
  "description": "Một bộ quiz đa dạng thú vị",
  "language": "vi",
  "questions": [
    {
      "type": "single_choice",
      "question": "Nội dung câu hỏi",
      "options": ["Lựa chọn 1", "Lựa chọn 2", "Lựa chọn 3"],
      "correctIndex": 0,
      "explanation": "Giải thích tùy chọn"
    },
    {
      "type": "true_false",
      "question": "Nội dung câu hỏi",
      "correctAnswer": true,
      "explanation": "Giải thích tùy chọn"
    }
  ]
}

Yêu cầu:
- Tạo 12 câu hỏi
- Kết hợp các chủ đề: văn hóa đại chúng, ẩm thực, động vật, thể thao, công nghệ
- Kết hợp cả hai loại single_choice và true_false
- Giữ cho nội dung nhẹ nhàng và giải trí
- Thay đổi số lượng lựa chọn (2-5 lựa chọn)
                """.trimIndent()
            ),
            PromptItem(
                id = "true_false",
                title = "Tập trung Đúng/Sai",
                content = """
Tạo một bộ quiz tập trung vào Đúng/Sai ở định dạng JSON. Chỉ trả về JSON hợp lệ, không có markdown fences hay bình luận.

Schema:
{
  "title": "Thử thách Đúng hay Sai",
  "description": "Bạn có thể phát hiện sự thật?",
  "language": "vi",
  "questions": [
    {
      "type": "true_false",
      "question": "Câu phát biểu cần đánh giá",
      "correctAnswer": true,
      "explanation": "Giải thích"
    }
  ]
}

Yêu cầu:
- Tạo 15-20 câu hỏi đúng/sai
- CHỈ sử dụng loại true_false
- Bao gồm nhiều sự thật thú vị
- Làm cho một số câu khó nhưng công bằng
- Cung cấp giải thích rõ ràng
                """.trimIndent()
            )
        )
    }
}
