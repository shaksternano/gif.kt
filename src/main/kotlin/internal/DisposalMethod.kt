package io.github.shaksternano.gifcodec.internal

/**
 * Indicates the way in which a frame is to
 * be treated after being displayed.
 *
 * Documentation taken from the
 * [GIF89a specification](https://www.w3.org/Graphics/GIF/spec-gif89a.txt).
 */
enum class DisposalMethod(
    val id: Int,
) {
    /**
     * The decoder is not required to take
     * any action.
     */
    UNSPECIFIED(0),

    /**
     * The frame is to be left in place.
     */
    DO_NOT_DISPOSE(1),

    /**
     * The area used by the frame must be
     * restored to the background color.
     */
    RESTORE_TO_BACKGROUND_COLOR(2),

    /**
     * The decoder is required to restore the
     * area overwritten by the frame with what
     * was there prior to rendering the frame.
     */
    RESTORE_TO_PREVIOUS(3);

    companion object {
        fun fromId(id: Int): DisposalMethod {
            return entries.firstOrNull { it.id == id }
                ?: throw NoSuchElementException("No DisposalMethod with id $id")
        }
    }
}
