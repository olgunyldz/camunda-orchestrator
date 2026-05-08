# Pool Orchestrator

Camunda BPM tabanlı kredi başvuru havuz orkestrasyon servisi. Müşteri segmentine göre başvuruları ilgili kontrol havuzlarına (KBS, LKS, TBH) yönlendirir ve her adımı takip eder.

## Genel Mimari

```
POST /api/basvuru/olustur
        │
        ▼
BasvuruController ──► DB kaydet ──► Camunda başlat (Business Key: basvuruNo)
                                          │
                                          ▼
                                  PoolRouterDelegate
                                  (segment'e göre yönlendir)
                                          │
                          ┌───────────────┴───────────────┐
                          ▼                               ▼
                   KBS Havuzu                        BYPASS
                 (Sub-Process)                    (finishTask)
                  TcknKontrol
                  RiskKontrol   ◄─── boundary event (max 3 retry)
                  AdresKontrol
```

### Havuz Yönlendirme Kuralları

| Segment | KBS Kontrolleri | LKS |
|---------|----------------|-----|
| VIP | TcknKontrol → RiskKontrol → AdresKontrol | Aktif |
| Diğer | TcknKontrol | Pasif |

## Teknolojiler

- **Java 21** (Virtual Threads aktif)
- **Spring Boot 3.2.5**
- **Camunda BPM 7.21** (Embedded Engine)
- **PostgreSQL 15**
- **Lombok**

## Başlatma

### 1. Veritabanını başlat

```bash
docker-compose up -d
```

### 2. Uygulamayı başlat

```bash
./mvnw spring-boot:run
```

### 3. Camunda Cockpit

```
http://localhost:8080/camunda
Kullanıcı: admin
Şifre:     admin
```

## API

### Başvuru Oluştur

```http
POST /api/basvuru/olustur
Content-Type: application/json

{
  "musteriTipi": "BIREYSEL",
  "tutar": 50000.0,
  "segment": "VIP",
  "tckn": "12345678901",
  "addressId": 1
}
```

**Yanıt:**
```
Başvuru oluşturuldu. No: BSV-3F2A1B4C
```

**Validasyon kuralları:**
- `musteriTipi`, `segment`, `tckn` — boş olamaz
- `tutar` — pozitif sayı olmalı
- `addressId` — zorunlu

## Ortam Değişkenleri

| Değişken | Varsayılan | Açıklama |
|----------|-----------|----------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/camunda_db` | PostgreSQL bağlantı URL'i |
| `DB_USER` | `camunda` | Veritabanı kullanıcısı |
| `DB_PASS` | `camunda_password` | Veritabanı şifresi |
| `NVI_URL` | `https://api.nvi.gov.tr/v1/dogrula` | NVI doğrulama servisi |

## Proje Yapısı

```
src/main/java/com/bank/workflow/
├── api/                    # REST controller
├── annotation/             # @CamundaPoolStep
├── aspect/                 # DelegateLoggingAspect (süre & hata takibi)
├── delegate/               # Camunda JavaDelegate'ler
│   ├── PoolRouterDelegate      # Segment bazlı yönlendirme
│   ├── KbsDinamikKontrolDelegate  # Strateji pattern ile KBS
│   ├── LksKontrolDelegate
│   └── TbhKontrolDelegate
├── entity/                 # Basvuru, BasvuruTarihce
├── listener/               # HavuzDurumGuncellemeListener
├── repository/             # JPA repository'ler
├── service/                # DynamicWorkflowService, RiskService, AddressService
└── strategy/               # KbsKontrolStrategy implementasyonları
    ├── TcknKontrolStrategy
    ├── RiskKontrolStrategy
    └── AdresKontrolStrategy
```

## BPMN Süreci

Süreç uygulama başlangıcında `DynamicWorkflowService` tarafından programatik olarak oluşturulur ve Camunda'ya deploy edilir. BPMN dosyası yoktur, kod doğrudan model üretir.

**KBS Hata Yönetimi:** Subprocess içinde `ERR_KBS_RESTART` hatası fırlatıldığında boundary event devreye girer. En fazla 3 deneme yapılır; başarısız olursa `status=KBS_HATA` ile süreç sonlanır.

## Format & Build

```bash
# Kod formatla
./mvnw spotless:apply

# Format kontrol (CI'da otomatik çalışır)
./mvnw spotless:check

# Build
./mvnw clean package
```

Spotless (Google Java Format) `verify` fazında otomatik çalışır.
