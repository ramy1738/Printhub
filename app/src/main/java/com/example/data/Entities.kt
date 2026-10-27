package com.example.data

data class PrintJobEntity(
    val id: Long = 0,
    val title: String,
    val fileUriString: String,
    val printType: String, // PHOTO, PDF, DOCUMENT, COMPOSED
    val actionType: String, // PRINTED, SENT
    val copies: Int,
    val colorMode: String, // COLOR, MONOCHROME
    val paperSize: String, // A4, A3, LETTER, PHOTO_4X6
    val duplexMode: String, // SINGLE_SIDED, DOUBLE_SIDED
    val bindingOption: String, // NONE, TOP_LEFT_STAPLE, SPIRAL
    val recipient: String?,
    val instructions: String?,
    val timestamp: Long = System.currentTimeMillis()
)

data class PrintShopEntity(
    val id: Long = 0,
    val name: String,
    val email: String,
    val phone: String,
    val notes: String,
    val isFavorite: Boolean = false
)
