package controller;

import dto.BisIsiAssistantRequest;
import dto.BisIsiAssistantResponse;
import model.User;
import service.BisIsiAssistantService;
import service.OperationAccessService;
import service.BisIsiAssistantRateLimiter;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operation/bis-isi")
public class BisIsiAssistantController {

    private final BisIsiAssistantService assistant;
    private final OperationAccessService access;
    private final BisIsiAssistantRateLimiter rateLimiter;    

    public BisIsiAssistantController(
            BisIsiAssistantService assistant,
            OperationAccessService access,
            BisIsiAssistantRateLimiter rateLimiter
    ) {
        this.assistant = assistant;
        this.access = access;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/assistant")
    public BisIsiAssistantResponse ask(
            Authentication authentication,
            @RequestBody BisIsiAssistantRequest request
    ) { 
        User currentUser = access.requireAdmin(authentication);

        try (
            BisIsiAssistantRateLimiter.Permit ignored = rateLimiter.acquire(currentUser.getId())
        ){
        
        String answer = assistant.answer(
                currentUser,
                request
        );

        return new BisIsiAssistantResponse(answer);
    }
}
}