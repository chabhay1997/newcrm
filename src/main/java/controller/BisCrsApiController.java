package controller;

import dto.BisCrsCreateRequest;
import dto.BisCrsCreateResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.BisCrsRenewalCreationService;
import service.OperationAccessService;

import java.net.URI;

@RestController
@RequestMapping("/api/operation/bis-crs")
public class BisCrsApiController {
    private final OperationAccessService access;
    private final BisCrsRenewalCreationService creation;

    public BisCrsApiController(OperationAccessService access, BisCrsRenewalCreationService creation) {
        this.access = access;
        this.creation = creation;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BisCrsCreateResponse> create(@RequestBody BisCrsCreateRequest request,
                                                       Authentication authentication) {
        BisCrsCreateResponse response = creation.create(access.require(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/operation/bis-crs/" + response.id()))
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}
