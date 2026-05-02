# ☁️ CloudCLI — Multi-Database Backup Tool

> A powerful, easy-to-use CLI tool for backing up databases to the cloud.

[![Release](https://img.shields.io/github/v/release/saqlainansari/cloudcli)](https://github.com/saqlainansari/cloudcli/releases)
[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://adoptium.net)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

---

## ⚡ Quick Install

### macOS / Linux
```bash
curl -fsSL https://raw.githubusercontent.com/saqlainansari/cloudcli/main/install.sh | bash
```

### Manual (All Platforms)
1. Download `cloudcli.jar` from [Releases](https://github.com/saqlainansari/cloudcli/releases/latest)
2. Run:
```bash
java -jar cloudcli.jar
```

### Requirements
- **Java 17+** — [Download here](https://adoptium.net/temurin/releases/?version=17)

---

## 🚀 Getting Started

```bash
# Launch CloudCLI
cloudcli

# Register a new account
# → You'll be auto-logged in and receive a welcome email!

# Or login to existing account
# → Access the full backup dashboard
```

---

## 🗄️ Supported Databases

| Database | Backup | Restore | Test Connection |
|----------|--------|---------|-----------------|
| MySQL | ✅ | ✅ | ✅ |
| PostgreSQL | ✅ | ✅ | ✅ |
| MongoDB | ✅ | ✅ | ✅ |
| SQLite | ✅ | ✅ | ✅ |
| Supabase | ✅ | ✅ | ✅ |

---

## ☁️ Supported Cloud Storage

| Provider | Free Tier |
|----------|-----------|
| Backblaze B2 | 10 GB free |
| AWS S3 | 5 GB free |
| Wasabi | Paid |
| Local | Unlimited |

---

## 📧 Features

- 🔐 **Multi-user authentication** with session persistence
- 📦 **Backup & restore** any database with one command
- ☁️ **Cloud storage** — store backups securely in the cloud
- 📧 **Email notifications** — beautiful HTML emails on backup events
- 🎉 **Welcome email** — sent automatically on registration
- 💻 **Interactive shell** — TAB completion, command history
- 🎨 **Guided wizard** — step-by-step backup creation
- 📥 **Download backups** — retrieve from cloud anytime
- 🗜️ **Compression** — gzip compression to save storage

---

## 📖 Usage

```bash
cloudcli                    # Launch unified interface
cloudcli --help             # Show all commands
cloudcli backup --help      # Backup command help
cloudcli register --help    # Register command help
```

### Direct Commands
```bash
# Register
cloudcli register -e email@example.com -u username -p password

# Login
cloudcli login -u username -p password

# Backup SQLite
cloudcli backup --type=sqlite --database=mydb.db

# Backup PostgreSQL
cloudcli backup --type=postgres --database=mydb --host=localhost --username=postgres

# List backups
cloudcli list

# Download backup
cloudcli download --id <backup-id>
```

---

## ⚙️ Configuration

Copy `.env.production` to `.env` and fill in your credentials:

```bash
cp .env.production .env
```

```env
# Supabase (user database)
DATABASE_URL=jdbc:postgresql://db.xxx.supabase.co:5432/postgres?sslmode=require
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your-password

# Backblaze B2 (backup storage)
BACKBLAZE_ACCESS_KEY=your-key-id
BACKBLAZE_SECRET_KEY=your-secret-key

# Email notifications
EMAIL_ENABLED=true
EMAIL_USERNAME=your@gmail.com
EMAIL_PASSWORD=your-app-password
EMAIL_TO=your@gmail.com
```

---

## 🏗️ Build from Source

```bash
# Clone
git clone https://github.com/saqlainansari/cloudcli.git
cd cloudcli

# Build
./mvnw clean package -DskipTests

# Run
java -jar target/cloudcli-0.0.1-SNAPSHOT.jar
```

---

## 👨‍💻 Author

**Saqlain Zarjis Ansari**

- 📞 Phone: +91 8442883695
- 📧 Email: saqlainzarjisansari@gmail.com
- 💼 LinkedIn: [linkedin.com/in/saqlain-zarjis-ansari-108b2621b](https://www.linkedin.com/in/saqlain-zarjis-ansari-108b2621b/)

---

## 📄 License

MIT License — see [LICENSE](LICENSE) file for details.
