package br.com.felipebrandao.vidya.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReceitaWSResponse(

        String status,
        String cnpj,
        String nome,
        String fantasia,
        String abertura,
        String situacao,
        String tipo,
        String porte,
        String logradouro,
        String numero,
        String complemento,
        String cep,
        String bairro,
        String municipio,
        String uf,
        String email,
        String telefone,

        @JsonProperty("natureza_juridica")
        String naturezaJuridica,

        @JsonProperty("capital_social")
        String capitalSocial,

        @JsonProperty("atividade_principal")
        List<Atividade> atividadePrincipal,

        @JsonProperty("atividades_secundarias")
        List<Atividade> atividadesSecundarias,

        List<QSA> qsa
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Atividade(String code, String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QSA(
            String nome,
            String qual,

            @JsonProperty("pais_origem")
            String paisOrigem,

            @JsonProperty("nome_rep_legal")
            String nomeRepLegal,

            @JsonProperty("qual_rep_legal")
            String qualRepLegal
    ) {}
}

