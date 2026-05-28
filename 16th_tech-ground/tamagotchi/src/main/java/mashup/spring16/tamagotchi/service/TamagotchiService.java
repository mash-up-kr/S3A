package mashup.spring16.tamagotchi.service;

import mashup.spring16.tamagotchi.domain.Member;
import mashup.spring16.tamagotchi.domain.Tamagotchi;
import mashup.spring16.tamagotchi.dto.TamagotchiCreateRequest;
import mashup.spring16.tamagotchi.repository.MemberRepository;
import mashup.spring16.tamagotchi.repository.TamagotchiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TamagotchiService {

    private final TamagotchiRepository tamagotchiRepository;
    private final MemberRepository memberRepository;

    public TamagotchiService(TamagotchiRepository tamagotchiRepository, MemberRepository memberRepository) {
        this.tamagotchiRepository = tamagotchiRepository;
        this.memberRepository = memberRepository;
    }

    public Tamagotchi create(Long memberId, TamagotchiCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        return tamagotchiRepository.save(new Tamagotchi(member, request.character(), request.name()));
    }
}
