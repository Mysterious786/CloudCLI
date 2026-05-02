#!/bin/bash

# CloudCLI Installer
# Usage: curl -fsSL https://raw.githubusercontent.com/saqlainansari/cloudcli/main/install.sh | bash

set -e

# ── Config ────────────────────────────────────────────────────────────────────
REPO="Mysterious786/cloudcli"
INSTALL_DIR="/usr/local/bin"
JAR_DIR="$HOME/.cloudcli"
JAR_NAME="cloudcli.jar"
CLI_NAME="cloudcli"
VERSION="latest"
GITHUB_API="https://api.github.com/repos/$REPO/releases/latest"
JAVA_MIN_VERSION=17

# ── Colors ────────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# ── Banner ────────────────────────────────────────────────────────────────────
echo ""
echo -e "${CYAN}${BOLD}"
echo "  ╔═══════════════════════════════════════════════╗"
echo "  ║         ☁️  CloudCLI Installer                ║"
echo "  ║     Multi-Database Backup CLI Tool            ║"
echo "  ║     by Saqlain Zarjis Ansari                  ║"
echo "  ╚═══════════════════════════════════════════════╝"
echo -e "${NC}"

# ── Helper functions ──────────────────────────────────────────────────────────
info()    { echo -e "${BLUE}[INFO]${NC}  $1"; }
success() { echo -e "${GREEN}[✓]${NC}    $1"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $1"; }
error()   { echo -e "${RED}[✗]${NC}    $1"; exit 1; }
step()    { echo -e "\n${BOLD}${CYAN}▶ $1${NC}"; }

# ── Step 1: Check OS ──────────────────────────────────────────────────────────
step "Checking system..."

OS="$(uname -s)"
ARCH="$(uname -m)"

case "$OS" in
  Linux*)   OS_TYPE="Linux" ;;
  Darwin*)  OS_TYPE="macOS" ;;
  *)        error "Unsupported OS: $OS. CloudCLI supports macOS and Linux." ;;
esac

info "OS: $OS_TYPE ($ARCH)"

# ── Step 2: Check Java ────────────────────────────────────────────────────────
step "Checking Java installation..."

if ! command -v java &> /dev/null; then
    echo ""
    warn "Java not found! CloudCLI requires Java $JAVA_MIN_VERSION or higher."
    echo ""
    echo -e "${YELLOW}Install Java using one of these methods:${NC}"
    echo ""
    
    if [ "$OS_TYPE" = "macOS" ]; then
        echo "  ${BOLD}Option 1 - Homebrew (recommended):${NC}"
        echo "    brew install openjdk@17"
        echo ""
        echo "  ${BOLD}Option 2 - Download directly:${NC}"
        echo "    https://adoptium.net/temurin/releases/?version=17"
    else
        echo "  ${BOLD}Ubuntu/Debian:${NC}"
        echo "    sudo apt update && sudo apt install openjdk-17-jdk"
        echo ""
        echo "  ${BOLD}Fedora/RHEL:${NC}"
        echo "    sudo dnf install java-17-openjdk"
        echo ""
        echo "  ${BOLD}Download directly:${NC}"
        echo "    https://adoptium.net/temurin/releases/?version=17"
    fi
    echo ""
    error "Please install Java $JAVA_MIN_VERSION+ and run this installer again."
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | awk -F '"' '{print $2}' | awk -F '.' '{print $1}')
if [ "$JAVA_VERSION" -lt "$JAVA_MIN_VERSION" ] 2>/dev/null; then
    error "Java $JAVA_MIN_VERSION+ required. Found Java $JAVA_VERSION. Please upgrade."
fi

success "Java $JAVA_VERSION found"

# ── Step 3: Check curl/wget ───────────────────────────────────────────────────
step "Checking download tools..."

if command -v curl &> /dev/null; then
    DOWNLOADER="curl"
    success "curl found"
elif command -v wget &> /dev/null; then
    DOWNLOADER="wget"
    success "wget found"
else
    error "Neither curl nor wget found. Please install one and try again."
fi

# ── Step 4: Get latest version ────────────────────────────────────────────────
step "Fetching latest version..."

if [ "$DOWNLOADER" = "curl" ]; then
    RELEASE_INFO=$(curl -s "$GITHUB_API" 2>/dev/null)
else
    RELEASE_INFO=$(wget -qO- "$GITHUB_API" 2>/dev/null)
fi

if [ -z "$RELEASE_INFO" ]; then
    warn "Could not fetch release info from GitHub. Using direct download..."
    DOWNLOAD_URL="https://github.com/$REPO/releases/latest/download/$JAR_NAME"
    RELEASE_VERSION="latest"
else
    RELEASE_VERSION=$(echo "$RELEASE_INFO" | grep '"tag_name"' | sed -E 's/.*"([^"]+)".*/\1/')
    DOWNLOAD_URL=$(echo "$RELEASE_INFO" | grep "browser_download_url.*$JAR_NAME" | sed -E 's/.*"([^"]+)".*/\1/')
    
    if [ -z "$DOWNLOAD_URL" ]; then
        DOWNLOAD_URL="https://github.com/$REPO/releases/latest/download/$JAR_NAME"
    fi
fi

info "Version: $RELEASE_VERSION"
info "Download URL: $DOWNLOAD_URL"

# ── Step 5: Create install directory ─────────────────────────────────────────
step "Setting up directories..."

mkdir -p "$JAR_DIR"
success "Created $JAR_DIR"

