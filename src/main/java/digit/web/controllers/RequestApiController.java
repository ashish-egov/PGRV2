package digit.web.controllers;

import digit.service.PgrService;
import digit.web.models.RequestSearchCriteria;
import digit.web.models.ServiceRequest;
import digit.web.models.ServiceResponse;

import org.egov.common.contract.models.RequestInfoWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import java.io.IOException;
import jakarta.validation.Valid;

@jakarta.annotation.Generated(value = "org.egov.codegen.SpringBootCodegen", date = "2024-11-18T11:00:08.741990949+05:30[Asia/Kolkata]")
@Controller
@RequestMapping("/v2")
public class RequestApiController {

    @Autowired
    private PgrService pgrService;

    /**
     * Creates a new service request and returns the response.
     *
     * @param requestBody The service request details provided in the request body.
     * @return The created service response wrapped in a ResponseEntity.
     * @throws IOException If an I/O error occurs during processing.
     */
    @RequestMapping(value = "/_create", method = RequestMethod.POST)
    public ResponseEntity<ServiceResponse> requestsCreatePost(@Valid @RequestBody ServiceRequest requestBody)
            throws IOException {

        ServiceResponse response = pgrService.create(requestBody);
        return ResponseEntity.ok(response); // Return the created response
    }

    /**
     * Searches for service requests based on provided criteria.
     *
     * @param requestInfoWrapper The request information wrapper containing request
     *                           details.
     * @param criteria           The search criteria for the request.
     * @return The service response wrapped in a ResponseEntity (currently not
     *         implemented).
     */
    @RequestMapping(value = "/_search", method = RequestMethod.POST)
    public ResponseEntity<ServiceResponse> requestsSearchPost(@Valid @RequestBody RequestInfoWrapper requestInfoWrapper,
            @Valid @ModelAttribute RequestSearchCriteria criteria) {

        ServiceResponse response = pgrService.search(requestInfoWrapper.getRequestInfo(), criteria);
        return new ResponseEntity<ServiceResponse>(HttpStatus.NOT_IMPLEMENTED); // Return not implemented status
    }

}