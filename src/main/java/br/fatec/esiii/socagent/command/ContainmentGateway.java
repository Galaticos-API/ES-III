package br.fatec.esiii.socagent.command;

/**
 * Porta de saida para os sistemas que efetivamente executam a contencao
 * (EDR, firewall, NAC).
 *
 * <p>Isolar essa fronteira permite que os comandos sejam testados sem
 * infraestrutura e que a implementacao real seja trocada sem tocar no
 * restante do agente.
 */
public interface ContainmentGateway {

    String isolateHost(String hostname, String reason);

    String restoreHost(String hostname);

    String blockIp(String ipAddress, String reason);

    String unblockIp(String ipAddress);

    String collectForensics(String hostname);

    boolean isIsolated(String hostname);
}
