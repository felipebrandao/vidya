package br.com.felipebrandao.vidya.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Descrição / Nome do campo na API Sankhya */
    @Column(nullable = false, length = 255)
    private String nome;

    /** CNPJ ou CPF (CGC_CPF) */
    @Column(name = "cgc_cpf", nullable = false, length = 20)
    private String cgcCpf;

    /** Nome do Parceiro (NOMEPARC) */
    @Column(nullable = false, length = 255)
    private String nomeparc;

    /** Razão Social (RAZAOSOCIAL) */
    @Column(name = "razao_social", nullable = false, length = 255)
    private String razaoSocial;

    /** Código da cidade no Sankhya (CODCID) */
    @Column(nullable = false)
    private Integer codcid;

    /**
     * Tipo de Pessoa (TIPPESSOA): 'F' = Física, 'J' = Jurídica
     */
    @Column(name = "tip_pessoa", nullable = false, length = 1)
    private String tipPessoa;

    /** Classificação de ICMS (CLASSIFICMS) */
    @Column(name = "classific_ms", nullable = false, length = 10)
    private String classificMs;

    /** Código retornado pelo ERP Sankhya após criação do parceiro */
    @Column(name = "cod_sankhya")
    private Integer codSankhya;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onPrePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

