package br.fatec.esiii.socagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada do agente de triagem de incidentes de seguranca.
 */
@SpringBootApplication
public class SocAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocAgentApplication.class, args);
    }
}
