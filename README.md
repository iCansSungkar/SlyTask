<div align="center">

<!-- Title Animasi / Elegan -->
<img src="./app/src/main/res/drawable/logo.png" alt="SlyTask" width="150px" />
<h1>⚡ SLYTASK ⚡</h1>
<p align="center">
  <strong>An elegant, powerful, and modern Mobile Legends utility tool built with Jetpack Compose.</strong>
</p>

<!-- Badges Kece -->
<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Built%20With-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Requirements-Root%20%2F%20Magisk-red?style=for-the-badge" alt="Root Required">
  <img src="https://img.shields.io/github/license/iCansSungkar/SlyTask?style=for-the-badge&color=22c55e" alt="License">
</p>

---

<p align="center">
  <a href="#-key-features">Fitur Utama</a> •
  <a href="#-tech-stack">Teknologi</a> •
  <a href="#%EF%B8%8F-how-it-works">Cara Kerja</a> •
  <a href="#-disclaimer">Disclaimer</a> •
  <a href="#-credits">Kredit</a>
</p>

</div>

<br>

## 📌 Tentang SlyTask

**SlyTask** adalah aplikasi utilitas Android modern yang dirancang khusus untuk membantu pemain *Mobile Legends: Bang Bang* mengelola multi-akun secara instan dan aman. Aplikasi ini dikembangkan menggunakan pendekatan desain **Neo-Bento Dashboard UI** untuk mempermudah manajemen akun (smurf/utama) serta proses pembuatan akun baru tanpa perlu melewati proses unduh ulang data game (*resource*)[cite: 1].

> 💡 **Fun Fact & Evolution:** Projek ini merupakan evolusi dan hasil adaptasi penuh dari *bash script* automasi yang sebelumnya saya buat, yaitu [MoLeTo (Mobile Legends Tools)](https://github.com/iCansSungkar/MoLeTo). Logika automasi berbasis shell tersebut kini telah dimigrasikan ke dalam aplikasi Android berbasis GUI yang jauh lebih interaktif, elegan, dan modern[cite: 1].

---

## 🚀 Key Features

- 🔄 **Instant Account Switcher:** Cadangkan sesi login secara offline dari direktori data sistem dan muat kembali akun yang berbeda dalam hitungan detik[cite: 1].
- 👥 **Instant Guest Account Creator:** Membuat akun *guest* baru secara instan melalui sistem otomatisasi penonaktifan *Google Play Services* (GMS) sementara[cite: 1].
- 🎨 **Neo-Bento UI Design:** Antarmuka modern yang bersih, intuitif, mendukung peralihan *Dark Mode* & *Light Mode*, serta sistem multi-bahasa (Bahasa Indonesia & English)[cite: 1].
- 💻 **System Shell Terminal Logs:** Fitur *live terminal* interaktif langsung di dalam aplikasi untuk memantau jalannya perintah *superuser (su)* secara real-time[cite: 1].
- 🛡️ **Dual Execution Mode:** Mendukung *Real Root Mode* (perintah riil via biner root) serta *Simulation Sandbox Mode* untuk keperluan testing pengembang[cite: 1].

---

## 🛠️ Tech Stack

Aplikasi ini dikembangkan menggunakan teknologi mutakhir dalam ekosistem Android[cite: 1]:
- **Language:** Kotlin[cite: 1]
- **UI Framework:** Jetpack Compose (Material Design 3)[cite: 1]
- **State Management:** Kotlin Coroutines & Reactive StateFlow[cite: 1]
- **Architecture:** MVVM dengan integrasi backend `MLAccountManager`[cite: 1]
- **Root Executor:** Superuser Binary Handler (Magisk / APatch integration)[cite: 1]

---

## 🕹️ How It Works (Cara Kerja)

### 1. Sistem Ganti Akun
Aplikasi membaca dan menyalin data sesi enkripsi login MLBB yang tersimpan di dalam folder `/data/data/com.mobile.legends`[cite: 1]. Saat Anda memuat (*switch*) akun, aplikasi akan menimpa berkas sesi tersebut secara aman tanpa mengganggu file aset visual (3D/audio) game[cite: 1].

### 2. Pembuatan Akun Baru (Instant Guest)
Untuk melewati pembatasan Google, aplikasi mendeteksi saat MLBB berjalan di *foreground* lalu menonaktifkan *Google Play Services* (`com.google.android.gms`) dalam durasi waktu tertentu menggunakan *countdown timer*[cite: 1]. Setelah akun berhasil dibuat, GMS akan otomatis diaktifkan kembali[cite: 1].

---

## ⚠️ Disclaimer

> [!WARNING]
> **PENGGUNAAN RESIKO SENDIRI (USE AT YOUR OWN RISK)**
> 
> * **Bukan Aplikasi Resmi:** SlyTask adalah aplikasi pihak ketiga dan **SAMA SEKALI BUKAN** aplikasi resmi dari, berafiliasi dengan, atau didukung oleh **Shanghai Moonton Technology Co., Ltd.**
> * **Tidak Merugikan Pihak Manapun:** Aplikasi ini dibuat murni sebagai alat bantu utilitas manajemen data lokal perangkat untuk efisiensi pengguna. Aplikasi ini **tidak mengandung cheat, script modifikasi game, hack skin, bypass pembelian in-app, atau tindakan ilegal lainnya** yang merugikan pihak Moonton maupun ekosistem pemain lainnya.
> * **Persyaratan Sistem:** Projek ini membutuhkan **Akses Root Superuser**[cite: 1]. Pengembang tidak bertanggung jawab atas kegagalan sistem, hilangnya data akun, atau masalah performa pada perangkat Anda.
> * **Keamanan Akun:** Pastikan Anda telah mengaitkan (*bind*) akun utama Anda ke Moonton/Facebook/TikTok sebelum menggunakan fitur ganti akun guna menghindari hilangnya akses[cite: 1].

---

## 🤝 Credits & Acknowledgements

Projek ini berhasil dikembangkan berkat kerja sama tim hebat berikut[cite: 1]:

* **Ihsan Sungkar** ([@iCansSungkar](https://github.com/iCansSungkar)) — *Lead Developer & Creator*[cite: 1]
* **AI Assistant (DeepMind Antigravity)** — *AI Specialist & Architecture Advisor*[cite: 1]
* **Ramadhan Sungkar** ([@adanSncrs](https://github.com/adanSncrs)) — *QA, Core Tester & Bug Hunter*[cite: 1]

---

<div align="center">
  <p>Maintained with ❤️ by <a href="https://github.com/iCansSungkar">iCansSungkar</a></p>
</div>
