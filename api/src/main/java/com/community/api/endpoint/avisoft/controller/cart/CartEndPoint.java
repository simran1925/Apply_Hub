package com.community.api.endpoint.avisoft.controller.cart;

import com.broadleafcommerce.rest.api.endpoint.BaseEndpoint;
import com.community.api.component.Constant;
import com.community.api.endpoint.avisoft.controller.Customer.CustomerEndpoint;
import com.community.api.endpoint.serviceProvider.ServiceProviderEntity;
import com.community.api.entity.CombinedOrderDTO;
import com.community.api.entity.CustomCustomer;
import com.community.api.entity.CustomOrderState;
import com.community.api.entity.CustomProduct;
import com.community.api.entity.CustomerReferrer;
import com.community.api.entity.ErrorResponse;
import com.community.api.entity.OrderCustomerDetailsDTO;
import com.community.api.entity.OrderDTO;
import com.community.api.entity.Post;
import com.community.api.services.*;
import com.community.api.services.exception.ExceptionHandlingImplement;
import com.fasterxml.jackson.annotation.JsonBackReference;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.persistence.Status;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.service.CatalogService;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.domain.OrderAttribute;
import org.broadleafcommerce.core.order.domain.OrderAttributeImpl;
import org.broadleafcommerce.core.order.domain.OrderItem;
import org.broadleafcommerce.core.order.domain.OrderItemAttribute;
import org.broadleafcommerce.core.order.service.OrderItemService;
import org.broadleafcommerce.core.order.service.OrderService;
import org.broadleafcommerce.core.order.service.call.OrderItemRequest;
import org.broadleafcommerce.core.order.service.type.OrderStatus;
import org.broadleafcommerce.profile.core.domain.Customer;
import org.broadleafcommerce.profile.core.domain.CustomerAddress;
import org.broadleafcommerce.profile.core.service.CustomerService;
import org.hibernate.validator.constraints.Currency;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.transaction.Transactional;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneId;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.community.api.component.Constant.ORDER_STATE_NEW;

import static com.community.api.services.ServiceProvider.ServiceProviderServiceImpl.getLongList;

@RestController
@RequestMapping(value = "/cart",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}
)
public class CartEndPoint extends BaseEndpoint {
    private CustomerService customerService;
    private OrderService orderService;
    private CatalogService catalogService;
    private ExceptionHandlingImplement exceptionHandling;
    private EntityManager entityManager;
    private OrderItemService orderItemService;
    private CartService cartService;
    private ResponseService responseService;
    private SharedUtilityService sharedUtilityService;
    private ReserveCategoryService reserveCategoryService;
    private ProductReserveCategoryFeePostRefService reserveCategoryFeePostRefService;
    private OrderDTOService orderDTOService;
    private GenderService genderService;

    // Setter-based injection
    @Autowired
    public void setCustomerService(CustomerService customerService) {
        this.customerService = customerService;
    }
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    public void setGenderService(GenderService genderService) {
        this.genderService = genderService;
    }
    @Autowired
    private CustomerEndpoint customerEndpoint;
    @Autowired
    public void setOrderDTOService(OrderDTOService orderDTOService) {
        this.orderDTOService = orderDTOService;
    }
    @Autowired
    private CustomerAddressFetcher addressFetcher;
    @Autowired
    public void setSharedUtilityService(SharedUtilityService sharedUtilityService) {
        this.sharedUtilityService = sharedUtilityService;
    }

    @Autowired
    private ProductReserveCategoryFeePostRefService productReserveCategoryFeePostRefService;

