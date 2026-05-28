package mashup.spring16.tamagotchi.service;

import mashup.spring16.tamagotchi.domain.Member;
import mashup.spring16.tamagotchi.domain.Tamagotchi;
import mashup.spring16.tamagotchi.dto.TamagotchiCreateRequest;
import mashup.spring16.tamagotchi.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class TamagotchiServiceTest {

    @Autowired
    TamagotchiService tamagotchiService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    @Test
    void create_success() {
        Member member = memberRepository.save(new Member("user1", passwordEncoder.encode("pass")));

        Tamagotchi tamagotchi = tamagotchiService.create(
                member.getId(),
                new TamagotchiCreateRequest("토키", "내토키")
        );

        assertThat(tamagotchi.getName()).isEqualTo("내토키");
        assertThat(tamagotchi.getCharacter()).isEqualTo("토키");
        assertThat(tamagotchi.getMember().getId()).isEqualTo(member.getId());
    }

    @Test
    void create_invalid_member_throws() {
        assertThatThrownBy(() -> tamagotchiService.create(
                999L,
                new TamagotchiCreateRequest("토키", "내토키")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("회원을 찾을 수 없습니다.");
    }
}
