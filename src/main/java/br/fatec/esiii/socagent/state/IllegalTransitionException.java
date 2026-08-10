package br.fatec.esiii.socagent.state;

/**
 * Lancada quando se tenta uma operacao que a fase atual do incidente nao permite.
 *
 * <p>E o mecanismo que torna o fluxo deterministico: nenhuma acao de contencao
 * pode ocorrer antes da aprovacao humana, porque o estado simplesmente nao
 * expoe a operacao.
 */
public class IllegalTransitionException extends RuntimeException {

    private final IncidentPhase currentPhase;
    private final String attemptedOperation;

    public IllegalTransitionException(IncidentPhase currentPhase, String attemptedOperation) {
        super("Operacao '%s' nao e permitida na fase %s"
                .formatted(attemptedOperation, currentPhase));
        this.currentPhase = currentPhase;
        this.attemptedOperation = attemptedOperation;
    }

    public IncidentPhase currentPhase() {
        return currentPhase;
    }

    public String attemptedOperation() {
        return attemptedOperation;
    }
}
