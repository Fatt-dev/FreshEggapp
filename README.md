<p align="center">
  <img src="logo/logo.png" alt="Fresh Egg Logo" width="120"/>
</p>

<h1 align="center">🥚 FRESH EGG</h1>

<p align="center">
  <b>Aplikasi Android untuk Deteksi Kesegaran Telur Berbasis CNN</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Platform"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/ML-PyTorch_Mobile-EE4C2C?logo=pytorch&logoColor=white" alt="PyTorch"/>
  <img src="https://img.shields.io/badge/Min_SDK-24_(Android_7.0)-34A853" alt="Min SDK"/>
  <img src="https://img.shields.io/badge/Version-1.0-blue" alt="Version"/>
</p>

---

## 📖 Tentang Aplikasi

**FRESH EGG** adalah aplikasi Android yang memanfaatkan model **Convolutional Neural Network (CNN)** untuk mengklasifikasikan kualitas telur menjadi **segar** atau **busuk** hanya dengan sekali foto. Aplikasi ini menggunakan **PyTorch Mobile Lite** untuk menjalankan inferensi model secara langsung di perangkat (on-device), sehingga tidak memerlukan koneksi internet.

### ✨ Fitur Utama

| Fitur | Deskripsi |
|---|---|
| 📷 **Ambil Foto Kamera** | Gunakan kamera perangkat untuk memotret telur secara langsung |
| 🖼️ **Pilih dari Galeri** | Pilih foto telur yang sudah ada di galeri |
| 🧠 **Klasifikasi AI** | Analisis otomatis menggunakan model CNN (PyTorch Mobile) |
| ⚡ **On-Device Inference** | Semua proses berjalan di perangkat, tanpa perlu internet |
| 🔄 **Koreksi Rotasi EXIF** | Otomatis memperbaiki orientasi foto dari kamera |

---

## 🖥️ Alur Layar Aplikasi

```
┌─────────────┐     ┌─────────────────┐     ┌──────────────┐     ┌───────────────┐
│   Splash    │ ──▶ │   Onboarding    │ ──▶ │    Scan      │ ──▶ │    Result     │
│   Screen    │ tap │    Screen       │ btn │   Screen     │ btn │   Screen     │
│             │     │ "START SCANNING"│     │ Foto + Ambil │     │ Segar/Busuk  │
└─────────────┘     └─────────────────┘     └──────────────┘     └───────────────┘
                                                                        │
                                                                        │ btn
                                                                        ▼
                                                               Kembali ke Onboarding
```

1. **Splash Screen** — Layar pembuka, ketuk di mana saja untuk melanjutkan
2. **Onboarding Screen** — Penjelasan singkat aplikasi + tombol *Start Scanning*
3. **Scan Screen** — Ambil/pilih foto telur, lalu tekan *Analisis Sekarang*
4. **Result Screen** — Menampilkan hasil: ✅ Segar (aman) atau ❌ Busuk (tidak aman)

---

## 🧠 Arsitektur Model CNN

Model klasifikasi menggunakan arsitektur CNN yang di-*export* ke format **PyTorch Lite** (`.pt`).

### Preprocessing Pipeline

```
Input Gambar
    │
    ▼
Resize (256 × 256)
    │
    ▼
Center Crop (224 × 224)
    │
    ▼
Normalisasi ImageNet
  Mean: [0.485, 0.456, 0.406]
  Std:  [0.229, 0.224, 0.225]
    │
    ▼
Inferensi Model
    │
    ▼
Softmax → Probabilitas
    │
    ▼
Hasil: "segar" atau "busuk"
```

### Kelas Output

| Indeks | Label | Keterangan |
|--------|-------|------------|
| 0 | `busuk` | Telur tidak aman untuk dikonsumsi |
| 1 | `segar` | Telur aman untuk dikonsumsi |

---

## 🛠️ Tech Stack

| Komponen | Teknologi |
|---|---|
| **Bahasa** | Kotlin |
| **Min SDK** | 24 (Android 7.0 Nougat) |
| **Target SDK** | 34 (Android 14) |
| **Build System** | Gradle 8.5 + AGP 8.2.2 |
| **ML Framework** | PyTorch Mobile Lite 2.1.0 |
| **UI** | XML Layout + View Binding |
| **Layout** | ConstraintLayout |
| **Design** | Material Design 1.11.0 |
| **Image Handling** | ExifInterface 1.3.7 |

---

## 📁 Struktur Proyek

```
FreshEgg/
├── app/
│   ├── src/main/
│   │   ├── java/com/fresh/egg/
│   │   │   ├── MainActivity.kt        # Activity utama (navigasi & UI)
│   │   │   └── Classifier.kt          # Kelas klasifikasi CNN
│   │   ├── assets/
│   │   │   └── egg_classifier.pt       # Model PyTorch Lite
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml   # Layout semua layar
│   │   │   ├── drawable/               # Ikon, background, asset gambar
│   │   │   ├── font/                   # Font VT323 (pixel style)
│   │   │   ├── values/                 # Strings, colors, themes
│   │   │   └── xml/                    # FileProvider paths
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── assets/                             # Asset gambar tambahan
├── logo/                               # Logo aplikasi
├── build.gradle.kts                    # Build config project
├── settings.gradle.kts
├── gradle.properties
└── gradlew / gradlew.bat
```

---

## 🚀 Cara Menjalankan

### Prasyarat

- **Android Studio** Hedgehog (2023.1.1) atau lebih baru
- **JDK 17**
- **Android SDK 34**

### Langkah-Langkah

1. **Clone repository**
   ```bash
   git clone https://github.com/<username>/FreshEgg.git
   cd FreshEgg
   ```

2. **Buka di Android Studio**
   - File → Open → Pilih folder project

3. **Sync Gradle**
   - Tunggu Android Studio mendownload semua dependensi

4. **Jalankan Aplikasi**
   - Pilih device/emulator → Klik **Run ▶**
   - Untuk fitur kamera, gunakan perangkat fisik

---

## 📦 Dependencies

```kotlin
// AndroidX Core
androidx.core:core-ktx:1.12.0
androidx.appcompat:appcompat:1.6.1
androidx.constraintlayout:constraintlayout:2.1.4
androidx.exifinterface:exifinterface:1.3.7

// Material Design
com.google.android.material:material:1.11.0

// PyTorch Mobile Lite (On-Device ML)
org.pytorch:pytorch_android_lite:2.1.0
org.pytorch:pytorch_android_torchvision_lite:2.1.0
```

---

## 🔐 Permissions

| Permission | Kegunaan |
|---|---|
| `CAMERA` | Mengambil foto telur menggunakan kamera perangkat |

> **Catatan:** Izin kamera bersifat opsional (`android:required="false"`). Aplikasi tetap bisa digunakan dengan memilih foto dari galeri.

---

## 👨‍💻 Kontributor

Dibuat sebagai bagian dari proyek **SEC 2026**.

---

## 📄 Lisensi

Proyek ini dibuat untuk keperluan edukasi.
