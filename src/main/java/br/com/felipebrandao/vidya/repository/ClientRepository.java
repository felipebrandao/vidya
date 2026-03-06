package br.com.felipebrandao.vidya.repository;

import br.com.felipebrandao.vidya.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByCgcCpf(String cgcCpf);

    boolean existsByCgcCpf(String cgcCpf);

    Optional<Client> findByCodSankhya(Integer codSankhya);

    List<Client> findAllByOrderByNomeAsc();
}

