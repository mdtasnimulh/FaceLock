# Face Lock Demo

A small Android app: register your face once, then every time the app is opened (or comes back
from the background) it asks you to match your face before showing the Home screen. The match
accuracy is shown when you log in.

Kotlin, Jetpack Compose, MVVM, Hilt, clean architecture with a DDD-style domain.

## How it works
1. First launch: you type your name, then the camera scans your face live. Only GOOD frames count
   (one face, looking straight, eyes open, big enough, not too close). Each goes through ML Kit
   detection, eye alignment and MobileFaceNet (the same model and pipeline as the NID SDK). Six good
   frames are averaged into one template. If a second person steps into the frame, a dialog stops
   the registration.
2. The profile (name + template) is turned into JSON, encrypted with Tink AES-256-GCM (the Tink
   keyset is protected by an Android Keystore master key) and only the encrypted Base64 text is
   written to DataStore.
3. Home screen shows "Home". Whenever the whole app goes to the background it locks again.
4. The lock screen starts the face unlock by itself. Four good frames are averaged and compared
   with the stored template (cosine similarity). Frames that disagree with each other are
   discarded, and a batch that does not match is repeated once before the login is rejected (the
   same idea as the SDK's LivenessSessionIntegrity). The accuracy is always shown, for example
   `92.4%`. At or above 80 % you enter the app; below that you see the score and can scan again.
   If a second face appears, a dialog asks that only one person uses the camera.

## Layers
    domain/        pure Kotlin rules: value objects (DisplayName, FaceEmbedding, Similarity),
                   aggregate (UserProfile), FaceMatchPolicy, repository + recognizer ports, use cases
    data/          Tink cipher, encrypted DataStore store, repository, MobileFaceNet + ML Kit recognizer
    presentation/  Compose screens + ViewModels (register, lock, home), AppLockManager
    di/            Hilt modules

## Run it
Open the folder in Android Studio (Ladybug or newer), let Gradle sync, run on a real device with a
front camera. The model is already in `app/src/main/assets/models/mobile_face_net.tflite`.

## Tuning
`FaceMatchPolicy.MATCH_THRESHOLD` (default 0.80). Same person is usually 0.7 to 1.0, a different
person about 0.0 to 0.4. 80 % is strict: if you are rejected too often in dim light, lower it a bit
(for example 0.70). Frame quality limits are in `FaceQualityPolicy` (same values as the SDK).

## Limits of this demo
* No liveness check: a photo of the registered person held up to the camera can pass. Add a
  liveness step (blink / head turn) before using this for anything real.
* "Accuracy" is the similarity score between two faces, not a statistical accuracy.
* One user per install. "Reset face login" wipes the stored face.
* Backups are disabled on purpose: the encryption key lives in the Keystore and cannot be restored.
