# NotifForwarder - Panduan Build dari Termux

## Cara 1: Build via GitHub Actions (Direkomendasikan)

### Setup sekali di Termux
```bash
pkg install git
git config --global user.name "namamu"
git config --global user.email "email@kamu.com"
```

### Upload ke GitHub
```bash
# Extract zip, masuk folder
cd /sdcard/Download/NotifForwarder

git init
git add .
git commit -m "first commit"
git branch -M main
git remote add origin https://github.com/USERNAMEMU/NotifForwarder.git
git push -u origin main
```
> Saat push: masukkan username GitHub + Personal Access Token
> Buat token: GitHub → Settings → Developer settings → Personal access tokens → Generate new token (centang `repo`)

### Download APK
1. Buka GitHub → tab **Actions**
2. Tunggu workflow selesai (~5 menit)
3. Klik **NotifForwarder-debug** → Download

---

## Cara 2: Build lokal di Termux (tanpa internet terus-menerus)

```bash
# Install dependensi
pkg install openjdk-17

# Setup Android SDK
mkdir -p ~/android-sdk/cmdline-tools
cd ~/android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest
cd ~

# Set environment
export ANDROID_SDK_ROOT=~/android-sdk
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:~/android-sdk/cmdline-tools/latest/bin:~/android-sdk/platform-tools

# Install SDK components
sdkmanager --licenses
sdkmanager "platforms;android-34" "build-tools;34.0.0"

# Masuk folder project
cd /sdcard/Download/NotifForwarder

# Generate gradle wrapper (sekali saja)
chmod +x gradlew

# Build APK
./gradlew assembleDebug
```

APK ada di: `app/build/outputs/apk/debug/app-debug.apk`

### Install APK
```bash
cp app/build/outputs/apk/debug/app-debug.apk /sdcard/
```
Lalu buka file manager → tap APK → install.

---

## Cara Pakai Aplikasi

1. Buka app → isi **Bot Token** dan **Chat ID** → Simpan
2. Tekan **Izinkan Akses Notifikasi** → pilih Notif Forwarder → aktifkan
3. Opsional: tekan **Pilih Aplikasi** untuk filter
4. Tekan **Sembunyikan Ikon** agar tidak terlihat di beranda

### Membuka app saat tersembunyi
Buka dial pad → ketik: `*#*#8642#*#*`

### Cara dapat Bot Token & Chat ID
1. Chat `@BotFather` di Telegram → `/newbot` → ikuti langkah → catat **token**
2. Chat bot kamu → buka browser: `https://api.telegram.org/bot<TOKEN>/getUpdates`
3. Catat nilai **chat.id** dari response JSON
