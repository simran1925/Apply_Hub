package com.community.api.endpoint.serviceProvider;

import com.community.api.entity.ActionLog;
import com.community.api.entity.CustomerReferrer;
import com.community.api.entity.OrderRequest;
import com.community.api.entity.Privileges;
import com.community.api.entity.QualificationDetails;
import com.community.api.entity.ResizedImage;
import com.community.api.entity.ServiceProviderAcceptedOrders;
import com.community.api.entity.ServiceProviderAddress;
import com.community.api.entity.ServiceProviderInfra;
import com.community.api.entity.ServiceProviderLanguage;
import com.community.api.entity.ServiceProviderRank;
import com.community.api.entity.ServiceProviderTest;
import com.community.api.entity.ServiceProviderTestStatus;
import com.community.api.entity.Skill;
import com.community.api.utils.ServiceProviderDocument;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.micrometer.core.lang.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_provider")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Inheritance(strategy = InheritanceType.JOINED)
public class ServiceProviderEntity  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long service_provider_id;

    @Column
    private String type="PROFESSIONAL";

    private Integer totalScore=0;

    private String user_name;


    /*@Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "businessPhoto", columnDefinition="BLOB")
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "business_photo_id")
    @OneToOne(cascade = CascadeType.ALL)
    private Document business_photo;*/


    @Pattern(regexp = "^[a-zA-Z]+( [a-zA-Z]+)*$", message = "First name must contain only alphabets")
    private String first_name;
    @Pattern(regexp = "^[a-zA-Z]+( [a-zA-Z]+)*$", message = "Last name must contain only alphabets")
    private String last_name;
    //@TODO-countryCode to country_code for both customer and service provider
    private String country_code;

    @Pattern(regexp = "^[a-zA-Z]+( [a-zA-Z]+)*$", message = "Father's name must contain only alphabets")
    private String father_name;
    @Pattern(regexp = "^[a-zA-Z]+( [a-zA-Z]+)*$", message = "Mother's name must contain only alphabets")
    @Column(name = "mother_name")
    private String mother_name;

