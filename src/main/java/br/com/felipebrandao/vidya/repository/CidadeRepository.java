package br.com.felipebrandao.vidya.repository;

import br.com.felipebrandao.vidya.entity.Cidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CidadeRepository extends JpaRepository<Cidade, Long> {

    Optional<Cidade> findByCodcid(Integer codcid);

    boolean existsByCodcid(Integer codcid);
}

