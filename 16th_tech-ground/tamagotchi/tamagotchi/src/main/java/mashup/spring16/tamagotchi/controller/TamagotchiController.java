package mashup.spring16.tamagotchi.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TamagotchiController {

    @GetMapping("/tamagotchi")
    public String tamagotchi() {
        return "tamagotchi";
    }
}
