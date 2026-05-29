package mashup.spring16.tamagotchi.service;

import lombok.RequiredArgsConstructor;
import mashup.spring16.tamagotchi.domain.Member;
import mashup.spring16.tamagotchi.domain.Tamagotchi;
import mashup.spring16.tamagotchi.dto.TamagotchiCreateRequest;
import mashup.spring16.tamagotchi.repository.MemberRepository;
import mashup.spring16.tamagotchi.repository.TamagotchiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TamagotchiService {

    private final TamagotchiRepository tamagotchiRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Tamagotchi create(Long memberId, TamagotchiCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        return tamagotchiRepository.save(new Tamagotchi(member, request.character(), request.name()));
    }

    @Transactional
    public void feed(String apiToken, long inputTokens, long outputTokens) {
        Member member = memberRepository.findByApiToken(apiToken)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 API 토큰입니다."));
        Tamagotchi tamagotchi = tamagotchiRepository.findByMemberId(member.getId())
                .orElseThrow(() -> new IllegalArgumentException("다마고치를 찾을 수 없습니다."));
        tamagotchi.feed(inputTokens, outputTokens);
    }
}
