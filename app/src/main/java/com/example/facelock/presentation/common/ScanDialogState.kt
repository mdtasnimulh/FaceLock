package com.example.facelock.presentation.common

import com.example.facelock.domain.service.FaceLoginException

/** A message the user has to acknowledge in a dialog. */
data class ScanDialogState(val title: String, val message: String)

/** Maps a failed scan to a dialog, or null when it only needs an on-screen hint. */
fun Throwable?.toDialogOrNull(): ScanDialogState? = when (this) {
    is FaceLoginException.MultipleFaces -> ScanDialogState(
        title = "More than one face found",
        message = "Only one person can scan at a time. Make sure you are alone in front of the camera, then try again.",
    )
    else -> null
}
