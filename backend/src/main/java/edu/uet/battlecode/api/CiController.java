package edu.uet.battlecode.api;

import edu.uet.battlecode.db.CiService;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/webhooks")
public class CiController {
    private final CiService ci;
    public CiController(CiService ci) { this.ci = ci; }

    public record UploadRequest(UUID submissionId, String sha256) {}
    public record ResultRequest(UUID submissionId, boolean passed, String sha256,
                                String validationLogUrl, String compileLogUrl, String errorSummary) {}

    @PostMapping("/ci-upload-url")
    public Map<String, String> upload(@RequestHeader(value = "X-CI-Token", required = false) String token,
                                      @RequestBody UploadRequest input) {
        try {
            return Map.of("put_url", ci.prepareUpload(input.submissionId(), token, input.sha256()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid CI upload request");
        }
    }

    @PostMapping("/ci-result")
    public Map<String, Boolean> result(@RequestHeader(value = "X-CI-Token", required = false) String token,
                                       @RequestBody ResultRequest input) {
        boolean accepted = ci.complete(input.submissionId(), token, input.passed(), input.sha256(),
                input.validationLogUrl(), input.compileLogUrl(), input.errorSummary());
        if (!accepted) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid CI callback");
        return Map.of("accepted", true);
    }
}
