package digit.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import org.egov.common.contract.request.User;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.Builder;

/**
 * RequestHeader should be used to carry meta information about the requests to
 * the server as described in the fields below. All eGov APIs will use
 * requestHeader as a part of the request body to carry this meta information.
 * Some of this information will be returned back from the server as part of the
 * ResponseHeader in the response body to ensure correlation.
 */
@Schema(description = "RequestHeader should be used to carry meta information about the requests to the server as described in the fields below. All eGov APIs will use requestHeader as a part of the request body to carry this meta information. Some of this information will be returned back from the server as part of the ResponseHeader in the response body to ensure correlation.")
@Validated
@jakarta.annotation.Generated(value = "org.egov.codegen.SpringBootCodegen", date = "2024-11-18T11:00:08.741990949+05:30[Asia/Kolkata]")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestHeader {
        @JsonProperty("apiInfo")

        @Valid
        private APIInfo apiInfo = null;

        @JsonProperty("deviceDetail")

        @Valid
        private DeviceDetail deviceDetail = null;

        @JsonProperty("ts")
        @NotNull

        private Long ts = null;

        @JsonProperty("action")
        @NotNull

        @Size(max = 32)
        private String action = null;

        @JsonProperty("key")

        @Size(max = 256)
        private String key = null;

        @JsonProperty("msgId")
        @NotNull

        @Size(max = 256)
        private String msgId = null;

        @JsonProperty("requesterId")

        @Size(max = 256)
        private String requesterId = null;

        @JsonProperty("authToken")

        private String authToken = null;

        @JsonProperty("userInfo")

        @Valid
        private User userInfo = null;

        @JsonProperty("correlationId")

        private String correlationId = null;

        @JsonProperty("signature")

        private String signature = null;

}
