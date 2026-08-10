package br.fatec.esiii.socagent.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.fatec.esiii.socagent.domain.Alert;
import br.fatec.esiii.socagent.domain.Ioc;
import br.fatec.esiii.socagent.domain.Severity;
import br.fatec.esiii.socagent.domain.TriageVerdict;
import br.fatec.esiii.socagent.observer.AgentEventBus;

class IncidentStateTest {

    private Incident novoIncidente() {
        Alert alerta = new Alert("ALR-1", "suricata", "WKS-4471",
                "Trafego de exfiltracao detectado", Severity.CRITICAL, Instant.now(),
                List.of(Ioc.ip("185.220.101.7")));
        return new Incident("INC-1", List.of(alerta), new AgentEventBus());
    }

    private TriageVerdict verdadeiroPositivo() {
        return new TriageVerdict(TriageVerdict.Classification.TRUE_POSITIVE, 0.9,
                "Padrao compativel com exfiltracao", List.of("T1041"));
    }

    @Test
    @DisplayName("percorre o fluxo completo ate a contencao")
    void fluxoCompleto() {
        Incident incidente = novoIncidente();
        assertThat(incidente.phase()).isEqualTo(IncidentPhase.RECEIVED);

        incidente.triage(verdadeiroPositivo());
        assertThat(incidente.phase()).isEqualTo(IncidentPhase.TRIAGING);

        incidente.correlate();
        assertThat(incidente.phase()).isEqualTo(IncidentPhase.CORRELATING);

        incidente.requestApproval();
        assertThat(incidente.phase()).isEqualTo(IncidentPhase.AWAITING_APPROVAL);

        incidente.approve("analista.turno1");
        assertThat(incidente.phase()).isEqualTo(IncidentPhase.CONTAINING);
        assertThat(incidente.allowsContainmentCommands()).isTrue();

        incidente.close("Host isolado e evidencias preservadas");
        assertThat(incidente.phase()).isEqualTo(IncidentPhase.CLOSED);
    }

    @Test
    @DisplayName("nao permite contencao antes da aprovacao humana")
    void contencaoExigeAprovacao() {
        Incident incidente = novoIncidente();
        incidente.triage(verdadeiroPositivo());
        incidente.correlate();

        assertThat(incidente.allowsContainmentCommands()).isFalse();

        assertThatThrownBy(() -> incidente.approve("analista.turno1"))
                .isInstanceOf(IllegalTransitionException.class)
                .hasMessageContaining("CORRELATING");
    }

    @Test
    @DisplayName("aprovacao negada encerra o incidente sem conter")
    void aprovacaoNegadaEncerra() {
        Incident incidente = novoIncidente();
        incidente.triage(verdadeiroPositivo());
        incidente.correlate();
        incidente.requestApproval();

        incidente.deny("analista.turno1", "host critico em janela de faturamento");

        assertThat(incidente.phase()).isEqualTo(IncidentPhase.CLOSED);
        assertThat(incidente.isApproved()).isFalse();
        assertThat(incidente.allowsContainmentCommands()).isFalse();
    }

    @Test
    @DisplayName("incidente encerrado nao aceita mais nenhuma operacao")
    void estadoTerminalRejeitaTudo() {
        Incident incidente = novoIncidente();
        incidente.triage(new TriageVerdict(TriageVerdict.Classification.FALSE_POSITIVE, 0.95,
                "Trafego legitimo de backup", List.of()));
        incidente.close("Falso positivo confirmado");

        assertThat(incidente.phase()).isEqualTo(IncidentPhase.CLOSED);
        assertThatThrownBy(incidente::correlate).isInstanceOf(IllegalTransitionException.class);
        assertThatThrownBy(() -> incidente.approve("qualquer")).isInstanceOf(IllegalTransitionException.class);
    }
}
