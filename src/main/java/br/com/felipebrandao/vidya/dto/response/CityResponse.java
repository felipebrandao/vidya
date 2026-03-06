package br.com.felipebrandao.vidya.dto.response;

public record CityResponse(
        Long id,
        Integer codcid,
        String nome,
        String uf
) {}

