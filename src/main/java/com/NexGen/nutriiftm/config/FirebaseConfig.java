package com.NexGen.nutriiftm.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Inicializa o Firebase Admin SDK usando o caminho do arquivo de
 * credenciais definido na variável de ambiente FIREBASE_CREDENTIALS_PATH.
 *
 * Esse arquivo JSON é secreto e NUNCA deve ser commitado no repositório —
 * ele deve morar fora da pasta do projeto (ex: ~/.secrets/nutriiftm/).
 *
 * Se a variável de ambiente não estiver definida, a aplicação ainda sobe
 * (não quebra o resto do sistema), mas login com Firebase não vai
 * funcionar até a variável ser configurada corretamente.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() {
        String caminho = System.getenv("FIREBASE_CREDENTIALS_PATH");

        if (caminho == null || caminho.isBlank()) {
            log.error("FIREBASE_CREDENTIALS_PATH não definida. Login com Firebase ficará inoperante.");
            return null;
        }

        try (FileInputStream credenciais = new FileInputStream(caminho)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credenciais))
                    .build();

            if (!FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.getInstance();
            }
            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK inicializado com sucesso.");
            return app;

        } catch (IOException e) {
            log.error("Erro ao ler o arquivo de credenciais do Firebase em '{}'. " +
                    "Verifique se o caminho está correto e se o arquivo existe.", caminho, e);
            return null;
        }
    }
}