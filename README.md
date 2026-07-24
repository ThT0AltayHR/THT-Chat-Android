<h1 align="center">
  <img src="https://img.shields.io/badge/THT--Chat%2B-v3.0-darkblue?style=for-the-badge&logo=android" alt="THT-Chat+"/>
</h1>

<p align="center">
  <strong>🔒 Güvenli İletişim — Özgür Sohbet</strong><br/>
  <em>AltayHR Developer</em>
</p>

<p align="center">
  <a href="https://turkhackteam.org">turkhackteam.org</a> ·
  <a href="https://thtakademi.com.tr">thtakademi.com.tr</a>
</p>

<p align="center">
  <!-- APK İndir butonu — Actions'dan son başarılı APK -->
  <a href="https://github.com/ThT0AltayHR/THT-Chat-Android/releases/latest/download/THT-Chat-plus-v3.0.apk">
    <img src="https://img.shields.io/badge/⬇️%20APK%20İndir-Android-green?style=for-the-badge&logo=android" alt="APK İndir"/>
  </a>
</p>

---

## 📱 Uygulama Hakkında

Java ve Android Studio ile geliştirilen, **Firebase** altyapılı tam özellikli Türkçe mesajlaşma uygulaması.  
THT-Chat+, Discord benzeri grup sistemi, WebRTC sesli arama, sesli mesajlaşma ve link önizleme gibi gelişmiş özellikler sunar.

| Alan | Değer |
|---|---|
| **Paket Adı** | `com.turkhackteam.org` |
| **Geliştirici** | AltayHR Developer |
| **Min SDK** | Android 8.0 (API 26) |
| **Versiyon** | 3.0 |

---

## ✨ Özellikler

### 💬 Mesajlaşma
- ✅ Gerçek zamanlı birebir mesajlaşma (Firebase Realtime DB)
- ✅ **Resim gönderme** (Firebase Storage)
- ✅ **Sesli mesaj gönderme** — basılı tut, bırak gönder
- ✅ **Link önizleme** — OG etiketleri ile otomatik önizleme (Jsoup)
- ✅ Mesaj düzenleme ve silme (uzun basınca)
- ✅ Ekran görüntüsü algılama bildirimi

### 📞 Sesli Arama
- ✅ **WebRTC tabanlı gerçek sesli arama** (Google STUN sunucuları)
- ✅ Mikrofon kapatma / hoparlör açma
- ✅ Çağrı süresi sayacı
- ✅ Firebase Realtime DB ile sinyal ağı

### 👥 Discord Benzeri Grup Sistemi
- ✅ **Grup oluşturma** — zorunlu kural girişi ile
- ✅ **Hoş geldiniz ekranı** — gruba ilk katılımda grup logosu + açılış poster ekranı
- ✅ **Kural kabul zorunluluğu** — aşağı kaydırmadan kabul butonu aktif olmaz
- ✅ **Metin kanalları** — varsayılan: `#genel`, `#duyurular`
- ✅ **Sesli kanallar** — varsayılan: 3 adet, admin istediği kadar ekleyebilir
- ✅ **Hiyerarşik yönetim sistemi:**
  - 👑 Admin — tam yetki
  - 🔵 Moderatör — kısıtlama kontrolü
  - 👤 Üye — standart erişim
- ✅ **Admin yetki devri** — adminlik başkasına devredilebilir
- ✅ **Grup ayarları (admin/mod):**
  - 🔗 Link paylaşımını engelle / aç
  - 🎥 Video paylaşımını engelle / aç
  - 📁 Dosya paylaşımını engelle / aç
  - 💬 Tüm kullanıcılar için mesajı kapat / aç
- ✅ Sesli kanallarda WebRTC ile gerçek zamanlı ses iletişimi

### 🔐 Güvenlik & Hesap
- ✅ Firebase Auth (e-posta + şifre)
- ✅ Profil fotoğrafı yükleme
- ✅ Kullanıcı adıyla arama
- ✅ Çevrimiçi / son görülme durumu

---

## 🛡️ İzinler

```
RECORD_AUDIO       — Sesli mesaj + WebRTC arama
CAMERA             — Fotoğraf çekme
READ/WRITE_STORAGE — Medya gönderme
READ_CONTACTS      — Rehberden uygulama kullanıcılarını bulma
INTERNET           — Firebase + WebRTC
MODIFY_AUDIO_SETTINGS — Hoparlör yönetimi
```

---

## 🔧 Kurulum

1. Android Studio'da projeyi açın
2. Firebase'de proje oluşturun ve `google-services.json` dosyasını `app/` klasörüne ekleyin
3. Firebase Console'da Authentication (Email/Password) ve Realtime Database'i aktif edin
4. Projeyi çalıştırın

---

## 📦 APK İndirme

> **Otomatik derleme** her `main` push'unda GitHub Actions ile çalışır.

<p align="center">
  <a href="https://github.com/ThT0AltayHR/THT-Chat-Android/releases/latest/download/THT-Chat-plus-v3.0.apk">
    <img src="https://img.shields.io/badge/⬇️%20En%20Son%20APK'yı%20İndir-v3.0-brightgreen?style=for-the-badge&logo=android&logoColor=white" alt="APK İndir"/>
  </a>
</p>

---

*THT-Chat+ — AltayHR Developer tarafından geliştirilmiştir.*
