package mashup.spring16.tamagotchi.repository;

import mashup.spring16.tamagotchi.domain.Tamagotchi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TamagotchiRepository extends JpaRepository<Tamagotchi, Long> {
    Optional<Tamagotchi> findByMemberId(Long memberId);
}
