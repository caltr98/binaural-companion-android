# Binaural Companion

A native Android prototype that adds a quiet, locally generated binaural layer while Spotify—or any other audio app—continues playing through the same headphones.

## Download

Download the latest signed Android APK from [GitHub Releases](https://github.com/caltr98/binaural-companion-android/releases/latest).

Android will ask you to approve installation from your browser or file manager because this direct release is distributed outside Google Play. Release checksums are published with every APK.

## What works

- One tap starts the binaural layer and opens Spotify.
- A foreground media-playback service keeps the layer running when Spotify is in front.
- `AudioTrack` synthesizes independent left/right PCM tones in real time.
- Android mixes the layer with the existing media stream; the app deliberately does not request audio focus.
- Wired, USB, classic Bluetooth, BLE, and hearing-device outputs are detected.
- Headphone removal stops playback through `ACTION_AUDIO_BECOMING_NOISY`.
- Six intention-based modes cover wind-down, calm focus, deep work, reset, an experimental 40 Hz setting, and a neutral control.
- Start/end frequency controls support steady sessions or gradual glides from 0–40 Hz.
- Each stereo pair is centered on the selected carrier so tuning the difference does not shift the average pitch.
- An optional Gateway-inspired preparation guides distraction parking, slow breathing, one concrete intention, and body relaxation.
- Private before/after calm, focus, and energy ratings create a local response log.
- No microphone, account, analytics, internet, or audio-capture permission is used.

## The Spotify boundary

Android and Spotify do not provide a general third-party API for transparently capturing and rewriting Spotify's protected raw audio. Spotify App Remote controls playback and exposes player state; it does not provide the decoded audio stream. This app therefore uses the safe platform-native design: Spotify plays the original track and Android mixes a second local stereo stream into the same output.

The primary button combines the two user actions—start the layer, then open Spotify—without requiring a Spotify developer account, OAuth client ID, or Premium subscription. A later product version could add App Remote transport controls after registering a Spotify developer application, but that is not required for the core audio coexistence.

## Evidence, CIA archive material, and product language

The supplied Curtis thesis describes the basic acoustic construction (two carrier tones, one per ear) and also gives an important caution: more research is needed, multiple physiological and psychological factors are involved, and binaural beats alone may have limited effects. The supplied psycho-oncology article reports a very small uncontrolled experience involving guided Hemi-Sync recordings within broader care. It is not evidence that this standalone tone generator treats cancer, pain, fatigue, depression, or any other condition.

The additional 1983 Army paper *Analysis and Assessment of Gateway Process* was found in the CIA FOIA Reading Room. It is an assessment written by an Army officer, not a CIA clinical trial or a modern scientific review. Its practical preparation sequence combines visualization, resonant breathing/humming, an affirmation or intention, physical relaxation, stereo audio, practice, and feedback. The app adapts only the low-risk, ordinary elements of that sequence.

The CIA archive also preserves documents that make conflicting claims. A Monroe Institute report explicitly says it does not provide conventional statistical tables or proof, while a separate Army review states that concentration benefits were not convincingly demonstrated and medical claims were supported only by testimonials. Being hosted or declassified by the CIA means the record is publicly accessible; it does not mean the CIA endorses or validates its contents.

Official archive sources reviewed:

- [Analysis and Assessment of Gateway Process](https://www.cia.gov/readingroom/docs/CIA-RDP96-00788R001700210016-5.pdf)
- [Monroe Institute report](https://www.cia.gov/readingroom/docs/CIA-RDP96-00788R001700210025-5.pdf)
- [Gateway Program overview](https://www.cia.gov/readingroom/docs/CIA-RDP96-00788R001700210040-8.pdf)
- [Army review identifying unsupported concentration and medical claims](https://www.cia.gov/readingroom/docs/CIA-RDP96-00788R001800020001-1.pdf)

For that reason, the app:

- presents presets as session intentions, not promised brain states;
- includes a 0 Hz identical-tone control for personal A/B comparison;
- labels evidence confidence and records only private self-ratings;
- does not operationalize remote viewing, out-of-body, medical-healing, or “time-space” claims;
- makes no diagnostic or therapeutic claims;
- requires a safety acknowledgement;
- caps the local layer at 16% PCM amplitude and defaults to 6%; and
- clearly states that it is an independent educational prototype.

## Build

Requirements: JDK 17 and Android SDK 37.

```powershell
cd hemi-sync-android
.\gradlew.bat testDebugUnitTest assembleDebug
```

The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

Release signing is configured only through the `BINAURAL_KEYSTORE_FILE`, `BINAURAL_KEYSTORE_PASSWORD`, `BINAURAL_KEY_ALIAS`, and `BINAURAL_KEY_PASSWORD` environment variables. Signing files and credentials must never be committed.

## Privacy

The app collects and transmits no user data. Optional self-ratings remain in private on-device storage. See the complete [privacy policy](PRIVACY.md).

## Project map

- `audio/StereoToneGenerator.kt` — pure, unit-tested PCM synthesis.
- `audio/BinauralPlaybackService.kt` — background playback, timer, fades, notification, and route reporting.
- `audio/AudioRouteMonitor.kt` — headphone availability.
- `audio/SessionJournal.kt` — private on-device before/after ratings and session summary.
- `ui/BinauralCompanionApp.kt` — Compose interface, preparation flow, safety gate, Spotify handoff, and evidence boundary.

## Trademark note

This prototype is not affiliated with or endorsed by Spotify, the Monroe Institute, or Interstate Industries. Spotify and Hemi-Sync are trademarks of their respective owners. A production release should complete a formal name and trademark review before store publication.
