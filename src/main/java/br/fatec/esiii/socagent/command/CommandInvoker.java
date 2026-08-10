package br.fatec.esiii.socagent.command;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.springframework.stereotype.Component;

import br.fatec.esiii.socagent.observer.AgentEvent;
import br.fatec.esiii.socagent.observer.AgentEvent.EventType;
import br.fatec.esiii.socagent.observer.AgentEventBus;
import br.fatec.esiii.socagent.state.Incident;

/**
 * Invoker do padrao Command: enfileira, autoriza, executa e desfaz comandos
 * sem conhecer nenhuma implementacao concreta.
 *
 * <p>Aqui os tres padroes se encontram. O invoker consulta o
 * {@link Incident} (State) para saber se acoes destrutivas estao liberadas, e
 * publica cada passo no {@link AgentEventBus} (Observer). Nenhum comando
 * destrutivo escapa da barreira: a checagem e feita no invoker, nao em cada
 * comando, de modo que um comando novo ja nasce protegido.
 */
@Component
public class CommandInvoker {

    private final AgentEventBus eventBus;
    private final Deque<AgentCommand> queue = new ArrayDeque<>();
    private final Deque<AgentCommand> undoStack = new ArrayDeque<>();
    private final List<ExecutionRecord> history = new ArrayList<>();

    public CommandInvoker(AgentEventBus eventBus) {
        this.eventBus = eventBus;
    }

    /** Registro imutavel de uma execucao, base da trilha de auditoria. */
    public record ExecutionRecord(String commandName, String description, CommandResult result) {
    }

    public void enqueue(Incident incident, AgentCommand command) {
        queue.addLast(command);
        eventBus.publish(AgentEvent.of(EventType.COMMAND_QUEUED, incident.id(),
                command.name(), command.description()));
    }

    /**
     * Executa toda a fila. Comandos que exigem aprovacao sao recusados enquanto
     * o incidente nao estiver na fase de contencao.
     */
    public List<ExecutionRecord> executeQueue(Incident incident) {
        List<ExecutionRecord> executed = new ArrayList<>();
        while (!queue.isEmpty()) {
            executed.add(execute(incident, queue.pollFirst()));
        }
        return executed;
    }

    public ExecutionRecord execute(Incident incident, AgentCommand command) {
        if (command.requiresApproval() && !incident.allowsContainmentCommands()) {
            CommandResult refused = CommandResult.refused(
                    "Bloqueado: '%s' exige aprovacao humana e o incidente esta na fase %s"
                            .formatted(command.name(), incident.phase()));
            return record(incident, command, refused, EventType.COMMAND_FAILED);
        }

        CommandResult result;
        try {
            result = command.execute();
        } catch (RuntimeException ex) {
            result = CommandResult.failure(
                    "Falha ao executar '%s': %s".formatted(command.name(), ex.getMessage()),
                    java.time.Duration.ZERO);
        }

        if (result.success() && command.undoable()) {
            undoStack.push(command);
        }
        return record(incident, command, result,
                result.success() ? EventType.COMMAND_EXECUTED : EventType.COMMAND_FAILED);
    }

    /**
     * Desfaz o ultimo comando reversivel executado. Em resposta a incidentes,
     * reverter uma contencao equivocada rapidamente vale tanto quanto aplica-la.
     */
    public ExecutionRecord undoLast(Incident incident) {
        AgentCommand command = undoStack.poll();
        if (command == null) {
            return new ExecutionRecord("undo", "nada a desfazer",
                    CommandResult.refused("Nenhum comando reversivel no historico"));
        }
        CommandResult result = command.undo();
        ExecutionRecord entry = new ExecutionRecord(command.name(), "desfazer: " + command.description(), result);
        history.add(entry);
        eventBus.publish(AgentEvent.of(EventType.COMMAND_UNDONE, incident.id(),
                command.name(), result.output()));
        return entry;
    }

    private ExecutionRecord record(Incident incident, AgentCommand command,
            CommandResult result, EventType type) {
        ExecutionRecord entry = new ExecutionRecord(command.name(), command.description(), result);
        history.add(entry);
        eventBus.publish(AgentEvent.of(type, incident.id(), command.name(), result.output())
                .with("success", result.success()));
        return entry;
    }

    public List<ExecutionRecord> history() {
        return List.copyOf(history);
    }

    public int pendingCount() {
        return queue.size();
    }

    public int undoableCount() {
        return undoStack.size();
    }

    public void reset() {
        queue.clear();
        undoStack.clear();
        history.clear();
    }
}
