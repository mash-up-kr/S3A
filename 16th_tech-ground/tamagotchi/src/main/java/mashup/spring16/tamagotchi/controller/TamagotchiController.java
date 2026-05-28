package mashup.spring16.tamagotchi.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TamagotchiController {

    @GetMapping("/tamagotchi")
    public String tamagotchi(HttpSession session) {
        if (session.getAttribute("memberId") == null) {
            return "redirect:/login";
        }
        return "tamagotchi";
    }
}
