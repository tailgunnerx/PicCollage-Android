package com.tailgunnerx.piccollage

enum class CollageType(
    val displayName: String,
    val photoCount: Int,
    val description: String
) {
    // 2 photos
    SPLIT_VERTICAL("Side by Side", 2, "Two columns, left & right"),
    SPLIT_HORIZONTAL("Top & Bottom", 2, "Two rows, top & bottom"),
    // 3 photos
    BIG_LEFT("Big Left", 3, "One large left + two stacked right"),
    BIG_RIGHT("Big Right", 3, "Two stacked left + one large right"),
    // 4 photos
    GRID_2X2("Grid 2×2", 4, "Classic four-square grid"),
    // 4 photos
    BIG_TOP("Big Top", 4, "One wide top + three below"),
    // 4 photos
    BIG_BOTTOM("Big Bottom", 4, "Three on top + one wide bottom"),
    // 5 photos
    GRID_ASYMMETRIC("Asymmetric 5", 5, "Two large + three small"),
    // 6 photos
    GRID_2X3("Grid 2×3", 6, "Six equal cells, 2 columns × 3 rows")
}
