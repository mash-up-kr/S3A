package mashup.spring16.tamagotchi.controller;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import mashup.spring16.tamagotchi.domain.Member;
import mashup.spring16.tamagotchi.dto.LoginRequest;
import mashup.spring16.tamagotchi.dto.SignupRequest;
import mashup.spring16.tamagotchi.dto.TamagotchiCreateRequest;
import mashup.spring16.tamagotchi.service.MemberService;
import mashup.spring16.tamagotchi.service.TamagotchiService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private static final String SESSION_MEMBER_ID = "memberId";
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg");
    private static final String PET_IMAGE_PATH = "classpath:/static/images/pet/*";

    private final MemberService memberService;
    private final TamagotchiService tamagotchiService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        try {
            Member member = memberService.login(new LoginRequest(username, password));
            session.setAttribute(SESSION_MEMBER_ID, member.getId());
            return "redirect:/tamagotchi";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "login";
        }
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String username,
                         @RequestParam String password,
                         @RequestParam String passwordConfirm,
                         HttpSession session,
                         Model model) {
        try {
            Member member = memberService.signup(new SignupRequest(username, password, passwordConfirm));
            session.setAttribute(SESSION_MEMBER_ID, member.getId());
            return "redirect:/signup/select";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "signup";
        }
    }

    @GetMapping("/signup/select")
    public String select(Model model, HttpSession session) throws Exception {
        if (session.getAttribute(SESSION_MEMBER_ID) == null) {
            return "redirect:/signup";
        }
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

    @PostMapping("/signup/select")
    public String selectSubmit(@RequestParam String character,
                               @RequestParam String name,
                               HttpSession session) {
        Long memberId = (Long) session.getAttribute(SESSION_MEMBER_ID);
        if (memberId == null) {
            return "redirect:/signup";
        }
        tamagotchiService.create(memberId, new TamagotchiCreateRequest(character, name));
        session.removeAttribute(SESSION_MEMBER_ID);
        return "redirect:/login";
    }

    private boolean hasImageExtension(String name) {
        String lower = name.toLowerCase();
        return IMAGE_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }
}
