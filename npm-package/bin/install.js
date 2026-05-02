#!/usr/bin/env node

/**
 * CloudCLI Post-install script
 * Downloads the JAR on first install
 */

const https = require('https');
const fs = require('fs');
const path = require('path');
const os = require('os');

const REPO = 'saqlainansari/cloudcli';
const JAR_NAME = 'cloudcli.jar';
const INSTALL_DIR = path.join(os.homedir(), '.cloudcli');
const JAR_PATH = path.join(INSTALL_DIR, JAR_NAME);

// Colors
const GREEN  = '\x1b[32m';
const CYAN   = '\x1b[36m';
const YELLOW = '\x1b[33m';
const RED    = '\x1b[31m';
const BOLD   = '\x1b[1m';
const RESET  = '\x1b[0m';

console.log(`\n${CYAN}${BOLD}`);
console.log('  ╔═══════════════════════════════════════════════╗');
console.log('  ║         ☁️  CloudCLI Installer                ║');
console.log('  ║     Multi-Database Backup CLI Tool            ║');
console.log('  ╚═══════════════════════════════════════════════╝');
console.log(`${RESET}\n`);

// Check Java
const { execSync } = require('child_process');

try {
    const javaVersion = execSync('java -version 2>&1').toString();
    console.log(`${GREEN}✓${RESET} Java found`);
} catch (e) {
    console.log(`${RED}✗ Java not found!${RESET}`);
    console.log(`\n${YELLOW}CloudCLI requires Java 17+. Install it:${RESET}`);
    console.log('  macOS:  brew install openjdk@17');
    console.log('  Linux:  sudo apt install openjdk-17-jdk');
    console.log('  Download: https://adoptium.net/temurin/releases/?version=17\n');
    process.exit(1);
}

// Create install dir
if (!fs.existsSync(INSTALL_DIR)) {
    fs.mkdirSync(INSTALL_DIR, { recursive: true });
}

// Skip if JAR already exists
if (fs.existsSync(JAR_PATH)) {
    console.log(`${GREEN}✓${RESET} CloudCLI already installed at ${JAR_PATH}`);
    console.log(`\n${BOLD}Run: cloudcli${RESET}\n`);
    process.exit(0);
}

// Get latest release URL
console.log('  Fetching latest release...');

const options = {
    hostname: 'api.github.com',
    path: `/repos/${REPO}/releases/latest`,
    headers: { 'User-Agent': 'cloudcli-npm-installer' }
};

https.get(options, (res) => {
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
        try {
            const release = JSON.parse(data);
            const asset = release.assets && release.assets.find(a => a.name === JAR_NAME);
            
            if (!asset) {
                // Fallback to direct URL
                downloadJar(`https://github.com/${REPO}/releases/latest/download/${JAR_NAME}`);
            } else {
                downloadJar(asset.browser_download_url);
            }
        } catch (e) {
            downloadJar(`https://github.com/${REPO}/releases/latest/download/${JAR_NAME}`);
        }
    });
}).on('error', () => {
    downloadJar(`https://github.com/${REPO}/releases/latest/download/${JAR_NAME}`);
});

function downloadJar(url) {
    console.log(`  Downloading CloudCLI...`);
    
    followRedirects(url, (finalUrl) => {
        const file = fs.createWriteStream(JAR_PATH);
        
        https.get(finalUrl, (response) => {
            const total = parseInt(response.headers['content-length'], 10);
            let downloaded = 0;
            
            response.on('data', (chunk) => {
                downloaded += chunk.length;
                file.write(chunk);
                
                if (total) {
                    const pct = Math.round((downloaded / total) * 100);
                    process.stdout.write(`\r  Progress: ${pct}%`);
                }
            });
            
            response.on('end', () => {
                file.end();
                console.log(`\n${GREEN}✓${RESET} Downloaded to ${JAR_PATH}`);
                console.log(`\n${GREEN}${BOLD}`);
                console.log('  ╔═══════════════════════════════════════════════╗');
                console.log('  ║     ✅ CloudCLI Installed Successfully!       ║');
                console.log('  ╚═══════════════════════════════════════════════╝');
                console.log(`${RESET}`);
                console.log(`${BOLD}Get started:${RESET}`);
                console.log('  cloudcli              # Launch CloudCLI');
                console.log('  cloudcli --help       # Show all commands\n');
            });
            
            file.on('error', (err) => {
                console.error(`${RED}✗ Download failed: ${err.message}${RESET}`);
                process.exit(1);
            });
        }).on('error', (err) => {
            console.error(`${RED}✗ Download failed: ${err.message}${RESET}`);
            process.exit(1);
        });
    });
}

function followRedirects(url, callback) {
    const urlObj = new URL(url);
    https.get({ hostname: urlObj.hostname, path: urlObj.pathname + urlObj.search, headers: { 'User-Agent': 'cloudcli-npm-installer' } }, (res) => {
        if (res.statusCode === 301 || res.statusCode === 302) {
            followRedirects(res.headers.location, callback);
        } else {
            callback(url);
        }
    }).on('error', () => callback(url));
}