    @Autowired
    public void setResponseService(ResponseService responseService) {
        this.responseService = responseService;
    }
    @Autowired
    public void setReserveCategoryService(ReserveCategoryService reserveCategoryService){
        this.reserveCategoryService=reserveCategoryService;
    }
    @Autowired
    public void setReserveCategoryFeePostRefService(ProductReserveCategoryFeePostRefService reserveCategoryFeePostRefService)
    {
        this.reserveCategoryFeePostRefService=reserveCategoryFeePostRefService;
    }
    @Autowired
    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }
    @Autowired
    private OrderStatusByStateService orderStatusByStateService;
    @Autowired
    public void setCatalogService(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Autowired
    public void setExceptionHandling(ExceptionHandlingImplement exceptionHandling) {
        this.exceptionHandling = exceptionHandling;
    }

    @Autowired
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Autowired
    public void setOrderItemService(OrderItemService orderItemService) {
        this.orderItemService = orderItemService;
    }

    @Autowired
    public void setCartService(CartService cartService) {
        this.cartService = cartService;
    }

    @Transactional
    @RequestMapping(value = "empty/{customerId}", method = RequestMethod.DELETE)
    public ResponseEntity<?> emptyTheCart(@PathVariable Long customerId) { //@TODO-empty cart should remove each item one by one
        try {
            Long id = Long.valueOf(customerId);
            if (isAnyServiceNull()) {
                return ResponseService.generateErrorResponse("One or more Services not initialized", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if(id==null)
                return ResponseService.generateErrorResponse("Customer Id not specified",HttpStatus.BAD_REQUEST);
            Customer customer = customerService.readCustomerById(customerId);//finding the customer to get cart associated with it
            Order cart = null;
            if (customer == null) {
                return ResponseService.generateErrorResponse("Customer not found for this Id", HttpStatus.NOT_FOUND);
            } else {
                CustomCustomer customCustomer = entityManager.find(CustomCustomer.class, customer.getId());
                cart = this.orderService.findCartForCustomer(customer);
                if (cart == null) {
                    return ResponseService.generateErrorResponse("Cart Not Found", HttpStatus.NOT_FOUND);
                }
                if(cart.getOrderItems().isEmpty())
                    return ResponseService.generateErrorResponse("Cart already empty",HttpStatus.OK);
                if (cart.getStatus().equals(OrderStatus.IN_PROCESS)) {//ensuring its cart and not an order
                    List<OrderItem> items = cart.getOrderItems();
                    Iterator<OrderItem> iterator = items.iterator();

                    while (iterator.hasNext()) {
                        OrderItem item = iterator.next();
                        iterator.remove();
                        Product product = findProductFromItemAttribute(item);
                        CustomProduct customProduct = entityManager.find(CustomProduct.class, product.getId());
                        if (customCustomer != null && customProduct != null) {
                            if (!customCustomer.getCartRecoveryLog().contains(customProduct))
                                customCustomer.getCartRecoveryLog().add(customProduct);
                        }
                        entityManager.remove(item);
                    }
                    entityManager.merge(cart);
                    entityManager.merge(customCustomer);
                    return ResponseService.generateSuccessResponse("Cart is empty now", null, HttpStatus.OK);
                } else
                    return ResponseService.generateErrorResponse("Error removing all items from cart", HttpStatus.INTERNAL_SERVER_ERROR);
            }

        }catch (NumberFormatException e) {
            return ResponseService.generateErrorResponse("Invalid customerId: expected a Long", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {

            exceptionHandling.handleException(e);
            return ResponseService.generateErrorResponse("Error removing all items from cart : " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @RequestMapping(value = "add-to-cart/{customerId}/{productId}", method = RequestMethod.POST)
    public ResponseEntity<?> addToCart(@PathVariable long customerId, @PathVariable long productId,@RequestBody Map<String,Object>map) {
        try {
            Long id = Long.valueOf(customerId);
            if (isAnyServiceNull()) {
                return ResponseService.generateErrorResponse("One or more Services not initialized", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if(id==null)
                return ResponseService.generateErrorResponse("Customer Id not specified",HttpStatus.BAD_REQUEST);
            Customer customer = customerService.readCustomerById(customerId);
            CustomCustomer customCustomer=entityManager.find(CustomCustomer.class,customerId);
            if (customer == null||customCustomer==null) {
                return ResponseService.generateErrorResponse("Customer not found for this Id", HttpStatus.NOT_FOUND);
            }
            if (customer.getFirstName() == null ||
                    customer.getLastName() == null ||
                    customer.getEmailAddress() == null ||
                    customCustomer.getCategory() == null ||
                    customer.getUsername() == null ||
                    customer.getPassword() == null||
                    customCustomer.getGender()==null)
            {
                return ResponseService.generateErrorResponse(
                        "All fields must be completed: First Name, Last Name, Primary Email, Username, Password, Gender and Category are required before setting up the cart.",
                        HttpStatus.BAD_REQUEST
                );
            }

            Order cart = orderService.findCartForCustomer(customer);
            if (cart == null) {
                cart = orderService.createNewCartForCustomer(customer);
                cart.setOrderNumber("C-"+customerId);
              /*  cart.setName("CART-"+customerId);*/
                }
            Product product = catalogService.findProductById(productId);
            if (product == null) {
                return ResponseService.generateErrorResponse("Product not found", HttpStatus.NOT_FOUND);
            }
            CustomProduct customProduct=entityManager.find(CustomProduct.class,productId);
            List<Long>postPreference=getLongList(map,"postPreference");
            if(postPreference.isEmpty()&&customProduct.getPosts().size()>1)
                return ResponseService.generateErrorResponse("Post Preference cannot be empty",HttpStatus.BAD_REQUEST);
            Long reserveCategoryId=reserveCategoryService.getCategoryByName(customCustomer.getCategory()).getReserveCategoryId();
            if(reserveCategoryId==null)
                return ResponseService.generateErrorResponse("Invalid Category",HttpStatus.INTERNAL_SERVER_ERROR);
            double noReserveCategoryFee=0.0;
            /*if(reserveCategoryService.getReserveCategoryFee(productId,reserveCategoryId)==null) {
                //return ResponseService.generateErrorResponse("Cannot add product to cart :Fee not specified for your category", HttpStatus.UNPROCESSABLE_ENTITY);
                noReserveCategoryFee=reserveCategoryService.getReserveCategoryFee(productId,1L);//1 for general
            }*/

            /*if(productReserveCategoryFeePostRefService.getCustomProductReserveCategoryFeePostRefByProductIdAndReserveCategoryId(product.getId(),.getFee()==null)
            {

            }*/
            if ((((Status) customProduct).getArchived() == 'Y' || !customProduct.getDefaultSku().getActiveEndDate().after(new Date()))) {
                return ResponseService.generateErrorResponse("Cannot add an archived product",HttpStatus.BAD_REQUEST);
            }
            OrderItemRequest orderItemRequest = new OrderItemRequest();
            orderItemRequest.setProduct(product);
            orderItemRequest.setOrder(cart);
            orderItemRequest.setQuantity(1);
            orderItemRequest.setCategory(product.getCategory());
            orderItemRequest.setItemName(product.getName());
            Map<String, String> atrtributes = orderItemRequest.getItemAttributes();
            atrtributes.put("productId", product.getId().toString());
            List<Long>actualPostIds=new ArrayList<>();
            for(Post post:customProduct.getPosts())
            {
                actualPostIds.add(post.getPostId());
            }
            if(customProduct.getPosts().size()>=2)
            {
                for(Long pId:postPreference)
                {
                    if(!actualPostIds.contains(pId))
                        return ResponseService.generateErrorResponse("Invalid post id in preference list",HttpStatus.BAD_REQUEST);
                }
                if(postPreference.size()<1&&customProduct.getPosts().size()>=1)
                    return ResponseService.generateErrorResponse("Need to provide atleast one post for preference",HttpStatus.BAD_REQUEST);
                if(postPreference.size()>customProduct.getPosts().size())
                    return ResponseService.generateErrorResponse("Invalid post ids provided",HttpStatus.BAD_REQUEST);
                String postPreferenceString = postPreference.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                atrtributes.put("postPreference", postPreferenceString);
            } else if(customProduct.getPosts().size()==1){
                postPreference.removeAll(postPreference);
                postPreference.add(customProduct.getPosts().get(0).getPostId());
                String postPreferenceString = postPreference.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                atrtributes.put("postPreference",postPreferenceString);
            }
            else
            {
                atrtributes.put("postPreference","NO_AVAILABLE_POSTS");
            }
            orderItemRequest.setItemAttributes(atrtributes);
            OrderItem orderItem = orderItemService.createOrderItem(orderItemRequest);
            List<OrderItem> items = cart.getOrderItems();
            Map<String, Object> responseBody = new HashMap<>();
            boolean flag = false;
            for (OrderItem existingOrderItem : items) {
                if (Long.parseLong(existingOrderItem.getOrderItemAttributes().get("productId").getValue()) == productId) {
                    flag = true;
                    return ResponseService.generateErrorResponse(Constant.CANNOT_ADD_MORE_THAN_ONE_FORM, HttpStatus.UNPROCESSABLE_ENTITY);
                }
            }
            if (!flag)
                items.add(orderItem);
            cart.setOrderItems(items);
            responseBody.put("cart_id", cart.getId());
            responseBody.put("added_product_id", orderItem.getOrderItemAttributes().get("productId").getValue());
            return ResponseService.generateSuccessResponse("Cart updated", responseBody, HttpStatus.OK);

        }catch (NumberFormatException e) {
            return ResponseService.generateErrorResponse("Invalid customerId: expected a Long", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {

            return ResponseService.generateErrorResponse("Error adding item to cart : " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "number-of-items/{customerId}", method = RequestMethod.GET)
    public ResponseEntity<?> retrieveCartItemsCount(@PathVariable long customerId) {
        try {
            Long id = Long.valueOf(customerId);
            if(id==null)
                return ResponseService.generateErrorResponse("Customer Id not specified",HttpStatus.BAD_REQUEST);
            if (isAnyServiceNull()) {
                return ResponseService.generateErrorResponse("One or more Serivces not initialized", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            Customer customer = customerService.readCustomerById(customerId);
            Map<String, Object> responseBody = new HashMap<>();
            if (customer != null) {
                if (orderService.findCartForCustomer(customer) != null) {
                    responseBody.put("number_of_items", orderService.findCartForCustomer(customer).getOrderItems().size());
                    return ResponseService.generateSuccessResponse("Items in cart :", responseBody, HttpStatus.OK);
                } else
                    return ResponseService.generateErrorResponse("No items found", HttpStatus.NOT_FOUND);
            } else
                return ResponseService.generateErrorResponse("Customer not found", HttpStatus.NOT_FOUND);

        }catch (NumberFormatException e) {
            return ResponseService.generateErrorResponse("Invalid customerId: expected a Long", HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            exceptionHandling.handleException(e);
            return ResponseService.generateErrorResponse("Error retrieving cart", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonBackReference
    @RequestMapping(value = "preview-cart/{customerId}", method = RequestMethod.GET)
    public ResponseEntity<?> retrieveCartItems(@PathVariable long customerId, @RequestHeader(value = "inFunctionCall",required = false,defaultValue = "false")boolean inFunctionCall) {
        try {
            Customer customer = customerService.readCustomerById(customerId);
            Order cart = orderService.findCartForCustomer(customer);
            if (cart == null)
                return ResponseService.generateErrorResponse("Cart not activated", HttpStatus.OK);
            double productFee=0.0;
            Double individualFee=0.0;
            List<OrderItem>archievedItems=new ArrayList<>();
            Long id = Long.valueOf(customerId);
            if(id==null)
                return ResponseService.generateErrorResponse("Customer Id not specified",HttpStatus.BAD_REQUEST);
            Double subTotal = 0.0;
            Double platformfee=10.0;
            if (isAnyServiceNull()) {
                return ResponseService.generateErrorResponse("One or more Serivces not initialized", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if (customer == null) {
                return ResponseService.generateErrorResponse("customer does not exist", HttpStatus.NOT_FOUND);
            }
                CustomCustomer customCustomer=entityManager.find(CustomCustomer.class,customerId);
            Long reserveCategoryId=reserveCategoryService.getCategoryByName(customCustomer.getCategory()).getReserveCategoryId();;
            List<Product> listOfProducts = new ArrayList<>();
            List<OrderItem> orderItemList = cart.getOrderItems();
            if (orderItemList != null && (!orderItemList.isEmpty())) {
                Map<String, Object> response = new HashMap<>();
                List<Map<String, Object>> products = new ArrayList<>();
                for (OrderItem orderItem : orderItemList) {
                    Product product = findProductFromItemAttribute(orderItem);
                    CustomProduct customProduct=entityManager.find(CustomProduct.class,product.getId());
                    if (product != null) {
                        if ((((Status) customProduct).getArchived() == 'Y' || !customProduct.getDefaultSku().getActiveEndDate().after(new Date()))) {
                            archievedItems.add(orderItem);
                            continue;
                        }
                        Map<String, Object> productDetails = sharedUtilityService.createProductResponseMap(product, orderItem,customCustomer,genderService.getGenderByName(customCustomer.getGender()).getGenderId());
                        products.add(productDetails);
                        individualFee=reserveCategoryService.getReserveCategoryFee(product.getId(),reserveCategoryService.getCategoryByName(customCustomer.getCategory()).getReserveCategoryId(),genderService.getGenderByName(customCustomer.getGender()).getGenderId());//1 for general
                        if(individualFee==null)
                            individualFee=0.0;
                        if(individualFee==null)
                            //return ResponseService.generateErrorResponse("Cannot add product to cart :Fee not specified for your category", HttpStatus.UNPROCESSABLE_ENTITY);
                            individualFee=reserveCategoryService.getReserveCategoryFee(product.getId(),1L,genderService.getGenderByName(customCustomer.getGender()).getGenderId());//1 for general
                            if(individualFee==null)
                                individualFee=0.0;
                        }
                        productFee=productFee+individualFee;

                }
                subTotal=orderItemList.size()*platformfee+productFee;
                response.put("cart_id", cart.getId());
                response.put("products", products.toArray());
                response.put("sub_total", subTotal);
                response.put("price", productFee);
                response.put("total_platform_fee", orderItemList.size()*platformfee);
                for(OrderItem orderItem:archievedItems)
                {
                    cart.getOrderItems().remove(orderItem);
                }
                archievedItems.clear();
                if(!inFunctionCall)
                    return ResponseService.generateSuccessResponse("Cart items", response, HttpStatus.OK);
                else
                    return ResponseService.generateSuccessResponse("Cart items after modifying post preference", response, HttpStatus.OK);
            } else
                return ResponseService.generateErrorResponse("No items in cart", HttpStatus.OK);

        }catch (NumberFormatException e) {
            return ResponseService.generateErrorResponse("Invalid customerId: expected a Long", HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            exceptionHandling.handleException(e);
            return ResponseService.generateErrorResponse("Error retrieving cart Items", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @RequestMapping(value = "remove-item/{customerId}/{orderItemId}", method = RequestMethod.DELETE)
    public ResponseEntity<?> removeCartItems(
            @PathVariable long customerId,
            @PathVariable Long orderItemId) {
        try {
            Long id = Long.valueOf(customerId);
            if(id==null)
                return ResponseService.generateErrorResponse("Customer Id not specified",HttpStatus.BAD_REQUEST);
            if (isAnyServiceNull()) {
                return ResponseService.generateErrorResponse("One or more Services not initialized", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            Customer customer = customerService.readCustomerById(customerId);
            if (customer == null) {
                return ResponseService.generateErrorResponse("Invalid request: Customer does not exist", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            CustomCustomer customCustomer = entityManager.find(CustomCustomer.class, customer.getId());
            Order cart = orderService.findCartForCustomer(customer);
            if (cart == null || cart.getOrderItems() == null) {
                return ResponseService.generateErrorResponse("Cart Empty", HttpStatus.OK);
            }
            OrderItem orderItemToRemove=null;
            for(OrderItem orderItem:cart.getOrderItems())
            {
                if(orderItem.getId().equals(orderItemId)) {
                    orderItemToRemove = orderItem;
                    break;
                }
            }
            if(orderItemToRemove==null)
            {
                return ResponseService.generateErrorResponse("Item to remove not found",HttpStatus.NOT_FOUND);
            }
            long pid=Long.parseLong(orderItemToRemove.getOrderItemAttributes().get("productId").getValue());
            CustomProduct customProduct=entityManager.find(CustomProduct.class,pid);
            if (customProduct != null) {
                if (!customCustomer.getCartRecoveryLog().contains(customProduct))
                    customCustomer.getCartRecoveryLog().add(customProduct);
            }
            boolean itemRemoved = cartService.removeItemFromCart(cart, orderItemId);
            /*OrderItem orderItem=entityManager.find(OrderItem.class,orderItemId);*/
            if(itemRemoved)
            {
                return ResponseService.generateSuccessResponse("Item Removed", null, HttpStatus.OK);
            } else {
                return ResponseService.generateErrorResponse("Error removing item from cart: item not present in cart", HttpStatus.NOT_FOUND);
            }

        }catch (NumberFormatException e) {
            return ResponseService.generateErrorResponse("Invalid customerId: expected a Long", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            exceptionHandling.handleException(e);
            return ResponseService.generateErrorResponse("Error deleting", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @Transactional
    @RequestMapping(value = "place-order/{customerId}", method = RequestMethod.POST)
    public ResponseEntity<?> placeOrder(@PathVariable Long customerId,@RequestBody Map<String,Object>map,@RequestHeader(value = "Authorization")String authHeader) {
        try {
            CustomProduct customProduct=null;
           /* Long id = Long.valueOf(customerId);*/
            List<Long>orderItemIds=getLongList(map,"orderItemIds");
            if(customerId==null)
                return ResponseService.generateErrorResponse("Customer Id not specified",HttpStatus.BAD_REQUEST);
            Map<String, Object> responseMap = new HashMap<>();
            List<Order> individualOrders = new ArrayList<>();
            Customer customer = customerService.readCustomerById(customerId);
            CustomCustomer customCustomer=entityManager.find(CustomCustomer.class,customerId);
            if (customer == null|| customCustomer==null)
                return ResponseService.generateErrorResponse("Customer not found", HttpStatus.NOT_FOUND);
            Order cart = orderService.findCartForCustomer(customer);
            if (cart == null)
                return ResponseService.generateErrorResponse("Cart not found", HttpStatus.NOT_FOUND);
            if(cart.getOrderItems().isEmpty())
                return ResponseService.generateErrorResponse("Cart is empty",HttpStatus.NOT_FOUND);
            List<Long>cartItemIds=new ArrayList<>();
            List<String>errors=new ArrayList<>();
            int batchNumber=0;
            if(customCustomer.getNumberOfOrders()==null)
                batchNumber=1;
            else
            {
                batchNumber=customCustomer.getNumberOfOrders();
                ++batchNumber;
            }
            for(OrderItem orderItem : cart.getOrderItems())
            {
                cartItemIds.add(orderItem.getId());
            }
            if(orderItemIds.isEmpty())
                return ResponseService.generateErrorResponse("No items Selected",HttpStatus.BAD_REQUEST);

                for (Long orderItemId:orderItemIds)
            {
                if(!cartItemIds.contains(orderItemId))
                {
                    errors.add("Order Item Id : "+orderItemId+" does not belong to cart");
                }
            }
            List<Order>newOrders=new ArrayList<>();
            if(!errors.isEmpty())
                return ResponseService.generateErrorResponse("Error Placing order : "+errors.toString(),HttpStatus.BAD_REQUEST);
            for (OrderItem orderItem : cart.getOrderItems()) {
                if (orderItemIds.contains(orderItem.getId())) {
                    Product product = findProductFromItemAttribute(orderItem);
                    if(product!=null)
                        customProduct=entityManager.find(CustomProduct.class,product.getId());

                    Order individualOrder = orderService.createNamedOrderForCustomer(orderItem.getName(), customer);
                    individualOrder.setCustomer(customer);
                    individualOrder.setEmailAddress(customer.getEmailAddress());
                    individualOrder.setStatus(Constant.ORDER_STATUS_NEW);
                    OrderItemRequest orderItemRequest = new OrderItemRequest();
                    orderItemRequest.setProduct(product);
                    individualOrder.setCustomer(customer);
                    orderItemRequest.setOrder(individualOrder);
                    orderItemRequest.setQuantity(1);
                    orderItemRequest.setCategory(product.getCategory());
                    orderItemRequest.setItemName(product.getName());
                    Map<String, String> atrtributes = orderItemRequest.getItemAttributes();
                    atrtributes.put("productId", product.getId().toString());
                    //atrtributes.put("assigneeSPId",null);
                    orderItemRequest.setItemAttributes(atrtributes);
                    OrderItem orderItemForIndividualOrder = orderItemService.createOrderItem(orderItemRequest);
                    individualOrder.addOrderItem(orderItemForIndividualOrder);
                    Double platformFee=0.0;
                    if(customProduct.getPlatformFee()!=null)
                        platformFee= customProduct.getPlatformFee();
                    Money subTotal=new Money(platformFee);
                    individualOrder.setSubTotal(subTotal);
                    individualOrder.setOrderNumber("O-"+customer.getId()+"-B-"+batchNumber);
                    Double totalCost=reserveCategoryService.getReserveCategoryFee(product.getId(),reserveCategoryService.getCategoryByName(customCustomer.getCategory()).getReserveCategoryId(),genderService.getGenderByName(customCustomer.getGender()).getGenderId());
                    if(totalCost==null) {
                        //return ResponseService.generateErrorResponse("Cannot add product to cart :Fee not specified for your category", HttpStatus.UNPROCESSABLE_ENTITY);
                        totalCost = reserveCategoryService.getReserveCategoryFee(product.getId(), 1L, genderService.getGenderByName(customCustomer.getGender()).getGenderId());//1 for general
                    if(totalCost==null)
                        totalCost=0.0;

                    }totalCost=totalCost+platformFee;
                    Money total=new Money(totalCost);
                    individualOrder.setTotal(total);
                    LocalDateTime localDateTime = LocalDateTime.now();
                    Date date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
                    individualOrder.setSubmitDate(date);
                    individualOrder.setSubmitDate(date);
                    String retrievedPostPreferenceString =(String)(orderItem.getOrderItemAttributes().get("postPreference").getValue());
                    OrderAttributeImpl orderAttribute=new OrderAttributeImpl();
                    orderAttribute.setOrder(individualOrder);
                    orderAttribute.setName("postPreference");
                    orderAttribute.setValue(retrievedPostPreferenceString);
                    individualOrder.getOrderAttributes().put("sorted",orderAttribute);
                    entityManager.merge(individualOrder);
                    CustomOrderState orderState=new CustomOrderState();
                    orderState.setOrderStateId((ORDER_STATE_NEW.getOrderStateId()));
                    orderState.setOrderId(individualOrder.getId());
                    Integer orderStatusId=orderStatusByStateService.getOrderStatusByOrderStateId(ORDER_STATE_NEW.getOrderStateId()).get(0).getOrderStatusId();
                    orderState.setOrderStatusId(orderStatusId);
                    orderState.setOrderStatusId(orderStatusId);
                    entityManager.persist(orderState);
                    customerEndpoint.setReferrerForCustomer(customerId,customProduct.getUserId(),authHeader);
                    individualOrders.add(individualOrder);
                }
            }
                responseMap.put("Orders", individualOrders);
                List<OrderItem> items = cart.getOrderItems();
                Iterator<OrderItem> iterator = items.iterator();
                while (iterator.hasNext()) {
                    OrderItem item = iterator.next();
                    if(orderItemIds.contains(item.getId())) {
                        iterator.remove();
                        entityManager.remove(item);
                    }
                }
                customCustomer.setNumberOfOrders(batchNumber);
                ServiceProviderEntity refSp=entityManager.find(ServiceProviderEntity.class,customProduct.getUserId());
                if(refSp!=null) {
                    Query query=entityManager.createNativeQuery(Constant.CHECK_FOR_REPEATED_REF);
                    query.setParameter("customerId",customCustomer.getId());
                    query.setParameter("spId",customProduct.getUserId());
                    Integer result=((BigInteger) query.getSingleResult()).intValue();
                    if(result==0) {
                        CustomerReferrer customerReferrer = new CustomerReferrer();
                        customerReferrer.setCustomer(customCustomer);
                        customerReferrer.setServiceProvider(refSp);
                        customerReferrer.setCreatedAt(LocalDateTime.now());
                        customCustomer.getMyReferrer().add(customerReferrer);
                    }
                }
                entityManager.merge(customCustomer);
                entityManager.merge(cart);
                List<CombinedOrderDTO> orderDTOS=new ArrayList<>();
                for(Order order:individualOrders)
                {
                    CustomOrderState orderState=entityManager.find(CustomOrderState.class,order.getId());
                    OrderCustomerDetailsDTO customerDetailsDTO=new OrderCustomerDetailsDTO(customerId,customer.getFirstName()+" "+customCustomer.getLastName(),customer.getEmailAddress(),customCustomer.getMobileNumber(),addressFetcher.fetch(customer),customer.getUsername());
                    orderDTOS.add(orderDTOService.wrapOrder(order,orderState,null,customerDetailsDTO));
                }
                return ResponseService.generateSuccessResponse("Order Placed", orderDTOS, HttpStatus.OK);
        } catch (Exception e) {

            exceptionHandling.handleException(e);
            return ResponseService.generateErrorResponse("Error placing order "+e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @RequestMapping(value ="cart-recovery-log/{customerId}",method = RequestMethod.GET)
    public ResponseEntity<?>getCartRecoveryLog(@PathVariable Long customerId)
    {
        try{
            Long id = Long.valueOf(customerId);
            if(id==null)
                return ResponseService.generateErrorResponse("Customer Id not specified",HttpStatus.BAD_REQUEST);
                CustomCustomer customCustomer=entityManager.find(CustomCustomer.class,customerId);
                if(customCustomer==null)
                    return ResponseService.generateErrorResponse("Cannot find customer for this id",HttpStatus.NOT_FOUND);
                List<Map<String,Object>>productList=new ArrayList<>();
                for(Product product:customCustomer.getCartRecoveryLog())
                {
                    productList.add(sharedUtilityService.createProductResponseMap(product,null,customCustomer,genderService.getGenderByName(customCustomer.getGender()).getGenderId()));
                }
                return ResponseService.generateSuccessResponse("Cart Recovery Log : ",productList,HttpStatus.OK);

            }catch (NumberFormatException e) {
                return ResponseService.generateErrorResponse("Invalid customerId: expected a Long", HttpStatus.BAD_REQUEST);
            }catch (Exception e) {

            exceptionHandling.handleException(e);
            return ResponseService.generateErrorResponse("Error fetching recovery log", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @Transactional
    @RequestMapping(value = "{customerId}/update-preference/{productId}", method = RequestMethod.PUT)
    public ResponseEntity<?> updatePreference(@PathVariable Long customerId,@PathVariable Long productId,@RequestBody Map<String, Object> map,@RequestParam Long orderItemId) {
        try {
            List<Long> postPreference = getLongList(map, "postPreference");
            CustomProduct customProduct = entityManager.find(CustomProduct.class, productId);
            if (customProduct == null)
                return ResponseService.generateErrorResponse("Invalid product id provided", HttpStatus.NOT_FOUND);
            if(customProduct.getPosts().size()>=1) {
                List<Long> actualPostIds = new ArrayList<>();
                for (Post post : customProduct.getPosts()) {
                    actualPostIds.add(post.getPostId());
                }
                for (Long pId : postPreference) {
                    if (!actualPostIds.contains(pId))
                        return ResponseService.generateErrorResponse("Invalid post id in preference list", HttpStatus.BAD_REQUEST);
                }
                if (postPreference.size() < 1)
                    return ResponseService.generateErrorResponse("Need to provide atleast one post for preference", HttpStatus.BAD_REQUEST);
                if (postPreference.size() > customProduct.getPosts().size())
                    return ResponseService.generateErrorResponse("Invalid post ids provided", HttpStatus.BAD_REQUEST);
                String postPreferenceString = postPreference.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                String sql = "UPDATE blc_order_item_attribute " +
                        "SET value = ? " +
                        "WHERE order_item_id = ? " +
                        "AND name = 'postPreference' " +
                        "AND EXISTS (SELECT 1 FROM blc_order_item WHERE order_item_id = ?)";
                int rowsUpdated = jdbcTemplate.update(sql, postPreferenceString, orderItemId, orderItemId);
                if (rowsUpdated >= 0) {
                    return retrieveCartItems(customerId, true);
                }
            }else
                return ResponseService.generateErrorResponse("No Posts available for product",HttpStatus.NOT_FOUND);
        }catch (PersistenceException persistenceException)
        {
            exceptionHandling.handleException(persistenceException);
        } catch(Exception exception)
        {
            exceptionHandling.handleException(exception);
        }
        return ResponseService.generateErrorResponse("Error updating post preference", HttpStatus.BAD_REQUEST);

}
    private boolean isAnyServiceNull() {
        return customerService == null || orderService == null || catalogService == null;
    }

    public Product findProductFromItemAttribute(OrderItem orderItem) {
        Long productId = Long.parseLong(orderItem.getOrderItemAttributes().get("productId").getValue());
        Product product = catalogService.findProductById(productId);
        return product;
    }

    }

