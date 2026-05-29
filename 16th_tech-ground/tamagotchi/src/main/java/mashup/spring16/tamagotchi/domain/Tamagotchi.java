package mashup.spring16.tamagotchi.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "tamagotchi")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
public class Tamagotchi {

    private static final long[] LEVEL_THRESHOLDS = {100, 500, 2000, 10000, 50000, 200000, 500000, 1000000, 5000000};

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    @OneToOne
    @JoinColumn(name = "member_id", unique = true, nullable = false)
    private Member member;

    @NonNull
    @Column(name = "pet_character", nullable = false)
    private String character;

    @NonNull
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int level = 1;

    @Column(nullable = false)
    private int hunger = 100;

    @Column(nullable = false)
    private long experience = 0;

    @Column
    private LocalDateTime lastActiveAt;

    public void feed(long inputTokens, long outputTokens) {
        if (lastActiveAt != null) {
            long hours = ChronoUnit.HOURS.between(lastActiveAt, LocalDateTime.now());
            this.hunger = (int) Math.max(0, hunger - hours * 10L);
        }
        int hungerGain = (int) (inputTokens / 2000 + outputTokens / 200);
        this.hunger = Math.min(100, this.hunger + hungerGain);
        this.experience += inputTokens / 1000 + outputTokens / 100;
        this.lastActiveAt = LocalDateTime.now();
        checkLevelUp();
    }

    // 조회 시 DB에 저장하지 않고 현재 허기를 계산
    public int computeCurrentHunger() {
        if (lastActiveAt == null) return hunger;
        long hours = ChronoUnit.HOURS.between(lastActiveAt, LocalDateTime.now());
        return (int) Math.max(0, hunger - hours * 10L);
    }

    public long expForNextLevel() {
        if (level >= 10) return experience;
        return LEVEL_THRESHOLDS[level - 1];
    }

    private void checkLevelUp() {
        while (level < 10 && experience >= LEVEL_THRESHOLDS[level - 1]) {
            level++;
        }
    }
}
