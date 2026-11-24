package com.waypoint.project_tcc_app.config;

import com.waypoint.project_tcc_app.service.NotificacaoService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class NotificacaoLegadoMigracaoRunner implements ApplicationRunner {

    private final NotificacaoService notificacaoService;

    public NotificacaoLegadoMigracaoRunner(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @Override
    public void run(ApplicationArguments args) {
        notificacaoService.migrarAvisosLegadosSeNecessario();
    }
}
