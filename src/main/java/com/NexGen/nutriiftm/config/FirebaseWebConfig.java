package com.NexGen.nutriiftm.config;

import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expõe a configuração PÚBLICA do Firebase (client-side) para as páginas
 * Thymeleaf (login, cadastro, recuperar-senha) via model attribute
 * "firebaseWeb".
 *
 * IMPORTANTE: essas chaves (apiKey, authDomain, etc.) NÃO são secretas —
 * elas ficam expostas no HTML do navegador por natureza do Firebase Auth
 * client-side. Isso é diferente do FIREBASE_CREDENTIALS_PATH usado em
 * FirebaseConfig.java, que sim é secreto (Admin SDK, server-side).
 *
 * Valores vêm do Firebase Console > Configurações do projeto > Geral >
 * "Seus apps" > app Web > SDK setup and configuration.
 */
@Configuration
public class FirebaseWebConfig {

    @Bean
    public FirebaseWeb firebaseWeb() {
        return new FirebaseWeb(
                System.getenv("FIREBASE_API_KEY"),
                System.getenv("FIREBASE_AUTH_DOMAIN"),
                System.getenv("FIREBASE_PROJECT_ID"),
                System.getenv("FIREBASE_STORAGE_BUCKET"),
                System.getenv("FIREBASE_MESSAGING_SENDER_ID"),
                System.getenv("FIREBASE_APP_ID")
        );
    }

    @Getter
    public static class FirebaseWeb {
        private final String apiKey;
        private final String authDomain;
        private final String projectId;
        private final String storageBucket;
        private final String messagingSenderId;
        private final String appId;

        public FirebaseWeb(String apiKey, String authDomain, String projectId,
                            String storageBucket, String messagingSenderId, String appId) {
            this.apiKey = apiKey;
            this.authDomain = authDomain;
            this.projectId = projectId;
            this.storageBucket = storageBucket;
            this.messagingSenderId = messagingSenderId;
            this.appId = appId;
        }
    }
}