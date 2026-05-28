package mashup.spring16.tamagotchi.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import mashup.spring16.tamagotchi.domain.Tamagotchi;
import mashup.spring16.tamagotchi.repository.TamagotchiRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class TamagotchiController {

    private final TamagotchiRepository tamagotchiRepository;

    @GetMapping("/tamagotchi")
    public String tamagotchi(HttpSession session, Model model) {
        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) {
            return "redirect:/login";
        }

        Optional<Tamagotchi> tamagotchi = tamagotchiRepository.findByMemberId(memberId);
        if (tamagotchi.isEmpty()) {
            // 다마고치 없는 경우 캐릭터 선택으로 이동 (session의 memberId는 유지)
            return "redirect:/signup/select";
        }

        model.addAttribute("character", tamagotchi.get().getCharacter());
        model.addAttribute("petName", tamagotchi.get().getName());
        return "tamagotchi";
    }
}
