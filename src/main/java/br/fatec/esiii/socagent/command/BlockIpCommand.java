package br.fatec.esiii.socagent.command;

import java.time.Duration;
import java.time.Instant;

/**
 * Bloqueia um endereco IP na borda da rede. Destrutiva, aprovavel e reversivel.
 */
public record BlockIpCommand(ContainmentGateway gateway, String ipAddress, String reason)
        implements AgentCommand {

    @Override
    public String name() {
        return "block_ip";
    }

    @Override
    public String description() {
        return "Bloquear o IP %s na borda (%s)".formatted(ipAddress, reason);
    }

    @Override
    public boolean requiresApproval() {
        return true;
    }

    @Override
    public boolean undoable() {
        return true;
    }

    @Override
    public CommandResult execute() {
        Instant start = Instant.now();
        String output = gateway.blockIp(ipAddress, reason);
        return CommandResult.ok(output, Duration.between(start, Instant.now()));
    }

    @Override
    public CommandResult undo() {
        Instant start = Instant.now();
        String output = gateway.unblockIp(ipAddress);
        return CommandResult.ok(output, Duration.between(start, Instant.now()));
    }
}