//    @Pattern(regexp = "^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[0-2])-(\\d{4})$", message = "Date of birth must be in the format DD-MM-YYYY")
    private String date_of_birth;

    @Pattern(regexp = "^[0-9]{12}$", message = "Aadhaar number must be a 12-digit numeric value")
    @Size(min = 12, max = 12, message = "Aadhaar number must be exactly 12 digits long")
    @Size(min = 12, max = 12)
    private String aadhaar_number;

    @Nullable
    @Size(min = 10, max = 10)
    @Pattern(regexp = "^[A-Z]{5}\\d{4}[A-Z]{1}$", message = "Invalid format. Use 5 uppercase letters, 4 digits, and 1 uppercase letter.")

    private String pan_number;

    @Column(name = "archived", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isArchived = false;
    @Size(min = 9, max = 13)
    public String mobileNumber;
    private String otp;
    @Nullable
    @Size(min = 9, max = 13)
    @Pattern(regexp="^[6789]\\d{9,13}$",message = "Mobile number should be 10 digits in length and should begin with either 6,7,8,9")
    public String secondary_mobile_number;
    private int role;
    @Size(min = 9, max = 13)
    @Pattern(regexp = "^\\d{9,13}$", message = "WhatsApp number should be between 9 and 13 digits in length")
    public String whatsapp_number;
    @Email(message = "invalid email format")
    /*@Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",message = "Please enter a valid email address.")*/
    public String primary_email;

    @Nullable
    @Email(message = "invalid email format")
    /*@Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Please enter a valid email address.")*/
    public String secondary_email;
    @JsonIgnore
    private String password;
    @Nullable
    private Boolean is_running_business_unit=false;

    @Nullable
    private String business_name;
    @Nullable
    private String business_location;
    @Nullable
    @Email
    private String business_email;
    @Nullable
    private Integer number_of_employees;

    @Nullable
    private Boolean isCFormAvailable;
    @Nullable
    private String registration_number;

    @Nullable
    private String partTimeOrFullTime="PART TIME";

    private Integer businessUnitInfraScore=0;
    private Integer workExperienceScore=0;
    private Integer qualificationScore=0;
    private Integer technicalExpertiseScore=0;
    private Integer staffScore=0;
    private Integer writtenTestScore;
    private Integer imageUploadScore;
    private Integer partTimeOrFullTimeScore=0;
    private Integer infraScore=0;
    @ManyToMany
    @JoinTable(
            name = "service_provider_skill", // The name of the join table
            joinColumns = @JoinColumn(name = "service_provider_id"), // Foreign key for ServiceProvider
            inverseJoinColumns = @JoinColumn(name = "skill_id")) // Foreign key for Skill
    private List<Skill> skills;

    private Boolean has_technical_knowledge;

    @Min(0)
    private Integer work_experience_in_months;

    private String highest_qualification;
    private String name_of_institute;
    private String year_of_passing;
    private String board_or_university;
    private String total_marks;
    private String marks_obtained;
    private String cgpa;
    private Double latitude,longitude;
    private int rank;
    private int signedUp=0;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "service_provider_id")
    private List<ServiceProviderAddress> spAddresses;

    @OneToOne(cascade = CascadeType.ALL,orphanRemoval = true)
    @JoinColumn(name = "status_id", referencedColumnName = "status_id")
    private ServiceProviderStatus status;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}) // Only persist/merge, no REMOVE
    @JoinColumn(name="test_status_id", referencedColumnName = "test_status_id")
    private ServiceProviderTestStatus testStatus;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}) // Only persist/merge, no REMOVE
    @JoinColumn(name="rank_id", referencedColumnName = "rank_id")
    private ServiceProviderRank ranking;

    @ManyToMany
    @JoinTable(
            name = "service_provider_privileges", // The name of the join table
            joinColumns = @JoinColumn(name = "service_provider_id"), // Foreign key for ServiceProvider
            inverseJoinColumns = @JoinColumn(name = "privilege_id")) // Foreign key for Privilege
    private List<Privileges> privileges;
    @ManyToMany
    @JoinTable(
            name = "service_provider_infra", // The name of the join table
            joinColumns = @JoinColumn(name = "service_provider_id"), // Foreign key for ServiceProvider
            inverseJoinColumns = @JoinColumn(name = "infra_id")) // Foreign key for Skill
    private List<ServiceProviderInfra> infra;
    @ManyToMany
    @JoinTable(
            name = "service_provider_languages", // The name of the join table
            joinColumns = @JoinColumn(name = "service_provider_id"), // Foreign key for ServiceProvider
            inverseJoinColumns = @JoinColumn(name = "language_id")) // Foreign key for Skill
    private List<ServiceProviderLanguage> languages;

    @JsonIgnore
    @OneToMany(mappedBy = "service_provider", cascade = CascadeType.ALL, orphanRemoval = true,fetch = FetchType.EAGER)
    private List<ServiceProviderTest> serviceProviderTests;

/* @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}) // Only persist/merge, no REMOVE
 @JoinColumn(name="test_status_id", referencedColumnName = "test_status_id")
 private ServiceProviderTestStatus testStatus;*/

/* @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}) // Only persist/merge, no REMOVE
 @JoinColumn(name="rank_id", referencedColumnName = "rank_id")
 private ServiceProviderRank ranking;*/

    @JsonIgnore
    @OneToMany(mappedBy = "serviceProvider", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResizedImage> resizedImages;

    @JsonIgnore
    private String token;
    @Column
    private Integer totalSkillTestPoints;

    @JsonBackReference("service-provider-referrals-back")
    @OneToMany(mappedBy = "serviceProvider", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerReferrer> myReferrals = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "serviceProviderEntity", cascade = CascadeType.ALL, orphanRemoval = true,fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<ServiceProviderDocument> documents;


    @Nullable
    @JsonManagedReference("qualificationDetailsList-service-provider")
    @OneToMany(mappedBy = "service_provider", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QualificationDetails> qualificationDetailsList = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "serviceProvider", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderRequest> orderRequests;

    @JsonIgnore
    @OneToMany(mappedBy = "serviceProvider", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceProviderAcceptedOrders> acceptedOrders;
    @Column(name="is_active",columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isActive;

    @Column(name="maximum_ticket_size")
    private Integer maximumTicketSize;

    @Column(name="maximum_binding_size")
    private Integer maximumBindingSize;

    @Column(name="ticket_completed")    // will keep track of number of ticket completed by the serviceProvider.
    private Long ticketCompleted=0L;

    @Column(name="ticket_pending")      // will keep track of number of ticket pending for the corresponding SP.
    private Integer ticketPending=0;

    @Column(name="ticket_assigned")
    private Integer ticketAssigned=0;

    @JsonIgnore
    @OneToMany(mappedBy = "serviceProvider", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActionLog> actionLogs;

}


