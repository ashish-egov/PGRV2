package digit.web.controllers;

import digit.service.PgrService;
import digit.web.models.CountResponse;
import digit.web.models.SearchRequest;
import digit.web.models.ServiceRequest;
import digit.web.models.ServiceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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
        // Create the service and get the response
        ServiceResponse response = pgrService.create(requestBody);
        // Return the response
        return ResponseEntity.ok(response);
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
    public ResponseEntity<ServiceResponse> requestsSearchPost(@Valid @RequestBody SearchRequest searchRequest) {
        // Search for service requests
        ServiceResponse response = pgrService.search(searchRequest);
        // Return the search response
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Updates an existing service request and returns the response.
     *
     * @param request The service request details provided in the request body.
     * @return The updated service response wrapped in a ResponseEntity.
     * @throws IOException If an I/O error occurs during processing.
     */
    @RequestMapping(value = "/_update", method = RequestMethod.POST)
    public ResponseEntity<ServiceResponse> requestsUpdatePost(@Valid @RequestBody ServiceRequest request)
            throws IOException {
        // Update the service and get the response
        ServiceResponse response = pgrService.update(request);
        // Return the response
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Retrieves the count of service requests matching the provided search
     * criteria.
     *
     * @param searchRequest The search criteria to be used to fetch the count.
     * @return The count of matching service requests wrapped in a ResponseEntity.
     * @throws IOException If an I/O error occurs during processing.
     */
    @RequestMapping(value = "/_count", method = RequestMethod.POST)
    public ResponseEntity<CountResponse> requestsCountPost(@Valid @RequestBody SearchRequest searchRequest)
            throws IOException {
        // Update the service and get the response
        CountResponse response = pgrService.count(searchRequest);
        // Return the response
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}