package com.aniglow.repository;

import com.aniglow.entity.AgentCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentCharacterRepository extends JpaRepository<AgentCharacter, Long> {

    Optional<AgentCharacter> findByCodeAndEnabledTrue(String code);

    Optional<AgentCharacter> findByCode(String code);

    List<AgentCharacter> findByEnabledTrueOrderBySortOrderAscIdAsc();

    boolean existsByCode(String code);

    long countByEnabledTrue();
}
