package br.com.felipebrandao.vidya.repository;

import br.com.felipebrandao.vidya.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, Long> {
}

