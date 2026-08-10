package br.fatec.esiii.socagent.strategy;

import java.util.List;
import java.util.Optional;

import br.fatec.esiii.socagent.domain.TriageVerdict;
import br.fatec.esiii.socagent.state.Incident;

/**
 * Porta de saida para o modelo de linguagem.
 *
 * <p>Os planejadores dependem desta interface, nunca do Spring AI diretamente.
 * Isso permite testar cada estrategia com um analista falso e deterministico,
 * e trocar o modelo aberto sem tocar na logica de planejamento.
 */
public interface ThreatAnalyst {

    /** Classifica o incidente: verdadeiro positivo, falso positivo ou revisao. */
    TriageVerdict classify(Incident incident);

    /** Produz o plano completo de resposta de uma unica vez. */
    List<ProposedAction> proposePlan(Incident incident);

    /** Sugere a proxima acao a partir do que ja foi observado. Vazio encerra o ciclo. */
    Optional<ProposedAction> proposeNext(Incident incident, List<String> observations);
}
