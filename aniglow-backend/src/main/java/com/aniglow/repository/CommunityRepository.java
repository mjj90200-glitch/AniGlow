package com.aniglow.repository;

import com.aniglow.entity.Community;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityRepository extends JpaRepository<Community, Long> {

    Optional<Community> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = "relatedAnime")
    List<Community> findAllByOrderByFeaturedDescHeatScoreDescPostCountDescCreatedAtAsc();
}
