package dev.stephyu.conversation.adapter.outbound.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmResult {
    @Description("First name of the person. JSON key must be exactly 'firstname'.")
    @JsonProperty("firstname")
    String firstname;

    @Description("Last name of the person. JSON key must be exactly 'lastname'.")
    @JsonProperty("lastname")
    String lastname;

    @Description("Intent de l'utilisateur, par exemple : 'book_flight', 'check_weather', etc.")
    @JsonProperty("intent")
    String intent;

    @JsonProperty("address")
    Address address;

    @Override
    public String toString() {
        return "LlmResult{"
            + "firstname='" + firstname + '\''
            + ", lastname='" + lastname + '\''
            + ", intent='" + intent + '\''
            + ", address=" + address
            + '}';
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Description("an address")
    static class Address {
        @JsonProperty("street")
        String street;

        @JsonProperty("streetNumber")
        Integer streetNumber;

        @JsonProperty("city")
        String city;

        @Override
        public String toString() {
            return "Address{"
                + "street='" + street + '\''
                + ", streetNumber=" + streetNumber
                + ", city='" + city + '\''
                + '}';
        }
    }
}

