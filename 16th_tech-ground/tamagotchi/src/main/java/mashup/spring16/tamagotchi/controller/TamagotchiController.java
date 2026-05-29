package mashup.spring16.tamagotchi.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import mashup.spring16.tamagotchi.domain.Member;
import mashup.spring16.tamagotchi.domain.Tamagotchi;
import mashup.spring16.tamagotchi.repository.MemberRepository;
import mashup.spring16.tamagotchi.repository.TamagotchiRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class TamagotchiController {

    private final TamagotchiRepository tamagotchiRepository;
    private final MemberRepository memberRepository;

    @GetMapping("/tamagotchi")
    public String tamagotchi(HttpSession session, Model model) {
        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) {
            return "redirect:/login";
        }

        Optional<Tamagotchi> tamagotchi = tamagotchiRepository.findByMemberId(memberId);
        if (tamagotchi.isEmpty()) {
            return "redirect:/signup/select";
        }

        Tamagotchi pet = tamagotchi.get();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("회원을 찾을 수 없습니다."));

        model.addAttribute("character", pet.getCharacter());
        model.addAttribute("petName", pet.getName());
        model.addAttribute("level", pet.getLevel());
        model.addAttribute("hunger", pet.computeCurrentHunger());
        model.addAttribute("experience", pet.getExperience());
        model.addAttribute("expForNextLevel", pet.expForNextLevel());
        model.addAttribute("apiToken", member.getApiToken());
        return "tamagotchi";
    }
}
