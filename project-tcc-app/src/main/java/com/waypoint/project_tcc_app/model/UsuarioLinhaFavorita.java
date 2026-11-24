package com.waypoint.project_tcc_app.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario_linha_favorita")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioLinhaFavorita {

    @EmbeddedId
    private UsuarioLinhaFavoritaId id;

    @NotNull
    @ManyToOne
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @NotNull
    @ManyToOne
    @MapsId("linhaId")
    @JoinColumn(name = "linha_id", nullable = false)
    private Linha linha;
}
