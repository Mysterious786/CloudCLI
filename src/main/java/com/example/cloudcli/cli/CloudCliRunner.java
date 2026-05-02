package com.example.cloudcli.cli;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Main CLI runner that integrates Picocli with Spring Boot
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Command(
    name = "cloudcli",
    description = "Multi-Database Backup CLI Tool",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    subcommands = {
        TuiShell.class,
        UnifiedShell.class,
        EnhancedInteractiveShell.class,
        InteractiveCommand.class,
        BackupCommand.class,
        RestoreCommand.class,
        DownloadCommand.class,
        ConnectionTestCommand.class,
        ScheduleCommand.class,
        UpdateCommand.class,
        AuthCommand.RegisterCommand.class,
        AuthCommand.LoginCommand.class,
        AuthCommand.LogoutCommand.class,
        AuthCommand.WhoAmICommand.class,
        ListBackupsCommand.class
    }
)
public class CloudCliRunner implements CommandLineRunner, ExitCodeGenerator {
    
    private final CommandLine.IFactory factory;
    private int exitCode;
    
    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            // Launch full-screen TUI by default
            log.info("No arguments provided, launching TUI...");
            args = new String[]{"tui"};
        }
        
        CommandLine commandLine = new CommandLine(this, factory);
        exitCode = commandLine.execute(args);
    }
    
    @Override
    public int getExitCode() {
        return exitCode;
    }
}
