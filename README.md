# SARK Audio Android
Most powerful and feature-rich all-in-one audio toolbox, synthesizer, editor, recorder, and mastering suite for Android.
Package name: `sark.audio`

---

## 🚀 Overview & Feature Suite

SARK Audio provides an extensive suite of **over 30 production-grade, mathematically accurate DSP audio tools and processors**. Every feature is an actual implementation without mockups, simulations, or placeholders.

Built with **Modern Android Jetpack Compose**, **Material 3 Expressive Themes**, and responsive audio pipeline architecture.

---

## 🛠️ The 30+ Audio Engines & Tools

### 1. Audio Creator & Synthesizers
- **Waveform Generator**: Sine, Square, Triangle, Sawtooth, Chirp (linear frequency sweep), White Noise, and Pink Noise (Paul Kellet's filter algorithm).
- **DTMF Generator**: Standard telephony dual-tone multi-frequency synthesizer (digits 0-9, \*, #, A-D).
- **Morse Code Synthesizer**: Converts text into ITU-standard audible Morse code tones.
- **Digital Silence Generator**: Microsecond-accurate digital zero-signal generator.
- **SFX Generator & Overlay**: Procedural audio sound effects (Censor beep, Sub-drop impact boom, Frequency riser, Laser zap, Applause burst).

### 2. Audio Editor, Trimmer & Splitter
- **Audio Trimmer & Cutter**: Sample-accurate boundary trimming and split deletion.
- **Fade Shaper**: Linear and logarithmic / S-curve fade-in and fade-out envelope shaping.
- **Silence Remover**: Automatic leading, trailing, and internal silence stripping with configurable dBFS threshold.
- **Track Splitter**: Chunk audio by fixed durations, silence pauses, or custom cue markers.
- **Phase Inverter**: 180° polarity inversion for stereo widening and phase cancellation.
- **Audio Reverser**: Reverses audio buffers in-place.
- **Silence Inserter**: Inject exact duration digital silence at arbitrary timestamp offsets.

### 3. Studio Audio Recorder & Audio Manager
- **PCM Studio Recorder**: Real-time microphone audio capture with live spectrum meter and waveform feedback.
- **Audio Library Manager**: Real-world file management to inspect, save, rename, delete, and organize recordings.

### 4. Multi-track Mixing & Remixing
- **Audio Merger & Joiner**: Sequential track concatenator with crossfade blending.
- **Multi-Track Parallel Mixer**: Parallel multi-channel summing with per-track gain, offset delays, and tanh soft-saturation limiting.
- **Stereo & Channel Processor**: Stereo-to-mono downmix, mono-to-stereo upmix, channel splitting, channel swap, and constant-power balance panning.
- **Seamless Loop Creator**: Repeats audio buffers N times with click-free crossfade seam blending.

### 5. Vocal, Karaoke & Voice Suite
- **Vocal Remover & Karaoke Generator**: Center-channel cancellation with low-end bass preservation.
- **Vocal Isolator**: Center-panned vocal extraction.
- **Vocal Autotune**: Fundamental pitch detection via auto-correlation and scale quantization (Chromatic, Major, Minor, Pentatonic).
- **Voice Changer**: Formant and pitch transposition presets (Chipmunk, Deep Monster, Robot ring modulation, Alien vibrato, Helium, Megaphone radio).
- **Musical Stems Splitter**: Spectral deconstruction into Vocals, Bass, Drums, and Melody stems.
- **Voice Clone Timbre Simulator**: Formant frequency cluster estimation (F1, F2, F3) and resonator filter modeling.

### 6. Sound Mastering, EQ & Effects
- **Sound Mastering Engine**: Multi-stage mastering with warm tape saturation, multi-band shaping, stereo width expansion, and -0.1 dBFS true peak limiter.
- **Parametric Equalizer**: 3-band and multi-band Biquad IIR filters (Robert Bristow-Johnson Audio EQ Cookbook).
- **8D Spatial Audio**: Immersive 360-degree rotating binaural headphone audio with interaural time delay (ITD), level difference (ILD), and room diffusion reverberation.
- **Dynamics Compressor**: Studio dynamic range compression with threshold, ratio, attack, release, and makeup gain.
- **Delay & Echo Unit**: Ring-buffered feedback delay network with millisecond timing and wet/dry blending.
- **Stereo Chorus**: Dual LFO phase-modulated delay lines for vocal doubling and instrument thickening.
- **Waveshaping Distortion**: Non-linear tube saturation, fuzz, and soft-knee overdrive.
- **Bitcrusher & Lo-Fi**: Downsampling decimator and bit-depth quantization (1 to 16 bits).
- **Echo & Reverb Remover**: Transient envelope gating and spectral de-reverberation.

### 7. Conversion, Metadata & Analysis
- **Chromatic Instrument Tuner**: Real-time pitch tracking with cents deviation (-50 to +50 cents) and in-tune indicator.
- **FFT Spectrum Analyzer**: Cooley-Tukey Radix-2 FFT computing decibel frequency spectrum and peak fundamental pitch.
- **Format Inspector & Converter**: Comprehensive WAV file specifications and real-time resampler.
- **Bit Depth Converter**: 8-bit unsigned PCM and 16-bit signed PCM conversion with TPDF dithering.
- **Audio Tag & Metadata Editor**: Reads and writes RIFF INFO chunk metadata tags (Title, Artist, Album, Year, Genre, Track, Comments).
- **Bulk Audio Converter**: Batch queue processor for sample rate, channel count, and bit depth conversion.
- **Bulk Video to Audio Extractor**: Extracts PCM audio from MP4, MKV, and WEBM video containers using Android MediaExtractor and MediaCodec.

---

## 📱 Material Expressive Design
- Dynamic dark & light themes leveraging the Material 3 Expressive design tokens.
- Fluid waveform visualizers with scrubbing, seeking, and marker boundaries.
- Categorized tools workbench with instant real-time search.

---

## 🧪 Testing & Verification
All core DSP algorithms and utilities are verified by automated JUnit unit tests:
```bash
./gradlew testDebugUnitTest
```
Build the debug APK:
```bash
./gradlew assembleDebug
```
