package com.omnidot.core

enum class DotShape(val label: String) {
    CIRCLE("Circle"),
    ROUNDED("Rounded square"),
    SQUARE("Square");

    companion object {
        fun fromStored(value: String?): DotShape =
            entries.firstOrNull { it.name == value } ?: CIRCLE
    }
}

enum class LabelPosition(val label: String) {
    BELOW("Below the dots"),
    ABOVE("Above the dots");

    companion object {
        fun fromStored(value: String?): LabelPosition =
            entries.firstOrNull { it.name == value } ?: BELOW
    }
}
