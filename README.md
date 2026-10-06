<div align="center">

<!-- Title Animation / Elegant -->
<img src="./app/src/main/res/drawable/logo.png" alt="SlyTask" width="150px" />
<h1>⚡ SLYTASK ⚡</h1>
<p align="center">
  <strong>An elegant, powerful, and modern Mobile Legends utility tool built with Jetpack Compose.</strong>
</p>

<!-- Cool Badges -->
<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Built%20With-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Requirements-Root%20%2F%20Magisk-red?style=for-the-badge" alt="Root Required">
  <img src="https://img.shields.io/github/license/iCansSungkar/SlyTask?style=for-the-badge&color=22c55e" alt="License">
</p>

</div>

---

<div align="center">
  <a href="#about-slytask">About</a> •
  <a href="#-key-features">Key Features</a> •
  <a href="#-tech-stack">Tech Stack</a> •
  <a href="#-how-it-works">How It Works</a> •
  <a href="#%EF%B8%8F-disclaimer">Disclaimer</a> •
  <a href="#-credits--acknowledgements">Credits</a>
</div>

<br>

### 📌 About SlyTask

**SlyTask** is a modern Android utility app specifically designed to help *Mobile Legends: Bang Bang* players manage multiple accounts instantly and securely. The app is developed using a **Neo-Bento Dashboard UI** approach to simplify account management (smurf/main) and the creation of new accounts without having to re-download game resources.

> 💡 **Fun Fact & Evolution:** This project is an evolution and full adaptation of an automation bash script I previously created, [MoLeTo (Mobile Legends Tools)](https://github.com/iCansSungkar/MoLeTo). The shell-based automation logic has now been migrated into an Android GUI app that is much more interactive, elegant, and modern.

---

### 🚀 Key Features

- 🔄 **Instant Account Switcher:** Back up login sessions offline from the system data directory and reload different accounts in seconds.
- 👥 **Instant Guest Account Creator:** Create new guest accounts instantly through an automated system that temporarily disables Google Play Services (GMS).
- 🎨 **Neo-Bento UI Design:** A clean, intuitive modern interface supporting Dark Mode & Light Mode transitions, as well as a multi-language system (Indonesian & English).
- 💻 **System Shell Terminal Logs:** An interactive live terminal feature directly within the app to monitor real-time superuser (su) command executions.
- 🛡️ **Dual Execution Mode:** Supports Real Root Mode (actual execution via root binary) and Simulation Sandbox Mode for developer testing purposes.

---

### 🛠️ Tech Stack

This application is built using cutting-edge technologies within the Android ecosystem:
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material Design 3)
- **State Management:** Kotlin Coroutines & Reactive StateFlow
- **Architecture:** MVVM with `MLAccountManager` backend integration
- **Root Executor:** Superuser Binary Handler (Magisk / APatch integration)

---

### 🕹️ How It Works

#### 1. Account Switching System
The app reads and copies MLBB login encryption session data stored in the `/data/data/com.mobile.legends` folder. When you switch accounts, the app securely overwrites the session files without disrupting the game's visual assets (3D/audio).

#### 2. New Account Creation (Instant Guest)
To bypass Google restrictions, the app detects when MLBB is running in the foreground and disables Google Play Services (`com.google.android.gms`) for a specific duration using a countdown timer. Once the account is successfully created, GMS will automatically be re-enabled.

---

### ⚠️ Disclaimer

> [!WARNING]
> **USE AT YOUR OWN RISK**
> 
> * **Not an Official App:** SlyTask is a third-party application and is **ABSOLUTELY NOT** an official app of, affiliated with, or endorsed by **Shanghai Moonton Technology Co., Ltd.**
> * **No Harm Intended:** This app was created purely as a local device data management utility tool for user efficiency. It **does not contain cheats, game modification scripts, skin hacks, in-app purchase bypasses, or any illegal activities** that harm Moonton or the player ecosystem.
> * **System Requirements:** This project requires **Superuser Root Access**. The developer is not responsible for any system failures, loss of account data, or performance issues on your device.
> * **Account Security:** Ensure you have bound your main account to Moonton/Facebook/TikTok before using the account switching feature to prevent loss of access.

---

### 🤝 Credits & Acknowledgements

This project was successfully developed thanks to the collaboration of the following great team:

* **Ihsan Sungkar** ([@iCansSungkar](https://github.com/iCansSungkar)) — *Lead Developer & Creator*
* **Ramadhan Sungkar** ([@adanSncrs](https://github.com/adanSncrs)) — *QA, Core Tester & Bug Hunter*

---
