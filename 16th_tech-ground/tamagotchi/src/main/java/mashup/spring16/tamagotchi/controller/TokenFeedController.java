package mashup.spring16.tamagotchi.controller;

import lombok.RequiredArgsConstructor;
import mashup.spring16.tamagotchi.dto.TokenFeedRequest;
import mashup.spring16.tamagotchi.service.TamagotchiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TokenFeedController {

    private final TamagotchiService tamagotchiService;

    @PostMapping("/tokens")
    public ResponseEntity<Void> feed(@RequestBody TokenFeedRequest request) {
        try {
            tamagotchiService.feed(request.apiToken(), request.inputTokens(), request.outputTokens());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}