package br.com.felipebrandao.vidya.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CityBatchRepository {

    private final JdbcTemplate jdbcTemplate;

    public void batchUpsert(List<Object[]> rows) {
        if (rows.isEmpty()) return;

        String sql = """
                INSERT INTO cidades (codcid, nome, uf, updated_at)
                VALUES (?, ?, ?, NOW())
                ON CONFLICT (codcid) DO UPDATE
                    SET nome       = EXCLUDED.nome,
                        uf         = EXCLUDED.uf,
                        updated_at = NOW()
                """;

        jdbcTemplate.batchUpdate(sql, rows);
    }
}

