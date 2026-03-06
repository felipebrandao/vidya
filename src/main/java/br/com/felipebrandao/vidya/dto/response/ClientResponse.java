package br.com.felipebrandao.vidya.dto.response;

import br.com.felipebrandao.vidya.entity.PersonType;

import java.time.LocalDateTime;

public record ClientResponse(
        Long id,
        String nome,
        String cgcCpf,
        String nomeparc,
        String razaoSocial,
        Integer codcid,
        PersonType tipPessoa,
        String classificMs,
        Integer codSankhya,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

