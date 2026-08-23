# Binaural Companion

A native Android research-and-wellness prototype that generates binaural beats, 40 Hz tone pips, amplitude-modulated tones, and click trains while Spotify—or another audio app—continues playing through the same headphones.

## Download

Download the latest signed Android APK from [GitHub Releases](https://github.com/caltr98/binaural-companion-android/releases/latest).

Android will ask you to approve installation from your browser or file manager because this direct release is distributed outside Google Play. Release checksums are published with every APK.

## What works

- One tap starts the selected auditory layer and opens Spotify.
- A foreground media-playback service keeps the layer running when Spotify is in front.
- `AudioTrack` synthesizes independent left/right PCM waveforms in real time at 48 kHz.
- Android mixes the layer with the existing media stream; the app deliberately does not request audio focus.
- Optional Music assist detects concurrent media playback and smoothly applies a capped 1.25× gain to only the local layer. It never captures song audio.
- Wired, USB, classic Bluetooth, BLE, and hearing-device outputs are detected.
- Headphone removal stops playback through `ACTION_AUDIO_BECOMING_NOISY`.
- Nine evidence-labeled modes include ordinary binaural presets, a neutral control, 40 Hz binaural beats, MIT/Martorell tone pips, human ASSR amplitude modulation, and human ASSR clicks.
- Start/end frequency controls support steady sessions or gradual glides from 0–40 Hz.
- Each stereo pair is centered on the selected carrier so tuning the difference does not shift the average pitch.
- An optional Gateway-inspired preparation guides distraction parking, slow breathing, one concrete intention, and body relaxation.
- Private before/after calm, focus, and energy ratings create a local response log.
- No microphone, account, analytics, internet, or audio-capture permission is used.

## The Spotify boundary

Android and Spotify do not provide a general third-party API for transparently capturing and rewriting Spotify's protected raw audio. Spotify App Remote controls playback and exposes player state; it does not provide the decoded audio stream. Android's playback-capture API also requires a visible user-approved MediaProjection session, microphone permission, and permission from the source player to be captured. This app therefore uses the private platform-native design: Spotify plays the original track and Android mixes a second local stereo stream into the same output.

The signals remain present under sample-wise digital mixing, and a unit test verifies that both binaural carrier components survive a simulated stereo-music mix. That does **not** prove physiological entrainment: music can perceptually mask a quiet layer, and neural response depends on audibility, headphones, hearing, attention, and the listener. Music assist can improve audibility, but it cannot measure Spotify loudness or verify a brain response without EEG. For the closest comparison with a published research waveform, play that mode alone with Music assist off.

The primary button combines the two user actions—start the layer, then open Spotify—without requiring a Spotify developer account, OAuth client ID, or Premium subscription. A later product version could add App Remote transport controls after registering a Spotify developer application, but that is not required for the core audio coexistence.

## Auditory evidence boundary

The protocol labels are deliberately specific:

- **MIT tone pips** reproduce the auditory stimulus described by Martorell et al. (2019): 1 ms, 10 kHz tones every 25 ms (40 pips/s). The key pathology and memory findings were in 5XFAD and tauopathy **mice**, not proof of a human treatment. The user-supplied website incorrectly ties this auditory waveform to Iaccarino et al. (2016), which studied 40 Hz **visual flicker** in mice.
- **Human AM 40** uses a 1 kHz tone with a 100% 40 Hz amplitude envelope. Human auditory steady-state response (ASSR) research consistently shows strong phase-locked activity around 40 Hz. This is an EEG response, not evidence of general cognitive enhancement.
- **Human clicks 40** uses alternating-polarity 1 ms clicks at 40/s. Clicks reliably evoke ASSR but are jarring, and published work found click responses more affected by distraction than flutter amplitude-modulated tones.
- **Binaural 40** preserves the gentler stereo-difference option. Comparative human studies found a 40 Hz binaural-beat ASSR, but with lower amplitude than the acoustic/monaural beat response.
- Music-embedded 40 Hz amplitude modulation has evoked ASSR in human studies and can be more pleasant than conventional pips. Those engineered tracks modulate the music itself; this app cannot rewrite a DRM-protected Spotify stream, so its separate overlay is not identical to that method.

Primary and platform sources:

- [Martorell et al., Cell (2019): auditory GENUS in mice](https://pmc.ncbi.nlm.nih.gov/articles/PMC6774262/)
- [Schwarz & Taylor, Clinical Neurophysiology (2005): binaural vs monaural 40 Hz ASSR](https://pubmed.ncbi.nlm.nih.gov/15721080/)
- [Voicikas et al., Neuroscience Letters (2016): AM flutter vs click ASSR](https://doi.org/10.1016/j.neulet.2016.07.019)
- [Yokota et al., Frontiers in Human Neuroscience (2024): gamma music ASSR](https://pmc.ncbi.nlm.nih.gov/articles/PMC10808749/)
- [Android playback-capture requirements and source-player policy](https://developer.android.com/media/platform/av-capture)

## CIA archive material and product language

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

- `audio/StereoToneGenerator.kt` — binaural, carrier-centered PCM synthesis.
- `audio/GammaStimulusGenerators.kt` — exact-timing tone-pip, AM, and click generators.
- `audio/MusicMixPolicy.kt` — capped concurrent-media gain policy without content capture.
- `audio/BinauralPlaybackService.kt` — background playback, timer, fades, notification, and route reporting.
- `audio/AudioRouteMonitor.kt` — headphone availability.
- `audio/SessionJournal.kt` — private on-device before/after ratings and session summary.
- `ui/BinauralCompanionApp.kt` — Compose interface, preparation flow, safety gate, Spotify handoff, and evidence boundary.

## Trademark note

This prototype is not affiliated with or endorsed by Spotify, the Monroe Institute, or Interstate Industries. Spotify and Hemi-Sync are trademarks of their respective owners. A production release should complete a formal name and trademark review before store publication.