# ── Step 6: Download JAR ──────────────────────────────────────────────────────
step "Downloading CloudCLI..."

JAR_PATH="$JAR_DIR/$JAR_NAME"

if [ "$DOWNLOADER" = "curl" ]; then
    curl -L --progress-bar "$DOWNLOAD_URL" -o "$JAR_PATH"
else
    wget --show-progress -q "$DOWNLOAD_URL" -O "$JAR_PATH"
fi

if [ ! -f "$JAR_PATH" ]; then
    error "Download failed. Please check your internet connection and try again."
fi

success "Downloaded to $JAR_PATH"

# ── Step 7: Create launcher script ───────────────────────────────────────────
step "Creating launcher script..."

LAUNCHER_PATH="$JAR_DIR/$CLI_NAME"

cat > "$LAUNCHER_PATH" << EOF
#!/bin/bash
# CloudCLI Launcher
# Auto-generated by installer

CLOUDCLI_JAR="$JAR_DIR/$JAR_NAME"
CLOUDCLI_ENV="$JAR_DIR/.env"

# Auto-load .env if it exists
if [ -f "\$CLOUDCLI_ENV" ]; then
    export \$(cat "\$CLOUDCLI_ENV" | grep -v '^#' | grep -v '^\$' | xargs)
fi

if [ ! -f "\$CLOUDCLI_JAR" ]; then
    echo "❌ CloudCLI JAR not found at \$CLOUDCLI_JAR"
    echo "   Please reinstall: curl -fsSL https://raw.githubusercontent.com/$REPO/main/install.sh | bash"
    exit 1
fi

exec java -jar "\$CLOUDCLI_JAR" "\$@"
EOF

chmod +x "$LAUNCHER_PATH"
success "Created launcher at $LAUNCHER_PATH"

# ── Step 8: Add to PATH ───────────────────────────────────────────────────────
step "Adding CloudCLI to PATH..."

# Try to install to /usr/local/bin (system-wide)
if [ -w "$INSTALL_DIR" ]; then
    ln -sf "$LAUNCHER_PATH" "$INSTALL_DIR/$CLI_NAME"
    success "Installed to $INSTALL_DIR/$CLI_NAME (system-wide)"
    PATH_ADDED=true
elif sudo -n true 2>/dev/null; then
    sudo ln -sf "$LAUNCHER_PATH" "$INSTALL_DIR/$CLI_NAME"
    success "Installed to $INSTALL_DIR/$CLI_NAME (system-wide, with sudo)"
    PATH_ADDED=true
else
    # Fall back to user's local bin
    LOCAL_BIN="$HOME/.local/bin"
    mkdir -p "$LOCAL_BIN"
    ln -sf "$LAUNCHER_PATH" "$LOCAL_BIN/$CLI_NAME"
    
    # Add to shell profile
    SHELL_PROFILE=""
    if [ -f "$HOME/.zshrc" ]; then
        SHELL_PROFILE="$HOME/.zshrc"
    elif [ -f "$HOME/.bashrc" ]; then
        SHELL_PROFILE="$HOME/.bashrc"
    elif [ -f "$HOME/.bash_profile" ]; then
        SHELL_PROFILE="$HOME/.bash_profile"
    fi
    
    if [ -n "$SHELL_PROFILE" ]; then
        if ! grep -q "$LOCAL_BIN" "$SHELL_PROFILE"; then
            echo "" >> "$SHELL_PROFILE"
            echo "# CloudCLI" >> "$SHELL_PROFILE"
            echo "export PATH=\"\$PATH:$LOCAL_BIN\"" >> "$SHELL_PROFILE"
            success "Added $LOCAL_BIN to PATH in $SHELL_PROFILE"
        fi
    fi
    
    PATH_ADDED=false
    warn "Installed to $LOCAL_BIN/$CLI_NAME (user-local)"
fi

# ── Step 9: Verify installation ───────────────────────────────────────────────
step "Verifying installation..."

if command -v cloudcli &> /dev/null || [ -f "$INSTALL_DIR/$CLI_NAME" ] || [ -f "$HOME/.local/bin/$CLI_NAME" ]; then
    success "CloudCLI installed successfully!"
else
    warn "Installation complete but 'cloudcli' may not be in PATH yet."
fi

# ── Done! ─────────────────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}${BOLD}"
echo "  ╔═══════════════════════════════════════════════╗"
echo "  ║     ✅ CloudCLI Installed Successfully!       ║"
echo "  ╚═══════════════════════════════════════════════╝"
echo -e "${NC}"

echo -e "${BOLD}Version:${NC}  $RELEASE_VERSION"
echo -e "${BOLD}Location:${NC} $JAR_PATH"
echo ""
echo -e "${BOLD}${CYAN}Get started:${NC}"
echo ""

if [ "$PATH_ADDED" = false ]; then
    echo -e "  ${YELLOW}First, reload your shell:${NC}"
    echo "    source $SHELL_PROFILE"
    echo "    # or open a new terminal"
    echo ""
fi

echo "  cloudcli              # Launch CloudCLI"
echo "  cloudcli --help       # Show all commands"
echo "  cloudcli --version    # Show version"
echo ""
echo -e "${BOLD}${CYAN}First time? Run:${NC}"
echo "  cloudcli              # Register or login to get started"
echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "  Developed by ${BOLD}Saqlain Zarjis Ansari${NC}"
echo -e "  LinkedIn: ${BLUE}https://linkedin.com/in/saqlain-zarjis-ansari-108b2621b${NC}"
echo -e "  Phone: +91 8442883695"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
