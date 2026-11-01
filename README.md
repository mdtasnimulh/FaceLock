# Face Lock Demo

A small Android app: register your face once, then every time the app is opened (or comes back
from the background) it asks you to match your face before showing the Home screen. The match
accuracy is shown when you log in.

Kotlin, Jetpack Compose, MVVM, Hilt, clean architecture with a DDD-style domain.

## How it works
1. First launch: you type your name, then the app takes 3 photos of your face. Each photo goes
   through ML Kit face detection, eye alignment and MobileFaceNet (the same model and pipeline
   as the NID SDK). The 3 embeddings are averaged into one template.
2. The profile (name + template) is turned into JSON, encrypted with Tink AES-256-GCM (the Tink
   keyset is protected by an Android Keystore master key) and only the encrypted Base64 text is
   written to DataStore.
3. Home screen shows "Home". Whenever the whole app goes to the background it locks again.
4. On the lock screen, "Verify face" takes a photo, compares it with the stored template (cosine
   similarity) and shows the accuracy, for example `Match 87.4%`. At or above 60 % you enter the
   app; below that you see the score and can try again.

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
`FaceMatchPolicy.MATCH_THRESHOLD` (default 0.60). Same person is usually 0.7 to 1.0, a different
person about 0.0 to 0.4. Raise it to be stricter, lower it if you are rejected too often.

## Limits of this demo
* No liveness check: a photo of the registered person held up to the camera can pass. Add a
  liveness step (blink / head turn) before using this for anything real.
* "Accuracy" is the similarity score between two faces, not a statistical accuracy.
* One user per install. "Reset face login" wipes the stored face.
* Backups are disabled on purpose: the encryption key lives in the Keystore and cannot be restored.
