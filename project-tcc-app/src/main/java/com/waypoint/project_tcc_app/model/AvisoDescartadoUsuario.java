package com.waypoint.project_tcc_app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "aviso_descartado_usuario",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_aviso_descartado_usuario",
                columnNames = {"usuario_id", "aviso_oficial_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvisoDescartadoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "aviso_oficial_id", nullable = false)
    private AvisoOficial avisoOficial;

    @NotNull
    @Column(name = "data_descarte", nullable = false)
    private LocalDateTime dataDescarte = LocalDateTime.now();
}
