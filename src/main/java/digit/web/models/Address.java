package digit.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.Builder;

/**
 * Representation of a address. Indiavidual APIs may choose to extend from this
 * using allOf if more details needed to be added in their case.
 */
@Schema(description = "Representation of a address. Indiavidual APIs may choose to extend from this using allOf if more details needed to be added in their case. ")
@Validated
@jakarta.annotation.Generated(value = "org.egov.codegen.SpringBootCodegen", date = "2024-11-18T11:00:08.741990949+05:30[Asia/Kolkata]")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Address {
        @JsonProperty("tenantId")
        @NotNull

        private String tenantId = null;

        @JsonProperty("doorNo")

        private String doorNo = null;

        @JsonProperty("plotNo")

        private String plotNo = null;

        @JsonProperty("id")

        private String id = null;

        @JsonProperty("landmark")

        private String landmark = null;

        @JsonProperty("city")

        private String city = null;

        @JsonProperty("district")

        private String district = null;

        @JsonProperty("region")

        private String region = null;

        @JsonProperty("state")

        private String state = null;

        @JsonProperty("country")

        private String country = null;

        @JsonProperty("pincode")

        private String pincode = null;

        @JsonProperty("additionDetails")

        private String additionDetails = null;

        @JsonProperty("buildingName")

        @Size(min = 2, max = 64)
        private String buildingName = null;

        @JsonProperty("street")

        @Size(min = 2, max = 64)
        private String street = null;

        @JsonProperty("locality")
        @NotNull

        @Valid
        private Boundary locality = null;

        @JsonProperty("geoLocation")

        @Valid
        private GeoLocation geoLocation = null;

}
