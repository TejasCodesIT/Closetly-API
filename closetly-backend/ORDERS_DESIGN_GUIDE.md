# Order System Design Refactoring Guide

## 📋 Problem Statement

Your application was trying to store product and price information directly in the `orders` table:
```sql
orders: id, customer_id, product_id, price, ...
```

This design is fundamentally flawed because:
1. **Multiple products per order**: Carts contain multiple items, but the table assumes one product per order
2. **SQL Errors**: Getting "Field 'product_id' doesn't have a default value" because inserting without these fields violates NOT NULL constraints
3. **Price history lost**: When a product price changes, order history loses the original price
4. **No support for cart-based checkout**: Can't properly handle the placeOrder() flow

## ✅ Solution: Order-OrderItem Pattern

### New Schema Design

```
orders (parent table)
├── id (PK)
├── customer_id (FK → users)
├── total_amount (SUM of all order_items.total_price)
├── total_items (SUM of all order_items.quantity)
├── status (PLACED, PAID, SHIPPED, DELIVERED, CANCELLED)
├── created_at
└── deleted

order_items (child table - one row per product in the order)
├── id (PK)
├── order_id (FK → orders)
├── product_id (FK → products)
├── quantity
├── unit_price (historical price at purchase time)
├── total_price (unit_price * quantity * rental_days if rental)
├── type (BUY or RENT)
├── start_date (for RENT only)
├── end_date (for RENT only)
├── rental_days (for RENT only)
└── created_at
```

### Example: Order with Multiple Items

**Scenario**: Customer orders 3 products (1 full purchase + 2 rentals)

**Data Model**:
```
Order (id=1, customer_id=5)
├── OrderItem 1: productId=10, quantity=2, unitPrice=50, totalPrice=100 (type=BUY)
├── OrderItem 2: productId=15, quantity=1, unitPrice=10/day, totalPrice=50 (type=RENT, 5 days)
└── OrderItem 3: productId=20, quantity=1, unitPrice=8/day, totalPrice=40 (type=RENT, 5 days)

Totals: totalAmount=190, totalItems=2 (buy) + 1 (rent) + 1 (rent) = 4
```

## 🔧 Implementation Steps

### Step 1: Clean Database Schema

**File**: `src/main/resources/db-migration-orders-refactor.sql`

Run this migration to remove old columns:
```sql
ALTER TABLE orders DROP FOREIGN KEY orders_ibfk_2;
ALTER TABLE orders DROP COLUMN IF EXISTS product_id;
ALTER TABLE orders DROP COLUMN IF EXISTS price;
```

**Or**, manually in MySQL:
```bash
# Connect to your database
mysql -u root -p closetly

# Run:
ALTER TABLE orders DROP COLUMN IF EXISTS product_id;
ALTER TABLE orders DROP COLUMN IF EXISTS price;
```

### Step 2: Verify Entity Mappings

✅ **Order.java**:
- No `product_id` or `price` columns
- Has `@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)`
- References OrderItem collection

✅ **OrderItem.java**:
- Has `@ManyToOne` reference to Order
- Has `@ManyToOne` reference to Product
- Stores `unitPrice`, `totalPrice`, `quantity`
- Supports rental dates for RENT items

### Step 3: Understanding the Service Layer

The `OrderService` already handles cart-based orders correctly:

```java
// 1. Get customer and cart
User customer = userRepository.findByEmail(email);
List<CartItem> cartItems = cartItemRepository.findByUserIdWithProducts(customer.getId());

// 2. Validate and convert each cart item to OrderItem
for (CartItem cartItem : cartItems) {
    OrderItem orderItem = OrderItem.builder()
        .product(cartItem.getProduct())
        .quantity(cartItem.getQuantity())
        .unitPrice(cartItem.getType() == BUY ? product.getSalePrice() : product.getRentPricePerDay())
        .totalPrice(calculated_total)
        .type(cartItem.getType())
        .startDate(cartItem.getStartDate())  // for rentals
        .endDate(cartItem.getEndDate())      // for rentals
        .build();
}

// 3. Calculate totals
double totalAmount = orderItems.stream().mapToDouble(OrderItem::getTotalPrice).sum();
int totalItems = orderItems.stream().mapToInt(OrderItem::getQuantity).sum();

// 4. Create order with items
Order order = Order.builder()
    .customer(customer)
    .orderItems(orderItems)  // Set entire list
    .totalAmount(totalAmount)
    .totalItems(totalItems)
    .status(OrderStatus.PLACED)
    .build();

// 5. Save (cascade saves all order items)
orderRepository.save(order);
```

## 🚀 Usage Examples

### Example 1: Creating an Order from Cart

```java
@PostMapping("/placeOrder")
public ResponseEntity<OrderDTO> placeOrder(
    @RequestHeader("Authorization") String bearerToken
) {
    String email = extractEmailFromToken(bearerToken);
    
    // This automatically converts all cart items to order items
    OrderDTO order = orderService.placeOrder(email);
    
    return ResponseEntity.ok(order);
}
```

### Example 2: Fetching Order with All Items

```java
// Automatically fetches order with all nested items and products (no N+1 queries)
Optional<Order> order = orderRepository.findByIdWithItemsAndProducts(orderId);

// Returns:
// Order {
//   id: 1,
//   customer: User { ... },
//   orderItems: [
//     OrderItem { product: Product { ... }, quantity: 2, ... },
//     OrderItem { product: Product { ... }, quantity: 1, ... }
//   ],
//   totalAmount: 150,
//   totalItems: 3
// }
```

