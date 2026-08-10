package br.fatec.esiii.socagent.command;

/**
 * Acao executavel do agente (padrao Command).
 *
 * <p>Encapsular a acao como objeto e o que permite enfileirar, auditar, exigir
 * aprovacao e desfazer de forma uniforme. O {@link CommandInvoker} trata todos
 * os comandos igualmente, sem conhecer nenhum deles.
 *
 * <p>Decisao de projeto deliberada: comandos sao <b>codigo Java</b>, nunca
 * texto gerado pelo modelo. O modelo escolhe qual comando invocar e com quais
 * parametros; o que cada comando faz e determinado em tempo de compilacao.
 */
public interface AgentCommand {

    /** Identificador estavel, usado no log de auditoria. */
    String name();

    /** Descricao legivel exibida na GUI e na solicitacao de aprovacao. */
    String description();

    CommandResult execute();

    /**
     * Comandos que alteram o estado de um ativo exigem aprovacao humana e so
     * podem rodar quando o incidente esta na fase de contencao.
     */
    default boolean requiresApproval() {
        return false;
    }

    default boolean undoable() {
        return false;
    }

    /** Acao compensatoria. So e chamada se {@link #undoable()} for verdadeiro. */
    default CommandResult undo() {
        throw new UnsupportedOperationException(
                "Comando '%s' nao suporta desfazer".formatted(name()));
    }
}
