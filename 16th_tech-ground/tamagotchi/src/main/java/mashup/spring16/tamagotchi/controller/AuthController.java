package mashup.spring16.tamagotchi.controller;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg");
    private static final String PET_IMAGE_PATH = "classpath:/static/images/pet/*";

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String submit(@RequestParam String username, @RequestParam String password) {
        return "redirect:/tamagotchi";
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signupSubmit(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam("passwordConfirm") String passwordConfirm) {
        return "redirect:/signup/select";
    }

    @GetMapping("/signup/select")
    public String select(Model model) throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(PET_IMAGE_PATH);

        List<String> images = Arrays.stream(resources)
            .map(Resource::getFilename)
            .filter(name -> name != null && hasImageExtension(name))
            .sorted(Comparator.naturalOrder())
            .toList();

        model.addAttribute("images", images);
        return "select";
    }

    private boolean hasImageExtension(String name) {
        String lower = name.toLowerCase();
        return IMAGE_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }
}
