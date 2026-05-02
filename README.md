# ☁️ CloudCLI — Multi-Database Backup Tool

> A powerful, production-ready CLI tool for backing up databases to the cloud with multi-user authentication, email notifications, and a beautiful interactive interface.

**Project URL:** https://github.com/Mysterious786/CloudCLI

[![Release](https://img.shields.io/github/v/release/Mysterious786/CloudCLI)](https://github.com/Mysterious786/CloudCLI/releases/latest)
[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://adoptium.net)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![npm](https://img.shields.io/badge/npm-%40saqlain8442%2Fcloudcli-red)](https://www.npmjs.com/package/@saqlain8442/cloudcli)

---

## ⚡ Quick Install

### Option 1 — curl (macOS / Linux)
```bash
curl -fsSL https://raw.githubusercontent.com/Mysterious786/CloudCLI/main/install.sh | bash
```

### Option 2 — npm
```bash
npm install -g @saqlain8442/cloudcli
```

### Option 3 — Direct JAR Download
Download `cloudcli.jar` from [Releases](https://github.com/Mysterious786/CloudCLI/releases/latest) and run:
```bash
java -jar cloudcli.jar
```

### Option 4 — Self Update (if already installed)
```bash
cloudcli update
```

> **Requirement:** Java 17+ — [Download here](https://adoptium.net/temurin/releases/?version=17)

---

## 🚀 Getting Started

```bash
# Launch CloudCLI
cloudcli

# First time? Register an account
# → Auto-logged in after registration
# → Beautiful welcome email sent automatically

# Already have an account? Login
# → Access the full backup dashboard
```

---

## 🗄️ Supported Databases

| Database | Backup | Restore | Test Connection |
|----------|:------:|:-------:|:---------------:|
| MySQL | ✅ | ✅ | ✅ |
| PostgreSQL | ✅ | ✅ | ✅ |
| MongoDB | ✅ | ✅ | ✅ |
| SQLite | ✅ | ✅ | ✅ |
| Supabase | ✅ | ✅ | ✅ |

---

## ☁️ Supported Cloud Storage

| Provider | Free Tier | Notes |
|----------|-----------|-------|
| Backblaze B2 | 10 GB free | Default provider |
| AWS S3 | 5 GB free | S3-compatible |
| Wasabi | Paid | Cheaper than AWS |
| Local | Unlimited | For development |

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🔐 Multi-user Auth | Register, login, session persistence |
| 📦 Backup & Restore | One-command backup for any database |
| ☁️ Cloud Storage | Backblaze B2, AWS S3, Wasabi |
| 📧 Email Notifications | Beautiful HTML emails on backup events |
| 🎉 Welcome Email | Auto-sent on new user registration |
| 💻 Command Shell | TAB completion, command history |
| 🎨 Interactive Wizard | Step-by-step guided backup creation |
| 📥 Download Backups | Pick from numbered list, custom filename |
| 🗜️ Compression | gzip compression to save storage |
| 🔄 Self Update | `cloudcli update` to get latest version |
| 🗃️ Supabase DB | Production PostgreSQL via Supabase |

---

## 📖 Usage

```bash
cloudcli                    # Launch interactive interface
cloudcli --help             # Show all commands
cloudcli update             # Update to latest version
```

### Direct Commands
```bash
# Register a new account
cloudcli register -e email@example.com -u username -p password

# Login
cloudcli login -u username -p password

# Backup SQLite database
cloudcli backup --type=sqlite --database=mydb.db

# Backup PostgreSQL database
cloudcli backup --type=postgres --database=mydb --host=localhost --username=postgres

# Backup with compression
cloudcli backup --type=sqlite --database=mydb.db --compress

# List all your backups
cloudcli list

# Download a backup (interactive)
cloudcli interactive  # Choose option 4

# Download by ID
cloudcli download --id <backup-id>

# Test database connection
cloudcli test --type=postgres --host=localhost --database=mydb --username=postgres

# Logout
cloudcli logout
```

---

## ⚙️ Configuration

Create `~/.cloudcli/.env` with your credentials:

```env
# Cloud Storage (Backblaze B2)
BACKBLAZE_ACCESS_KEY=your-key-id
BACKBLAZE_SECRET_KEY=your-secret-key
BACKBLAZE_BUCKET=your-bucket-name
BACKBLAZE_REGION=us-east-005
BACKBLAZE_ENDPOINT=https://s3.us-east-005.backblazeb2.com

# User Database (Supabase/PostgreSQL)
DATABASE_URL=jdbc:postgresql://db.xxx.supabase.co:5432/postgres?sslmode=require
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your-password

# Email Notifications (Gmail)
EMAIL_ENABLED=true
EMAIL_USERNAME=your@gmail.com
EMAIL_PASSWORD=your-gmail-app-password
EMAIL_FROM=your@gmail.com
EMAIL_TO=your@gmail.com
```

> **Gmail App Password:** Go to [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords) to generate one.

---

## 🏗️ Build from Source

```bash
# Clone the repository
git clone https://github.com/Mysterious786/CloudCLI.git
cd CloudCLI

# Build
./mvnw clean package -DskipTests

# Run
java -jar target/cloudcli-0.0.1-SNAPSHOT.jar
```

---

## 🏛️ Architecture

Built with:
- **Java 17** + **Spring Boot 3.3.5**
- **Picocli 4.7.5** — CLI framework
- **JLine 3** — Interactive shell with TAB completion
- **Lanterna 3** — Full-screen TUI
- **Hibernate JPA** — Database ORM
- **AWS SDK v2** — S3-compatible storage
- **JavaMail** — HTML email notifications

Design Patterns used:
- Strategy (backup strategies per DB type)
- Factory (storage provider selection)
- Repository (data access layer)
- Observer (notification system)
- Facade (backup orchestration)

---

## 👨‍💻 Author

**Saqlain Zarjis Ansari**

- 📞 Phone: [+91 8442883695](tel:+918442883695)
- 📧 Email: [saqlainzarjisansari@gmail.com](mailto:saqlainzarjisansari@gmail.com)
- 💼 LinkedIn: [linkedin.com/in/saqlain-zarjis-ansari-108b2621b](https://www.linkedin.com/in/saqlain-zarjis-ansari-108b2621b/)

---

## 📄 License

MIT License — see [LICENSE](LICENSE) file for details.

---

## 🔗 Links

- **GitHub:** https://github.com/Mysterious786/CloudCLI
- **Releases:** https://github.com/Mysterious786/CloudCLI/releases/latest
- **npm:** https://www.npmjs.com/package/@saqlain8442/cloudcli
- **Install Script:** https://raw.githubusercontent.com/Mysterious786/CloudCLI/main/install.sh