### Example 3: Getting Customer's Orders

```java
// Fetches all orders for a customer with eager loading
List<Order> orders = orderRepository.findByCustomerIdWithItemsAndProducts(customerId);

// Each order comes pre-loaded with all items and products
for (Order order : orders) {
    for (OrderItem item : order.getOrderItems()) {
        // No additional queries needed
        System.out.println(item.getProduct().getTitle());
    }
}
```

## 🗄️ Database Schema (Generated by Hibernate)

Hibernate will automatically create these tables based on your entities:

```sql
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    total_amount DOUBLE NOT NULL,
    total_items INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT 0,
    FOREIGN KEY (customer_id) REFERENCES users(id)
);

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DOUBLE NOT NULL,
    total_price DOUBLE NOT NULL,
    type VARCHAR(20) NOT NULL,
    start_date DATE,
    end_date DATE,
    rental_days INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);
```

## ⚠️ Troubleshooting

### Issue: "Field 'product_id' doesn't have a default value"

**Cause**: Old schema still has `product_id` column in orders table

**Solution**:
1. Run the migration SQL to drop the column
2. Or delete the database and restart the app (Hibernate will recreate it)
3. Or manually: `ALTER TABLE orders DROP COLUMN product_id;`

### Issue: Order items not saved

**Cause**: Cascade not configured on Order entity

**Solution**: Ensure Order.java has:
```java
@OneToMany(
    mappedBy = "order",
    cascade = CascadeType.ALL,
    orphanRemoval = true,
    fetch = FetchType.LAZY
)
private List<OrderItem> orderItems;
```

### Issue: N+1 Queries when fetching orders

**Cause**: Not using the eager-loading repository methods

**Solution**: Use these methods instead:
```java
// ✅ GOOD - Eager loads items and products
orderRepository.findByIdWithItemsAndProducts(orderId);

// ❌ AVOID - Will trigger N+1 queries
orderRepository.findById(orderId);

// ✅ GOOD - Eager loads for customer's orders
orderRepository.findByCustomerIdWithItemsAndProducts(customerId);

// ❌ AVOID - Will trigger N+1 queries
orderRepository.findAll();
```

## 📊 Data Flow: Cart → Order

```
1. USER ADDS ITEMS TO CART
   CartItem(productId=1, quantity=2, type=BUY)
   CartItem(productId=2, quantity=1, type=RENT, startDate=..., endDate=...)
   ↓
2. CALL orderService.placeOrder(email)
   ↓
3. FETCH CartItems from database
   ↓
4. VALIDATE each item
   ↓
5. CREATE OrderItems from CartItems
   OrderItem(product=Product1, quantity=2, unitPrice=50, totalPrice=100, type=BUY)
   OrderItem(product=Product2, quantity=1, unitPrice=10/day, totalPrice=50, type=RENT)
   ↓
6. CALCULATE totals
   totalAmount = 100 + 50 = 150
   totalItems = 2 + 1 = 3
   ↓
7. CREATE Order
   Order(customer=User1, orderItems=[...], totalAmount=150, totalItems=3)
   ↓
8. SAVE Order (cascade saves all OrderItems)
   ↓
9. CLEAR Cart
   ↓
10. RETURN OrderDTO to client
```

## 🔍 API Endpoints

### Create Order from Cart
```http
POST /api/orders/place
Authorization: Bearer <token>
```
Response:
```json
{
  "id": 1,
  "customerId": 5,
  "customerName": "John Doe",
  "customerEmail": "john@example.com",
  "orderItems": [
    {
      "id": 1,
      "productId": 10,
      "productTitle": "Blue Dress",
      "quantity": 2,
      "unitPrice": 50,
      "totalPrice": 100,
      "type": "BUY"
    },
    {
      "id": 2,
      "productId": 15,
      "productTitle": "Red Jacket",
      "quantity": 1,
      "unitPrice": 10,
      "totalPrice": 50,
      "type": "RENT",
      "startDate": "2024-03-25",
      "endDate": "2024-03-30",
      "rentalDays": 6
    }
  ],
  "totalAmount": 150,
  "totalItems": 3,
  "status": "PLACED",
  "createdAt": "2024-03-23T10:30:00"
}
```

### Get User's Orders
```http
GET /api/orders
Authorization: Bearer <token>
```

### Get Order Details
```http
GET /api/orders/{orderId}
Authorization: Bearer <token>
```

## ✨ Key Design Advantages

| Aspect | Old Design | New Design |
|--------|-----------|-----------|
| **Multiple items** | ❌ One product per order | ✅ Multiple OrderItems per Order |
| **Price history** | ❌ Current price | ✅ Stored at purchase time |
| **Cart checkout** | ❌ Not supported | ✅ Full cart support |
| **Rental data** | ❌ N/A | ✅ Dates and days stored |
| **Scalability** | ❌ Limited | ✅ Scales with order complexity |
| **Queries** | ⚠️ N+1 risk | ✅ Eager loading available |

## 🎯 Next Steps

1. ✅ Run the database migration SQL
2. ✅ Test the placeOrder endpoint with multiple items
3. ✅ Verify order_items are being created correctly
4. ✅ Monitor logs for any cascade errors
5. ✅ Update frontend to handle multi-item order responses
