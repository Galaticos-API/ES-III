package br.fatec.esiii.socagent.mitre;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import br.fatec.esiii.socagent.domain.MitreTechnique;

/**
 * Base local de tecnicas MITRE ATT&CK.
 *
 * <p>Mantida em memoria e offline por decisao de projeto: o agente precisa
 * funcionar sem rede e sem servico externo, e o subconjunto abaixo cobre os
 * cenarios usados na demonstracao.
 */
@Component
public class MitreRepository {

    private static final Map<String, MitreTechnique> TECHNIQUES = Map.of(
            "T1041", new MitreTechnique("T1041", "Exfiltration Over C2 Channel", "Exfiltration",
                    "Adversarios enviam dados roubados pelo mesmo canal usado para comando e controle."),
            "T1071", new MitreTechnique("T1071", "Application Layer Protocol", "Command and Control",
                    "Comunicacao com a infraestrutura do atacante disfarcada em protocolos legitimos."),
            "T1059", new MitreTechnique("T1059", "Command and Scripting Interpreter", "Execution",
                    "Execucao de comandos e scripts por interpretadores como PowerShell e bash."),
            "T1110", new MitreTechnique("T1110", "Brute Force", "Credential Access",
                    "Tentativas repetidas de autenticacao para descobrir credenciais validas."),
            "T1486", new MitreTechnique("T1486", "Data Encrypted for Impact", "Impact",
                    "Cifragem de dados da vitima para interromper a operacao, tipico de ransomware."),
            "T1021", new MitreTechnique("T1021", "Remote Services", "Lateral Movement",
                    "Uso de servicos remotos legitimos como RDP e SSH para mover-se pela rede."),
            "T1567", new MitreTechnique("T1567", "Exfiltration Over Web Service", "Exfiltration",
                    "Envio de dados para servicos web legitimos usados como canal de saida."));

    public MitreTechnique find(String techniqueId) {
        if (techniqueId == null) {
            return MitreTechnique.unknown("desconhecida");
        }
        String normalized = techniqueId.trim().toUpperCase();
        return TECHNIQUES.getOrDefault(normalized, MitreTechnique.unknown(normalized));
    }

    public List<MitreTechnique> findAll(List<String> ids) {
        return ids.stream().map(this::find).toList();
    }

    public int size() {
        return TECHNIQUES.size();
    }
}
