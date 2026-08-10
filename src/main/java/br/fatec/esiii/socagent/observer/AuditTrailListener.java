package br.fatec.esiii.socagent.observer;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

/**
 * Observador que materializa a trilha de auditoria do incidente.
 *
 * <p>Em um SOC real esta trilha e requisito de conformidade: toda acao tomada
 * sobre um ativo precisa ser reconstituivel. Aqui ela e mantida em memoria e
 * exposta para a GUI e para o relatorio final.
 */
@Component
public class AuditTrailListener implements AgentEventListener {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(java.time.ZoneId.systemDefault());

    private final List<String> entries = new CopyOnWriteArrayList<>();

    @Override
    public void onEvent(AgentEvent event) {
        entries.add("%s | %-20s | %s%s".formatted(
                TIME.format(event.occurredAt()),
                event.type(),
                event.title(),
                event.detail() == null || event.detail().isBlank() ? "" : " -- " + event.detail()));
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }

    public void clear() {
        entries.clear();
    }
}
