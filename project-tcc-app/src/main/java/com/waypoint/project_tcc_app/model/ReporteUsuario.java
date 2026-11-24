package com.waypoint.project_tcc_app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reporte_usuario",
        indexes = {
                @Index(name = "idx_reporte_usuario_parada_data", columnList = "usuario_id, parada_id, data_hora"),
                @Index(name = "idx_reporte_data_hora", columnList = "data_hora")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReporteUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "linha_id", nullable = false)
    private Linha linha;

    @ManyToOne
    @JoinColumn(name = "parada_id")
    private Parada parada;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_reporte", nullable = false, length = 20)
    private TipoReporte tipoReporte;

    @PastOrPresent
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "data_hora")
    private LocalDateTime dataHora = LocalDateTime.now();

    @Column(name = "blocos_confiabilidade_recompensados", nullable = false, columnDefinition = "integer default 0")
    private Integer blocosConfiabilidadeRecompensados = 0;
}
