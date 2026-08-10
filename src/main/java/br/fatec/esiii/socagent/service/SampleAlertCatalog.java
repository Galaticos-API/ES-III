package br.fatec.esiii.socagent.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Component;

import br.fatec.esiii.socagent.domain.Alert;
import br.fatec.esiii.socagent.domain.Ioc;
import br.fatec.esiii.socagent.domain.Severity;

/**
 * Cenarios de alerta usados na demonstracao.
 *
 * <p>Reproduzem padroes documentados no MITRE ATT&CK, com hosts e enderecos
 * ficticios. O IP 185.220.101.7 pertence a faixa historicamente associada a
 * nos de saida Tor, escolhido por ser reconhecivel em apresentacao sem
 * envolver nenhum alvo real.
 */
@Component
public class SampleAlertCatalog {

    public record Scenario(String name, String description, List<Alert> alerts) {
    }

    public List<Scenario> scenarios() {
        return List.of(exfiltracao(), forcaBruta(), ransomware(), falsoPositivo());
    }

    public Scenario byName(String name) {
        return scenarios().stream()
                .filter(scenario -> scenario.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cenario desconhecido: " + name));
    }

    private Scenario exfiltracao() {
        Instant now = Instant.now();
        return new Scenario("Exfiltracao via canal C2",
                "Volume anomalo de saida para infraestrutura suspeita",
                List.of(
                        new Alert("ALR-1001", "suricata", "WKS-4471",
                                "Volume de saida 480 MB para destino externo em 12 minutos",
                                Severity.CRITICAL, now.minus(12, ChronoUnit.MINUTES),
                                List.of(Ioc.ip("185.220.101.7"))),
                        new Alert("ALR-1002", "zeek", "WKS-4471",
                                "Conexao TLS persistente com certificado autoassinado",
                                Severity.HIGH, now.minus(15, ChronoUnit.MINUTES),
                                List.of(Ioc.ip("185.220.101.7"), Ioc.host("rundll32.exe")))));
    }

    private Scenario forcaBruta() {
        Instant now = Instant.now();
        return new Scenario("Forca bruta em VPN",
                "Sequencia de falhas de autenticacao seguida de sucesso",
                List.of(
                        new Alert("ALR-2001", "wazuh", "VPN-GW-01",
                                "312 falhas de autenticacao para a conta j.silva em 4 minutos",
                                Severity.HIGH, now.minus(30, ChronoUnit.MINUTES),
                                List.of(new Ioc(Ioc.IocType.USER_ACCOUNT, "j.silva"),
                                        Ioc.ip("45.155.205.233"))),
                        new Alert("ALR-2002", "wazuh", "VPN-GW-01",
                                "Autenticacao bem-sucedida da mesma origem apos as falhas",
                                Severity.CRITICAL, now.minus(26, ChronoUnit.MINUTES),
                                List.of(new Ioc(Ioc.IocType.USER_ACCOUNT, "j.silva"),
                                        Ioc.ip("45.155.205.233")))));
    }

    private Scenario ransomware() {
        Instant now = Instant.now();
        return new Scenario("Cifragem em massa",
                "Renomeacao acelerada de arquivos em compartilhamento de rede",
                List.of(
                        new Alert("ALR-3001", "edr", "FS-PROD-02",
                                "1.847 arquivos renomeados com extensao desconhecida em 3 minutos",
                                Severity.CRITICAL, now.minus(5, ChronoUnit.MINUTES),
                                List.of(Ioc.host("svchost32.exe"),
                                        new Ioc(Ioc.IocType.FILE_HASH,
                                                "d41d8cd98f00b204e9800998ecf8427e"))),
                        new Alert("ALR-3002", "edr", "FS-PROD-02",
                                "Exclusao de copias de sombra via vssadmin",
                                Severity.CRITICAL, now.minus(4, ChronoUnit.MINUTES),
                                List.of(Ioc.host("vssadmin.exe")))));
    }

    private Scenario falsoPositivo() {
        Instant now = Instant.now();
        return new Scenario("Backup noturno",
                "Trafego alto de saida em janela de manutencao conhecida",
                List.of(
                        new Alert("ALR-4001", "suricata", "BKP-SRV-01",
                                "Transferencia de 12 GB para destino externo na janela 02:00-04:00",
                                Severity.MEDIUM, now.minus(2, ChronoUnit.HOURS),
                                List.of(Ioc.ip("203.0.113.45"),
                                        Ioc.host("veeam-agent")))));
    }
}
