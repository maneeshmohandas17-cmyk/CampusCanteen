package com.canteen.campuscanteen.repository;

import com.canteen.campuscanteen.model.Canteen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CanteenRepository extends JpaRepository<Canteen, Long> {

    Optional<Canteen> findByNameIgnoreCase(String name);

    Optional<Canteen> findByNameIgnoreCaseAndActiveTrue(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndActiveTrue(String name);

    List<Canteen> findAllByOrderByIdAsc();

    List<Canteen> findByActiveTrueOrderByIdAsc();
}
