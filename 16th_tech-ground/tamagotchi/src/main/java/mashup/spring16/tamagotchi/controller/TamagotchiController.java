package mashup.spring16.tamagotchi.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import mashup.spring16.tamagotchi.repository.TamagotchiRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
        tamagotchiRepository.findByMemberId(memberId).ifPresent(t -> {
            model.addAttribute("character", t.getCharacter());
            model.addAttribute("petName", t.getName());
        });
        return "tamagotchi";
    }
}
