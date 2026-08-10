package br.fatec.esiii.socagent.command;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Implementacao simulada da contencao, usada em laboratorio e na apresentacao.
 *
 * <p>Mantem o estado dos ativos em memoria para que o desfazer possa ser
 * demonstrado de verdade: isolar e restaurar produzem efeitos observaveis.
 */
@Component
public class SimulatedContainmentGateway implements ContainmentGateway {

    private final Set<String> isolatedHosts = ConcurrentHashMap.newKeySet();
    private final Set<String> blockedIps = ConcurrentHashMap.newKeySet();

    @Override
    public String isolateHost(String hostname, String reason) {
        if (!isolatedHosts.add(hostname)) {
            return "Host %s ja estava isolado".formatted(hostname);
        }
        return "Host %s isolado da rede. Motivo: %s".formatted(hostname, reason);
    }

    @Override
    public String restoreHost(String hostname) {
        if (!isolatedHosts.remove(hostname)) {
            return "Host %s nao estava isolado".formatted(hostname);
        }
        return "Host %s reconectado a rede".formatted(hostname);
    }

    @Override
    public String blockIp(String ipAddress, String reason) {
        if (!blockedIps.add(ipAddress)) {
            return "IP %s ja estava bloqueado".formatted(ipAddress);
        }
        return "IP %s bloqueado na borda. Motivo: %s".formatted(ipAddress, reason);
    }

    @Override
    public String unblockIp(String ipAddress) {
        if (!blockedIps.remove(ipAddress)) {
            return "IP %s nao estava bloqueado".formatted(ipAddress);
        }
        return "Bloqueio do IP %s removido".formatted(ipAddress);
    }

    @Override
    public String collectForensics(String hostname) {
        return "Coleta forense de %s concluida: memoria, processos e conexoes preservados"
                .formatted(hostname);
    }

    @Override
    public boolean isIsolated(String hostname) {
        return isolatedHosts.contains(hostname);
    }

    public Set<String> isolatedHosts() {
        return Set.copyOf(isolatedHosts);
    }

    public Set<String> blockedIps() {
        return Set.copyOf(blockedIps);
    }
}
