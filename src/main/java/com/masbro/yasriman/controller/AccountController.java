package com.masbro.yasriman.controller;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException; 
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.masbro.yasriman.dao.AccountDAO;
import com.masbro.yasriman.dao.DashboardDAO;
import com.masbro.yasriman.emailapi.controller.UserController;
import com.masbro.yasriman.emailapi.entity.User;
import com.masbro.yasriman.model.accounts;
import com.masbro.yasriman.model.orders;

import jakarta.mail.MessagingException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession; 

import jakarta.servlet.http.Part;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;



@Controller
public class AccountController extends HttpServlet {

     private final AccountDAO AccountDAO;

    @Autowired
    private UserController userController;

    private final DashboardDAO DashboardDAO;

    @Autowired
    public AccountController(AccountDAO AccountDAO, DashboardDAO dashboardDAO) {
        this.AccountDAO = AccountDAO;
        this.DashboardDAO = dashboardDAO;
    }


    @GetMapping("/signout")
    private String signoutCustomerAccount(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// TODO Auto-generated method stub
    	HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "signin";
	}

    @GetMapping("/signup")
    private String signupform( HttpSession session){
    	System.out.println("showsignupform");
        session.setAttribute("signinerror", "null");
        return "signup";
    }
    
    @PostMapping("/signup")
    public String createAccount(@RequestParam("firstname") String accountfirstname,
                                @RequestParam("lastname") String accountlastname,
                                @RequestParam("username") String accountusername,
                                @RequestParam("email") String accountemail,
                                @RequestParam("password") String accountpassword,
                                @RequestParam("phone") String accountphonenum,
                                @RequestParam("role") String accountrole,
                                HttpSession session) throws SQLException, IOException, ServletException, MessagingException {
        
        String accountStatus;
        if ("Staff".equalsIgnoreCase(accountrole)) {
            accountStatus = "Pending";
        } else if ("Customer".equalsIgnoreCase(accountrole)) {
            accountStatus = "Approved";
        } else {
            accountStatus = "Pending";  
        }
    
        accounts newAccount = new accounts(accountfirstname, accountlastname, accountusername, accountemail, accountpassword, accountphonenum, accountrole, accountStatus);
    
        try {
            if (AccountDAO.isEmailExists(accountemail)) {
                session.setAttribute("emailerror", "Email already exists");
                return "redirect:/signup";
            } else {
                AccountDAO.insertAccount(newAccount);
                // session.setAttribute("accountusername", accountusername);
                // session.setAttribute("accountfirstname", accountfirstname);
                // session.setAttribute("accountlastname", accountlastname);
                // session.setAttribute("accountphonenum", accountphonenum);
                // session.setAttribute("accountemail", accountemail);
                // session.setAttribute("accountrole", accountrole);
                session.setAttribute("signinerror", "null");
                
                User user = new User(accountfirstname, accountlastname, accountusername, accountemail, accountpassword, accountphonenum, accountrole);
                userController.signUp(user);
                
                return "redirect:/signin";
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
    

    @GetMapping("/signin")
    private String signinform( HttpSession session){
    	System.out.println("showsigninform" + session.getAttribute("signinerror"));
        return "signin";
    }

    @PostMapping("/signin")
    public String signinAccount(@RequestParam("email") String email,
                                @RequestParam("password") String password,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) throws ServletException, IOException {
    
        try {
            accounts account = AccountDAO.authenticateAccount(email, password);
            if (account != null) {
                if (("Staff".equalsIgnoreCase(account.getRole()) && "Pending".equalsIgnoreCase(account.getStatus())) || 
                    ("Supervisor".equalsIgnoreCase(account.getRole()) && "Pending".equalsIgnoreCase(account.getStatus()))
                    ) {
                    session.setAttribute("signinerror", "Your account is pending approval.");
                    return "redirect:/signin"; 
                }
    
                session.setAttribute("loggedinaccountid", account.getId());
                session.setAttribute("accountrole", account.getRole());
                session.setAttribute("accountusername", account.getUsername());
                session.setAttribute("accountfirstname", account.getFirstname());
                session.setAttribute("accountlastname", account.getLastname());
                session.setAttribute("accountpassword", account.getPassword());
                session.setAttribute("accountemail", account.getEmail());
                session.setAttribute("accountphonenum", account.getPhonenum());
                session.setAttribute("accountstreet", account.getStreet());
                session.setAttribute("accountstate", account.getState());
                session.setAttribute("accountcity", account.getCity());
                session.setAttribute("accountpostalcode", account.getPostalcode());
                session.setAttribute("accountpicture", account.getPicture());
                session.setAttribute("signinerror", "null");
    
                return "redirect:/"; 
            } else {
                session.setAttribute("signinerror", "Invalid email or password");
                return "redirect:/signin"; 
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
    

    @GetMapping("/staffdashboard")
    public String staffdashboard(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession();
        Integer loggedinaccountid = (Integer) session.getAttribute("loggedinaccountid");
        String accountrole = (String) session.getAttribute("accountrole");

        if ((loggedinaccountid != null && "Staff".equals(accountrole)) || "Supervisor".equals(accountrole)) {
            List<orders> orderDataList = DashboardDAO.getOrderData();
            model.addAttribute("orderDataList", orderDataList); // Add orderDataList to the model
            System.out.println("showDashboard");
            return "dashboard"; // Return the name of your Thymeleaf template
        } else {
            session.setAttribute("errorMessage", "You are not allowed to go here!!!");
            return "redirect:/error"; // Redirect to an error page if not authorized
        }
    }

    @GetMapping("/listallaccounts")
    public ModelAndView listAllAccounts(HttpServletRequest request) throws IOException {
        HttpSession session = request.getSession();
        Integer loggedinaccountid = (Integer) session.getAttribute("loggedinaccountid");
        String accountrole = (String) session.getAttribute("accountrole");

        ModelAndView modelAndView = new ModelAndView();

        if ("Staff".equals(accountrole) || "Supervisor".equals(accountrole)) {
            List<accounts> accountsList = AccountDAO.selectAllUsers();

             List<accounts> pendingStaffAccounts = accountsList.stream()
            .filter(account -> "Pending".equals(account.getStatus()))
            .collect(Collectors.toList()
            );

            if (pendingStaffAccounts.isEmpty()) {
                modelAndView.addObject("noPendingStaffs", true);  // Flag for no pending accounts
            } else {
                modelAndView.addObject("accounts", pendingStaffAccounts);  // Send the filtered list
            }
            
            modelAndView.addObject("accounts", accountsList);
            System.out.println("Accounts: " + accountsList); 
            modelAndView.addObject("loggedinaccountid", loggedinaccountid);
            modelAndView.setViewName("accounts");
            System.out.println("listallaccounts");
        } else {
            session.setAttribute("errorMessage", "You are not allowed to go here!!!");
            modelAndView.setViewName("redirect:/error");
        }
        return modelAndView;
    }

    @GetMapping("/viewcustomeraccount")
    public ModelAndView viewCustomerAccount(
            HttpServletRequest request,
            @RequestParam("uid") int accountid,
            @RequestParam("from") String from) throws IOException {

        HttpSession session = request.getSession();
        Integer loggedinaccountid = (Integer) session.getAttribute("loggedinaccountid");
        ModelAndView modelAndView = new ModelAndView();
        session.setAttribute("errorMessage", "");


        if ("edit".equals(from)) {
            if (loggedinaccountid != null && loggedinaccountid.equals(accountid)) {
                String accountfirstname = request.getParameter("firstname");
                String accountlastname = request.getParameter("lastname");
                String accountusername = request.getParameter("username");
                String accountphonenum = request.getParameter("phone");
                String accountcity = request.getParameter("city");
                String accountstate = request.getParameter("state");
                String accountstreet = request.getParameter("street");
                String postalCodeStr = request.getParameter("postalcode");

                // Debugging
                System.out.println("Received Parameters:");
                System.out.println("Firstname: " + accountfirstname);
                System.out.println("Lastname: " + accountlastname);
                System.out.println("Username: " + accountusername);
                System.out.println("Phone: " + accountphonenum);
                System.out.println("City: " + accountcity);
                System.out.println("State: " + accountstate);
                System.out.println("Street: " + accountstreet);
                System.out.println("Postal Code: " + postalCodeStr);

                if (postalCodeStr == null || postalCodeStr.isEmpty()) {
                    modelAndView.addObject("postalcodeerror", "Postal code is required");
                    modelAndView.setViewName("editprofile");
                    return modelAndView;
                }

                int accountpostalcode;
                try {
                    accountpostalcode = Integer.parseInt(postalCodeStr);
                } catch (NumberFormatException e) {
                    modelAndView.setViewName("editprofile");
                    modelAndView.addObject("error", "Invalid postal code");
                    return modelAndView;
                }

                byte[] accountpicture = null;
                Part filePart = null;
                try {
                    filePart = (Part) request.getPart("profilepic");
                } catch (IOException e) { 
                    e.printStackTrace();
                } catch (ServletException e) { 
                    e.printStackTrace();
                }
                if (filePart != null && ((jakarta.servlet.http.Part) filePart).getSize() > 0) {
                    try (InputStream fileContent = ((ServletRequest) filePart).getInputStream()) {
                        accountpicture = fileContent.readAllBytes();
                    }
                }

                try {
                    accounts updatedAccount = AccountDAO.editCustomerAccount(accountid, accountfirstname, accountlastname, accountusername, accountphonenum, accountstreet, accountstate, accountcity, accountpostalcode, accountpicture);
                    if (updatedAccount != null) {
                        request.setAttribute("accounts", updatedAccount);
                        session.setAttribute("loggedinaccountid", loggedinaccountid);
                        session.setAttribute("accountusername", updatedAccount.getUsername());
                        session.setAttribute("accountfirstname", updatedAccount.getFirstname());
                        session.setAttribute("accountlastname", updatedAccount.getLastname());
                        session.setAttribute("accountphonenum", updatedAccount.getPhonenum());
                        session.setAttribute("accountstreet", updatedAccount.getStreet());
                        session.setAttribute("accountstate", updatedAccount.getState());
                        session.setAttribute("accountcity", updatedAccount.getCity());
                        session.setAttribute("accountpostalcode", updatedAccount.getPostalcode());
                        session.setAttribute("accountpicture", updatedAccount.getPicture());

                        modelAndView.setViewName("viewprofile");
                    } else {
                        modelAndView.setViewName("editprofile");
                        modelAndView.addObject("error", "Update failed");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    modelAndView.setViewName("editprofile");
                    modelAndView.addObject("error", "Internal server error");
                }
            } else {
                session.setAttribute("errorMessage", "null");
                modelAndView.setViewName("redirect:/error");
            }
        } else if ("dashboard".equals(from)) {
            String accountrole = (String) session.getAttribute("accountrole");
            if ("Staff".equals(accountrole) || "Supervisor".equals(accountrole)) {
                accounts accounts = AccountDAO.viewCustomerAccount(accountid);
                modelAndView.addObject("accounts", accounts);
                modelAndView.setViewName("viewaccounts");
                System.out.println("From dashboard");
            } else {
                session.setAttribute("errorMessage", "You are not allowed to go here!!!");
                modelAndView.setViewName("redirect:/error");
            }
        } else if ("dashboardupdate".equals(from)) {
            String accountrole = (String) session.getAttribute("accountrole");
            if ("Supervisor".equals(accountrole)) {
                accounts accounts = AccountDAO.viewCustomerAccount(accountid);
                List<String> allRoles = AccountDAO.getAllRoles();
                modelAndView.addObject("accounts", accounts);
                modelAndView.addObject("allRoles", allRoles);
                modelAndView.setViewName("updateaccounts");
                System.out.println("From dashboardupdate");
            } else {
                session.setAttribute("errorMessage", "You are not allowed to go here!!!");
                modelAndView.setViewName("redirect:/listallaccounts");
            }
        } else if ("index".equals(from)) {
            if (loggedinaccountid != null && loggedinaccountid.equals(accountid)) {
                accounts accounts = AccountDAO.viewCustomerAccount(accountid);
                request.setAttribute("accounts", accounts);
                modelAndView.setViewName("viewprofile");
            } else {
                session.setAttribute("errorMessage", "You are not allowed to go here!!!");
                modelAndView.setViewName("redirect:/error");
            }
        }

        return modelAndView;
    }

    @GetMapping("/editcustomeraccount")
    public ModelAndView editCustomerAccountForm(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam("uid") int accountid) throws IOException {
                

        HttpSession session = request.getSession();
        Integer loggedinaccountid = (Integer) session.getAttribute("loggedinaccountid");
        ModelAndView modelAndView = new ModelAndView(); 

        if (loggedinaccountid != null && loggedinaccountid.equals(accountid)) {
            accounts accounts = AccountDAO.viewCustomerAccount(accountid);
            modelAndView.addObject("accounts", accounts);
            modelAndView.setViewName("editprofile");
            System.out.println("editcustomeraccountform");
        } else {
            session.setAttribute("errorMessage", "You are not allowed to go here!!!");
            modelAndView.setViewName("redirect:/error");
        }

        return modelAndView;
    }
    
    @PostMapping("/editcustomeraccount")
    public ModelAndView editCustomerAccount(
        HttpServletRequest request,
        HttpServletResponse response,
        @RequestParam("profilepic") MultipartFile file,
        @RequestParam("username") String username,
        @RequestParam("firstname") String firstname,
        @RequestParam("lastname") String lastname,
        @RequestParam("street") String street,
        @RequestParam("state") String state,
        @RequestParam("city") String city,
        @RequestParam("postalcode") int postalcode,
        @RequestParam("phone") String phone,
        @RequestParam("uid") int uid,
        @RequestParam("profilePictureBlob") String profilePictureBlob,
        @RequestParam(value = "removeProfilePic", required = false) String removeProfilePic
        ) {

        System.out.println("Inside editcustomeraccount()");
        HttpSession session = request.getSession();
        Integer loggedinaccountid = (Integer) session.getAttribute("loggedinaccountid");
        ModelAndView modelAndView = new ModelAndView();

        if (loggedinaccountid != null && loggedinaccountid.equals(uid)) {
            try {
                byte[] picture = null;
                // Check if we should remove the profile picture
                if ("true".equals(removeProfilePic)) {
                    picture = null; // Set picture to null to remove it
                } else {
                    try {
                        if (!file.isEmpty()) {
                            picture = file.getBytes();
                        }
                        else if (profilePictureBlob != null && !profilePictureBlob.isEmpty()) {
                            picture = Base64.decodeBase64(profilePictureBlob);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } 
                // Update account details
                accounts updatedAccount = AccountDAO.editCustomerAccount(uid, firstname, lastname, username, phone, street, state, city, postalcode, picture);

                if (updatedAccount != null) {
                    // Update session attributes
                    session.setAttribute("accountusername", updatedAccount.getUsername());
                    session.setAttribute("accountfirstname", updatedAccount.getFirstname());
                    session.setAttribute("accountlastname", updatedAccount.getLastname());
                    session.setAttribute("accountphonenum", updatedAccount.getPhonenum());
                    session.setAttribute("accountstreet", updatedAccount.getStreet());
                    session.setAttribute("accountstate", updatedAccount.getState());
                    session.setAttribute("accountcity", updatedAccount.getCity());
                    session.setAttribute("accountpostalcode", updatedAccount.getPostalcode());
                    session.setAttribute("accountpicture", updatedAccount.getPicture());

                    // Redirect to the viewcustomeraccount method
                    modelAndView.setViewName("redirect:/viewcustomeraccount?uid=" + uid + "&from=index");
                } else {
                    System.out.println("error else1");
                    modelAndView.addObject("error", "Update failed");
                    modelAndView.setViewName("editprofile");
                }

            } catch (Exception e) {
                System.out.println("error else2");
                e.printStackTrace();
                modelAndView.addObject("error", "Internal server error");
                modelAndView.setViewName("editprofile");
            }
        } else {
            System.out.println("error else3");
            request.getSession().setAttribute("errorMessage", "You are not allowed to go here!!!");
            modelAndView.setViewName("redirect:/error");
        }

        return modelAndView;
    }

    @PostMapping("/updatecustomeraccount")
    public ModelAndView updateCustomerAccountForm(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam("uid") int accountid) throws IOException {

        HttpSession session = request.getSession();
        String role = (String) session.getAttribute("accountrole");
        ModelAndView modelAndView = new ModelAndView();

        if ("Supervisor".equals(role)) {
            try {
                int supervisorid = (int) session.getAttribute("loggedinaccountid"); // Get the supervisor ID
                String accountrole = request.getParameter("role");
                String accountusername = request.getParameter("username");
                String accountfirstname = request.getParameter("firstname");
                String accountlastname = request.getParameter("lastname");
                String accountemail = request.getParameter("email");
                String accountpassword = request.getParameter("password");
                String accountphonenum = request.getParameter("phonenum");
                String accountstreet = request.getParameter("street");
                String accountstate = request.getParameter("state");
                String accountcity = request.getParameter("city");
                int accountpostalcode = Integer.parseInt(request.getParameter("postalcode"));
                String accountstatus = request.getParameter("accountstatus");

                accounts updatedAccount = AccountDAO.updateCustomerAccount(accountid, accountrole, accountusername,
                        accountfirstname, accountlastname, accountemail, accountpassword, accountphonenum,
                        accountstreet, accountstate, accountcity, accountpostalcode, supervisorid, accountstatus);

                String supervisorName = AccountDAO.getSupervisorNameById(supervisorid);
                updatedAccount.setSupervisor(supervisorName);

                modelAndView.addObject("accounts", updatedAccount);
                modelAndView.setViewName("viewaccounts");
                System.out.println("updatecustomeraccount");

            } catch (SQLException e) {
                e.printStackTrace();
                modelAndView.setViewName("error");
            }
        } else {
            session.setAttribute("errorMessage", "You are not allowed to go here!!!");
            modelAndView.setViewName("redirect:/error");
        }

        return modelAndView;
    }



    @GetMapping("/account/{id}/picture")
    public ResponseEntity<byte[]> getAccountPicture(@PathVariable("id") int accountId) {
        try {
            accounts account = AccountDAO.viewCustomerAccount(accountId);
            byte[] image = account.getPicture();

            if (image != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.IMAGE_JPEG); // or other appropriate media type
                return new ResponseEntity<>(image, headers, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/deletecustomeraccount")
    private void deleteCustomerAccount(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        HttpSession session = request.getSession();
        String role = (String) session.getAttribute("accountrole");

        if ("Supervisor".equals(role)) {
            int accountid = Integer.parseInt(request.getParameter("uid"));
            AccountDAO.deleteAccount(accountid);
            response.sendRedirect("listallaccounts");
        } else {
            session.setAttribute("errorMessage", "Only supervisors can delete accounts.");
            response.sendRedirect("listallaccounts");
        }
    }

    // Approve the account
@GetMapping("/approve/{id}/{username}/{email}")
public String approveAccount(@PathVariable("id") int accountId, @PathVariable("username") String accountUsername, @PathVariable("email") String accountEmail, RedirectAttributes redirectAttributes) throws SQLException, MessagingException {
    try {
        AccountDAO.updateAccountStatus(accountId, "Approved"); // Update the status to 'Approved'
        User user = new User(accountUsername, accountEmail);
        userController.accountApproved(user);
        redirectAttributes.addFlashAttribute("message", "Account approved successfully.");
    } catch (SQLException e) {
        e.printStackTrace();
        redirectAttributes.addFlashAttribute("error", "Error occurred while approving the account.");
    }
    return "redirect:/listallaccounts"; // Redirect to the pending accounts page
}

// Reject the account (delete)
@GetMapping("/reject/{id}/{username}/{email}")
public String rejectAccount(@PathVariable("id") int accountId, @PathVariable("username") String accountUsername, @PathVariable("email") String accountEmail, RedirectAttributes redirectAttributes) throws SQLException, MessagingException {
    try {
        AccountDAO.deleteAccount(accountId); // Delete the account
        User user = new User(accountUsername, accountEmail);
        userController.accountRejected(user);
        redirectAttributes.addFlashAttribute("message", "Account rejected and deleted successfully.");
    } catch (SQLException e) {
        e.printStackTrace();
        redirectAttributes.addFlashAttribute("error", "Error occurred while rejecting the account.");
    }
    return "redirect:/listallaccounts"; // Redirect to the pending accounts page
}





    
}
