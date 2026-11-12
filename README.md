# Hardtekk Electribe

A professional mobile groovebox and sampler for Android, inspired by the **Korg Electribe 2** with a **Hardtekk/Hardtechno** aesthetic.

![Hardtekk Electribe](https://img.shields.io/badge/Android-8.0%2B-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5.12-orange)
![License](https://img.shields.io/badge/License-MIT-green)

## Features

### 🎹 **16 Trigger Pads**
- Electribe-style rubber pads with neon pink/magenta illumination
- Velocity-sensitive with visual feedback
- Polyphonic playback (32 voices)
- Per-pad choke groups for realistic drum behavior

### 🎵 **Step Sequencer (16/32 Steps)**
- Classic Electribe workflow: select pad → program steps
- 4 patterns with 16 or 32 steps each
- Live recording with quantization (1/4, 1/8, 1/16, 1/32)
- Swing control (-50% to +50%)
- Parameter locks per step
- Chase LED visualization synced to BPM

### ✂️ **Sample Slicer / Chopper**
- Load full songs (MP3/WAV) from device storage
- Real-time waveform visualization
- Draggable start/end markers for slice selection
- Preview slices before assigning
- Assign slices directly to any of the 16 pads
- Fade in/out for clean slices

### 🔊 **Built-in Hardtekk Sound Generator**
Procedurally generates authentic sounds on first launch:
- **Hardtekk Kick** - Punchy, distorted kick with fast pitch drop
- **Gabber Kick** - Extreme sub-bass with hard clipping
- **Offbeat Bass** - Acid-style 303 bassline
- **Snare** - Tight electronic snare with noise
- **Clap** - Multi-tap early reflection clap
- **Closed/Open Hi-Hats** - Metallic synthesized hats
- **Acid Stab** - Resonant 303-style synth stab
- **Ride** - Inharmonic bell-like ride cymbal
- **909/808 Kicks** - Classic Roland emulations
- **Percussion & FX** - Additional textures

### 🎛️ **Real-Time DSP Effects**
All effects process audio in real-time with minimal latency:

| Effect | Parameters |
|--------|------------|
| **Low-Pass Filter** | Cutoff (20Hz-20kHz), Resonance (0.1-20) |
| **High-Pass Filter** | Cutoff, Resonance |
| **Pitch/Tune** | ±24 semitones, ±100 cents fine tune |
| **Distortion/Drive** | Soft Clip, Hard Clip, Tube, Wave Fold, Bitcrush, Overdrive, Fuzz |
| **Delay** | Time (1ms-2s), Feedback (0-98%), Mix, Ping-Pong, Stereo Spread |
| **Master Volume** | 0-100% |

### 🎨 **Pink-Skin Visual Design**
- Dark anthracite chassis (#121212)
- Neon pink primary (#FF007F)
- Magenta accents (#FF00FF)
- LED status indicators (Green/Red/Amber/Blue)
- 16 distinct pad colors
- Glowing active states
- Hardware-inspired UI

## Architecture

```
app/src/main/java/com/hardtekk/electribe/
├── audio/                  # Audio engine & DSP
│   ├── AudioEngine.kt      # Main audio coordinator
│   ├── SamplePlayer.kt     # Polyphonic sample playback
│   ├── SoundGenerator.kt   # Procedural sound synthesis
│   └── DSP/                # Digital Signal Processing
│       ├── BiquadFilter.kt # IIR filters (LPF, HPF, BPF, Notch)
│       ├── Distortion.kt   # Multiple distortion algorithms
│       ├── Delay.kt        # Stereo delay with feedback
│       └── PitchShifter.kt # Pitch shifting (resample/grain/PSOLA)
├── sequencer/              # Sequencing engine
│   ├── SequencerEngine.kt  # High-precision step sequencer
│   └── PatternManager.kt   # Pattern save/load (JSON)
├── model/                  # Data models
│   ├── AppState.kt         # Global app state
│   ├── PadConfig.kt        # Per-pad configuration
│   ├── Pattern.kt          # Pattern & step data
│   └── SampleData.kt       # Sample metadata & slices
├── storage/                # Persistence
│   └── SampleRepository.kt # SQLite sample database
├── ui/                     # Jetpack Compose UI
│   ├── components/         # Reusable UI components
│   │   ├── TriggerPad.kt   # 16 trigger pads
│   │   ├── RotaryKnob.kt   # Hardware-style knobs
│   │   ├── TransportControls.kt # Play/Stop/Rec
│   │   ├── BPMDisplay.kt   # BPM readout & tap tempo
│   │   ├── PatternSelector.kt # 4 pattern buttons
│   │   ├── ModeSelector.kt # Trigger/Seq/Slicer modes
│   │   └── WaveformView.kt # Waveform with slice markers
│   ├── screens/            # Mode screens
│   │   ├── TriggerModeScreen.kt
│   │   ├── StepSequencerScreen.kt
│   │   └── SampleSlicerScreen.kt
│   ├── theme/              # Material3 theming
│   │   ├── Color.kt        # Hardtekk color palette
│   │   ├── Type.kt         # Typography
│   │   ├── Shape.kt        # Component shapes
│   │   └── Theme.kt        # Theme composition
│   └── MainScreen.kt       # Screen router
├── utils/                  # Utilities
│   ├── FileUtils.kt        # File operations & SAF
│   ├── AudioFileLoader.kt  # WAV/MP3 metadata & waveforms
│   └── WavWriter.kt        # WAV export
├── HardtekkApplication.kt  # Application class
└── MainActivity.kt         # Main activity
```

## Building

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34

### Local Build
```bash
# Clone the repository
git clone https://github.com/yourusername/hardtekk-electribe.git
cd hardtekk-electribe

# Make gradlew executable
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# APK location: app/build/outputs/apk/debug/app-debug.apk
```

### CI/CD (GitHub Actions)
The repository includes a GitHub Actions workflow (`.github/workflows/build_apk.yml`) that:
1. Sets up JDK 17 and Android SDK 34
2. Generates debug keystore
3. Builds `assembleDebug`
4. Uploads `app-debug.apk` as artifact
5. Runs lint and unit tests

On every push to `main`, a new debug APK is available in the Actions artifacts.

## Usage

### Trigger Mode (Default)
1. Tap pads to play sounds
2. Adjust effects with 6 rotary knobs:
   - **LPF** - Low-pass filter cutoff
   - **HPF** - High-pass filter cutoff
   - **PITCH** - Global pitch ±24 semitones
   - **DRIVE** - Distortion amount
   - **DELAY** - Delay send level
   - **MASTER** - Master volume
3. Use transport: ▶ PLAY, ■ STOP, ● REC
4. Change BPM with +/- or tap tempo
5. Switch patterns 1-4

### Step Sequencer Mode
1. Press **MODE** to switch to **SEQ**
2. Select a pad on the left
3. Tap step buttons (1-16/32) to program pattern
4. Current step highlighted in green
5. Hold step + turn knob for parameter locks
6. Press **REC** + tap pads for live recording
7. Adjust swing with slider

### Sample Slicer Mode
1. Press **MODE** to switch to **SLC**
2. Tap **LOAD SONG** to select audio file
3. Waveform displays with draggable markers
4. Drag **START** (green) and **END** (red) markers
5. Press **PREVIEW** to hear slice
6. Select target pad, press **ASSIGN**
7. Slice saved to pad for trigger/sequencer use

### Loading Custom Samples
- Long-press any pad in Trigger mode
- Select WAV/MP3 from file picker
- Sample assigned to that pad
- Adjust start/end offsets in pad config

## Technical Details

### Audio Engine
- **AudioTrack** in streaming mode with `PERFORMANCE_MODE_LOW_LATENCY`
- 256-frame buffer (configurable)
- 44.1kHz / 48kHz sample rate
- 32-voice polyphony with voice stealing
- Per-voice effects chain (filter → distortion → delay)
- Master effects bus (filter → distortion → delay)

### Low Latency Optimizations
- High-priority audio thread (`Thread.MAX_PRIORITY`)
- Minimal allocations in audio loop
- Coefficient smoothing for filter changes
- Lock-free ring buffers for delay lines
- `FLAG_LOW_LATENCY` audio attributes

### Procedural Sound Generation
All built-in sounds generated mathematically at runtime:
- Additive synthesis with harmonic series
- Exponential pitch envelopes
- Waveshaping distortion (tanh, hard clip, fold)
- Noise generators with filtering
- No external audio assets required

### Data Persistence
- **Patterns**: JSON files in `files/patterns/`
- **Samples**: SQLite database with Room-like wrapper
- **User samples**: Copied to `files/samples/user/`
- **Slices**: Stored in database with references

## Permissions

| Permission | Purpose |
|------------|---------|
| `RECORD_AUDIO` | Metronome/preview (not used for recording) |
| `READ_MEDIA_AUDIO` (API 33+) | Load user samples/songs |
| `READ_EXTERNAL_STORAGE` (API ≤32) | Legacy storage access |
| `FOREGROUND_SERVICE` | Audio engine background operation |
| `WAKE_LOCK` | Keep CPU active for audio |

## Configuration

### gradle.properties
```properties
org.gradle.jvmargs=-Xmx4096m
android.enableJetifier=true
kotlin.incremental=true
```

### Build Variants
- **debug** - Debuggable, signed with debug keystore
- **release** - Minified, ProGuard optimized

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## License

MIT License - see [LICENSE](LICENSE) for details.

## Acknowledgments

- Inspired by **Korg Electribe 2** workflow and layout
- Hardtekk/Hardtechno sound design references
- Android AudioTrack low-latency guidelines
- Jetpack Compose Material3 components
- Kotlin Coroutines for async operations

## Roadmap

- [ ] MIDI input/output support
- [ ] Ableton Link synchronization
- [ ] Sample recording from microphone
- [ ] Pattern chaining (song mode)
- [ ] Probability/ratchet per step
- [ ] LFO modulation for parameters
- [ ] Export stems / mixdown to WAV
- [ ] Preset management
- [ ] Performance macros

---

**Built with ❤️ for the Hardtekk community**

*Drop the bass. Raise the tempo. 🎛️💗*