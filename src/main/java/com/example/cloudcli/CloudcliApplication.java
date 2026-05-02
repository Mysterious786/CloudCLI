package com.example.cloudcli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for CloudCLI - Multi-Database Backup Tool
 * 
 * Features:
 * - Multiple database support: MySQL, PostgreSQL, MongoDB, SQLite, Supabase
 * - Multiple storage providers: Local, AWS S3, Backblaze B2, Wasabi
 * - Notifications: Console, Email, Slack
 * - Scheduling support for automated backups
 * - Compression support
 */
@SpringBootApplication
@EnableScheduling
public class CloudcliApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudcliApplication.class, args);
	}
}
