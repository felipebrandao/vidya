package br.com.felipebrandao.vidya.dto.response;

import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String nome,
        String cgcCpf,
        String nomeparc,
        String razaoSocial,
        Integer codcid,
        String tipPessoa,
        String classificMs,
        Integer codSankhya,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

