package mashup.spring16.tamagotchi.service;

import mashup.spring16.tamagotchi.domain.Member;
import mashup.spring16.tamagotchi.dto.LoginRequest;
import mashup.spring16.tamagotchi.dto.SignupRequest;
import mashup.spring16.tamagotchi.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberServiceTest {

    @Autowired
    MemberService memberService;

    @Autowired
    MemberRepository memberRepository;

    @Test
    void signup_success() {
        Member member = memberService.signup(new SignupRequest("user1", "pass123", "pass123"));

        assertThat(member.getUsername()).isEqualTo("user1");
        assertThat(memberRepository.findByUsername("user1")).isPresent();
    }

    @Test
    void signup_duplicate_username_throws() {
        memberService.signup(new SignupRequest("user1", "pass123", "pass123"));

        assertThatThrownBy(() -> memberService.signup(new SignupRequest("user1", "other", "other")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 아이디입니다.");
    }

    @Test
    void signup_password_mismatch_throws() {
        assertThatThrownBy(() -> memberService.signup(new SignupRequest("user1", "pass123", "wrong")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }

    @Test
    void login_success() {
        memberService.signup(new SignupRequest("user1", "pass123", "pass123"));

        Member member = memberService.login(new LoginRequest("user1", "pass123"));

        assertThat(member.getUsername()).isEqualTo("user1");
    }

    @Test
    void login_wrong_username_throws() {
        assertThatThrownBy(() -> memberService.login(new LoginRequest("nobody", "pass123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    @Test
    void login_wrong_password_throws() {
        memberService.signup(new SignupRequest("user1", "pass123", "pass123"));

        assertThatThrownBy(() -> memberService.login(new LoginRequest("user1", "wrong")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("아이디 또는 비밀번호가 올바르지 않습니다.");
    }
}
