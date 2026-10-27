package com.example.model

enum class AppLanguage(val code: String, val displayName: String, val isRtl: Boolean) {
    FRENCH("fr", "Français", false),
    ENGLISH("en", "English", false),
    ARABIC("ar", "العربية", true);

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: FRENCH
        }
    }
}

enum class PrintType {
    PHOTO,
    PDF,
    DOCUMENT,
    COMPOSED
}

enum class ColorMode(val labelKey: String) {
    COLOR("color_full"),
    MONOCHROME("color_bw")
}

enum class PaperSize(val displayName: String) {
    A4("A4 (210 x 297 mm)"),
    A3("A3 (297 x 420 mm)"),
    LETTER("US Letter (8.5 x 11 in)"),
    PHOTO_4X6("Photo (10 x 15 cm / 4x6 in)")
}

enum class PageOrientation {
    PORTRAIT,
    LANDSCAPE
}

enum class DuplexMode {
    SINGLE_SIDED,
    DOUBLE_SIDED
}

enum class FitMode {
    FIT_PAGE,
    FILL_PAGE
}

enum class BindingOption {
    NONE,
    TOP_LEFT_STAPLE,
    SPIRAL
}

enum class JobActionType {
    PRINTED,
    SENT
}

data class PrintOptions(
    val copies: Int = 1,
    val colorMode: ColorMode = ColorMode.COLOR,
    val paperSize: PaperSize = PaperSize.A4,
    val orientation: PageOrientation = PageOrientation.PORTRAIT,
    val duplexMode: DuplexMode = DuplexMode.SINGLE_SIDED,
    val fitMode: FitMode = FitMode.FIT_PAGE,
    val bindingOption: BindingOption = BindingOption.NONE,
    val customNotes: String = ""
)
