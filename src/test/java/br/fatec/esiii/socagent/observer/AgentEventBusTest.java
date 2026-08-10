package br.fatec.esiii.socagent.observer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.fatec.esiii.socagent.observer.AgentEvent.EventType;

class AgentEventBusTest {

    @Test
    @DisplayName("difunde o evento para todos os observadores inscritos")
    void difundeParaTodos() {
        AgentEventBus bus = new AgentEventBus();
        List<String> recebidosA = new ArrayList<>();
        List<String> recebidosB = new ArrayList<>();

        bus.subscribe(event -> recebidosA.add(event.title()));
        bus.subscribe(event -> recebidosB.add(event.title()));

        bus.publish(AgentEvent.of(EventType.STATE_CHANGED, "INC-1", "mudou", null));

        assertThat(recebidosA).containsExactly("mudou");
        assertThat(recebidosB).containsExactly("mudou");
    }

    @Test
    @DisplayName("falha de um observador nao impede os demais de receber")
    void falhaIsolada() {
        AgentEventBus bus = new AgentEventBus();
        List<String> sobreviventes = new ArrayList<>();

        bus.subscribe(event -> {
            throw new IllegalStateException("observador com defeito");
        });
        bus.subscribe(event -> sobreviventes.add(event.title()));

        bus.publish(AgentEvent.of(EventType.COMMAND_EXECUTED, "INC-1", "executado", null));

        assertThat(sobreviventes).containsExactly("executado");
    }

    @Test
    @DisplayName("o registrar inscreve todos os observadores declarados como bean")
    void registrarInscreveBeans() {
        AgentEventBus bus = new AgentEventBus();
        AuditTrailListener auditoria = new AuditTrailListener();

        new EventListenerRegistrar(bus, List.of(auditoria, new ConsoleListener()));
        assertThat(bus.listenerCount()).isEqualTo(2);

        bus.publish(AgentEvent.of(EventType.INCIDENT_CREATED, "INC-1", "aberto", "2 alertas"));

        assertThat(auditoria.entries()).hasSize(1);
        assertThat(auditoria.entries().getFirst()).contains("INCIDENT_CREATED").contains("aberto");
    }

    @Test
    @DisplayName("observador pode filtrar os tipos que lhe interessam")
    void filtraPorTipo() {
        AgentEventBus bus = new AgentEventBus();
        List<String> apenasComandos = new ArrayList<>();

        bus.subscribe(new AgentEventListener() {
            @Override
            public void onEvent(AgentEvent event) {
                apenasComandos.add(event.title());
            }

            @Override
            public boolean supports(AgentEvent event) {
                return event.type() == EventType.COMMAND_EXECUTED;
            }
        });

        bus.publish(AgentEvent.of(EventType.STATE_CHANGED, "INC-1", "transicao", null));
        bus.publish(AgentEvent.of(EventType.COMMAND_EXECUTED, "INC-1", "comando", null));

        assertThat(apenasComandos).containsExactly("comando");
    }
}
