package com.example.facelock.domain.model

/** Why a camera frame cannot be used, with the sentence shown to the user. */
enum class FrameIssue(val hint: String) {
    EyesNotVisible("Keep both eyes visible"),
    TooFar("Move a little closer"),
    TooClose("Move back a little"),
    TurnedAway("Look straight at the camera"),
    EyesClosed("Open your eyes"),
}

/** What the recognizer found in one camera frame. */
sealed interface FrameAnalysis {
    data object NoFace : FrameAnalysis
    data class MultipleFaces(val count: Int) : FrameAnalysis
    data class Unusable(val issue: FrameIssue) : FrameAnalysis
    data class Good(val embedding: FaceEmbedding) : FrameAnalysis
}

/** Live progress of a scan, shown to the user while frames are being collected. */
sealed interface ScanProgress {
    /** [issue] is null while no face is visible yet. */
    data class Guidance(val issue: FrameIssue?) : ScanProgress
    data class Collecting(val collected: Int, val target: Int) : ScanProgress

    /** The first batch of frames did not match; a second one is being collected. */
    data object Retrying : ScanProgress
}
