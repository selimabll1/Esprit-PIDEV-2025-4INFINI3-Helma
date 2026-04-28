package com.helma.helmabackend.dto.crowdfunding;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ApplicationRaiseContactStepRequest {

    public boolean useProfileContact = true;

    @NotBlank(message = "contactFirstName is required")
    @Size(min = 2, max = 80, message = "contactFirstName must be between 2 and 80 characters")
    public String contactFirstName;

    @NotBlank(message = "contactLastName is required")
    @Size(min = 2, max = 80, message = "contactLastName must be between 2 and 80 characters")
    public String contactLastName;

    @Size(max = 120, message = "contactTitle must be at most 120 characters")
    public String contactTitle;

    @NotBlank(message = "contactEmail is required")
    @Email(message = "contactEmail must be a valid email")
    @Size(max = 180, message = "contactEmail must be at most 180 characters")
    public String contactEmail;

    @Size(max = 30, message = "contactPhone must be at most 30 characters")
    public String contactPhone;
}