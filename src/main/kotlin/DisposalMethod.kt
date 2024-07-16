package io.github.shaksternano.gifcodec

enum class DisposalMethod(
    val id: Int,
) {
    UNSPECIFIED(0),
    DO_NOT_DISPOSE(1),
    RESTORE_TO_BACKGROUND_COLOR(2),
    RESTORE_TO_PREVIOUS(3),
}
