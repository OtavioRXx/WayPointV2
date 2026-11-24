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

import java.time.LocalTime;

@Entity
@Table(
        name = "horario_parada",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_horario_parada",
                columnNames = {"parada_id", "horario_previsto"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HorarioParada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "parada_id", nullable = false)
    private Parada parada;

    @NotNull
    @Column(name = "horario_previsto", nullable = false)
    private LocalTime horarioPrevisto;
}
