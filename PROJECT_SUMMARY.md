# Hardtekk Electribe - Project Summary

## Complete File List (65 files)

### Root Configuration
```
├── build.gradle.kts                    # Root build script
├── settings.gradle.kts                 # Project settings
├── gradle.properties                   # Gradle configuration
├── gradlew                             # Unix wrapper script
├── gradlew.bat                         # Windows wrapper script
├── gradle/wrapper/gradle-wrapper.properties
└── .github/workflows/build_apk.yml     # CI/CD pipeline
```

### App Module - Build
```
app/
├── build.gradle.kts                    # App dependencies & config
├── proguard-rules.pro                  # ProGuard rules
```

### App Module - Manifest & Resources
```
app/src/main/
├── AndroidManifest.xml                 # Full permissions & config
├── res/
│   ├── values/
│   │   ├── strings.xml                 # All UI strings (60+)
│   │   ├── colors.xml                  # Hardtekk pink-skin palette (40+ colors)
│   │   └── themes.xml                  # Material3 themes
│   ├── drawable/
│   │   ├── ic_launcher_foreground.xml  # Adaptive icon foreground
│   │   ├── ic_launcher_background.xml  # Icon background
│   │   └── adaptive_icon_background.xml
│   ├── mipmap-anydpi-v26/
│   │   └── ic_launcher.xml             # Adaptive icon config
│   └── xml/
│       ├── file_paths.xml              # FileProvider paths
│       ├── data_extraction_rules.xml   # Backup rules
│       └── backup_rules.xml            # Full backup config
```

### App Module - Kotlin Source (38 files)

#### Models (4)
```
model/
├── AppState.kt          # Global state (BPM, mode, pattern, effects)
├── PadConfig.kt         # Per-pad config (16 params + serializers)
├── Pattern.kt           # Pattern/StepData/ParameterLock
└── SampleData.kt        # Sample metadata + SliceData
```

#### Audio Engine (6)
```
audio/
├── AudioEngine.kt       # Main coordinator (transport, BPM, pads)
├── SamplePlayer.kt      # 32-voice polyphonic playback
├── SoundGenerator.kt    # Procedural Hardtekk sound synthesis
└── DSP/
    ├── BiquadFilter.kt  # LPF/HPF/BPF/Notch/Peak/Shelving
    ├── Distortion.kt    # 7 algorithms (Soft/Hard/Tube/Fold/Bitcrush/OD/Fuzz)
    ├── Delay.kt         # Stereo/Ping-Pong/Spread with tempo sync
    └── PitchShifter.kt  # Resample/Grain/PSOLA/PhaseVocoder
```

#### Sequencer (2)
```
sequencer/
├── SequencerEngine.kt   # High-precision audio-thread sequencer
└── PatternManager.kt    # JSON pattern persistence
```

#### Storage (1)
```
storage/
└── SampleRepository.kt  # SQLite sample/slice database
```

#### UI - Theme (4)
```
ui/theme/
├── Color.kt      # 40+ Hardtekk colors + pad palette
├── Type.kt       # Typography (mono for readouts, sans for UI)
├── Shape.kt      # Hardware-style rounded corners
└── Theme.kt      # Material3 dark theme composition
```

#### UI - Components (6)
```
ui/components/
├── TriggerPad.kt       # 16 pads with glow/velocity
├── RotaryKnob.kt       # Hardware potentiometer
├── TransportControls.kt # Play/Stop/Rec + BPM display
├── PatternSelector.kt  # 4 pattern buttons + copy/paste
├── ModeSelector.kt     # Trigger/Seq/Slicer tabs
└── WaveformView.kt     # Waveform + slice markers + playhead
```

#### UI - Screens (3)
```
ui/screens/
├── TriggerModeScreen.kt      # Main performance view
├── StepSequencerScreen.kt    # Step programming grid
└── SampleSlicerScreen.kt     # Song load/slice/assign
```

#### UI - Main (2)
```
ui/
├── MainScreen.kt       # Mode router
└── (in MainActivity)
```

#### Application & Entry (2)
```
├── HardtekkApplication.kt  # App class + audio init
└── MainActivity.kt         # Fullscreen landscape activity
```

#### Utilities (3)
```
utils/
├── FileUtils.kt         # SAF, file ops, formatting
├── AudioFileLoader.kt   # WAV/MP3 metadata + waveforms
└── WavWriter.kt         # WAV export + slice extraction
```

### Tests (2)
```
test/
└── ExampleUnitTest.kt          # 10 unit tests (DSP, models, etc.)

androidTest/
└── ExampleInstrumentedTest.kt  # Resource/theme tests
```

### Documentation
```
├── README.md              # Full documentation
└── PROJECT_SUMMARY.md     # This file
```

## Key Features Implemented

### ✅ Audio Engine
- AudioTrack low-latency streaming (256 frames, HIGH_PRIORITY thread)
- 32-voice polyphony with choke groups & voice stealing
- Per-voice FX chain: Filter → Distortion → Delay
- Master FX bus: Filter → Distortion → Delay
- Coefficient smoothing for zipper-free parameter changes

### ✅ Procedural Sound Generation
Generates 16 Hardtekk sounds on first launch:
1. Hardtekk Kick, 2. Gabber Kick, 3. Offbeat Bass, 4. Snare
5. Clap, 6. Closed HH, 7. Open HH, 8. Acid Stab
9. Ride, 10. 909 Kick, 11. 808 Kick, 12. Perc 1
13. Perc 2, 14. FX 1, 15. FX 2, 16. Empty

### ✅ Three Modes
| Mode | Features |
|------|----------|
| **Trigger** | 16 velocity pads, 6 knobs, transport, BPM, patterns |
| **Step Sequencer** | 16/32 steps, pad selector, live rec, swing, param locks |
| **Sample Slicer** | Waveform, draggable markers, preview, assign to pads |

### ✅ Real-Time DSP
- **BiquadFilter**: 7 types, 20Hz-20kHz, Q=0.1-20, smoothed
- **Distortion**: 7 algorithms, drive 0-20x, mix 0-100%
- **Delay**: 1ms-2s, feedback 0-98%, ping-pong, tempo sync
- **PitchShifter**: ±24 semitones, ±100 cents, 4 modes

### ✅ Pink-Skin UI
- Anthracite (#121212) backgrounds
- Neon Pink (#FF007F) primary
- Magenta (#FF00FF) secondary
- 16 distinct pad colors with active glow
- LED indicators (Green/Red/Amber/Blue)
- Hardware-style knobs, buttons, displays

### ✅ CI/CD Pipeline
GitHub Actions workflow:
- JDK 17 + Android SDK 34
- Gradle caching
- Debug keystore generation
- `assembleDebug` build
- APK artifact upload (7-day retention)
- Lint + Unit tests as separate jobs

## Build Instructions

```bash
cd hardtekk-electribe
chmod +x gradlew
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

## Requirements Met

✅ Kotlin + Jetpack Compose (Material3)
✅ Min SDK 26, Target SDK 34
✅ Landscape forced orientation
✅ Low-latency AudioTrack engine
✅ Procedural sound generation (no external assets)
✅ Custom sample loading (SAF)
✅ 16/32-step sequencer with live recording
✅ Sample slicer with waveform visualization
✅ Real-time DSP effects (filter, distortion, delay, pitch)
✅ GitHub Actions APK build
✅ Zero TODOs - all files complete and compilable