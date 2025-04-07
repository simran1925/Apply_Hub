package com.community.api.endpoint.avisoft.controller.Account;

import com.community.api.component.Constant;
import com.community.api.component.JwtUtil;
import com.community.api.endpoint.avisoft.controller.otpmodule.OtpEndpoint;
import com.community.api.entity.CustomAdmin;
import com.community.api.entity.CustomCustomer;

import com.community.api.entity.Role;
import com.community.api.services.*;
import com.community.api.services.Admin.AdminService;
import com.community.api.services.ServiceProvider.ServiceProviderServiceImpl;
import com.community.api.services.exception.ExceptionHandlingImplement;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isNumeric;

@RestController
@RequestMapping(value = "/account",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
)
public class AccountEndPoint {
    private CustomerService customerService;
    private JwtUtil jwtUtil;
    private ExceptionHandlingImplement exceptionHandling;

    private EntityManager em;
    private TwilioService twilioService;
    private CustomCustomerService customCustomerService;
    private PasswordEncoder passwordEncoder;
    private AdminService adminService;

    @Autowired
    private SharedUtilityService sharedUtilityService;
    @Autowired
    private SanitizerService sanitizerService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private ServiceProviderServiceImpl serviceProviderService;


    @Autowired
    public void setCustomerService(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Autowired
    public void setJwtUtil(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Autowired
    public void setExceptionHandling(ExceptionHandlingImplement exceptionHandling) {
        this.exceptionHandling = exceptionHandling;
    }

    @Autowired
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Autowired
    public void setTwilioService(TwilioService twilioService) {
        this.twilioService = twilioService;
    }

    @Autowired
    private ResponseService responseService;

    @Autowired
    public void setCustomCustomerService(CustomCustomerService customCustomerService) {
        this.customCustomerService = customCustomerService;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/login-with-otp")
    @ResponseBody
    public ResponseEntity<?> verifyAndLogin(@RequestBody Map<String, Object> loginDetails, HttpSession session) {
        try {
            if (!sharedUtilityService.validateInputMap(loginDetails).equals(SharedUtilityService.ValidationResult.SUCCESS)) {
                return ResponseService.generateErrorResponse("Invalid Request Body", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            String roleName = roleService.findRoleName((Integer) loginDetails.get("role"));
            if (roleName.equals("EMPTY"))
                return ResponseService.generateErrorResponse("Role not found", HttpStatus.NOT_FOUND);

            String mobileNumber = (String) loginDetails.get("mobileNumber");

            if (mobileNumber != null) {

                int i = 0;
                for (; i < mobileNumber.length(); i++) {
                    if (mobileNumber.charAt(i) != '0')
                        break;
                }
                mobileNumber = mobileNumber.substring(i);
                loginDetails.put("mobileNumber", mobileNumber);
                if (customCustomerService.isValidMobileNumber(mobileNumber) && isNumeric(mobileNumber)) {
                    return loginWithPhoneOtp(loginDetails, session);
                } else {
                    return responseService.generateErrorResponse(ApiConstants.INVALID_MOBILE_NUMBER, HttpStatus.BAD_REQUEST);
                }
            } else {
                return loginWithUsernameOtp(loginDetails, session);
            }
        }catch (PersistenceException persistenceException)
        {
            return ResponseService.generateErrorResponse("Error logging in :"+persistenceException.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
        }
        catch (Exception e) {
            exceptionHandling.handleException(e);
            return responseService.generateErrorResponse(ApiConstants.SOME_EXCEPTION_OCCURRED + e.getMessage(), HttpStatus.BAD_REQUEST);

        }
    }

    @PostMapping("/admin-login-with-otp")
    @ResponseBody
    public ResponseEntity<?> verifyAndLoginAdmin(@RequestBody Map<String, Object> loginDetails, HttpSession session) {
        try {
            if (!sharedUtilityService.validateInputMap(loginDetails).equals(SharedUtilityService.ValidationResult.SUCCESS)) {
                return ResponseService.generateErrorResponse("Invalid Request Body", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            Integer roleId = null;

            if (loginDetails.containsKey("role")) {
                Object roleValue = loginDetails.get("role");
                if (roleValue instanceof Integer) {
                    roleId = (Integer) roleValue;
                }
            }
            if (roleId == null) {
                throw new IllegalArgumentException("Role information is missing or invalid.");
            }
            String roleName = roleService.findRoleName(roleId);
            if (roleName.equals("EMPTY"))
                return ResponseService.generateErrorResponse("Role not found", HttpStatus.NOT_FOUND);
            //validating input map

            loginDetails = sanitizerService.sanitizeInputMap(loginDetails);//@TODO-Need to sanitize this too
            String mobile_number = (String) loginDetails.get("mobileNumber");
            String username = (String) loginDetails.get("username");
            if (mobile_number == null && username == null)
            {
                throw new IllegalArgumentException("Either mobile number or username is required");
            }

            if (mobile_number != null) {

                int i = 0;
                for (; i < mobile_number.length(); i++) {
                    if (mobile_number.charAt(i) != '0')
                        break;
                }

                mobile_number = mobile_number.substring(i);
                loginDetails.put("mobileNumber", mobile_number);
                if (customCustomerService.isValidMobileNumber(mobile_number) && isNumeric(mobile_number)) {
                    return loginWithPhoneOtp(loginDetails, session);

                } else {
                    return responseService.generateErrorResponse(ApiConstants.INVALID_MOBILE_NUMBER, HttpStatus.BAD_REQUEST);
                }
            } else {
                return loginWithUsernameOtp(loginDetails, session);
            }
        } catch (Exception e) {
            exceptionHandling.handleException(e);
            return responseService.generateErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);

        }
    }

    @Transactional

    @PostMapping("/login-with-password")
    @ResponseBody
    public ResponseEntity<?> loginWithPassword(@RequestBody Map<String, Object> loginDetails,@RequestParam(value = "tempAuth",required = false,defaultValue = "false")Boolean tempAuth, HttpSession session,@RequestHeader(value = "Authorization",required = false)String authHeadReq,HttpServletRequest request) {
        try {
            String roleName = roleService.findRoleName((Integer) loginDetails.get("role"));
            if (roleName.equals("EMPTY"))
                return ResponseService.generateErrorResponse("Role not found", HttpStatus.NOT_FOUND);

            String mobileNumber = (String) loginDetails.get("mobileNumber");
            String username = (String) loginDetails.get("username");
            if (mobileNumber != null) {
                if (mobileNumber.startsWith("0"))
                    mobileNumber = mobileNumber.substring(1);
                if (customCustomerService.isValidMobileNumber(mobileNumber) && isNumeric(mobileNumber)) {
                    return loginWithCustomerPassword(loginDetails,tempAuth,authHeadReq,session,request);
                } else {
                    return responseService.generateErrorResponse(ApiConstants.INVALID_MOBILE_NUMBER, HttpStatus.BAD_REQUEST);
                }
            } else if (username != null) {
                return loginWithUsername(loginDetails, session,tempAuth,request,authHeadReq);
            } else {
                return responseService.generateErrorResponse(ApiConstants.INVALID_DATA, HttpStatus.INTERNAL_SERVER_ERROR);

            }
        } catch (Exception e) {
            exceptionHandling.handleException(e);
            return responseService.generateErrorResponse(ApiConstants.SOME_EXCEPTION_OCCURRED + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

    @RequestMapping(value = "phone-otp", method = RequestMethod.POST)
    private ResponseEntity<?> loginWithPhoneOtp(Map<String, Object> loginDetails, HttpSession session) throws UnsupportedEncodingException, UnsupportedEncodingException {
        try {
            String roleToCheck=null;
            if (loginDetails == null) {
                return responseService.generateErrorResponse(ApiConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);

            }

            String mobileNumber = (String) loginDetails.get("mobileNumber");
            String countryCode = (String) loginDetails.get("countryCode");
            Integer role = (Integer) loginDetails.get("role");
            if (mobileNumber == null) {
                return responseService.generateErrorResponse(ApiConstants.INVALID_MOBILE_NUMBER, HttpStatus.BAD_REQUEST);

            } else if (role == null) {
                return responseService.generateErrorResponse(ApiConstants.ROLE_EMPTY, HttpStatus.BAD_REQUEST);

            }
            if (countryCode == null || countryCode.isEmpty()) {
                countryCode = Constant.COUNTRY_CODE;
            }
            String updated_mobile = mobileNumber;
            if (mobileNumber.startsWith("0")) {
                updated_mobile = mobileNumber.substring(1);
            }
            if (roleService.findRoleName(role).equals(Constant.roleUser)) {
                CustomCustomer customerRecords = customCustomerService.findCustomCustomerByPhone(mobileNumber, countryCode);
                if (customerRecords == null) {
                    return responseService.generateErrorResponse(ApiConstants.NO_RECORDS_FOUND, HttpStatus.NOT_FOUND);

                }
                if (customerService == null) {
                    return responseService.generateErrorResponse("Customer service is not initialized.", HttpStatus.INTERNAL_SERVER_ERROR);

                }
                Customer customer = customerService.readCustomerById(customerRecords.getId());
                if (customer != null) {

                    ResponseEntity<Map<String, Object>> otpResponse = twilioService.sendOtpToMobile(updated_mobile, countryCode,null);

                    Map<String, Object> responseBody = otpResponse.getBody();


                    if (responseBody.get("otp") != null) {
                        return responseService.generateSuccessResponse((String) responseBody.get("message"), (String) responseBody.get("otp"), HttpStatus.OK);
                    } else {
                        return responseService.generateErrorResponse((String) responseBody.get("message"), HttpStatus.BAD_REQUEST);
                    }
                } else {
                    return responseService.generateErrorResponse(ApiConstants.NO_RECORDS_FOUND, HttpStatus.NOT_FOUND);
                }
            } else if (roleService.findRoleName(role).equals(Constant.roleServiceProvider)) {
                if (serviceProviderService.findServiceProviderByPhone(mobileNumber, countryCode) != null) {
                    if (serviceProviderService.findServiceProviderByPhone(mobileNumber, countryCode).getOtp() != null) {
                        responseService.generateErrorResponse(ApiConstants.NO_RECORDS_FOUND, HttpStatus.NOT_FOUND);

                    }
                    return serviceProviderService.sendOtp(mobileNumber, countryCode, session);

                } else {
                    return responseService.generateErrorResponse(ApiConstants.NO_RECORDS_FOUND, HttpStatus.NOT_FOUND);
                }

            }

            else if (roleService.findRoleName(role).equals(Constant.ADMIN) ) {
                CustomAdmin customAdmin = adminService.findAdminByPhone(mobileNumber,countryCode);
                if (customAdmin != null) {
                    roleToCheck=roleService.findRoleName(customAdmin.getRole());
                    if (roleToCheck.equals(Constant.ADMIN)) {
                        if (adminService.findAdminByPhone(mobileNumber, countryCode).getOtp() != null) {
                            responseService.generateErrorResponse(ApiConstants.NO_RECORDS_FOUND, HttpStatus.NOT_FOUND);
                        }
                        return adminService.sendOtpForAdmin(mobileNumber, countryCode, session);
                    }
                    else{
                        return responseService.generateErrorResponse("Custom Admin with mobileNumber " + mobileNumber + " does not have "+ roleService.findRoleName(role)+" role", HttpStatus.BAD_REQUEST);
                    }
                }
                else {
                    return responseService.generateErrorResponse(ApiConstants.NO_RECORDS_FOUND, HttpStatus.NOT_FOUND);
                }
            }
            else if (roleService.findRoleName(role).equals(Constant.SUPER_ADMIN) ) {
                CustomAdmin customAdmin = adminService.findAdminByPhone(mobileNumber,countryCode);
                if (customAdmin != null) {
                    roleToCheck=roleService.findRoleName(customAdmin.getRole());
                    if (roleToCheck.equals(Constant.SUPER_ADMIN)) {
                        if (adminService.findAdminByPhone(mobileNumber, countryCode).getOtp() != null) {
                            responseService.generateErrorResponse(ApiConstants.NO_RECORDS_FOUND, HttpStatus.NOT_FOUND);
                        }
                        return adminService.sendOtpForAdmin(mobileNumber, countryCode, session);
                    }
                    else{
                        return responseService.generateErrorResponse("Custom Admin with mobileNumber " + mobileNumber + " does not have "+ roleService.findRoleName(role)+" role", HttpStatus.BAD_REQUEST);
                    }
                }
                else {
                    return responseService.generateErrorResponse(ApiConstants.NO_RECORDS_FOUND, HttpStatus.NOT_FOUND);
                }
            }
            else if (roleService.findRoleName(role).equals(Constant.roleAdminServiceProvider) ) {
                CustomAdmin customAdmin = adminService.findAdminByPhone(mobileNumber,countryCode);
                if (customAdmin != null) {
                    roleToCheck=roleService.findRoleName(customAdmin.getRole());
                    if (roleToCheck.equals(Constant.roleAdminServiceProvider)) {
                        if (adminService.findAdminByPhone(mobileNumber, countryCode).getOtp() != null) {
                            responseService.generateErrorResponse(ApiConstants.NO_RECORDS_FOUND, HttpStatus.NOT_FOUND);
                        }
                        return adminService.sendOtpForAdmin(mobileNumber, countryCode, session);
                    }
                    else{
                        return responseService.generateErrorResponse("Custom Admin with mobileNumber " + mobileNumber + " does not have "+ roleService.findRoleName(role)+" role", HttpStatus.BAD_REQUEST);
                    }
                }
                else {
                    return responseService.generateErrorResponse(ApiConstants.NO_RECORDS_FOUND, HttpStatus.NOT_FOUND);
                }
            }
            else {
                responseService.generateErrorResponse(ApiConstants.ROLE_EMPTY, HttpStatus.BAD_REQUEST);
            }
            return responseService.generateErrorResponse("Role not specified", HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            exceptionHandling.handleException(e);
            return responseService.generateErrorResponse(ApiConstants.SOME_EXCEPTION_OCCURRED + e.getMessage(), HttpStatus.BAD_REQUEST);

        }
    }

    @Transactional
    @RequestMapping(value = "admin-login-with-password", method = RequestMethod.POST)
    public ResponseEntity<?> adminLoginWithPassword(@RequestBody Map<String, Object> loginDetails, HttpSession session, HttpServletRequest request)
    {
        try {
            String roleToCheck=null;
            if (loginDetails == null) {
                return responseService.generateErrorResponse(ApiConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }

            String username = (String) loginDetails.get("username");
            String mobileNumber = (String) loginDetails.get("mobileNumber");
            String password = (String) loginDetails.get("password");
            Integer role = (Integer) loginDetails.get("role");
            String countryCode = (String) loginDetails.getOrDefault("countryCode", Constant.COUNTRY_CODE);

            if(mobileNumber==null && username==null)
            {
                return ResponseService.generateErrorResponse("Either mobileNumber or username should be provided",HttpStatus.BAD_REQUEST );
            }
            if ( password == null || role == null ) {
                return responseService.generateErrorResponse("password, role cannot be empty", HttpStatus.BAD_REQUEST);
            }
            String usernameOrMobileNumber= null;
            String returnMessageToshow=null;
            if(username!=null)
            {
                usernameOrMobileNumber=username;
                returnMessageToshow="username";
            }
            else if(mobileNumber!=null)
            {
                usernameOrMobileNumber=mobileNumber;
                returnMessageToshow="mobile Number";
            }
            if(roleService.findRoleName(role).equals(Constant.ADMIN))
            {
                CustomAdmin customAdmin=null;
                if(username!=null)
                {
                     customAdmin=adminService.findAdminByUsername(username);
                }
                else if(mobileNumber!=null)
                {
                    customAdmin=adminService.findAdminByPhone(mobileNumber,countryCode);
                }
                if (customAdmin == null) {
                    return responseService.generateErrorResponse("Custom Admin with "+ usernameOrMobileNumber+" not found", HttpStatus.NOT_FOUND);
                }
                roleToCheck=roleService.findRoleName(customAdmin.getRole());
                if (roleToCheck.equals(Constant.ADMIN)) {
                    return adminService.loginWithPasswordForAdmin(loginDetails, request,session);
                }
                else{
                    return responseService.generateErrorResponse("Custom Admin with " +returnMessageToshow +" "+usernameOrMobileNumber+" does not have "+ roleService.findRoleName(role)+" role", HttpStatus.BAD_REQUEST);
                }
            }
            if(roleService.findRoleName(role).equals(Constant.SUPER_ADMIN))
            {
                CustomAdmin customAdmin=null;
                if(username!=null)
                {
                    customAdmin=adminService.findAdminByUsername(username);
                }
                else if(mobileNumber!=null)
                {
                    customAdmin=adminService.findAdminByPhone(mobileNumber,countryCode);
                }
                if (customAdmin == null) {
                    return responseService.generateErrorResponse("Custom Admin with "+ usernameOrMobileNumber+" not found", HttpStatus.NOT_FOUND);
                }
                roleToCheck=roleService.findRoleName(customAdmin.getRole());
                if (roleToCheck.equals(Constant.SUPER_ADMIN)) {
                    return adminService.loginWithPasswordForAdmin(loginDetails, request,session);
                }
                else{
                    return responseService.generateErrorResponse("Custom Admin with " +returnMessageToshow +" "+usernameOrMobileNumber+" does not have "+ roleService.findRoleName(role)+" role", HttpStatus.BAD_REQUEST);
                }
            } if(roleService.findRoleName(role).equals(Constant.roleAdminServiceProvider))
            {
                CustomAdmin customAdmin=null;
                if(username!=null)
                {
                    customAdmin=adminService.findAdminByUsername(username);
                }
                else if(mobileNumber!=null)
                {
                    customAdmin=adminService.findAdminByPhone(mobileNumber,countryCode);
                }
                if (customAdmin == null) {
                    return responseService.generateErrorResponse("Custom Admin with "+ usernameOrMobileNumber+" not found", HttpStatus.NOT_FOUND);
                }
                roleToCheck=roleService.findRoleName(customAdmin.getRole());
                if (roleToCheck.equals(Constant.roleAdminServiceProvider)) {
                    return adminService.loginWithPasswordForAdmin(loginDetails, request,session);
                }
                else{
                    return responseService.generateErrorResponse("Custom Admin with " +returnMessageToshow +" "+usernameOrMobileNumber+" does not have "+ roleService.findRoleName(role)+" role", HttpStatus.BAD_REQUEST);
                }
            }

            else {
                return responseService.generateErrorResponse(ApiConstants.INVALID_ROLE, HttpStatus.BAD_REQUEST);
            }
        }  catch (IllegalArgumentException e) {
            return ResponseService.generateErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            exceptionHandling.handleException(e);
            return responseService.generateErrorResponse(ApiConstants.SOME_EXCEPTION_OCCURRED + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    @RequestMapping(value = "login-with-username", method = RequestMethod.POST)
    public ResponseEntity<?> loginWithUsername(@RequestBody Map<String, Object> loginDetails, HttpSession session,@RequestParam(value = "tempAuth",required = false,defaultValue = "false")Boolean tempAuth, HttpServletRequest request,@RequestHeader(value = "Authorization",required = false)String authHeadReq) {
        try {
            if (loginDetails == null) {
                return responseService.generateErrorResponse(ApiConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
            }
            String authHeader=Constant.BEARER_CONST;;
            String username = (String) loginDetails.get("username");
            String password = (String) loginDetails.get("password");
            Integer role = (Integer) loginDetails.get("role");

            if (username == null || password == null || role == null) {
                return responseService.generateErrorResponse("username or password or role cannot be empty", HttpStatus.BAD_REQUEST);
            }
            if (roleService.findRoleName(role).equals(Constant.roleUser)) {
                if (customerService == null) {
                    return responseService.generateErrorResponse("Customer service is not initialized.", HttpStatus.INTERNAL_SERVER_ERROR);
                }

                Customer customer = customerService.readCustomerByUsername(username);
                if (customer == null) {
                    return responseService.generateErrorResponse(ApiConstants.NO_RECORDS_FOUND, HttpStatus.NOT_FOUND);
                }
                CustomCustomer customCustomer = em.find(CustomCustomer.class, customer.getId());
                if(customCustomer.getArchived())
                    return ResponseService.generateErrorResponse("Your account is supsended ,please contact support.",HttpStatus.UNAUTHORIZED);
                if (passwordEncoder.matches(password, customer.getPassword())) {

                    String tokenKey = "authToken_" + customCustomer.getMobileNumber();
                    String existingToken = customCustomer.getToken();
                    authHeader=authHeader+existingToken;
                    String ipAddress = request.getRemoteAddr();
                    String userAgent = request.getHeader("User-Agent");
                    if(tempAuth.equals(true))
                    {
                        String token = jwtUtil.generateToken(customer.getId(), role, ipAddress, userAgent);
                        authHeader = authHeader + authHeadReq;
                        session.setAttribute(tokenKey, token);
                        OtpEndpoint.ApiResponse response = new OtpEndpoint.ApiResponse(token, sharedUtilityService.breakReferenceForCustomer(customer,authHeader,request), HttpStatus.OK.value(), HttpStatus.OK.name(),"User has been signed in");
                        return ResponseEntity.ok(response);
                    }
                    if (existingToken != null && jwtUtil.validateToken(existingToken, ipAddress, userAgent)) {
                        OtpEndpoint.ApiResponse response = new OtpEndpoint.ApiResponse(existingToken, sharedUtilityService.breakReferenceForCustomer(customer,authHeader,request), HttpStatus.OK.value(), HttpStatus.OK.name(),"User has been signed in");
                        return ResponseEntity.ok(response);

                    } else {
                        String token = jwtUtil.generateToken(customer.getId(), role, ipAddress, userAgent);
                            customCustomer.setToken(token);
                            authHeader = authHeader + token;
                            em.persist(customCustomer);
                        session.setAttribute(tokenKey, token);
                        OtpEndpoint.ApiResponse response = new OtpEndpoint.ApiResponse(token, sharedUtilityService.breakReferenceForCustomer(customer,authHeader,request), HttpStatus.OK.value(), HttpStatus.OK.name(),"User has been signed in");
                        return ResponseEntity.ok(response);
                    }
                } else {
                    return responseService.generateErrorResponse("Invalid password", HttpStatus.BAD_REQUEST);

                }
            } else if (roleService.findRoleName(role).equals(Constant.roleServiceProvider)) {
                return serviceProviderService.loginWithPassword(loginDetails, request,session);
            }
            else {
                return responseService.generateErrorResponse(ApiConstants.INVALID_ROLE, HttpStatus.BAD_REQUEST);

            }
        }  catch (IllegalArgumentException e) {
            return ResponseService.generateErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            exceptionHandling.handleException(e);
            return responseService.generateErrorResponse(ApiConstants.SOME_EXCEPTION_OCCURRED + e.getMessage(), HttpStatus.BAD_REQUEST);

        }
    }

    @RequestMapping(value = "username-otp", method = RequestMethod.POST)
    private ResponseEntity<?> loginWithUsernameOtp(
            @RequestBody Map<String, Object> loginDetails, HttpSession session) {
        try {
            if (loginDetails == null) {
                return responseService.generateErrorResponse(ApiConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);

            }
            String username = (String) loginDetails.get("username");
            Integer role = (Integer) loginDetails.get("role");

            if (username == null || role == null) {
                return responseService.generateErrorResponse(ApiConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);

            }
            if (customerService == null) {
                return responseService.generateErrorResponse(ApiConstants.CUSTOMER_SERVICE_NOT_INITIALIZED, HttpStatus.INTERNAL_SERVER_ERROR);

            }
            if (roleService.findRoleName(role).equals(Constant.roleUser)) {
                Customer customer = customerService.readCustomerByUsername(username);
                if (customer == null) {
                    return responseService.generateErrorResponse(ApiConstants.NO_RECORDS_FOUND, HttpStatus.NOT_FOUND);
                }
                CustomCustomer customCustomer = em.find(CustomCustomer.class, customer.getId());
                if (customCustomer != null) {
                    String storedOtp = customCustomer.getOtp();

                    ResponseEntity<Map<String, Object>> otpResponse = twilioService.sendOtpToMobile(customCustomer.getMobileNumber(), Constant.COUNTRY_CODE,null);
                    Map<String, Object> responseBody = otpResponse.getBody();
                    if (responseBody.get("otp")!=null) {
                        return responseService.generateSuccessResponse((String) responseBody.get("message"), responseBody.get("otp"), HttpStatus.OK);

                    } else {
                        return responseService.generateErrorResponse((String) responseBody.get("message"), HttpStatus.BAD_REQUEST);
                    }
                } else {
                    return responseService.generateErrorResponse(ApiConstants.NO_RECORDS_FOUND, HttpStatus.NOT_FOUND);

                }
            } else if (roleService.findRoleName(role).equals(Constant.roleServiceProvider)) {
                return serviceProviderService.loginWithUsernameAndOTP(username, session);
            } else if(roleService.findRoleName(role).equals(Constant.ADMIN) || roleService.findRoleName(role).equals(Constant.SUPER_ADMIN) || roleService.findRoleName(role).equals(Constant.roleAdminServiceProvider))
            {
                return adminService.loginAdminWithUsernameAndOTP(username,session,roleService.findRoleName(role));
            }
            else {
                return responseService.generateErrorResponse(ApiConstants.INVALID_ROLE, HttpStatus.BAD_REQUEST);

            }
        } catch (IllegalArgumentException e) {
            return ResponseService.generateErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }  catch (Exception e) {
            exceptionHandling.handleException(e);
            return responseService.generateErrorResponse(ApiConstants.SOME_EXCEPTION_OCCURRED + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "customer-login-with-password", method = RequestMethod.POST)
    public ResponseEntity<?> loginWithCustomerPassword(@RequestBody Map<String, Object> loginDetails,@RequestParam(name = "tempAuth",required = false,defaultValue = "false")Boolean tempAuth,@RequestHeader(value = "Authorization",required = false)String authHeadReq, HttpSession session,
                                                       HttpServletRequest request) {
        try {
            String authHeader=Constant.BEARER_CONST;
            if (loginDetails == null) {
                return responseService.generateErrorResponse(ApiConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);

            }
            String mobileNumber = (String) loginDetails.get("mobileNumber");
            String password = (String) loginDetails.get("password");
            String countryCode = (String) loginDetails.get("countryCode");
            Integer role = (Integer) loginDetails.get("role");

            if (mobileNumber == null || password == null || role == null) {
                return responseService.generateErrorResponse("number/password number cannot be empty", HttpStatus.UNAUTHORIZED);

            }
            if (countryCode == null) {
                countryCode = Constant.COUNTRY_CODE;
            }
            if (customerService == null) {
                return responseService.generateErrorResponse(ApiConstants.CATALOG_SERVICE_NOT_INITIALIZED, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            if (roleService.findRoleName(role).equals(Constant.roleUser)) {
                CustomCustomer existingCustomer = customCustomerService.findCustomCustomerByPhone(mobileNumber, countryCode);
                System.out.println("archived:"+existingCustomer.getArchived());
                if(existingCustomer.getArchived().equals(true))
                    return ResponseService.generateErrorResponse("Your account is suspended please contact suppot",HttpStatus.UNAUTHORIZED);
                if (existingCustomer != null) {
                    Customer customer = customerService.readCustomerById(existingCustomer.getId());
                    if (passwordEncoder.matches(password, existingCustomer.getPassword())) {
                        String tokenKey = "authToken_" + mobileNumber;
                        String existingToken = existingCustomer.getToken();
                        String ipAddress = request.getRemoteAddr();
                        String userAgent = request.getHeader("User-Agent");

                        if(tempAuth.equals(true))
                        {
                            String token = jwtUtil.generateToken(customer.getId(), role, ipAddress, userAgent);
                            authHeader = authHeader + authHeadReq;
                            session.setAttribute(tokenKey, token);
                            OtpEndpoint.ApiResponse response = new OtpEndpoint.ApiResponse(token, sharedUtilityService.breakReferenceForCustomer(customer,authHeader,request), HttpStatus.OK.value(), HttpStatus.OK.name(),"User has been signed in");
                            return ResponseEntity.ok(response);
                        }
                        if (existingToken != null && jwtUtil.validateToken(existingToken, ipAddress, userAgent)) {
                            authHeader=authHeader+existingToken;
                            return ResponseEntity.ok(new OtpEndpoint.ApiResponse(existingToken, sharedUtilityService.breakReferenceForCustomer(customer,authHeader,request), HttpStatus.OK.value(), HttpStatus.OK.name(),"User has been logged in"));

                        } else {
                            String token = jwtUtil.generateToken(existingCustomer.getId(), role, ipAddress, userAgent);
                            authHeader=authHeader+token;
                                existingCustomer.setToken(token);
                                em.persist(existingCustomer);
                            session.setAttribute(tokenKey, token);
                            return ResponseEntity.ok(new OtpEndpoint.ApiResponse(token, sharedUtilityService.breakReferenceForCustomer(customer,authHeader,request), HttpStatus.OK.value(), HttpStatus.OK.name(),"User has been logged in"));
                        }

                    } else {
                        return responseService.generateErrorResponse("Incorrect Password" , HttpStatus.UNAUTHORIZED);

                    }
                } else {
                    return responseService.generateErrorResponse(ApiConstants.NO_RECORDS_FOUND , HttpStatus.NOT_FOUND);

                }
            } else if (roleService.findRoleName(role).equals(Constant.roleServiceProvider)) {
                return serviceProviderService.loginWithPassword(loginDetails,request,session);
            } else  return responseService.generateErrorResponse(ApiConstants.INVALID_ROLE , HttpStatus.BAD_REQUEST);


        } catch (IllegalArgumentException e) {
            return ResponseService.generateErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }  catch (Exception e) {
            exceptionHandling.handleException(e);
            return responseService.generateErrorResponse(ApiConstants.SOME_EXCEPTION_OCCURRED + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

}