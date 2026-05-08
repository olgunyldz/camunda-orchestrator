# Pool Orchestrator

Camunda BPM tabanlı kredi başvuru havuz orkestrasyon servisi. Müşteri segmentine göre başvuruları KBS, LKS ve TBH kontrol havuzlarına sırayla yönlendirir; her adımı takip eder.

## Genel Mimari

```
POST /api/basvuru/olustur
        │
        ▼
BasvuruController ──► DB kaydet ──► Camunda başlat (Business Key: basvuruNo)
                                          │
                                          ▼
                                  PoolRouterDelegate
                                  (segment + strategy order'a göre yönlendir)
                                          │
                    ┌─────────────────────┼──────────────────────┐
                    ▼                     ▼                      ▼
             KBS Havuzu             LKS Havuzu             TBH Havuzu
            (Sub-Process)          (Sub-Process)          (Sub-Process)
          strategy chain           random %50             random %50
          TcknKontrol(1)        başarı/başarısız       başarı/başarısız
          RiskKontrol(2)               │                      │
          AdresKontrol(3)        ERR_LKS_FAIL           ERR_TBH_FAIL
                │                      │                      │
          ERR_KBS_RESTART         LKS_HATA →           TBH_HATA →
          (max 3 retry)           finishTask             finishTask
```

## Havuz Yönlendirme Kuralları

| Segment | KBS Kontrolleri (sıralı) | LKS | TBH |
|---------|--------------------------|-----|-----|
| VIP | TcknKontrol(1) → RiskKontrol(2) → AdresKontrol(3) | Aktif | Pasif |
| Diğer | TcknKontrol(1) | Pasif | Pasif |

## BPMN Akış Diyagramı

```
start → routeTask → [gateway]
                        ├── kbsRequired=true  → Sub_KbsHavuzu ──┐
                        └── kbsRequired=false ───────────────────┘
                                                                  ▼
                                                           [lksGateway]
                                         ├── lksRequired=true  → Sub_LksHavuzu ──┐
                                         └── lksRequired=false ───────────────────┘
                                                                                   ▼
                                                                            [tbhGateway]
                                                          ├── tbhRequired=true  → Sub_TbhHavuzu ──┐
                                                          └── tbhRequired=false ───────────────────┘
                                                                                                    ▼
                                                                                              finishTask → end
```

**Hata durumları:**

| Hata | Sonuç |
|------|-------|
| KBS hatası (≥3 retry) | `status = KBS_HATA` → finishTask |
| LKS başarısız | `status = LKS_HATA` → finishTask |
| TBH başarısız | `status = TBH_HATA` → finishTask |
| Tümü başarılı | `status = DONE` → finishTask |

## KBS Strategy Sıralaması

KBS havuzu içindeki kontroller `KbsKontrolStrategy.getOrder()` değerine göre otomatik sıralanır. `PoolRouterDelegate` segment kuralına göre hangi strategy'lerin çalışacağını filtreler; sırayı her strategy kendisi tanımlar.

```java
TcknKontrolStrategy  → order = 1   // TCKN doğrulama
RiskKontrolStrategy  → order = 2   // Risk skoru hesapla (kbs_riskScore set eder)
AdresKontrolStrategy → order = 3   // Adres doğrula (kbs_riskScore kullanır)
```

Yeni bir strategy eklemek için sadece `KbsKontrolStrategy` implemente edip `getOrder()` tanımlamak yeterlidir.

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

## Test — curl Komutları

### Standart müşteri (KBS)
```bash
curl -s -X POST http://localhost:8080/api/basvuru/olustur \
  -H "Content-Type: application/json" \
  -d '{
    "musteriTipi": "BIREYSEL",
    "tutar": 25000.0,
    "segment": "STANDART",
    "tckn": "12345678901",
    "addressId": 1
  }'
```

### VIP müşteri (KBS + LKS, random başarı/başarısız)
```bash
curl -s -X POST http://localhost:8080/api/basvuru/olustur \
  -H "Content-Type: application/json" \
  -d '{
    "musteriTipi": "BIREYSEL",
    "tutar": 150000.0,
    "segment": "VIP",
    "tckn": "98765432100",
    "addressId": 42
  }'
```

### Validasyon — tutar negatif (hata beklenir)
```bash
curl -s -X POST http://localhost:8080/api/basvuru/olustur \
  -H "Content-Type: application/json" \
  -d '{
    "musteriTipi": "BIREYSEL",
    "tutar": -500.0,
    "segment": "STANDART",
    "tckn": "12345678901",
    "addressId": 1
  }'
```

### Validasyon — zorunlu alan eksik (hata beklenir)
```bash
curl -s -X POST http://localhost:8080/api/basvuru/olustur \
  -H "Content-Type: application/json" \
  -d '{
    "musteriTipi": "BIREYSEL",
    "tutar": 10000.0,
    "segment": "STANDART",
    "addressId": 1
  }'
```

### Döngüde 10 başvuru (KBS retry ve LKS rastgeleliğini gözlemlemek için)
```bash
for i in $(seq 1 10); do
  echo "--- Başvuru $i ---"
  curl -s -X POST http://localhost:8080/api/basvuru/olustur \
    -H "Content-Type: application/json" \
    -d "{
      \"musteriTipi\": \"BIREYSEL\",
      \"tutar\": $((RANDOM % 100000 + 1000)).0,
      \"segment\": \"VIP\",
      \"tckn\": \"1234567890$i\",
      \"addressId\": $i
    }"
  echo ""
done
```

### Camunda Cockpit — süreç geçmişi
```
http://localhost:8080/camunda
Kullanıcı : admin
Şifre     : admin
```
Cockpit → Processes → MainPoolProcess → History

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
│   ├── PoolRouterDelegate         # Segment filtresi + strategy sıralaması
│   ├── KbsDinamikKontrolDelegate  # Strategy pattern ile KBS multi-instance
│   ├── LksKontrolDelegate         # Random başarı/başarısız
│   └── TbhKontrolDelegate         # Random başarı/başarısız
├── entity/                 # Basvuru, BasvuruTarihce
├── listener/               # HavuzDurumGuncellemeListener
├── repository/             # JPA repository'ler
├── service/                # DynamicWorkflowService, RiskService, AddressService
└── strategy/               # KbsKontrolStrategy implementasyonları
    ├── KbsKontrolStrategy     (interface: getKontrolAdi, getOrder, kontroluYap)
    ├── TcknKontrolStrategy    order=1
    ├── RiskKontrolStrategy    order=2
    └── AdresKontrolStrategy   order=3
```

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
