#!/usr/bin/env node

/**
 * CloudCLI Entry Point
 * Launches the Java JAR
 */

const { spawn } = require('child_process');
const path = require('path');
const os = require('os');
const fs = require('fs');

const JAR_PATH = path.join(os.homedir(), '.cloudcli', 'cloudcli.jar');

// Check if JAR exists
if (!fs.existsSync(JAR_PATH)) {
    console.error('\x1b[31m✗ CloudCLI JAR not found.\x1b[0m');
    console.error('  Try reinstalling: npm install -g cloudcli');
    process.exit(1);
}

// Launch the JAR passing all arguments
const args = ['-jar', JAR_PATH, ...process.argv.slice(2)];

const child = spawn('java', args, {
    stdio: 'inherit',
    env: process.env
});

child.on('error', (err) => {
    if (err.code === 'ENOENT') {
        console.error('\x1b[31m✗ Java not found!\x1b[0m');
        console.error('  Install Java 17+: https://adoptium.net/temurin/releases/?version=17');
    } else {
        console.error(`\x1b[31m✗ Error: ${err.message}\x1b[0m`);
    }
    process.exit(1);
});

child.on('exit', (code) => {
    process.exit(code || 0);
});
