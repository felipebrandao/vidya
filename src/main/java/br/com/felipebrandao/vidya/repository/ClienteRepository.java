package br.com.felipebrandao.vidya.repository;

import br.com.felipebrandao.vidya.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByCgcCpf(String cgcCpf);

    boolean existsByCgcCpf(String cgcCpf);

    List<Cliente> findAllByOrderByNomeAsc();
}

